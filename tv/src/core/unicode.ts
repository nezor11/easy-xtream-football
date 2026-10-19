/**
 * Letter and digit character classes for regexes, spelled out as ranges because the oldest TVs we
 * run on (webOS 4.x = Chrome 53) have neither `\p{L}` nor lookbehind. Use inside `[...]`.
 * Covers Latin, Greek, Cyrillic, Armenian, Hebrew, Arabic, Devanagari, Thai, Hangul, kana and CJK.
 */
export const L = 'A-Za-z\\u00AA\\u00B5\\u00BA\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u03FF\\u0400-\\u052F'
  + '\\u0531-\\u0587\\u05D0-\\u05EA\\u0620-\\u064A\\u066E-\\u06D3\\u0900-\\u0963\\u0E01-\\u0E3A\\u0E40-\\u0E4E'
  + '\\u1100-\\u11FF\\u3041-\\u3096\\u30A1-\\u30FA\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uAC00-\\uD7AF\\uF900-\\uFAFF';
export const N = '0-9\\u0660-\\u0669\\u06F0-\\u06F9\\u0966-\\u096F\\u0E50-\\u0E59\\uFF10-\\uFF19';
