/** Single entry point for channel data: the active source (Xtream / M3U / direct) → sports-grouped channels, guide. Port of ContentRepository. */
import { buildGroups, fillFromFolderSiblings } from './grouping';
import { ChannelGroup, EpgProgram, LiveChannel, Profile, XtreamProfile, bestVariant } from './model';
import { M3uParser, javaHash } from './m3u';
import { Store } from './storage';
import { fetchText, lastDownload, loadXtreamChannels, login, shortEpg } from './xtream';
import { indexGuides, normId } from './xmltv';
import { ensureLogoMap, logoFor } from './logos';

export { XtreamAuthError, HttpError } from './xtream';

type Binding = { kind: 'xtream'; profile: XtreamProfile } | { kind: 'm3u'; url: string } | { kind: 'direct'; url: string };

const CACHE_TTL_MS = 12 * 60 * 60 * 1000;
const EPG_TTL_MS = 2 * 60 * 60 * 1000;
const NOW_TTL_MS = 10 * 60 * 1000;
const NOW_EMPTY_TTL_MS = 60 * 1000;

export class M3uFormatError extends Error { constructor(public head: string) { super('Not an M3U playlist'); } }

const BANDWIDTH_PROBE_MIN_BYTES = 1_000_000;
/** A playlist download of 1 MB+ from the provider is a fair bandwidth probe (AVPlay exposes no byte counter). */
function recordBandwidth(): void {
  if (lastDownload.bytes >= BANDWIDTH_PROBE_MIN_BYTES && lastDownload.ms > 0) {
    const bps = lastDownload.bytes * 8 * 1000 / lastDownload.ms;
    Store.setBandwidthBps(bps);
    console.log('bandwidth probe', Math.round(lastDownload.bytes / 1024) + ' KB in ' + lastDownload.ms + ' ms = ' + (bps / 1e6).toFixed(1) + ' Mbps');
  }
}

let binding: Binding | null = null;
let activeProfileName: string | null = null;

// XMLTV state for the active M3U source
let m3uSourceKey = '', m3uEpgUrls: string[] = [], m3uNeededIds = new Set<string>();
let m3uEpgIndex: Map<string, EpgProgram[]> | null = null, m3uEpgBuiltKey = '', m3uEpgBuiltAt = 0, m3uEpgBuilding: Promise<Map<string, EpgProgram[]>> | null = null;
const nowCache = new Map<string, { expiresAt: number; program: EpgProgram | null }>();

function bindXtream(profile: XtreamProfile): void { binding = { kind: 'xtream', profile }; nowCache.clear(); }
function bindM3u(url: string): void { binding = { kind: 'm3u', url: url.trim() }; nowCache.clear(); }
function bindDirect(url: string): void { binding = { kind: 'direct', url: url.trim() }; nowCache.clear(); }

export const Repository = {
  activeProfileName(): string | null { return activeProfileName; },

  bindProfile(p: Profile): void {
    activeProfileName = p.name.trim() || null;
    if (p.type === 'DIRECT') bindDirect(p.m3uUrl);
    else if (p.type === 'M3U') bindM3u(p.m3uUrl);
    else bindXtream({ name: p.name, serverUrl: p.serverUrl, username: p.username, password: p.password });
  },

  /** Validates credentials against the panel; binds on success. Throws XtreamAuthError / HttpError / Error. */
  async validateXtream(profile: XtreamProfile): Promise<void> {
    await login(profile);
    bindXtream(profile);
  },

  /** Checks the URL returns an M3U (first line) and binds on success. */
  async validateM3u(url: string): Promise<void> {
    const text = await fetchText(url);
    const head = text.slice(0, 200).replace(/^\s+/, "");
    if (!/^#EXTM3U/i.test(head)) throw new M3uFormatError(head.slice(0, 60));
    bindM3u(url);
  },

  async loadLiveGroups(forceRefresh = false): Promise<ChannelGroup[]> {
    if (!binding) throw new Error('No active content source');
    if (binding.kind === 'direct') return loadDirect(binding.url);
    if (binding.kind === 'm3u') return loadM3uGroups(binding.url, forceRefresh);
    return loadXtreamGroups(binding.profile, forceRefresh);
  },

  clearCache(): void { Store.clearCache(); m3uEpgIndex = null; m3uEpgBuiltKey = ''; m3uEpgBuiltAt = 0; nowCache.clear(); },

  /** Logos for bare channels, borrowed from folder siblings (the iptv-org logo database is not used here). */
  /** Fills in logos for channels that carry none, from the published logo map; then bare ones borrow from folder siblings. */
  async enrichLogos(groups: ChannelGroup[]): Promise<ChannelGroup[]> {
    if (groups.every((g) => !!g.iconUrl)) return groups;
    const map = await ensureLogoMap();
    const byName = map ? groups.map((g) => g.iconUrl ? g : (logoFor(g.displayName) ? { ...g, iconUrl: logoFor(g.displayName) } : g)) : groups;
    return fillFromFolderSiblings(byName);
  },

  /** "Now / next" whatever the source; empty when no guide. */
  async epgFor(group: ChannelGroup): Promise<EpgProgram[]> {
    if (!binding) return [];
    if (binding.kind === 'xtream') return shortEpg(binding.profile, bestVariant(group).channel.streamId);
    if (binding.kind === 'm3u') return m3uEpg(group);
    return [];
  },

  /** The programme on air right now, or null. Cached briefly. */
  async nowProgram(group: ChannelGroup): Promise<EpgProgram | null> {
    const now = Date.now();
    const c = nowCache.get(group.key);
    if (c && now < c.expiresAt) return c.program;
    const epg = await this.epgFor(group);
    const program = epg.find((p) => p.nowFlag) || epg.find((p) => p.start >= 1 && p.start <= now && now < p.end) || null;
    nowCache.set(group.key, { expiresAt: now + (epg.length ? NOW_TTL_MS : NOW_EMPTY_TTL_MS), program });
    return program;
  },
};

async function loadM3uGroups(url: string, forceRefresh: boolean): Promise<ChannelGroup[]> {
  const key = 'm3u_' + javaHash(url);
  if (!forceRefresh) {
    const cached = Store.cachedGroups(key, CACHE_TTL_MS);
    if (cached) { bindEpgSource(url, cached.epgUrls, cached.groups); return cached.groups; }
  }
  const content = await fetchText(url, 120000);
  recordBandwidth();
  const groups = buildGroups(M3uParser.parse(content));
  const epgUrls = M3uParser.epgUrls(content);
  Store.cacheGroups(key, groups, epgUrls);
  bindEpgSource(url, epgUrls, groups);
  return groups;
}

function bindEpgSource(url: string, epgUrls: string[], groups: ChannelGroup[]): void {
  m3uSourceKey = url; m3uEpgUrls = epgUrls;
  m3uNeededIds = new Set(groups.map((g) => g.epgId).filter((id): id is string => !!id));
}

async function m3uEpg(group: ChannelGroup): Promise<EpgProgram[]> {
  if (!group.epgId) return [];
  const index = await ensureEpgIndex();
  return (index && index.get(normId(group.epgId))) || [];
}

async function ensureEpgIndex(): Promise<Map<string, EpgProgram[]> | null> {
  if (m3uEpgUrls.length === 0 || m3uNeededIds.size === 0) return null;
  const fresh = m3uEpgIndex && m3uEpgBuiltKey === m3uSourceKey && Date.now() - m3uEpgBuiltAt < EPG_TTL_MS;
  if (fresh) return m3uEpgIndex;
  if (!m3uEpgBuilding) {
    const key = m3uSourceKey;
    m3uEpgBuilding = indexGuides(m3uEpgUrls, m3uNeededIds).then((built) => { m3uEpgIndex = built; m3uEpgBuiltKey = key; m3uEpgBuiltAt = Date.now(); m3uEpgBuilding = null; return built; }, (e) => { m3uEpgBuilding = null; throw e; });
  }
  return m3uEpgBuilding;
}

async function loadXtreamGroups(profile: XtreamProfile, forceRefresh: boolean): Promise<ChannelGroup[]> {
  const key = 'xt_' + javaHash(profile.serverUrl + '|' + profile.username);
  if (!forceRefresh) { const cached = Store.cachedGroups(key, CACHE_TTL_MS); if (cached) return cached.groups; }
  const groups = buildGroups(await loadXtreamChannels(profile));
  recordBandwidth();
  Store.cacheGroups(key, groups, []);
  return groups;
}

function loadDirect(url: string): ChannelGroup[] {
  const m = /https?:\/\/([^/:]+)/i.exec(url);
  const name = (m ? m[1].replace(/^www\./, '') : '') || 'Direct';
  const channel: LiveChannel = { streamId: javaHash(url), name, iconUrl: null, categoryName: null, streamUrl: url };
  return [{ key: 'direct-' + javaHash(url), displayName: name, iconUrl: null, isFootball: false, variants: [{ channel, quality: 'UNKNOWN' }] }];
}
