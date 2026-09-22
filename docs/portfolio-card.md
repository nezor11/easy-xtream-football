# martinez.place — Card del portafolio para la app

Material para copiar en Sanity (slider "Portfolio"). Sigue la misma estructura que la card
**Easy Xtream Football Web** (`slide-easy-xtream-football-web`), que ya describe la landing;
esta describe la **app Android**. La URL de Play Store es definitiva (el paquete `com.footballxtream`
es permanente): https://play.google.com/store/apps/details?id=com.footballxtream

---

## Campos de la card

| Campo | Valor |
|---|---|
| `_id` | `slide-easy-xtream-football-app` |
| `name` | `Easy Xtream Football App` |
| `slideTitle` | `Easy Xtream Football` |
| `slideSummary` (EN) | `Open-source IPTV player for Android TV` |
| `slideSummary` (ES) | `Reproductor IPTV open source para Android TV` |
| `company` | `freelance` |
| `type` | `fresh` |
| `workDate` | `2026-09-01T12:00:00.000Z` (el primer commit es del 2026-06-15) |
| `backgroundColor` | `#0A0E12` (el mismo que la card de la landing) |
| `infoUrl` | `https://play.google.com/store/apps/details?id=com.footballxtream` |
| `icons` | `GitHubIcon`, `GitBranchIcon`. No existen iconos de Kotlin, Android ni Jetpack Compose en la web: conviene añadir `KotlinIcon`, `AndroidIcon` y `JetpackComposeIcon` al set y usarlos aquí |
| `slideImage` (300×350) | recorte vertical de `docs/store-assets/feature-1024x500.png` o del icono `icon-512.png` sobre fondo `#0A0E12` |
| `images` (1440×900) | recortes de `docs/store-assets/screenshots/01-channels.png` (canales), `02-profiles.png` (perfiles) y `04-add-profile.png` (alta de perfil) |
| `videoUrl` | `null` |
| `workDone` | `front_end`, `front_end_frameworks`, `ux_ui_design`, `performance_optimization`, `testing_debugging`, `security`, `version_control` |

Alt de las imágenes (EN / ES): `Easy Xtream Football channel grid on Android TV` / `Parrilla de canales de Easy Xtream Football en Android TV` · `Profile selector` / `Selector de perfiles` · `Add an Xtream or M3U profile` / `Alta de un perfil Xtream o M3U`.

---

## slideDesc — English

**Negrita** = marca `strong`; [texto](url) = marca `link` con su `markDef`.

[Easy Xtream Football](https://github.com/nezor11/easy-xtream-football) is a **free, open-source IPTV player** for Android TV, Google TV, Fire TV and phones, designed *TV-first*: everything works with the remote and the D-pad. It is built around live sport: it filters the sports channels of any **Xtream Codes account or M3U playlist**, groups the numbered variants of a channel into a folder with its logo and merges 4K/FHD/HD/SD feeds into a single logical channel. **The app is only the player**: it ships no content, users bring their own provider.

Written in **Kotlin with Jetpack Compose for TV** (MVVM, Room and DataStore for profiles, favourites and settings) on top of **Media3/ExoPlayer with an FFmpeg extension**, so AC-3, E-AC-3 and DTS audio still plays on devices that cannot decode it in hardware. Quality is chosen automatically from measured bandwidth and steps down on stalls, dead streams fall back to another variant, and a "Now / Next" guide comes from the Xtream API or from XMLTV. Credentials are encrypted at rest with the Android Keystore; no ads, no tracking, no accounts.

Localised in eighteen languages following the device language (English, Spanish, Catalan, Basque, Galician, Portuguese, French, Italian, German, Turkish, Polish, Arabic — with a right-to-left layout —, Indonesian, Vietnamese, Romanian, Greek, Croatian and Serbian), shipped in two flavours (a *full* build with the FFmpeg decoders and a 2.5 MB *lite* one) split per ABI, released under GPL-3.0 and distributed through Google Play. It has a companion [landing page](https://easy-xtream-football-web.vercel.app) that shares the same vector emblem as the launcher icon.

Available on [Google Play](https://play.google.com/store/apps/details?id=com.footballxtream).

---

## slideDesc — Español

[Easy Xtream Football](https://github.com/nezor11/easy-xtream-football) es un **reproductor IPTV gratuito y de código abierto** para Android TV, Google TV, Fire TV y móvil, diseñado *TV-first*: todo se maneja con el mando y la cruceta. Está pensado para el deporte en directo: filtra los canales deportivos de cualquier **cuenta de Xtream Codes o lista M3U**, agrupa las variantes numeradas de un canal en una carpeta con su logo y fusiona las señales 4K/FHD/HD/SD en un único canal lógico. **La app es solo el reproductor**: no incluye contenido, cada usuario conecta su propio proveedor.

Escrita en **Kotlin con Jetpack Compose for TV** (MVVM, Room y DataStore para perfiles, favoritos y ajustes) sobre **Media3/ExoPlayer con una extensión de FFmpeg**, de modo que el audio AC-3, E-AC-3 y DTS suena también en dispositivos que no lo decodifican por hardware. La calidad se elige automáticamente según el ancho de banda medido y baja de escalón cuando hay cortes, las señales caídas saltan a otra variante y la guía "Ahora / Después" llega por la API de Xtream o por XMLTV. Las credenciales se guardan cifradas con el Android Keystore; sin anuncios, sin seguimiento y sin cuentas.

Traducida a dieciocho idiomas siguiendo el idioma del dispositivo (inglés, español, catalán, euskera, gallego, portugués, francés, italiano, alemán, turco, polaco, árabe —con interfaz de derecha a izquierda—, indonesio, vietnamita, rumano, griego, croata y serbio), publicada en dos variantes (una *full* con los decodificadores de FFmpeg y una *lite* de 2,5 MB) separadas por ABI, bajo licencia GPL-3.0 y distribuida a través de Google Play. Tiene una [landing](https://easy-xtream-football-web.vercel.app) propia que comparte el mismo emblema vectorial que el icono de la app.

Disponible en [Google Play](https://play.google.com/store/apps/details?id=com.footballxtream).
