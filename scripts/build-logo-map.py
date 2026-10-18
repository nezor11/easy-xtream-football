#!/usr/bin/env python3
"""Builds docs/logos/sports.json: a compact channel-name -> logo-URL map from the public iptv-org database.

The apps use it to give a logo to channels whose playlist carries none (no image is stored here, only
URLs to the images iptv-org hosts). Keys are names normalised like LogoMatching does (lower case, letters
and digits only); values are URLs, with the very common imgur host abbreviated to "i:" to keep the file small.
Only sports and general channels are kept: the apps show sports playlists.

Usage: python3 -I scripts/build-logo-map.py [out.json]   (run weekly by .github/workflows/logo-map.yml)
"""
import datetime
import json
import sys
import urllib.request

CHANNELS_URL = 'https://iptv-org.github.io/api/channels.json'
LOGOS_URL = 'https://iptv-org.github.io/api/logos.json'
CATEGORIES = {'sports', 'general'}
IMGUR = 'https://i.imgur.com/'
VERSION = 1


def fetch(url):
    with urllib.request.urlopen(url, timeout=120) as r:
        return json.load(r)


def normalize(name):
    return ''.join(c for c in name.lower() if c.isalnum())


def build(channels, logos):
    # First logo per channel, preferring the channel-wide one (no feed) that is in use.
    logo_by_id = {}
    for logo in sorted(logos, key=lambda l: (l.get('feed') is not None, not l.get('in_use', True))):
        url = (logo.get('url') or '').strip()
        if url and logo['channel'] not in logo_by_id:
            logo_by_id[logo['channel']] = url
    out = {}
    for ch in channels:
        url = logo_by_id.get(ch.get('id'))
        if not url or not (CATEGORIES & set(ch.get('categories') or [])):
            continue
        if url.startswith(IMGUR):
            url = 'i:' + url[len(IMGUR):]
        for name in [ch.get('name') or ''] + list(ch.get('alt_names') or []):
            key = normalize(name)
            if key and key not in out:
                out[key] = url
    return out


def main():
    out_path = sys.argv[1] if len(sys.argv) > 1 else 'docs/logos/sports.json'
    channels, logos = fetch(CHANNELS_URL), fetch(LOGOS_URL)
    logo_map = build(channels, logos)
    if len(logo_map) < 5000:
        sys.exit(f'suspiciously small map ({len(logo_map)} entries), keeping the previous file')
    doc = {'v': VERSION, 'date': datetime.date.today().isoformat(), 'source': 'https://github.com/iptv-org/database',
           'imgur': IMGUR, 'logos': dict(sorted(logo_map.items()))}
    with open(out_path, 'w', encoding='utf-8') as f:
        json.dump(doc, f, separators=(',', ':'), ensure_ascii=False)
        f.write('\n')
    print(f'{len(logo_map)} names -> {out_path}')


if __name__ == '__main__':
    main()
