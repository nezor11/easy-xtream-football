"""Converts the Android app's res/values*/strings.xml into one JSON file for the Tizen app.

Output: {"en": {key: value}, "es": {...}, ...}. Android escapes are undone ("\\'" → "'", "\\n" → newline,
"\\@"), HTML-ish tags are kept as text, format args stay as %1$s / %d (resolved at runtime by i18n.ts).
Plurals become {"one": "...", "other": "..."} objects; string-arrays become JS arrays.
"""
import sys, json, os, re
import xml.etree.ElementTree as ET

res_dir, out_path = sys.argv[1], sys.argv[2]

def unescape(text):
    if text is None: return ''
    t = text.replace('\\n', '\n').replace("\\'", "'").replace('\\"', '"').replace('\\@', '@').replace('\\?', '?')
    t = re.sub(r'\s+', ' ', t) if '\n' not in text else t
    return t.strip()

def inner_text(el):
    # Keep inline markup (<b>, <u>) as plain text content.
    return unescape(''.join(el.itertext()))

def lang_of(folder):
    if folder == 'values': return 'en'
    tag = folder[len('values-'):]
    if re.fullmatch(r'v\d+', tag): return None          # API-level qualifier, not a language
    tag = tag.replace('b+', '').replace('+', '-')        # b+sr+Latn → sr-Latn
    tag = re.sub(r'-r([A-Z]{2})$', r'-\1', tag)          # zh-rTW → zh-TW
    return {'in': 'id'}.get(tag, tag)                    # Android's legacy code for Indonesian

out = {}
for folder in sorted(os.listdir(res_dir)):
    if not folder.startswith('values'): continue
    path = os.path.join(res_dir, folder, 'strings.xml')
    if not os.path.exists(path): continue
    lang = lang_of(folder)
    if lang is None: continue
    root = ET.parse(path).getroot()
    table = out.setdefault(lang, {})
    for el in root:
        name = el.get('name')
        if el.get('translatable') == 'false' and lang != 'en': continue
        if el.tag == 'string':
            table[name] = inner_text(el)
        elif el.tag == 'plurals':
            table[name] = {item.get('quantity'): inner_text(item) for item in el.findall('item')}
        elif el.tag == 'string-array':
            table[name] = [inner_text(item) for item in el.findall('item')]

with open(out_path, 'w', encoding='utf-8') as f:
    json.dump(out, f, ensure_ascii=False, separators=(',', ':'))
print(len(out), 'languages,', len(out['en']), 'keys in en')
