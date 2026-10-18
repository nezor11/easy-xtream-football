/**
 * Heuristics for the wildly inconsistent channel naming used by IPTV panels: quality tier, normalized
 * base name (so quality variants collapse into one logical channel) and sports / football detection.
 * A straight port of the Android ChannelNameParser — keep the two in sync.
 */
import { QualityName } from './model';

function escapeRe(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// Order matters: 4K before 2K before FHD before HD ("FULL HD" contains "HD"). Matches a quality either
// delimited or glued to the end of a word ("LaLigaTVHD"); must not start mid-number.
function tokenRegex(...tokens: string[]): RegExp {
  return new RegExp(`(?<![0-9])(${tokens.join('|')})(?![a-z0-9])`, 'iu');
}
const qualityPatterns: Array<[QualityName, RegExp]> = [
  ['UHD_4K', tokenRegex('4k', 'uhd', '2160p?')],
  ['QHD', tokenRegex('2k', '1440p?', 'qhd')],
  ['FHD', tokenRegex('fhd', 'full\\s*hd', '1080p?')],
  ['HD', tokenRegex('hd', '720p?')],
  ['SD', tokenRegex('sd', '480p?', '360p?')],
];
const allQualityTokens = /(?<![0-9])(4k|uhd|2160p?|2k|1440p?|qhd|fhd|full\s*hd|1080p?|hd|720p?|sd|480p?|360p?)(?![a-z0-9])/giu;
// Availability tags a panel appends to event-only feeds, whole phrases only.
const availabilityTags = /(?<!\p{L})(solo\s+eventos|only\s+events|events\s+only)(?!\p{L})/giu;
// Leading provider/country prefix: "ES|", "ES:", "EN -", "VIP >" and the pipe-wrapped "|IT| ".
const leadingPrefix = /^\s*[|:>\-•]?\s*[\p{L}0-9]{1,4}\s*[|:>\-•]\s*/u;
const bracketed = /[\[(][^\])]*[\])]/gu;
// Strip emoji / flags / symbol noise (keep letters, numbers, spaces, basic punctuation).
const noise = /[^\p{L}\p{N}\s&+./'-]/gu;
const multiSpace = /\s{2,}/g;

// Brand-agnostic on purpose: generic sport terms, sport types and competitions — never channel brands.
const sportsKeywords = [
  'sport', 'deporte', 'esporte', 'esport', 'spor', 'رياضة', 'رياضي', 'اسبور',
  'спорт', '체육', '运动', '體育',
  'futbol', 'fútbol', 'football', 'soccer', 'fußball', 'fussball', 'calcio', 'futebol',
  'voetbal', 'fudbal', 'ποδόσφαιρο', 'كرة',
  'laliga', 'la liga', 'liga', 'ligue', 'premier league', 'championship', 'bundesliga',
  'eredivisie', 'serie a', 'serie b', 'primeira', 'champions', 'uefa', 'europa league',
  'conference league', 'copa', 'libertadores', 'sudamericana', 'fa cup', 'efl', 'mls',
  'super lig', 'jupiler', 'superliga', 'eliteserien', 'allsvenskan', 'fifa',
  'baloncesto', 'basket', 'basketball', 'nba', 'acb', 'euroleague', 'euroliga', 'basquet',
  'tennis', 'tenis', 'atp', 'wta', 'roland garros', 'wimbledon',
  'f1 ', 'formula 1', 'fórmula 1', 'formula1', 'motogp', 'moto gp', 'nascar', 'indycar',
  'wrc', 'dakar', 'superbike', 'motor',
  'nfl', 'nhl', 'mlb', 'ufc', 'wwe', 'boxing', 'boxeo', 'boxe', 'mma', 'wrestling', 'fight',
  'combat', 'lucha',
  'rugby', 'cricket', 'golf', 'ciclismo', 'cycling', 'hockey', 'handball', 'balonmano',
  'voleibol', 'volleyball', 'volley', 'atletismo', 'athletics', 'padel', 'pádel', 'snooker',
  'darts', 'racing', 'olympic', 'olimpic', 'juegos olimpicos', 'extreme',
];
const footballKeywords = [
  'futbol', 'fútbol', 'football', 'soccer', 'fußball', 'fussball', 'calcio', 'futebol',
  'voetbal', 'fudbal', 'ποδόσφαιρο',
  'laliga', 'la liga', 'premier league', 'championship', 'bundesliga', 'eredivisie',
  'serie a', 'serie b', 'primeira liga', 'ligue 1', 'ligue 2',
  'champions', 'uefa', 'europa league', 'conference league', 'copa del rey', 'copa',
  'libertadores', 'sudamericana', 'fa cup', 'efl', 'mls', 'super lig', 'jupiler', 'fifa',
];
// Keywords match only at the start of a word (not preceded by a letter).
function boundaryRegex(keywords: string[]): RegExp {
  return new RegExp(`(?<!\\p{L})(${keywords.map(escapeRe).join('|')})`, 'iu');
}
const sportsRegex = boundaryRegex(sportsKeywords);
const footballRegex = boundaryRegex(footballKeywords);
// Strong roots that also count mid-word; "sport" excludes "transport"/"passport".
const sportsSubstringRegex = /(?<!tran)(?<!pas)sport|deporte|fútbol|futbol|football|calcio/iu;
const vodCategory = /\bvod\b/i;
const radioKeywords = [
  'radio', 'radios', 'rádio', 'rádios', 'radyo', 'радио', 'ραδιόφωνο', 'راديو', 'إذاعة',
  'วิทยุ', '電台', '电台', '廣播', 'रेडियो',
];
const radioRegex = new RegExp(`(?<!\\p{L})(${radioKeywords.map(escapeRe).join('|')})(?!\\p{L})`, 'iu');

const firstWord = /^\S+/u;
const nonLetters = /\P{L}+/gu;
const whitespace = /\s+/u;
const FOLDER_KEY_LEN = 6;

function trimChars(s: string, chars: string): string {
  let a = 0, b = s.length;
  while (a < b && chars.includes(s[a])) a++;
  while (b > a && chars.includes(s[b - 1])) b--;
  return s.slice(a, b);
}

function matchesAny(text: string | null | undefined, regex: RegExp): boolean {
  if (!text || text.trim() === '') return false;
  regex.lastIndex = 0;
  return regex.test(text);
}

/** Strips brackets, a leading provider prefix and symbol noise — but NOT quality tags. */
function normalizeForTokens(rawName: string): string {
  let name = rawName.replace(bracketed, ' ');
  name = name.replace(leadingPrefix, '');
  name = name.replace(noise, ' ');
  return name.replace(multiSpace, ' ').trim();
}

export const ChannelNameParser = {
  /** Folder/brand label: the channel's first word. */
  folderName(baseName: string): string {
    const trimmed = baseName.trim();
    const m = firstWord.exec(trimmed);
    const word = m ? trimChars(m[0], '-_·|:') : trimmed;
    return word.trim() === '' ? trimmed : word;
  },

  /** Grouping key: first word capped at FOLDER_KEY_LEN letters; short first words extend with the rest. */
  folderKey(baseName: string): string {
    const trimmed = baseName.trim();
    const m = firstWord.exec(trimmed);
    const firstWordLetters = (m ? m[0] : '').replace(nonLetters, '').toLowerCase();
    const basis = firstWordLetters.length >= 3 ? firstWordLetters : trimmed.replace(nonLetters, '').toLowerCase();
    if (basis.length >= FOLDER_KEY_LEN) return basis.slice(0, FOLDER_KEY_LEN);
    if (basis.length > 0) return basis;
    return trimmed.toLowerCase();
  },

  /** Longest common run of leading words ("C More Golf"/"C More Sport" → "C More"). */
  commonFolderName(names: string[]): string {
    if (names.length === 0) return '';
    const wordLists = names.map((n) => n.trim().split(whitespace).filter((w) => w !== ''));
    const first = wordLists[0];
    let common = 0;
    while (common < first.length && wordLists.every((l) => (l[common] || '').toLowerCase() === first[common].toLowerCase())) common++;
    return common > 0 ? first.slice(0, common).join(' ') : ChannelNameParser.folderName(names[0]);
  },

  quality(rawName: string): QualityName {
    const name = normalizeForTokens(rawName);
    for (const [quality, pattern] of qualityPatterns) if (pattern.test(name)) return quality;
    return 'UNKNOWN';
  },

  /** Canonical channel name with quality tags, availability tags, prefixes and noise removed. */
  baseName(rawName: string): string {
    const name = normalizeForTokens(rawName).replace(allQualityTokens, ' ').replace(availabilityTags, ' ');
    return trimChars(name.replace(multiSpace, ' ').trim(), '-|:.·').trim();
  },

  /** Stable identity used to group quality variants of the same channel. */
  groupKey(rawName: string): string {
    return ChannelNameParser.baseName(rawName).toLowerCase();
  },

  isSports(channelName: string, categoryName: string | null | undefined): boolean {
    const sport = (t: string | null | undefined) => matchesAny(t, sportsRegex) || matchesAny(t, sportsSubstringRegex);
    return sport(channelName) || sport(categoryName);
  },

  isFootball(channelName: string, categoryName: string | null | undefined): boolean {
    return matchesAny(channelName, footballRegex) || matchesAny(categoryName, footballRegex);
  },

  isVodCategory(category: string | null | undefined): boolean {
    return !!category && vodCategory.test(category);
  },

  /** "Radio" as a whole word in the category only — "Radio Marca TV" is television. */
  isRadioCategory(category: string | null | undefined): boolean {
    return matchesAny(category, radioRegex);
  },
};
