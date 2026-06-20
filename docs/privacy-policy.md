# Privacy Policy — Easy Xtream Football

_Last updated: 19 June 2026_

Easy Xtream Football ("the app") is an open-source IPTV player for Android TV and
mobile that plays live channels from a streaming service **you** provide (an
Xtream Codes account or an M3U/M3U8 playlist URL). This policy explains what the
app does — and does not — do with your data.

**Short version:** the app does not collect, transmit, or sell any personal data
to the developer or to any third party. Everything you enter stays on your
device.

## Information the app stores (on your device only)

- **Provider credentials and playlist URLs** — your Xtream username, password and
  server URL, or your M3U/M3U8/EPG URLs. These are stored **only on your device**,
  encrypted at rest with a key held in the Android Keystore (AES‑256‑GCM). They are
  **never** sent to the developer.
- **Preferences and app state** — favorites and their order, recently watched
  channels, the selected language, quality and player settings. Stored locally on
  the device.

The developer has **no servers** and **no account system**, and therefore has no
access to any of the above.

## Network connections the app makes

The app connects directly to the services **you** configure, plus a few public
sources to enrich the guide and logos:

- **Your IPTV provider** — to authenticate, list channels, fetch the programme
  guide (EPG) and stream video. Your credentials travel to that provider only, as
  required to log in. The app's developer is not that provider and is not involved
  in those requests.
- **Public channel/EPG sources** (e.g. iptv‑org, public XMLTV guides) — to fetch
  channel logos and electronic programme guide data. As with any web request,
  these third parties can see your device's IP address. The app downloads only
  public data and sends no personal information to them.

Because IPTV streams commonly use plain HTTP, data exchanged with your provider
may travel unencrypted over the network — this is a property of the IPTV service
you choose, not of the app.

## Information the app does NOT collect

- No analytics, telemetry, advertising, or tracking SDKs.
- No location, contacts, microphone, camera, or device identifiers.
- No user accounts, sign‑in, or cloud sync.
- No data is shared with or sold to third parties by the developer.

## Data deletion

All data lives on your device. You can remove it at any time by deleting a
profile inside the app, clearing the app's storage in system settings, or
uninstalling the app.

## Children

The app is not directed at children and does not knowingly collect any data from
anyone.

## Changes to this policy

If this policy changes, the updated version will be published at this same URL
with a new "Last updated" date.

## Contact

Questions about this policy: **contact@nezor.es**
Source code: <https://github.com/nezor11/easy-xtream-football>
