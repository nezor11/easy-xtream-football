/**
 * XMLTV guide files (declared by an M3U's x-tvg-url) → "now / next" index for a known set of channel ids.
 * Files may be gzipped. Scanned with regexes over the text (no DOM: guides run to hundreds of MB).
 */
import * as pako from 'pako';
import { EpgProgram } from './model';

const MAX_SOURCES = 8;

export function normId(id: string): string { return id.trim().toLowerCase(); }

/** Guides matching our channels' countries first (tvg-id suffix "Rete8Sport.it" → it), then "all sources", then the rest. */
export function prioritize(urls: string[], neededIds: Set<string>): string[] {
  const countries = new Set<string>();
  neededIds.forEach((id) => { const s = id.slice(id.lastIndexOf('.') + 1).toLowerCase(); if (id.includes('.') && /^[a-z]{2}$/.test(s)) countries.add(s); });
  if (countries.size === 0) return urls;
  const countryFeed = new RegExp('[_/.](' + Array.from(countries).join('|') + ')\\d*[._]');
  const rank = (u: string) => { const l = u.toLowerCase(); return countryFeed.test(l) ? 0 : l.includes('all_sources') ? 1 : 2; };
  return urls.map((u, i) => ({ u, i, r: rank(u) })).sort((a, b) => a.r - b.r || a.i - b.i).map((x) => x.u);
}

/** "20260525200000 +0200" or bare UTC → epoch millis, 0 if invalid. */
export function parseTime(value: string | null | undefined): number {
  if (!value) return 0;
  const v = value.trim();
  const m = /^(\d{14})\s*([+-]\d{4})?/.exec(v);
  if (!m) return 0;
  const d = m[1];
  const utc = Date.UTC(+d.slice(0, 4), +d.slice(4, 6) - 1, +d.slice(6, 8), +d.slice(8, 10), +d.slice(10, 12), +d.slice(12, 14));
  if (!m[2]) return utc;
  const sign = m[2][0] === '-' ? -1 : 1;
  const offsetMin = sign * (+m[2].slice(1, 3) * 60 + +m[2].slice(3, 5));
  return utc - offsetMin * 60000;
}

function decodeEntities(s: string): string {
  return s.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&#(\d+);/g, (_, n) => String.fromCharCode(+n)).replace(/&amp;/g, '&');
}

const programmeRe = /<programme\b([^>]*)>([\s\S]*?)<\/programme>/g;
const attrRe = (name: string) => new RegExp(name + '="([^"]*)"');
const titleRe = /<title\b[^>]*>([\s\S]*?)<\/title>/;

function parse(xml: string, needed: Set<string>, now: number, out: Map<string, EpgProgram[]>): void {
  programmeRe.lastIndex = 0;
  let m: RegExpExecArray | null;
  while ((m = programmeRe.exec(xml)) !== null) {
    const attrs = m[1];
    const ch = attrRe('channel').exec(attrs);
    if (!ch) continue;
    const channel = normId(decodeEntities(ch[1]));
    if (!needed.has(channel)) continue;
    const start = parseTime((attrRe('start').exec(attrs) || [])[1]);
    const stop = parseTime((attrRe('stop').exec(attrs) || [])[1]);
    const t = titleRe.exec(m[2]);
    const title = t ? decodeEntities(t[1].replace(/<[^>]+>/g, '')).trim() : '';
    if (title && (stop === 0 || stop >= now)) {
      let list = out.get(channel);
      if (!list) { list = []; out.set(channel, list); }
      list.push({ title, start, end: stop, nowFlag: false });
    }
  }
}

async function fetchXml(url: string): Promise<string> {
  const res = await fetch(url);
  if (!res.ok) throw new Error('HTTP ' + res.status);
  const buf = new Uint8Array(await res.arrayBuffer());
  const isGzip = buf.length > 2 && buf[0] === 0x1f && buf[1] === 0x8b;
  return isGzip ? pako.inflate(buf, { to: 'string' }) : new TextDecoder('utf-8').decode(buf);
}

/** Programmes per normalized channel id (sorted by start) for `neededIds`, from at most MAX_SOURCES guides. */
export async function indexGuides(urls: string[], neededIds: Set<string>): Promise<Map<string, EpgProgram[]>> {
  const out = new Map<string, EpgProgram[]>();
  if (urls.length === 0 || neededIds.size === 0) return out;
  const now = Date.now();
  const needed = new Set<string>(); neededIds.forEach((id) => needed.add(normId(id)));
  for (const url of prioritize(urls, neededIds).slice(0, MAX_SOURCES)) {
    try { parse(await fetchXml(url), needed, now, out); } catch (e) { console.warn('XMLTV source failed', e); }
  }
  out.forEach((list) => list.sort((a, b) => a.start - b.start));
  return out;
}
