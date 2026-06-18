# Easy Xtream Football

A free IPTV player for **Android TV / Google TV, Fire TV and Android phone/tablet**, with a
*TV-first* design (remote control, D-pad focus) and a focus on **live sports / football**.
It supports both the **Xtream Codes** protocol and **M3U / M3U-plus playlists**.

> **Status:** working. Login (Xtream or M3U) → browse sports channels → playback, with software
> Dolby audio, search, folders with logos and "continue watching". Tested on a Chromecast with
> Google TV. See the [Roadmap](#roadmap).

## Features

- **Two ways to connect**: an **Xtream** profile (server + username + password) or an **M3U
  playlist** (paste the URL). Multiple saved profiles picked from a selector; they can be
  **renamed and edited** (long-press a profile → *Edit* / *Delete*).
- **Sports only**: automatically filters sports/football channels (multi-language keywords) and
  **discards everything else, VOD (movies/series) and mis-categorized general channels**.
- **Folders by family**: groups the numbered variants of the same channel (*1/2/3…*) into one
  folder with its logo. Logos missing from the playlist are looked up in the public **iptv-org**
  database and cached on disk.
- **Deduplication by quality**: the variants of the same channel (4K/2K/FHD/HD/SD) are merged into
  a single logical channel, even when the quality is glued to the name (`…TVHD`).
- **Software audio (FFmpeg)**: decodes **AC-3 / E-AC-3 (Dolby), MP2 and DTS**, so channels the
  device cannot decode in hardware (typical of some premium channels) **still have sound**.
- **Search** to find channels by name instantly.
- **Continue watching**: remembers and resumes the last channel watched.
- **TV player**:
  - **Zapping with the remote** (up/down/left/right = change channel).
  - **Quality selector** with OK (Auto · 4K · 2K · FHD · HD · SD of the channel).
  - **Automatic quality** based on measured bandwidth, with automatic step-down on stalls; if a
    variant fails (dead stream) it tries another, and warns when all of them fail.
  - **Discreet overlay** (channel · quality · Mbps · resolution) at the bottom-left so it doesn't
    cover the scoreboard; transfer rate refreshes frequently.
  - **"Now / Next" EPG**: via the API on **Xtream** profiles, and via **XMLTV** on **M3U**
    playlists that declare their guide (`x-tvg-url` + `tvg-id`), downloaded and parsed in the
    background.
  - Extended pre-buffering to smooth out IPTV playback.
- **Favorites** (long-press a card).
- **Multi-language**: English, Spanish, Catalan, Basque, Galician, Portuguese, French and Italian —
  follows the device language, with an in-app picker on the profiles screen.
- UI built with **Jetpack Compose for TV**.

## Stack

- **Kotlin** + **Jetpack Compose for TV** (`androidx.tv:tv-material`)
- **Media3 / ExoPlayer** (+ HLS) with the **FFmpeg extension** (`io.github.anilbeesetti:nextlib-media3ext`)
  for IPTV audio codecs (AC-3/E-AC-3/MP2/DTS)
- **Retrofit + OkHttp + kotlinx.serialization** for the Xtream API; a custom M3U parser
- **Room** (profiles and favorites, with migrations so data isn't lost on upgrade) and
  **DataStore** (settings: quality, last channel)
- **Coil** for images; logos via the public **iptv-org** database
- **MVVM** architecture with manual dependency injection (`AppContainer`)

## How to add your playlist

In **Add**, choose the mode:

- **Xtream**: `server URL` (`http://host:port`), `username` and `password`.
- **M3U playlist**: paste your playlist URL (`…/get.php?...&type=m3u_plus` or similar).

> Note: some providers serve the Xtream API but block their `/live/` CDN for generic clients; in
> that case use **M3U** mode, which usually works.

## Build requirements

Builds with **Android Studio** (recommended — it ships JDK 17 and the SDK):

1. Install [Android Studio](https://developer.android.com/studio) and open the project folder.
2. Android Studio downloads the SDK (compileSdk 35) and syncs Gradle.
3. Run on a real **Android TV / Fire TV / phone** over ADB (recommended) or an emulator.

From the command line (requires JDK 17 and the Android SDK with `local.properties`):

```bash
./gradlew assembleFullDebug   # full build; use assembleLiteDebug for the small one
```

Two **flavors**: `full` bundles the FFmpeg software decoders (~7.5 MB) for the AC-3/E-AC-3/DTS/MP2
audio common on IPTV; `lite` drops them for a ~2.5 MB APK (channels using those codecs are then
silent). Each is also split **per ABI** (no universal APK), e.g. `app-full-armeabi-v7a-release.apk`.
Most Android TV / Fire TV devices use **arm64-v8a**.

> minSdk 24 (Android 7.0). Very old Fire TV devices on Fire OS 5 (Android 5.1) are not supported.

## Roadmap

- [ ] **Catch-up / timeshift** (`has_archive`).
- [ ] **Optional Chromecast** (decoupled to avoid depending on Google Play Services on Fire TV).
- [ ] (Optional) VOD / Movies and Series, if scope grows beyond sports.

Done recently: a **Settings screen** (clear channel/guide cache, in-app open-source licenses), a
**"Live now"** row (the programme on air on your favourites/recents) with **per-country EPG feed
prioritisation**, previous-channel zap, credentials encrypted at rest (AES-GCM with
an Android Keystore key), multi-language UI (English, Spanish, Catalan, Basque, Galician, Portuguese,
French, Italian — follows the device language, with an in-app picker), Room migrations (no destructive
wipe), EPG via XMLTV for M3U playlists, profile editing/renaming.

## Disclaimer

Easy Xtream Football is a **neutral client player** (like VLC): it does not include, host or
distribute any content or playlists. The user is solely responsible for the servers and
credentials they configure, the content they access, and for complying with applicable law.

## License

Distributed under **GPL-3.0**. See [LICENSE](LICENSE).

This software uses libraries from the **[FFmpeg](https://ffmpeg.org) project** under the **LGPLv3**,
bundled via **[NextLib](https://github.com/anilbeesetti/nextlib)** (GPL-3.0). Full third-party
attribution — FFmpeg, libvpx, Mbed TLS and the rest of the stack — is in
[THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
