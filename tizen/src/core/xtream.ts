/** Xtream Codes panel client (player_api.php) and stream URL builder. Port of XtreamClient/XtreamApi/StreamUrlBuilder. */
import { EpgProgram, LiveChannel, XtreamProfile } from './model';
import { ChannelNameParser } from './names';

export class XtreamAuthError extends Error { constructor() { super('Authentication rejected by the server'); } }
export class HttpError extends Error { constructor(public status: number) { super('HTTP ' + status); } }

/** Ensures a scheme, drops a pasted query / trailing .php script, single trailing slash. */
export function normalizeBaseUrl(raw: string): string {
  let url = raw.trim();
  if (url === '') return url;
  if (!/^https?:\/\//i.test(url)) url = 'http://' + url;
  url = url.split('?')[0].split('#')[0].replace(/\/+$/, '');
  const last = url.slice(url.lastIndexOf('/') + 1);
  if (/\.php$/i.test(last)) url = url.slice(0, url.lastIndexOf('/'));
  return url.replace(/\/+$/, '') + '/';
}

export function liveUrl(profile: XtreamProfile, streamId: number, extension = 'ts'): string {
  const base = normalizeBaseUrl(profile.serverUrl).replace(/\/+$/, '');
  return `${base}/live/${profile.username}/${profile.password}/${streamId}.${extension}`;
}

/** Size and duration of the last completed download, so a big playlist doubles as a bandwidth probe. */
export const lastDownload = { bytes: 0, ms: 0 };

export async function fetchText(url: string, timeoutMs = 60000): Promise<string> {
  const ctrl = typeof AbortController !== 'undefined' ? new AbortController() : null;
  const timer = ctrl ? setTimeout(() => ctrl.abort(), timeoutMs) : null;
  const started = Date.now();
  try {
    const res = await fetch(url.trim(), { signal: ctrl ? ctrl.signal : undefined });
    if (!res.ok) throw new HttpError(res.status);
    const text = await res.text();
    lastDownload.bytes = text.length; lastDownload.ms = Date.now() - started;
    return text;
  } finally { if (timer) clearTimeout(timer); }
}

async function fetchJson(url: string): Promise<any> {
  const text = await fetchText(url);
  try { return JSON.parse(text); } catch (e) { throw new Error('bad_response'); }
}

function api(profile: XtreamProfile, params: Record<string, string>): string {
  const q = Object.keys(params).map((k) => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`).join('&');
  return `${normalizeBaseUrl(profile.serverUrl)}player_api.php?username=${encodeURIComponent(profile.username)}&password=${encodeURIComponent(profile.password)}${q ? '&' + q : ''}`;
}

export async function login(profile: XtreamProfile): Promise<void> {
  const res = await fetchJson(api(profile, {}));
  const info = res && res.user_info;
  if (!info || Number(info.auth) !== 1) throw new XtreamAuthError();
}

export async function loadXtreamChannels(profile: XtreamProfile): Promise<LiveChannel[]> {
  const categories: any[] = await fetchJson(api(profile, { action: 'get_live_categories' }));
  const categoryName = new Map<string, string>();
  for (const c of categories || []) categoryName.set(String(c.category_id), String(c.category_name || '').trim());
  const streams: any[] = await fetchJson(api(profile, { action: 'get_live_streams' }));
  return (streams || []).map((s) => {
    const category = categoryName.get(String(s.category_id)) || null;
    const streamId = Number(s.stream_id) || 0;
    return {
      streamId,
      name: String(s.name || '').trim(),
      iconUrl: s.stream_icon ? String(s.stream_icon) : null,
      categoryName: category,
      streamUrl: liveUrl(profile, streamId),
      epgId: s.epg_channel_id ? String(s.epg_channel_id) : null,
      isRadio: String(s.stream_type || '').toLowerCase() === 'radio_streams' || ChannelNameParser.isRadioCategory(category),
    };
  });
}

function decodeBase64(v: string | null | undefined): string | null {
  if (!v) return null;
  try { return decodeURIComponent(escape(atob(v))); } catch (e) { try { return atob(v); } catch (e2) { return null; } }
}

/** Reads listings from `{"epg_listings":[...]}` or a bare array; titles are base64. */
function parseEpg(element: any): EpgProgram[] {
  const listings: any[] = Array.isArray(element) ? element : (element && Array.isArray(element.epg_listings) ? element.epg_listings : []);
  const out: EpgProgram[] = [];
  for (const l of listings) {
    const title = (decodeBase64(l.title) || '').trim();
    if (!title) continue;
    out.push({ title, start: (parseInt(l.start_timestamp, 10) || 0) * 1000, end: (parseInt(l.stop_timestamp, 10) || 0) * 1000, nowFlag: Number(l.now_playing) === 1 });
  }
  return out.sort((a, b) => a.start - b.start);
}

/** "Now / next" for a stream via get_short_epg, then get_simple_data_table; server quirks → empty. */
export async function shortEpg(profile: XtreamProfile, streamId: number): Promise<EpgProgram[]> {
  try {
    const a = parseEpg(await fetchJson(api(profile, { action: 'get_short_epg', stream_id: String(streamId), limit: '8' })));
    if (a.length) return a;
    return parseEpg(await fetchJson(api(profile, { action: 'get_simple_data_table', stream_id: String(streamId) })));
  } catch (e) { return []; }
}
