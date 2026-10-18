/** Parses an M3U / M3U-plus playlist into LiveChannels. Port of the Android M3uParser. */
import { LiveChannel } from './model';
import { ChannelNameParser } from './names';

// ISO 3166-1 alpha-2 plus "UK" (iptv-org), minus "TV" (the .tv vanity domain, not Tuvalu).
const ISO = ('AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BV BW BY BZ ' +
  'CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR ' +
  'GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP ' +
  'KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ ' +
  'NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU RW ' +
  'SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TW TZ ' +
  'UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW UK').split(' ');
const isoCountries = new Set(ISO);

function countryOf(tvgId: string | null): string | null {
  if (!tvgId) return null;
  const dot = tvgId.lastIndexOf('.');
  if (dot < 0) return null;
  const code = tvgId.slice(dot + 1).split('@')[0].toUpperCase();
  return code.length === 2 && isoCountries.has(code) ? code.toLowerCase() : null;
}

const tvgName = /tvg-name="([^"]*)"/i;
const tvgId = /tvg-id="([^"]*)"/i;
const groupTitle = /group-title="([^"]*)"/i;
const tvgLogo = /tvg-logo="([^"]*)"/i;
const radioAttr = /\bradio="(true|1|yes)"/i;
const epgUrlAttr = /(?:x-tvg-url|url-tvg|tvg-url)="([^"]*)"/i;
const vodExtensions = ['.mp4', '.mkv', '.avi', '.mov', '.flv', '.wmv', '.m4v'];
const vodGroup = /\bvod\b/i;
const geoBlockedTag = /\[geo-?blocked]/i;

function attr(re: RegExp, line: string): string | null {
  const m = re.exec(line);
  const v = m ? m[1].trim() : '';
  return v === '' ? null : v;
}

/** Java String.hashCode, so stream ids match the Android app's for the same URL. */
export function javaHash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return h;
}

function isVod(url: string, group: string | null): boolean {
  const u = url.toLowerCase();
  if (u.includes('/movie/') || u.includes('/series/') || u.includes('://vod.')) return true;
  if (vodExtensions.some((e) => u.endsWith(e))) return true;
  return !!group && vodGroup.test(group);
}

export const M3uParser = {
  parse(content: string): LiveChannel[] {
    const channels: LiveChannel[] = [];
    let name: string | null = null, group: string | null = null, logo: string | null = null, epgId: string | null = null;
    let geoBlocked = false, isRadio = false;
    const lines = content.split(/\r?\n/);
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      if (/^#EXTINF/i.test(line)) {
        const attrName = attr(tvgName, line);
        const comma = line.lastIndexOf(',');
        const trailing = comma >= 0 ? line.slice(comma + 1).trim() : '';
        name = attrName || trailing;
        group = attr(groupTitle, line);
        logo = attr(tvgLogo, line);
        epgId = attr(tvgId, line);
        geoBlocked = geoBlockedTag.test(line);
        isRadio = radioAttr.test(line) || ChannelNameParser.isRadioCategory(group);
      } else if (line === '' || line.startsWith('#')) {
        // other directives
      } else {
        // URL line closing the current entry. Keep live channels only; VOD is dropped.
        if (name && name.trim() !== '' && !isVod(line, group)) {
          channels.push({
            streamId: javaHash(line), name, iconUrl: logo, categoryName: group, streamUrl: line,
            epgId, country: countryOf(epgId), geoBlocked, isRadio,
          });
        }
        name = null; group = null; logo = null; epgId = null; geoBlocked = false; isRadio = false;
      }
    }
    return channels;
  },

  /** EPG (XMLTV) URLs declared on the #EXTM3U header, if any. */
  epgUrls(content: string): string[] {
    const header = content.split(/\r?\n/).find((l) => /^#EXTM3U/i.test(l.trim()));
    if (!header) return [];
    const m = epgUrlAttr.exec(header);
    if (!m) return [];
    return m[1].split(/[, ]/).map((s) => s.trim()).filter((s) => /^https?:\/\//i.test(s));
  },
};
