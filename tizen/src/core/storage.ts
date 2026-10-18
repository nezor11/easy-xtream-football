/** Profiles, settings, favorites, recents and the channel cache, on localStorage (app-private on Tizen). */
import { ChannelGroup, Profile, QualityMode } from './model';

const KEY = {
  profiles: 'exf.profiles', lastProfile: 'exf.last_profile', quality: 'exf.quality_mode',
  recents: 'exf.recent_channel_keys', favorites: 'exf.favorite_channel_keys', favoriteFolders: 'exf.favorite_folders',
  hints: 'exf.player_hints_shown', infoVisible: 'exf.channel_info_visible', coffeeDismissed: 'exf.coffee_reminder_dismissed',
  language: 'exf.language', cache: 'exf.cache.', bandwidth: 'exf.bandwidth_bps',
};
const MAX_RECENT = 10;
const CACHE_VERSION = 19; // keep in step with the Android app's ContentRepository.CACHE_VERSION

function read<T>(key: string, fallback: T): T {
  try { const v = localStorage.getItem(key); return v === null ? fallback : (JSON.parse(v) as T); } catch (e) { return fallback; }
}
function write(key: string, value: unknown): boolean {
  try { localStorage.setItem(key, JSON.stringify(value)); return true; } catch (e) { return false; }
}

type Listener = () => void;
const listeners: Listener[] = [];
function notify(): void { listeners.forEach((l) => { try { l(); } catch (e) { /* ignore */ } }); }

export const Store = {
  onChange(l: Listener): () => void { listeners.push(l); return () => { const i = listeners.indexOf(l); if (i >= 0) listeners.splice(i, 1); }; },

  // --- profiles ---
  profiles(): Profile[] { return read<Profile[]>(KEY.profiles, []).sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase())); },
  profile(id: number): Profile | undefined { return this.profiles().find((p) => p.id === id); },
  saveProfile(p: Omit<Profile, 'id'> & { id?: number }): Profile {
    const all = read<Profile[]>(KEY.profiles, []);
    let saved: Profile;
    if (p.id && all.some((x) => x.id === p.id)) { saved = { ...(p as Profile) }; all.splice(all.findIndex((x) => x.id === p.id), 1, saved); }
    else { const id = all.reduce((m, x) => Math.max(m, x.id), 0) + 1; saved = { ...p, id } as Profile; all.push(saved); }
    write(KEY.profiles, all); notify(); return saved;
  },
  deleteProfile(id: number): void { write(KEY.profiles, read<Profile[]>(KEY.profiles, []).filter((p) => p.id !== id)); notify(); },
  lastProfileId(): number | null { return read<number | null>(KEY.lastProfile, null); },
  setLastProfileId(id: number): void { write(KEY.lastProfile, id); },

  // --- settings ---
  /** Last measured network speed (bits per second), 0 when never measured. Used by the Auto quality. */
  bandwidthBps(): number { return read<number>(KEY.bandwidth, 0); },
  setBandwidthBps(bps: number): void { write(KEY.bandwidth, Math.max(0, Math.round(bps))); },
  qualityMode(): QualityMode { return read<QualityMode>(KEY.quality, 'AUTO'); },
  setQualityMode(m: QualityMode): void { write(KEY.quality, m); notify(); },
  channelInfoVisible(): boolean { return read<boolean>(KEY.infoVisible, true); },
  setChannelInfoVisible(v: boolean): void { write(KEY.infoVisible, v); notify(); },
  coffeeReminderDismissed(): boolean { return read<boolean>(KEY.coffeeDismissed, false); },
  setCoffeeReminderDismissed(v: boolean): void { write(KEY.coffeeDismissed, v); notify(); },
  playerHintsShown(): number { return read<number>(KEY.hints, 0); },
  incrementPlayerHintsShown(): void { write(KEY.hints, this.playerHintsShown() + 1); },
  /** BCP-47 tag chosen by the user, or '' for the system language. */
  language(): string { return read<string>(KEY.language, ''); },
  setLanguage(tag: string): void { write(KEY.language, tag); notify(); },

  // --- favorites & recents (keyed by ChannelGroup.key, so they survive profile switches) ---
  recentChannelKeys(): string[] { return read<string[]>(KEY.recents, []); },
  pushRecentChannel(key: string): void { write(KEY.recents, [key, ...this.recentChannelKeys().filter((k) => k !== key)].slice(0, MAX_RECENT)); notify(); },
  favoriteChannelKeys(): string[] { return read<string[]>(KEY.favorites, []); },
  toggleFavoriteChannel(key: string): boolean {
    const list = this.favoriteChannelKeys(); const i = list.indexOf(key);
    if (i >= 0) list.splice(i, 1); else list.push(key);
    write(KEY.favorites, list); notify(); return i < 0;
  },
  moveFavoriteChannel(key: string, delta: number): void {
    const list = this.favoriteChannelKeys(); const i = list.indexOf(key); const j = i + delta;
    if (i < 0 || j < 0 || j >= list.length) return;
    [list[i], list[j]] = [list[j], list[i]]; write(KEY.favorites, list); notify();
  },
  favoriteFolders(): string[] { return read<string[]>(KEY.favoriteFolders, []); },
  toggleFavoriteFolder(name: string): void {
    const list = this.favoriteFolders(); const i = list.indexOf(name);
    if (i >= 0) list.splice(i, 1); else list.push(name);
    write(KEY.favoriteFolders, list); notify();
  },

  // --- channel cache (12 h), best effort: a huge list may exceed the quota and simply isn't cached ---
  cachedGroups(sourceKey: string, ttlMs: number): { groups: ChannelGroup[]; epgUrls: string[] } | null {
    const c = read<{ v: number; at: number; groups: ChannelGroup[]; epgUrls: string[] } | null>(KEY.cache + sourceKey, null);
    if (!c || c.v !== CACHE_VERSION || Date.now() - c.at > ttlMs || !c.groups.length) return null;
    return { groups: c.groups, epgUrls: c.epgUrls || [] };
  },
  cacheGroups(sourceKey: string, groups: ChannelGroup[], epgUrls: string[]): void {
    if (!write(KEY.cache + sourceKey, { v: CACHE_VERSION, at: Date.now(), groups, epgUrls })) this.clearCache();
  },
  clearCache(): void {
    const dead: string[] = [];
    for (let i = 0; i < localStorage.length; i++) { const k = localStorage.key(i); if (k && k.startsWith(KEY.cache)) dead.push(k); }
    dead.forEach((k) => localStorage.removeItem(k));
  },
};
