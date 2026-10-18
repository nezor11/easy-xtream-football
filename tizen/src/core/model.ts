/** Data model shared by every source (Xtream API, M3U playlist, direct link). Ported from the Android app. */

export type QualityName = 'UHD_4K' | 'QHD' | 'FHD' | 'HD' | 'SD' | 'UNKNOWN';

export interface Quality {
  name: QualityName;
  rank: number;
  label: string;
  typicalBitrateBps: number;
}

export const QUALITY: Record<QualityName, Quality> = {
  UHD_4K: { name: 'UHD_4K', rank: 5, label: '4K', typicalBitrateBps: 20_000_000 },
  QHD: { name: 'QHD', rank: 4, label: '2K', typicalBitrateBps: 10_000_000 },
  FHD: { name: 'FHD', rank: 3, label: 'FHD', typicalBitrateBps: 6_000_000 },
  HD: { name: 'HD', rank: 2, label: 'HD', typicalBitrateBps: 3_000_000 },
  SD: { name: 'SD', rank: 1, label: 'SD', typicalBitrateBps: 1_200_000 },
  UNKNOWN: { name: 'UNKNOWN', rank: 0, label: 'SD', typicalBitrateBps: 1_500_000 },
};

/** Selectable tiers, highest first. */
export const QUALITY_TIERS: QualityName[] = ['UHD_4K', 'QHD', 'FHD', 'HD', 'SD'];

/** Quality filter chosen by the user (the top chips). */
export type QualityMode = 'AUTO' | 'UHD_4K' | 'FHD' | 'HD' | 'SD';
export const QUALITY_MODES: QualityMode[] = ['AUTO', 'UHD_4K', 'FHD', 'HD', 'SD'];
export function fixedQuality(mode: QualityMode): QualityName | null {
  return mode === 'AUTO' ? null : mode;
}

/** A single playable live channel. `streamId` is the source's own id; favorites key on ChannelGroup.key. */
export interface LiveChannel {
  streamId: number;
  name: string;
  iconUrl: string | null;
  categoryName: string | null;
  streamUrl: string;
  epgId?: string | null;
  country?: string | null;
  geoBlocked?: boolean;
  isRadio?: boolean;
}

export interface ChannelVariant {
  channel: LiveChannel;
  quality: QualityName;
}

/** A logical channel bundling its per-quality stream variants (highest quality first). */
export interface ChannelGroup {
  key: string;
  displayName: string;
  iconUrl: string | null;
  isFootball: boolean;
  variants: ChannelVariant[];
  epgId?: string | null;
  country?: string | null;
  geoBlocked?: boolean;
  isRadio?: boolean;
}

export function availableQualities(group: ChannelGroup): Set<QualityName> {
  return new Set(group.variants.map((v) => v.quality));
}
export function bestVariant(group: ChannelGroup): ChannelVariant {
  return group.variants[0];
}
export function variantFor(group: ChannelGroup, quality: QualityName): ChannelVariant | undefined {
  return group.variants.find((v) => v.quality === quality);
}
/** Highest available variant at or below `quality`; falls back to the lowest available. */
export function variantAtOrBelow(group: ChannelGroup, quality: QualityName): ChannelVariant {
  const rank = QUALITY[quality].rank;
  return group.variants.find((v) => QUALITY[v.quality].rank <= rank) || group.variants[group.variants.length - 1];
}

/** A family that bundles its numbered channels ("Canal Deporte" → 1, 2, 3…). */
export interface ChannelFolder {
  name: string;
  iconUrl: string | null;
  isFootball: boolean;
  channels: ChannelGroup[];
  country?: string | null;
  geoBlocked?: boolean;
  isRadio?: boolean;
}
export function isSingle(folder: ChannelFolder): boolean {
  return folder.channels.length === 1;
}

/** A programme (epoch millis, 0 when unknown). */
export interface EpgProgram {
  title: string;
  start: number;
  end: number;
  nowFlag: boolean;
}

export type ProfileType = 'XTREAM' | 'M3U' | 'DIRECT';

export interface Profile {
  id: number;
  name: string;
  type: ProfileType;
  serverUrl: string;
  username: string;
  password: string;
  /** M3U or direct stream URL (blank for Xtream profiles). */
  m3uUrl: string;
}

export interface XtreamProfile {
  name: string;
  serverUrl: string;
  username: string;
  password: string;
}
export function isComplete(p: XtreamProfile): boolean {
  return p.serverUrl.trim() !== '' && p.username.trim() !== '' && p.password.trim() !== '';
}
