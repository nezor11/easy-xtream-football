/** Flat channels → sports-only, quality-deduplicated groups → brand folders. Port of ChannelGrouping + ChannelsViewModel.foldIntoFolders. */
import { ChannelFolder, ChannelGroup, ChannelVariant, LiveChannel, QUALITY } from './model';
import { ChannelNameParser } from './names';

function firstNonNull<T, R>(items: T[], f: (t: T) => R | null | undefined): R | null {
  for (const it of items) { const v = f(it); if (v !== null && v !== undefined && v !== '') return v; }
  return null;
}

export function buildGroups(channels: LiveChannel[]): ChannelGroup[] {
  const sports = channels.filter((c) => !ChannelNameParser.isVodCategory(c.categoryName) && ChannelNameParser.isSports(c.name, c.categoryName));
  const byKey = new Map<string, LiveChannel[]>();
  for (const c of sports) {
    const k = ChannelNameParser.groupKey(c.name) || c.name.toLowerCase();
    const list = byKey.get(k);
    if (list) list.push(c); else byKey.set(k, [c]);
  }
  const groups: ChannelGroup[] = [];
  byKey.forEach((members, key) => {
    const variants: ChannelVariant[] = members
      .map((channel) => ({ channel, quality: ChannelNameParser.quality(channel.name) }))
      .sort((a, b) => QUALITY[b.quality].rank - QUALITY[a.quality].rank);
    const representative = variants[0].channel;
    groups.push({
      key,
      displayName: ChannelNameParser.baseName(representative.name) || representative.name,
      iconUrl: firstNonNull(variants, (v) => v.channel.iconUrl),
      isFootball: members.some((c) => ChannelNameParser.isFootball(c.name, c.categoryName)),
      variants,
      epgId: firstNonNull(variants, (v) => v.channel.epgId),
      country: firstNonNull(variants, (v) => v.channel.country),
      geoBlocked: variants.every((v) => !!v.channel.geoBlocked),
      isRadio: variants.every((v) => !!v.channel.isRadio),
    });
  });
  return groups.sort((a, b) => a.displayName.toLowerCase().localeCompare(b.displayName.toLowerCase()));
}

function channelNumber(displayName: string): number {
  const m = /(\d{1,3})\s*$/.exec(displayName.trim());
  return m ? parseInt(m[1], 10) : 0;
}

export function foldIntoFolders(groups: ChannelGroup[]): ChannelFolder[] {
  const byKey = new Map<string, ChannelGroup[]>();
  for (const g of groups) {
    const k = ChannelNameParser.folderKey(g.displayName);
    const list = byKey.get(k);
    if (list) list.push(g); else byKey.set(k, [g]);
  }
  const folders: ChannelFolder[] = [];
  byKey.forEach((members) => {
    const sorted = members.slice().sort((a, b) => channelNumber(a.displayName) - channelNumber(b.displayName));
    folders.push({
      name: ChannelNameParser.commonFolderName(sorted.map((g) => g.displayName)),
      iconUrl: firstNonNull(sorted, (g) => g.iconUrl),
      isFootball: sorted.some((g) => g.isFootball),
      channels: sorted,
      country: firstNonNull(sorted, (g) => g.country),
      geoBlocked: sorted.every((g) => !!g.geoBlocked),
      isRadio: sorted.every((g) => !!g.isRadio),
    });
  });
  return folders.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
}

/** Gives a group without a logo the logo of a sibling in its folder (numbered feeds borrow the main channel's). */
export function fillFromFolderSiblings(groups: ChannelGroup[]): ChannelGroup[] {
  const folderLogo = new Map<string, string>();
  for (const g of groups) {
    if (g.iconUrl) { const k = ChannelNameParser.folderKey(g.displayName); if (!folderLogo.has(k)) folderLogo.set(k, g.iconUrl); }
  }
  if (folderLogo.size === 0) return groups;
  return groups.map((g) => g.iconUrl ? g : (folderLogo.get(ChannelNameParser.folderKey(g.displayName)) ? { ...g, iconUrl: folderLogo.get(ChannelNameParser.folderKey(g.displayName))! } : g));
}
