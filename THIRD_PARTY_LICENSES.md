# Third-party licenses

Easy Xtream Football is distributed under the **GPL-3.0** (see [LICENSE](LICENSE)). It builds on the
open-source components below; this file is the attribution their licenses require.

## FFmpeg — LGPL v3

Audio/video software decoding is provided by **[FFmpeg](https://ffmpeg.org)**, bundled through the
NextLib Media3 extension (below). The FFmpeg build used carries **no GPL-only and no non-free
components**: NextLib compiles it **without** `--enable-gpl` and **without** `--enable-nonfree`, and
**with** `--enable-version3`, so the bundled FFmpeg is licensed under the **GNU Lesser General Public
License, version 3 (LGPL-3.0)**.

- Copyright © the FFmpeg developers.
- License: **LGPL-3.0** — https://www.gnu.org/licenses/lgpl-3.0.html (LGPLv3 grants additional
  permissions on top of the GPLv3 text already shipped in [LICENSE](LICENSE)).
- Source code: https://ffmpeg.org/download.html — exact build recipe:
  https://github.com/anilbeesetti/nextlib/blob/main/ffmpeg/setup.sh
- **Relinking (LGPL §4):** FFmpeg ships as a **dynamic** shared library (`.so`, built with
  `--enable-shared`), so a user can replace it with a modified FFmpeg build without rebuilding the app.

External libraries enabled in that FFmpeg build:

- **libvpx** (VP8/VP9) — BSD-3-Clause — © Google / the WebM project — https://www.webmproject.org/
- **Mbed TLS** — Apache-2.0 — © Arm Limited and contributors —
  https://www.trustedfirmware.org/projects/mbed-tls/

## NextLib — GPL-3.0

`io.github.anilbeesetti:nextlib-media3ext`, the Media3/ExoPlayer FFmpeg decoder extension that bundles
the FFmpeg libraries above.

- Copyright © Anil Kumar Beesetti and contributors.
- License: **GPL-3.0** — https://github.com/anilbeesetti/nextlib

## Other components (Apache-2.0 / BSD / MIT)

Permissively licensed; attribution kept here for completeness:

- **AndroidX** — Core, Lifecycle, Activity, Navigation, Room, DataStore, **Media3 / ExoPlayer**,
  **Jetpack Compose for TV** — Apache-2.0 — © The Android Open Source Project
- **Kotlin**, **kotlinx.coroutines**, **kotlinx.serialization** — Apache-2.0 — © JetBrains s.r.o.
- **OkHttp**, **Retrofit** — Apache-2.0 — © Square, Inc.
- **Coil** — Apache-2.0 — © Coil Contributors
- Channel logos via the **iptv-org** database — https://github.com/iptv-org/database

---

The full **GPL-3.0** text is in [LICENSE](LICENSE). The **LGPL-3.0** and the permissive licenses above
are available at the URLs listed.
