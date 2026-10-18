/**
 * Logos for channels that ship without one, from the map the repository publishes weekly out of the
 * iptv-org database (docs/logos/sports.json, built by scripts/build-logo-map.py). Port of Android's
 * LogoRepository + LogoMatching: which name keys to try, most specific first. Brand-agnostic.
 */
import { fetchText } from './xtream';

export const LOGO_MAP_URL = 'https://raw.githubusercontent.com/nezor11/easy-xtream-football/main/docs/logos/sports.json';
const CACHE_KEY = 'exf.logos';
const CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000;

interface LogoDoc { v: number; date: string; imgur: string; logos: Record<string, string>; }

let doc: LogoDoc | null = null;
let loading: Promise<LogoDoc | null> | null = null;

/** The map is keyed by name with everything but letters and digits removed. */
export function normalize(name: string): string { return name.toLowerCase().replace(/[^\p{L}\p{N}]/gu, ''); }

const LETTER = /\p{L}/u;
function variants(key: string): string[] {
  return [key, key.replace(/sports/g, 'sport'), key.replace(/sport(?!s)/g, 'sports'), key.replace(/(tv|channel)$/, '')];
}

/**
 * Keys to try in order: the exact name and its lexical variants, then progressively shorter runs of its
 * words (dropping a feed/region suffix first, then a country/provider prefix). A run keeps at least two
 * words with letters; one that lost its start must begin with a real word (3+ letters) and be 8+ chars,
 * so a generic tail never claims some unrelated channel's logo.
 */
export function candidates(displayName: string): string[] {
  const words = displayName.trim().split(/\s+/).filter((w) => w.length > 0);
  const keys: string[] = [];
  const add = (list: string[]) => { for (const k of list) if (k && keys.indexOf(k) < 0) keys.push(k); };
  add(variants(normalize(displayName)));
  const lettered = (run: string[]) => run.filter((w) => LETTER.test(w)).length;
  for (let end = words.length - 1; end >= 2; end--) {
    const run = words.slice(0, end);
    if (lettered(run) >= 2) add(variants(normalize(run.join(' '))));
  }
  for (let start = 1; start < words.length - 1; start++) {
    const run = words.slice(start);
    const key = normalize(run.join(' '));
    const firstLetters = (run[0].match(/\p{L}/gu) || []).length;
    if (lettered(run) >= 2 && firstLetters >= 3 && key.length >= 8) add(variants(key));
  }
  return keys;
}

function readCache(): LogoDoc | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    const c = JSON.parse(raw) as { at: number; doc: LogoDoc };
    return c && c.doc && c.doc.logos && Date.now() - c.at < CACHE_TTL_MS ? c.doc : null;
  } catch (e) { return null; }
}

/** Loads the map (cache for a week, else network). Resolves to null when unavailable; never throws. */
export function ensureLogoMap(): Promise<LogoDoc | null> {
  if (doc) return Promise.resolve(doc);
  if (loading) return loading;
  loading = (async () => {
    doc = readCache();
    if (doc) return doc;
    try {
      const text = await fetchText(LOGO_MAP_URL, 30000);
      const d = JSON.parse(text) as LogoDoc;
      if (!d || !d.logos) return null;
      doc = d;
      try { localStorage.setItem(CACHE_KEY, JSON.stringify({ at: Date.now(), doc: d })); } catch (e) { /* quota: fine, memory only */ }
      return doc;
    } catch (e) {
      console.warn('logo map unavailable', e);
      return null;
    } finally { loading = null; }
  })();
  return loading;
}

/** Logo URL for a channel name, or null. Needs ensureLogoMap() to have resolved. */
export function logoFor(name: string): string | null {
  if (!doc) return null;
  for (const key of candidates(name)) {
    const v = doc.logos[key];
    if (v) return v.startsWith('i:') ? doc.imgur + v.slice(2) : v;
  }
  return null;
}
