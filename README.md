# Easy Xtream Football

A free IPTV player for **Android TV / Google TV, Fire TV and Android phone/tablet**, with a
*TV-first* design (remote control, D-pad focus) and a focus on **live sports / football**.
It supports both the **Xtream Codes** protocol and **M3U / M3U-plus playlists**.

[**Google Play**](https://play.google.com/store/apps/details?id=com.footballxtream) ·
[**Amazon Appstore**](https://www.amazon.com/gp/mas/dl/android?p=com.footballxtream) ·
[**Website**](https://easy-xtream-football-web.vercel.app) ·
[Privacy policy](docs/privacy-policy.md)

> **Status:** published. On Google Play (phones, tablets and Android TV) and on the Amazon Appstore
> (Fire TV and Fire tablets). Developed and tested on a Chromecast with Google TV and an Android
> phone. See the [Roadmap](#roadmap).

## Features

- **Two ways to connect**: an **Xtream** profile (server + username + password) or an **M3U
  playlist** (paste the URL). Multiple saved profiles picked from a selector; they can be
  **renamed and edited** (long-press a profile → *Edit* / *Delete*).
- **Try it without a provider**: on a fresh install a *"Try with a sample radio playlist"* button
  adds one ordinary M3U profile — sports talk radio from several countries, using the stations' own
  public streams (kept in [`docs/playlists`](docs/playlists)). It is optional, and you can delete it
  like any other profile. (Google Play builds only; the Amazon Appstore build, `-Pstore=amazon`,
  is the bare player.)
- **Sports only**: automatically filters sports/football channels (multi-language keywords) and
  **discards everything else, VOD (movies/series) and mis-categorized general channels**.
- **Radio**: stations flagged by the playlist (`radio="true"`, Xtream `radio_streams`, a *Radio*
  category) — or simply streams with no video track — get their own screen with the station logo,
  its name and, when the stream announces it over ICY metadata, what is playing right now.
- **Folders by family**: groups the numbered variants of the same channel (*1/2/3…*) into one
  folder with its logo. Logos missing from the playlist are looked up in the public **iptv-org**
  database and cached on disk.
- **Deduplication by quality**: the variants of the same channel (4K/2K/FHD/HD/SD) are merged into
  a single logical channel, even when the quality is glued to the name (`…TVHD`).
- **Software audio (FFmpeg)**: decodes **AC-3 / E-AC-3 (Dolby), MP2 and DTS**, so channels the
  device cannot decode in hardware (typical of some premium channels) **still have sound**.
- **Search** to find channels by name instantly, and a filter row that scrolls sideways on a phone.
- **Continue watching**: remembers and resumes the last channel watched.
- **TV player**:
  - **Zapping with the remote** (left/right = channel, double press = folder).
  - **Pause**: the remote's ⏯ key, or **OK twice**; while paused a single OK resumes.
  - **OK menu** with sections: quality (Auto · 4K · 2K · FHD · HD · SD), audio track, subtitles,
    guide and channel info.
  - **Automatic quality** based on measured bandwidth, with automatic step-down on stalls; if a
    variant fails (dead stream) it tries another, and warns when all of them fail.
  - **Discreet overlay** (channel · quality · Mbps · resolution) at the bottom-left so it doesn't
    cover the scoreboard; transfer rate refreshes frequently.
  - **"Now / Next" EPG**: via the API on **Xtream** profiles, and via **XMLTV** on **M3U**
    playlists that declare their guide (`x-tvg-url` + `tvg-id`), downloaded and parsed in the
    background.
  - Extended pre-buffering to smooth out IPTV playback.
- **Touch controls on a phone**: swipe ◀▶ to change channel and ▲▼ for quality, tap for the menu,
  double tap to pause.
- **Favorites** (long-press a card), with custom ordering, favorite folders, and favorites that
  **carry over between the Xtream profile and the M3U playlist of the same provider**.
- **Multi-language**: 24 languages (English, Spanish, Catalan, Basque, Galician, Portuguese,
  French, Italian, German, Turkish, Polish, Arabic, Indonesian, Vietnamese, Romanian, Greek,
  Croatian, Serbian, Thai, Traditional Chinese, Russian, Dutch, Albanian and Hindi) — follows the
  device language, with an in-app picker on the profiles screen. Right-to-left layouts included.
- **Optional tip**: a *"buy me a coffee"* reminder that can be silenced for good. Where Google Play
  is available it is a consumable in-app product bought with the remote; elsewhere (Fire TV) a QR
  points at the project website. Nothing is ever locked behind it.
- UI built with **Jetpack Compose for TV**.

## Stack

- **Kotlin** + **Jetpack Compose for TV** (`androidx.tv:tv-material`)
- **Media3 / ExoPlayer** (+ HLS) with the **FFmpeg extension** (`io.github.anilbeesetti:nextlib-media3ext`)
  for IPTV audio codecs (AC-3/E-AC-3/MP2/DTS)
- **Retrofit + OkHttp + kotlinx.serialization** for the Xtream API; a custom M3U parser
- **Room** (profiles and favorites, with migrations so data isn't lost on upgrade) and
  **DataStore** (settings: quality, last channel)
- **Google Play Billing** for the optional tip (absent devices fall back to the QR)
- **Coil** for images; logos via the public **iptv-org** database
- **MVVM** architecture with manual dependency injection (`AppContainer`)

## How to add your playlist

In **Add**, choose the mode:

- **Xtream**: `server URL` (`http://host:port`), `username` and `password`.
- **M3U playlist**: paste your playlist URL (`…/get.php?...&type=m3u_plus` or similar).
- **Direct link**: a single HLS/DASH stream, played as-is.

> Note: some providers serve the Xtream API but block their `/live/` CDN for generic clients; in
> that case use **M3U** mode, which usually works.

## Build requirements

Builds with **Android Studio** (recommended — it ships a JDK and the SDK):

1. Install [Android Studio](https://developer.android.com/studio) and open the project folder.
2. Android Studio downloads the SDK (compileSdk 36) and syncs Gradle.
3. Run on a real **Android TV / Fire TV / phone** over ADB (recommended) or an emulator.

From the command line (requires a JDK 17+ and the Android SDK with `local.properties`):

```bash
./gradlew assembleFullDebug        # full build; use assembleLiteDebug for the small one
./gradlew bundleFullRelease        # the AAB uploaded to Google Play
./gradlew assembleFullRelease -Pstore=amazon   # per-ABI APKs + the universal one for the Amazon Appstore
```

Two **flavors**: `full` bundles the FFmpeg software decoders (~7.5 MB) for the AC-3/E-AC-3/DTS/MP2
audio common on IPTV; `lite` drops them for a much smaller APK (channels using those codecs are then
silent). Each flavor is split **per ABI** (`app-full-arm64-v8a-release.apk` and friends) and also
produces a **universal** APK. Most Android TV / Fire TV devices use **arm64-v8a**.

Release builds are signed when a gitignored `keystore.properties` is present (`storeFile`,
`storePassword`, `keyAlias`, `keyPassword`); without it they fall back to the debug key and are
**not** suitable for distribution.

> minSdk 24 (Android 7.0), targetSdk 36. Very old Fire TV devices on Fire OS 5 (Android 5.1) are
> not supported.

## Samsung TV (Tizen)

A port for Samsung Smart TVs lives in [`tizen/`](tizen/README.md): the same channel grouping, 24
languages, favorites and player (AVPlay), as a TypeScript web app for Tizen 5.5+. Tested on a Smart
Monitor M7. Not yet on the Samsung store. Channels without a logo get one from the
[logo map](docs/logos/sports.json) built weekly from the iptv-org database (URLs only).

## Roadmap

- [ ] **Samsung TV app on the Samsung Apps store** (code in `tizen/`, working on a real TV).
- [ ] **Catch-up / timeshift** (`has_archive`).
- [ ] **Browse playlists by country and sport** from the app, to add them in one step.
- [ ] **Optional Chromecast** (decoupled to avoid depending on Google Play Services on Fire TV).
- [ ] (Optional) VOD / Movies and Series, if scope grows beyond sports.

Done recently: release on **Google Play** and the **Amazon Appstore**, **24 languages** (RTL
included), **radio** support, **pause** with a double OK, **touch controls** on phones, a **sample radio
playlist** for a first run without a provider, favorites shared between profiles of the same
provider, a **Settings** screen (clear cache, in-app open-source licenses), a **"Live now"** row
with per-country EPG feed prioritisation, credentials encrypted at rest (AES-GCM with an Android
Keystore key), Room migrations, EPG via XMLTV for M3U playlists, and profile editing/renaming.

## Disclaimer

Easy Xtream Football is a **neutral client player** (like VLC): it does not include, host or
distribute any channel, subscription or content. The only playlist it can add for you is the
**optional sample** of sports radio stations described above (their own public streams), which you
choose to add and can delete at any time. The user is solely responsible for the servers and credentials they
configure, the content they access, and for complying with applicable law.

## License

Distributed under **GPL-3.0**. See [LICENSE](LICENSE).

This software uses libraries from the **[FFmpeg](https://ffmpeg.org) project** under the **LGPLv3**,
bundled via **[NextLib](https://github.com/anilbeesetti/nextlib)** (GPL-3.0). Full third-party
attribution — FFmpeg, libvpx, Mbed TLS and the rest of the stack — is in
[THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
