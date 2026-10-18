/** Strings from the Android app (converted at build time into strings.json), with Android-style format args. */
import { Store } from './storage';

type Table = Record<string, string | Record<string, string> | string[]>;
let tables: Record<string, Table> = {};
let current: Table = {};
let currentTag = 'en';

/** Same list and order as the Android LocaleHelper ('' = system). */
export const SUPPORTED_TAGS = ['', 'en', 'es', 'ca', 'eu', 'gl', 'pt', 'fr', 'it', 'de', 'tr', 'pl', 'ar', 'id', 'vi', 'ro', 'el', 'hr', 'sr-Latn', 'th', 'zh-TW', 'ru', 'nl', 'sq', 'hi'];
export const RTL_TAGS = ['ar'];

function systemTag(): string {
  const nav = (navigator.language || 'en').replace('_', '-');
  const lower = nav.toLowerCase();
  if (lower.startsWith('zh')) return 'zh-TW';
  if (lower.startsWith('sr')) return 'sr-Latn';
  const base = lower.split('-')[0];
  return SUPPORTED_TAGS.includes(base) ? base : 'en';
}

export function initStrings(data: Record<string, Table>): void {
  tables = data;
  applyLanguage(Store.language());
}

export function applyLanguage(tag: string): void {
  currentTag = tag && tables[tag] ? tag : systemTag();
  if (!tables[currentTag]) currentTag = 'en';
  current = tables[currentTag] || {};
  document.documentElement.lang = currentTag;
  document.documentElement.dir = RTL_TAGS.includes(currentTag) ? 'rtl' : 'ltr';
}

export function languageTag(): string { return currentTag; }

/** Looks up `key` with %1$s / %2$d / %s / %d substitution. Falls back to English, then to the key. */
export function t(key: string, ...args: Array<string | number>): string {
  let s = current[key];
  if (s === undefined) s = (tables.en || {})[key];
  if (typeof s !== 'string') return key;
  return format(s, args);
}

/** Plural string: {"one": …, "other": …}. */
export function plural(key: string, n: number): string {
  const p = (current[key] || (tables.en || {})[key]) as Record<string, string> | undefined;
  if (!p || typeof p !== 'object' || Array.isArray(p)) return String(n);
  const form = n === 1 && p.one ? p.one : p.other || p.one || '';
  return format(form, [n]);
}

function format(s: string, args: Array<string | number>): string {
  let auto = 0;
  return s.replace(/%(\d+)\$([sd])|%([sd])/g, (_, idx, _t1, _t2) => {
    const i = idx ? parseInt(idx, 10) - 1 : auto++;
    const v = args[i];
    return v === undefined || v === null ? '' : String(v);
  });
}
