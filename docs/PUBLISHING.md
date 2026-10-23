# Publicación en Google Play — estado y pasos

Checklist vivo del proceso de publicación de **Easy Xtream Football**.
Última actualización: 2026-10-12.

## Datos clave
- **Nombre de la app:** Easy Xtream Football
- **Nombre del paquete (permanente):** `com.footballxtream`
- **Cuenta de desarrollador:** Jorge Mtnez (personal) · contact@nezor.es
- **IDs Play Console:** developer `7549371768525842906` · app `4973475173609165901`
- **Play App Signing:** activado (el keystore local es la *clave de subida*).
- **AAB firmado (full, release):** `app/build/outputs/bundle/fullRelease/app-full-release.aab`
  - Regenerar con: `JAVA_HOME` al JBR → `gradlew :app:bundleFullRelease`
  - versionCode `4` · versionName `0.1.3` · minSdk **24 (Android 7)**.
- **Política de privacidad (URL):** https://github.com/nezor11/easy-xtream-football/blob/main/docs/privacy-policy.md

## Materiales de la ficha (en `docs/store-assets/`)
- Icono 512: `icon-512.png` · Gráfico destacado 1024×500: `feature-1024x500.png`
- Banner TV 1280×720: `tv-banner-1280x720.png`
- Capturas (TV/16:9, 1920×1080): `screenshots/01-channels · 02-profiles · 03-languages · 04-add-profile`
- Textos ES/EN: `docs/store-listing.md`

## Hecho ✅
- App creada en Play Console.
- AAB subido a **Prueba interna** (enlace opt-in: https://play.google.com/apps/internaltest/4701253984818511045).
- Declaraciones de **Contenido de la app** (10): política de privacidad, acceso (sin login), anuncios=No,
  clasificación de contenido (apta), audiencia 13+, seguridad de datos (no recoge ni comparte),
  gobierno/financiero/salud=No, categoría=Entretenimiento + contacto.
- **Ficha de Play Store** rellena (descripciones, icono, gráfico, capturas) — "lista para revisión".
- Cuenta de desarrollador **verificada**.
- **Build válido para Android TV** confirmado en el manifest (`leanback`, `touchscreen` no requerido,
  `LEANBACK_LAUNCHER`, banner). No hace falta recompilar para TV.
- Canal **Prueba cerrada – Alpha** montado: países = **Todo el mundo (177)**, notas de versión añadidas,
  app bundle 4 (0.1.3) en estado **Borrador** (NO enviado a revisión).

## Traspaso entre máquinas (2026-09-06)
- **Clave de subida:** `keystore.properties` + `keystore/easy-xtream-release.jks` están **solo en la máquina
  que subió 0.1.3–0.1.5** (no están en Gmail ni Drive). Copiarlos a la otra máquina (misma ruta, raíz del
  repo, gitignored) y guardar copia + contraseñas fuera de ambas. No pedir reinicio de clave: no se ha perdido.
- **0.1.6 (versionCode 7)** está en `origin/main` (commit `a2281da`): login con mando en TV (el teclado se
  abría al enfocar y OK insertaba texto), errores de conexión Xtream claros, credenciales sin autocorrección.
  **Pendiente:** en la máquina con la clave → `git pull` → `gradlew :app:bundleFullRelease` → verificar que el
  `.aab` NO va firmado con `CN=Android Debug` → subir a Prueba cerrada.
- **Play Console:** prueba cerrada Alpha activa desde 2026-08-18 (14 días cumplidos). Solicitud de acceso a
  producción enviada el 2026-09-06 a las 2:49 con respuestas que citan solo lo que hay en la 0.1.5. Esperar
  correo de Google (≤ 7 días). Si aprueban: Producción → nueva versión (promocionar 0.1.5 o subir 0.1.6) →
  países → enviar a revisión. Si rechazan: corregir el motivo del correo y reenviar.
- Al abrir el proyecto en cualquier máquina, pedir a Claude que lea este archivo antes de tocar Play.

## Estado a 2026-09-14 (máquina sin clave)
- ✅ **Acceso a producción CONCEDIDO** (visto en el Panel de Play Console el 2026-09-14). También se pueden
  abrir pruebas abiertas.
- ✅ **Primera versión de Producción ENVIADA A REVISIÓN el 2026-09-14**: bundle 6 (0.1.5) promocionado desde
  Prueba cerrada Alpha, lanzamiento completo, 177 países/regiones (176 + resto del mundo), publicación
  gestionada desactivada. Canal Producción: *Activo · En revisión*. Google avisa de hasta 7 días.
  Cuando se apruebe se publicará sola en https://play.google.com/store/apps/details?id=com.footballxtream
- Tras la aprobación: subir **0.1.6 firmada** (máquina con la clave) como actualización de Producción
  (arregla el login con el mando en TV) y sustituir `{{PLAY_URL}}` en `docs/portfolio-card.md` y en la landing.
- `origin/main` incluye 0.1.6 (`a2281da`) y la card del portafolio (`b0c021c`). Working tree limpio.
- Sigue pendiente, en la máquina con la clave: firmar y subir 0.1.6 a Prueba cerrada, y guardar copia de la clave
  fuera de ambas máquinas. Anotar aquí el resultado y hacer commit + push.

## Revisión del estado a 2026-09-16 (máquina sin clave)
- ⏳ **0.1.5 sigue "En revisión"**: en *Actividad de envíos* figura el envío nº 6 (Producción, bundle 6 / 0.1.5)
  enviado el 2026-09-14 a las 20:44. Menos de dos días; Google avisa de hasta 7 días y el primer lanzamiento en
  producción de una cuenta personal suele tardar más que las actualizaciones. **Nada que hacer mientras dure.**
- Comprobado y correcto: publicación gestionada desactivada (se publica sola en los 177 países al aprobarse),
  *Contenido de la aplicación* al día sin declaraciones pendientes, centro de notificaciones vacío (ni rechazo ni
  petición de información), bundle 6 con **SDK objetivo 36** (cumple el requisito de API vigente).
- Aviso en *Estado según las políticas* ("la app debe estar orientada a Android 16 / API 36; las actualizaciones
  con este problema se rechazarán"): afecta **solo al bundle 4 (0.1.3, SDK objetivo 35)**, aún activo en Prueba
  interna desde junio. No afecta al envío de producción. Desaparecerá al publicarse en producción una versión que
  cumpla (la 0.1.5). Opcional: retirar 0.1.3 de Prueba interna para dejarlo limpio; no es necesario.
- **Si el 2026-09-21 sigue "En revisión"**, contactar con el soporte de Play Console desde *Ayuda*.
- **Ficha: materiales de Android TV GUARDADOS pero NO ENVIADOS (2026-09-16, por navegador):**
  - La ficha no tenía sección Android TV. En *Ajustes avanzados → Factores de forma* solo estaba Android XR; se
    añadió **Android TV** (queda en configuración, **sin habilitar**: falta "Habilitar Android TV y aceptar la
    política de reseñas"). Se quita con *Eliminar* en esa pantalla si no se quiere.
  - En la ficha principal: **Banner de TV 1/1** (`tv-banner-1280x720.png`) y **capturas de TV 4/8** (las de la
    biblioteca del 23 de junio, 01→04, 1920×1080). Guardado sin errores y verificado tras recargar.
  - En *Resumen de publicación* figuran **2 cambios sin enviar**. Al pulsar "Enviar" la consola avisa: *"¿Quieres
    reiniciar tu revisión? Tienes una revisión en curso desde el 14 de septiembre; se cancelará y se reiniciará"*.
    **Cancelado: no enviar nada hasta que la 0.1.5 esté aprobada.** Los cambios quedan guardados.
  - Actividad de envíos: solo el nº 6. No hay nº 7.
  - **Plan:** tras la aprobación de 0.1.5 → subir **0.1.6 firmada** (arregla el login con el mando, que es justo lo
    que prueba la revisión de TV) → habilitar Android TV y aceptar la política → enviar todo junto a revisión.
    Al habilitar Android TV la consola pregunta cómo gestionar los lanzamientos de TV: elegir **"usar el mismo
    canal y artefactos que la app móvil"** (un solo bundle sirve para móvil y TV; mismos testers y países), no
    canales separados.
- **0.1.6 verificada en esta máquina (2026-09-16):** `gradlew :app:testFullReleaseUnitTest :app:bundleFullRelease`
  → BUILD SUCCESSFUL, 59 tests unitarios OK. El `.aab` sale con `CN=Android Debug` (aquí no hay clave), como se
  esperaba. En la máquina con la clave solo falta firmar y subir.
- **Notas de versión de 0.1.6** (copiar en Play al crear la versión; máx. 500 caracteres por idioma):
  - es-ES:
    ```
    <es-ES>
    • Android TV: el teclado ya no se abre al enfocar un campo con el mando; solo al pulsar OK. Se acabó el login que fallaba sin motivo.
    • Errores de conexión Xtream claros (credenciales, DNS, tiempo de espera, TLS, HTTP).
    • Usuario y contraseña sin autocorrección ni sugerencias del teclado.
    • En TV la contraseña se muestra por defecto, con opción de ocultarla.
    • La URL del servidor se limpia sola (query y /player_api.php).
    </es-ES>
    ```
  - en-US:
    ```
    <en-US>
    • Android TV: the keyboard no longer opens when focusing a field with the remote; only on OK. No more logins failing for no reason.
    • Clear Xtream connection errors (credentials, DNS, timeout, TLS, HTTP).
    • Username and password without keyboard autocorrect or suggestions.
    • On TV the password is shown by default, with an option to hide it.
    • The server URL is cleaned up automatically (query and /player_api.php).
    </en-US>
    ```
- **Repaso de calidad Android TV sobre el código (2026-09-16):** manifest, banner (640×360 nodpi), foco inicial en
  cada pantalla, indicador de foco, Back, pausa en segundo plano, audio focus y enlaces externos protegidos: OK.
  Dos mejoras opcionales antes de la revisión de TV: (1) márgenes de overscan (Google recomienda 48 dp laterales /
  27 dp arriba-abajo; Canales, Perfiles, Alta de perfil y los overlays del reproductor usan 20 dp), (2) teclas
  multimedia del mando (Play/Pause, Stop, Canal +/-) sin gestionar en el reproductor.
- **0.1.6 probada en el Chromecast con Google TV (2026-09-16, adb por red, IP 192.168.1.82):**
  - ✅ Login con el mando: el teclado NO se abre al enfocar un campo, solo al pulsar OK; ya no se cuela ninguna
    letra; Atrás cierra el teclado conservando el texto; arriba/abajo cambian de campo sin abrir el teclado.
  - ✅ Login real con cuenta Xtream: 712 canales cargados. El Chromecast queda con 0.1.6 (build debug de esta
    máquina) y el perfil restaurado.
  - ✅ Reproductor: reproduce, menú OK y overlay de info correctos. Foco inicial visible en cada pantalla.
  - ⚠️ **Bug de UX a corregir antes de la revisión de TV:** al fallar el login (p. ej. DNS) el mensaje de error se
    muestra encima del botón, pero el foco salta al campo URL y la pantalla sube, de modo que **el error queda
    fuera de la vista**. Causa probable: el botón se deshabilita mientras conecta y pierde el foco. Además el
    error se pinta en verde (color primario) en vez de en un color de error.
  - ℹ️ Overscan confirmado: en Canales el título queda a 20 dp del borde izquierdo (40 px en 1080p).
  - ✅ **Corregido en `main` el 2026-09-17** (entra en 0.1.6, aún sin firmar): error de login visible (el botón
    ya no pierde el foco) y en color de error; área segura de overscan en TV (`Modifier.tvSafeArea()`, 48 dp
    laterales) en Canales, Perfiles, Alta de perfil y overlays del reproductor; teclas multimedia del mando en
    el reproductor (Play/Pause con indicador, Stop sale, Canal +/-). Compila y pasa los 59 tests.
    ✅ **Probado en el Chromecast el 2026-09-20** (ver más abajo).
- **Ramas preparadas (NO mezclar hasta que la app esté publicada)**, hechas el 2026-09-16:
  - `feature/play-url` (este repo): `docs/portfolio-card.md` con la URL definitiva de Play en vez de `{{PLAY_URL}}`.
  - `feature/play-store-link` (repo `easy-xtream-football-web`): botón "Descargar en Google Play" en el hero y
    enlace en el footer; lint y build OK. Al mezclar en `main`, Vercel despliega solo.

## Revisión del estado a 2026-09-18
- ⏳ **0.1.5 sigue en revisión** (Resumen de publicación → *Cambios en revisión*: Producción 6 (0.1.5), lanzamiento
  completo, 176 países + resto del mundo). Sin rechazo ni petición de información.
- *Cambios sin enviar*: capturas de TV, banner de TV y factor de forma Android TV. **No enviar** hasta la aprobación
  (reiniciaría la revisión).
- Prueba abierta vacía (no se creó ninguna versión; no hace falta, el acceso a producción ya está concedido).
- Si el 2026-09-21 sigue en revisión → soporte de Play Console.

## Revisión del estado a 2026-09-20
- ⏳ **Sin cambios**: 0.1.5 sigue en revisión (Producción 6 (0.1.5), lanzamiento completo, 176 países + resto del
  mundo). Sin rechazo ni petición de información. Van 6 días desde el envío (2026-09-14).
- *Cambios sin enviar* siguen sin enviar (capturas de TV, banner de TV, factor de forma Android TV): correcto,
  enviarlos ahora reiniciaría la revisión.
- Última publicación efectiva: 2026-09-07. Publicación gestionada desactivada.
- ➡️ **Mañana 2026-09-21: si sigue en revisión, abrir incidencia en soporte de Play Console.**

### Correcciones de TV verificadas en el Chromecast (2026-09-20)
Chromecast con Google TV (`sabrina`, 1920x1080, densidad 320 → 1 dp = 2 px), adb por *depuración
inalámbrica* (`adb pair` con código; el puerto cambia en cada reinicio del dispositivo).
APK debug `full/armeabi-v7a` de 0.1.6 **con** el commit `59632a4` (la instalada era del 16, anterior al fix).
Ojo: este dispositivo es **armeabi-v7a**, la APK arm64 da `INSTALL_FAILED_NO_MATCHING_ABIS`.
- ✅ **Área segura de overscan**: en Canales el título arranca en x = 96 px = **48 dp** (antes 40 px / 20 dp).
  Perfiles y los overlays del reproductor también dentro del área segura.
- ✅ **Error de login visible y en color de error**: con una URL inexistente sale
  *"No se llega al servidor (DNS). Revisa la URL o prueba otro host de tu proveedor."* en **rojo**, a la vista,
  y **el botón conserva el foco** (la pantalla ya no se desplaza dejando el error fuera de cuadro).
  Queda cerrado el bug de UX del 2026-09-16.
- ✅ **Teclas multimedia del mando**: Play/Pause pausa y reanuda con indicador "Pausado"; Canal + pasa de
  1/39 a 2/39 y Canal − vuelve a 1/39, reproduciendo en ambos; Stop sale del reproductor a Canales.
- ✅ Sin regresiones: perfil conservado, 712 canales, teclado que solo se abre con OK, foco inicial visible
  en cada pantalla, sin crashes en logcat.
- **Conclusión: 0.1.6 está lista para firmar y subir** en cuanto se apruebe la 0.1.5.

### Prueba de esfuerzo con lista M3U grande (Chromecast, 2026-09-20)
Se dio de alta un perfil **Lista M3U** con la lista del proveedor (11 MB, **51.033 entradas**, 82% VOD/series).
- ✅ **La app aguanta**: sin crash, sin ANR y sin OutOfMemory en logcat.
- ✅ **El filtro de deporte funciona también en M3U**: de 51.033 entradas deja **715 canales** de directo,
  prácticamente los mismos 712 que da la API Xtream.
- 📈 **Memoria**: pico de ~235 MB PSS durante el parseo (a los ~28 s), estabilizado en ~147 MB. Tarda ~45 s
  en total en un Chromecast con Google TV.
- ✅ **Logos que faltaban: MEJORADO en `main` el 2026-09-22** (entra en 0.1.7). Con datos reales: la M3U del
  proveedor no trae **ningún** `tvg-logo` y todos sus `tvg-id` valen `ext`, así que el único origen es la base
  de iptv-org por nombre, que solo casaba el nombre exacto (M3U 302/715, Xtream 374/712). Ahora
  `LogoMatching` prueba variantes léxicas (sport/sports, "TV" final), luego el nombre sin sufijo (feed, región)
  y sin prefijo (país, proveedor) con guardas para no coger logos ajenos, y lo que queda sin logo toma el de un
  hermano de su carpeta. Simulado sobre las cachés del Chromecast: **M3U 578/715 (81%) y Xtream 625/712
  (88%)**, revisados uno a uno los casos difusos. Verificado en la tele. 6 tests nuevos, 69 en total.
- ✅ **Favoritos 39 → 34 entre perfiles: CORREGIDO en `main` el 2026-09-22** (entra en 0.1.7). La clave de
  favoritos es el nombre normalizado (`ChannelGroup.key`), no el id. Los 5 perdidos eran canales "solo
  eventos": la API los nombra `… (SOLO EVENTOS)` y la M3U `…  SOLO EVENTOS`; el parser quitaba lo que va
  entre paréntesis pero no la coletilla suelta. Ahora la etiqueta de disponibilidad se elimina en ambas
  formas (solo como frase completa) y `CACHE_VERSION` pasa a 18. Verificado en el Chromecast con las cachés
  reales: M3U pasa de 34 a **39/39 favoritos**, 715 grupos antes y después (ninguna fusión indebida), claves
  comunes Xtream/M3U 620 → 630, y Xtream idéntico (712 claves iguales una a una). 4 tests nuevos, 63 en total.
- ✅ **Regresión de TV corregida en `main` el 2026-09-22** (entra en 0.1.7): la **pulsación larga de OK en las
  tarjetas** (reordenar favoritos, menús de canal y carpeta) había dejado de funcionar en TV desde el soporte
  táctil (`414450f`), que cambió la `Card` de tv.material3 por un `combinedClickable` de Foundation, que solo
  detecta la pulsación larga con el dedo: con el mando abría el reproductor. Ahora la tarjeta trata las
  teclas de selección ella misma (primera repetición = pulsación larga; soltar sin ella = clic). Verificado
  en el Chromecast: larga → modo reordenar con ‹ › y barra Hecho/Quitar; ◀▶ mueven la tarjeta; corta →
  reproductor. **La 0.1.6 enviada a revisión lleva este fallo**; no bloquea nada, va en la siguiente.
  El mismo fallo afectaba a la **pantalla de perfiles** (mantener OK para editar/eliminar): corregido con el
  mismo modificador compartido (`Modifier.remoteCombinedClickable`, en `ui/components/RemoteClick.kt`) y
  verificado en el Chromecast (larga → menú Editar/Eliminar/Cancelar; corta → abre el perfil).
- ℹ️ Del proveedor: de los 3 hosts M3U solo responde uno; otro ya es un **dominio aparcado con publicidad**.
  Igual que en Xtream, los hosts son espejos de una sola cuenta con **1 conexión simultánea**.

## 🎉 PUBLICADA EN PRODUCCIÓN — 2026-09-21
- ✅ **Envío nº 6 (Producción, bundle 6 / 0.1.5) APROBADO Y PUBLICADO el 2026-09-21 a las 9:50** (enviado el
  2026-09-14 a las 20:44 → **7 días de revisión**). Estado en *Resumen de publicación*: **Publicada**,
  lanzamiento completo, 176 países/regiones + resto del mundo.
- ✅ **Ficha viva y comprobada** (HTTP 200, muestra "Easy Xtream Football" y la versión **0.1.5**):
  https://play.google.com/store/apps/details?id=com.footballxtream
- ✅ **Lanzamiento completo al 100%, comprobado en la consola el 2026-09-21** (*Producción → Versiones*):
  la versión 6 (0.1.5) figura como **"Disponible en Google Play"**, 1 código de versión, publicada el
  21 sept a las 9:50, **177 países** y **22.215 dispositivos compatibles**. **No hay porcentaje de
  despliegue por fases**: el "Iniciar lanzamiento completo" que se ve en el resumen es la *descripción de
  uno de los 3 cambios que se enviaron* en el envío nº 6, no un botón pendiente de pulsar. Primeras
  **2 descargas**.
- ⏳ **Quedan 3 cambios sin enviar** (los materiales de Android TV guardados el 2026-09-16). La consola ya
  dice *"Tus cambios ya se pueden enviar a revisión"*, sin el aviso de reiniciar la revisión: al no haber
  revisión en curso, enviarlos ya no cancela nada. Aun así, **esperar y enviarlos junto con la 0.1.6**
  (ver el plan de abajo), para que la revisión de TV vea ya los arreglos del mando.
- ℹ️ **Acceso a la consola desde la máquina sin clave:** en el Chrome de esta máquina la cuenta `u/0` es
  `martinezortiz@gmail.com` y la consola redirige a *crear cuenta de desarrollador*. La cuenta de
  desarrollador (`contact@nezor.es`) es **`u/1`**: usar URLs con `/console/u/1/...`.
- ❌ Ya **no** hace falta abrir incidencia en el soporte de Play Console.
- ✉️ **IARC Live Rating Notice** recibido el 2026-09-21 a las 9:51 (un minuto después de publicarse): la
  clasificación por edades generada por el cuestionario ya está activa. **No requiere ninguna acción.**
  - El **Global Rating ID** está en ese correo (de `noreply@globalratings.com`, asunto *IARC Live Rating
    Notice: Easy Xtream Football*). No se copia aquí a propósito: este `docs/` es público en GitHub y ese
    identificador es lo que sirve para reclamar la clasificación en otra tienda.
  - Solo hace falta **si algún día se publica en otra tienda con licencia IARC** (Amazon Appstore, Galaxy
    Store…): se pega el ID durante el alta y se reutiliza la clasificación.
  - Solo habría que **rehacer el cuestionario** si un cambio alterase las respuestas (anuncios, compras,
    contenido generado por usuarios…). La **0.1.6 no cambia ninguna**: son arreglos de TV.

### Siguientes pasos (en este orden) — pasos 1-3 HECHOS el 2026-09-21, ver la sección de la 0.1.6 más abajo
1. **En la máquina con la clave de subida** (`keystore.properties` + `keystore/easy-xtream-release.jks`;
   esta máquina NO la tiene): `git pull` → `gradlew :app:testFullReleaseUnitTest :app:bundleFullRelease` →
   comprobar que el `.aab` **no** va firmado con `CN=Android Debug` → crear versión de **Producción** con el
   bundle 7 (0.1.6) y las notas de versión de más abajo.
2. **Habilitar Android TV** en *Ficha → Ajustes avanzados → Factores de forma* y aceptar la política de
   reseñas. Cuando pregunte cómo gestionar los lanzamientos de TV, elegir **"usar el mismo canal y artefactos
   que la app móvil"** (un solo bundle para móvil y TV), no canales separados.
3. **Enviar todo junto a revisión**: 0.1.6 + banner de TV + capturas de TV + factor de forma Android TV.
   La 0.1.6 arregla justo lo que mira la revisión de TV (login con el mando, overscan, teclas multimedia).
4. ✅ **Ramas preparadas MEZCLADAS el 2026-09-21** (ambas borradas en local y en `origin`):
   - `feature/play-url` (este repo, merge `f7389bc`): la URL de Play en `docs/portfolio-card.md`; ya no queda
     ningún `{{PLAY_URL}}`. En `8af12cb` se quitaron además las notas de "solo cuando la app esté publicada",
     que ya no aplican: la card se puede copiar tal cual en Sanity.
   - `feature/play-store-link` (repo `easy-xtream-football-web`, merge `857f0c2`): CTA "Descargar en Google
     Play" / "Get it on Google Play" en el hero (GitHub pasa a botón secundario) y enlace en el footer.
     `next lint` sin avisos y `next build` OK antes de subir. **Desplegado en Vercel y verificado en vivo**
     en https://easy-xtream-football-web.vercel.app/es y `/en`.
5. Opcional: retirar el bundle 4 (0.1.3, SDK objetivo 35) de **Prueba interna** para que desaparezca el aviso
   de *Estado según las políticas* sobre la API 36.

## 0.1.6 ENVIADA A REVISIÓN — 2026-09-21 (máquina con la clave)
- ✅ **Compilada y firmada en la máquina con la clave**: `git pull` →
  `gradlew :app:testFullReleaseUnitTest :app:bundleFullRelease` → BUILD SUCCESSFUL, **59 tests, 0 fallos**.
  Firma comprobada con `keytool -printcert -jarfile`: `Owner: CN=Jorge Mtnez, OU=IT, O=nezor, L=Valencia,
  ST=Valencia, C=ES` (certificado de subida, **no** `CN=Android Debug`). AAB de 16 MB.
- ✅ **Versión de Producción creada**: bundle **7 (0.1.6)**, targetSDK 36, minAPI 24. Notas de versión en
  es-ES (424 car.) y en-US (416 car.). Lanzamiento completo al 100 %, 177 países, publicación gestionada
  desactivada (se publica sola al aprobarse). El bundle 6 (0.1.5) queda desactivado al reemplazarlo.
  Tamaño: 4,83 MB descarga nueva / 2,15 MB actualización. Mismos dispositivos compatibles que la 0.1.5.
- ✅ **Android TV habilitado** en *Ajustes avanzados → Factores de forma*: estado **Activo**, con **"mismo canal y
  artefactos que la app móvil"**. La activación se aplicó directamente: no pasa por la cola de revisión (la fila
  desapareció del resumen al enviar; comprobado después que sigue *Activo*).
- ✅ **Enviado a revisión el 2026-09-21** en un solo envío, **3 elementos en revisión**: Producción 7 (0.1.6),
  capturas de pantalla de Android TV (es-ES) y banner de TV (es-ES). Mensaje: *"Tus cambios están en proceso
  de revisión."*
- ℹ️ La subida del `.aab` se hace a mano con el selector de archivos: la subida directa del addon de Chrome
  tiene un tope de 10 MB y el bundle pesa 16 MB.
- ℹ️ Advertencia no bloqueante ignorada a propósito: *código nativo sin símbolos de depuración*. Las libs de
  FFmpeg (NextLib) y androidx vienen ya sin símbolos desde su autor; con `ndk.debugSymbolLevel` la extracción
  sale vacía (comprobado el 2026-09-02). No hay nada que subir.
- ⏳ **Esperar el correo de Google.** Referencia: la 0.1.5 tardó 7 días; con la revisión de TV puede tardar más.
  Si rechazan la parte de TV, la 0.1.5 publicada sigue viva: corregir el motivo y reenviar.
- ✅ **Copia de la clave de subida HECHA el 2026-09-22**: los dos ficheros (`keystore.properties` +
  `keystore/easy-xtream-release.jks`) van en un archivo **7-Zip cifrado con cabeceras cifradas** (`-mhe=on`,
  sin contraseña no se listan ni los nombres) en un **USB offline** (el del live de Fedora, que sigue arrancando).
  La contraseña del `.7z` es distinta de las del keystore y **no está ni en el USB ni en ningún repo**.
  Para llevar la clave a otra máquina: copiar el `.7z` del USB y, en la raíz del repo, `7z x clave-easy-xtream.7z`
  (deja los dos ficheros en su ruta; en Linux hace falta `p7zip`). Después comprobar la firma con
  `keytool -printcert -jarfile` → debe salir `CN=Jorge Mtnez`, no `CN=Android Debug`.

### Clave de subida restaurada en la máquina Linux (2026-09-22)
- `7z x /run/media/jmtnez/FEDORA-WS-L/clave-easy-xtream.7z -o<raíz del repo>` deja `keystore.properties` y
  `keystore/easy-xtream-release.jks` en su ruta. **Hay que hacerlo en una terminal real** (aquí, Ptyxis): el
  prompt de contraseña de 7-Zip no funciona a través del `!` de Claude Code, se corta con *"Break signaled"*.
  No hace falta editar nada: la ruta de `storeFile` es relativa y vale igual viniendo de Windows.
- Comprobado: Git ignora los dos ficheros (`*.jks` y `keystore.properties` en `.gitignore`) y el árbol
  sigue limpio.
- `gradlew :app:testFullReleaseUnitTest :app:bundleFullRelease` → BUILD SUCCESSFUL, **59 tests, 0 fallos**
  (relanzados con `--rerun`, no de la caché). AAB de 16.431.622 bytes.
- **Firma verificada, idéntica a la del Windows:** `Owner: CN=Jorge Mtnez, OU=IT, O=nezor, L=Valencia,
  ST=Valencia, C=ES`, válida hasta 2053-11-03, `SHA256: E9:9B:9E:F2:...:F8:F9`. **No** es `CN=Android Debug`.
- Entorno de esta máquina: SDK Platform 36 y **JDK 25** (no hace falta el 17, compila igual).

## 0.1.7 PREPARADA — 2026-09-22 (máquina Linux, ya con la clave)
- `versionCode` **8** · `versionName` **0.1.7**. Lleva los 4 arreglos del 2026-09-22 (favoritos entre perfiles,
  pulsación larga de OK en canales y perfiles, logos), probados en el Chromecast, **y el trabajo de móvil del
  mismo día**, probado en el Xiaomi del usuario (Android 16, arm64): gestos en el reproductor (deslizar ◀▶
  canal / ▲▼ calidad, toque = menú, opciones del menú tocables), chevrones ‹ › tocables para reordenar
  favoritos, y botón propio `AppButton` (dedo + mando) en vez del `Button` de tv.material3, que en móvil dejaba
  pasar el toque a lo que hubiera debajo (menú de perfil → abría el perfil en vez de editarlo).
  Ojo: en ese Xiaomi (HyperOS) `adb shell input` está bloqueado (`INJECT_EVENTS`); las pruebas táctiles las
  hace el usuario a mano y se verifican con `screencap`.
- **16 idiomas nuevos (2026-09-22)** → **24 en total**. Bloque 1: alemán, turco, polaco y árabe
  (`values-de/-tr/-pl/-ar`). Bloque 2: indonesio (`values-in`, código Android heredado; tag `id`), vietnamita,
  rumano, griego, croata y serbio latino (`values-b+sr+Latn`, tag `sr-Latn`; croata y serbio son ficheros
  distintos porque el léxico difiere: nogomet/fudbal, poslužitelj/server…). Bloque 3: tailandés, chino
  tradicional (`values-zh-rTW`, tag `zh-TW`: en China continental no hay Play, el público de Play es Taiwán,
  Hong Kong, Malasia y Singapur), ruso, neerlandés, albanés e hindi. 105 cadenas cada uno, plurales
  completos según el lint (rumano 3 formas, croata/serbio 3, ruso 4, árabe 6, polaco 4), `LocaleHelper.supportedTags`,
  autónimos en el selector y columnas en `translations.csv`. **Árabe verificado en RTL** en el móvil (perfiles,
  parrilla, reproductor, formulario): Compose invierte los layouts solo gracias a `supportsRtl`. Griego y
  tailandés verificados en pantalla como muestra de alfabetos no latinos. Al publicar la 0.1.7: ficha de Play con
  "24 idiomas" (`docs/store-listing.md` ya lo dice) y mezclar la rama `feature/12-languages` del repo web (ya dice 24).
- **Decisión pendiente: cuándo subirla.** La 0.1.6 sigue en revisión; crear otra versión de Producción la
  sustituye y reinicia la revisión. Nota: la 0.1.5 publicada (aprobada por Google) ya llevaba el fallo de la
  pulsación larga, así que no es motivo de rechazo conocido. Opción prudente: esperar el veredicto de la 0.1.6
  y subir la 0.1.7 justo después como actualización.
- **Notas de versión de 0.1.7** (máx. 500 caracteres por idioma):
  ```
  <es-ES>
  • Los favoritos se conservan entre el perfil Xtream y la lista M3U del mismo proveedor.
  • Android TV: vuelve a funcionar mantener OK sobre un canal, carpeta o perfil (reordenar favoritos, menús, editar).
  • Muchos más logos de canal, también en listas M3U sin logos.
  • Móvil: desliza ◀▶ para cambiar de canal y ▲▼ la calidad; toca para el menú. Menús de perfil y favoritos ya responden al dedo.
  • 16 idiomas nuevos, del alemán, el árabe o el turco al hindi, el tailandés o el chino tradicional.
  </es-ES>
  ```
  ```
  <en-US>
  • Favorites carry over between the Xtream profile and the M3U playlist of the same provider.
  • Android TV: holding OK on a channel, folder or profile works again (reorder favorites, menus, edit).
  • Many more channel logos, also for M3U playlists that ship none.
  • Phone: swipe ◀▶ to change channel and ▲▼ for quality; tap for the menu. Profile and favorites menus respond to touch.
  • 16 new languages, from German, Arabic or Turkish to Hindi, Thai or Traditional Chinese.
  </en-US>
  ```

## 0.1.8 PREPARADA — 2026-09-24 (máquina Linux)
- `versionCode` **9** · `versionName` **0.1.8**. Todo en `main` (commits del 2026-09-24). Firmados con la clave de
  subida (`CN=Jorge Mtnez`, SHA-256 `E9:9B:9E:F2:…`), copias listas para subir en `~/Downloads/easy-xtream-0.1.8/`:
  - Play: `easy-xtream-football-0.1.8-vc9.aab` (16,5 MB) = `app/build/outputs/bundle/fullRelease/app-full-release.aab`
  - Amazon: `easy-xtream-football-0.1.8-vc9-universal.apk` (26,8 MB) = `app/build/outputs/apk/full/release/app-full-universal-release.apk`
  - Regenerar: `./gradlew :app:bundleFullRelease :app:assembleFullRelease` (JDK 25 + SDK 36 en esta máquina).
- Contenido (probado en el Xiaomi por el usuario y en el Chromecast por adb):
  - **Radio**: emisoras marcadas en la lista (`radio="true"`, `stream_type=radio_streams`, categoría "Radio") o
    detectadas al reproducir sin pista de vídeo → pantalla con logo/icono, nombre y "Sonando: …" (ICY; casi todas
    las radios de tertulia lo mandan vacío y se muestra "Solo audio"). Icono de radio en las tarjetas.
  - **Pausa**: doble OK en el mando / doble toque en el móvil. En pausa, un solo OK o toque reanuda sin abrir el
    menú. La tecla ⏯ física sigue funcionando.
  - **Listas de ejemplo**: botón "Probar con listas de ejemplo" en el primer arranque y en la pantalla de perfiles
    vacía. Crea "Demo · Deporte en abierto" (iptv-org `categories/sports.m3u`) y "Demo · Radios deportivas"
    (`docs/playlists/sports-radio.m3u` de este repo, 14 emisoras de 9 países con streams oficiales). **La app sigue
    arrancando vacía**: decisión del usuario para no romper el argumento de "solo reproductor" ante las revisiones.
  - Filtros de la parrilla desplazables en horizontal (en el móvil en vertical no cabían).
- **Notas de versión de 0.1.8** (máx. 500 caracteres por idioma):
  ```
  <es-ES>
  • Radio: las emisoras de tu lista se ven con su logo, nombre y lo que está sonando.
  • Pausa: pulsa OK dos veces (o toca dos veces en el móvil). En pausa, un solo OK reanuda.
  • Listas de ejemplo: si aún no tienes proveedor, prueba la app con dos listas públicas de emisiones en abierto (deporte y radios deportivas), que puedes borrar cuando quieras.
  • Móvil: los filtros de la parrilla se desplazan en vertical.
  • Y todo lo de la 0.1.7: favoritos compartidos, más logos, gestos táctiles y 24 idiomas.
  </es-ES>
  ```
  ```
  <en-US>
  • Radio: stations in your playlist show their logo, name and what's on air.
  • Pause: press OK twice (or double-tap on a phone). While paused, a single OK resumes.
  • Sample playlists: no provider yet? Try the app with two public free-to-air playlists (sports TV and sports radio); delete them whenever you like.
  • Phone: the grid filters scroll in portrait.
  • Includes everything from 0.1.7: shared favorites, more logos, touch gestures and 24 languages.
  </en-US>
  ```
- **Ficha**: ✅ Play es-ES cambiada el 2026-09-26 (párrafo IMPORTANTE + "24 idiomas"). Play no tiene ficha en-US
  (pendiente crearla). ✅ Amazon (ES/EN): párrafo cambiado el 2026-09-29 al enviar la 0.1.8.

## 0.1.9 — Play Billing para el café (código listo el 2026-09-25, rama `feature/play-billing-coffee`)
- **2026-09-27 · versionCode 12** en Prueba interna: vuelve el **QR**, ahora hacia la landing
  (`qr_site.png` → https://easy-xtream-football-web.vercel.app, que desde ese día tiene sección propia de
  "Invítame a un café"). En la tarjeta del reproductor va al lado de la taza con el dominio debajo; en la
  sección Café crece a 148 dp; en Ajustes va junto a los precios. Verificado en el Chromecast con el APK
  firmado por Google. ⚠️ Con la vc12 el QR de 220 dp apilado dejaba el interruptor del recordatorio fuera
  de pantalla en Ajustes.
- **2026-09-27 · versionCode 13** (AAB firmado en `~/Downloads/easy-xtream-0.1.9-vc13/`), tras ver la vc12 en
  la tele: el **QR va primero** y la **taza a su derecha, ambos a 88 dp**, sin la URL escrita debajo; en la
  sección Café abierta el QR sube a 148 dp y la taza no sale; **Ajustes ya no muestra el QR cuando hay
  precios** (solo sin facturación, Fire TV), lo que además resuelve el desbordamiento de la vc12. QR
  regenerado con corrección **L** (29 módulos en vez de 33, cuadros 14 % más grandes; decodifica hasta
  130 px). ✅ **Publicada en Prueba interna el 2026-09-27 (05:35)**.
  ✅ **Verificada en el Chromecast el 2026-09-27 (13:55)** con el APK universal firmado por Google
  (`~/Downloads/13.apk`, Explorador de app bundles → 13 → Descargas → *APK firmado y universal*, instalado con
  `adb install -r -i com.android.vending`). La tarjeta sale con el QR a la izquierda y la taza a su derecha,
  sin URL debajo, y el QR **decodifica desde la captura de 1080p** (`https://easy-xtream-football-web.vercel.app`),
  tanto recortado como en el fotograma completo: a 176 px de 1920 (≈9 % del ancho) se escanea sin problema.
  ✅ **Confirmado por el usuario en su televisor el 2026-09-27**: *"se ve bien, el QR se escanea desde el sofá"*.
  El tamaño del QR queda cerrado: no hay que agrandarlo.
  ✅ Rama `feature/qr-landing` fusionada en `main` (bdfa1c3) ese mismo día.

- **2026-09-27 · versionCode 11** en Prueba interna: el recordatorio del café es ahora un botón (OK con el mando o
  un toque abre la sección Café con los precios) y lleva **un solo icono**: una taza dibujada (`ic_coffee`) en vez
  del ☕ repetido tres veces en la tarjeta y tres más en los botones de Ajustes. Verificado en el Chromecast con el
  APK firmado por Google (`~/Downloads/11.apk`): la tarjeta sale con una sola taza y **OK sobre ella abre la
  sección Café con los tres precios** (confirmado por el usuario el 2026-09-27).
  ⚠️ **Al probar con `adb shell input keyevent` se completó una compra de prueba no intencionada**
  (GPA.3330-3201-9075-08273, "Café y tostada", 4,99 EUR, de prueba → 0 €) porque una pulsación de OK cayó sobre la
  lista de cafés. **No pulsar OK a ciegas en el reproductor**: hacer captura antes de cada OK, o dejar que el
  usuario haga la interacción final. La cuenta pide además PIN de compra de Play.

- **Qué hace la app.** Al arrancar conecta con Google Play (`CoffeeBilling`, librería `com.android.billingclient:billing`
  8.3.0, la base Java porque la `-ktx` exige Kotlin 2.2 y el proyecto va en 2.0; lee la opción de compra con
  `oneTimePurchaseOfferDetailsList`, el modelo nuevo de Play, con el accesor antiguo de respaldo). Pide los productos consumibles
  `coffee_small`, `coffee_medium`, `coffee_large`. Si los obtiene: la sección **Café** del menú OK del reproductor
  lista los cafés con nombre y precio tal como estén en Play Console (localizados por Google), OK abre la hoja de
  pago de Play; el panel "Invítame a un café" de **Ajustes** muestra un botón por café. Tras pagar: consumo
  inmediato (se puede repetir), aviso "¡Gracias por el café!" y el recordatorio se silencia para siempre (se puede
  reactivar en Ajustes). Si Play no está (Fire TV, `Build.MANUFACTURER == Amazon`, cajas sin Play) o los productos
  no existen todavía → **todo sigue igual que hoy con el QR de Ko-fi**. Cancelar la hoja no muestra error.
- **Lo que tiene que hacer el usuario en Play Console** (cuenta contact@nezor.es, `/console/u/1/`):
  1. ✅ **Perfil de pagos creado el 2026-09-25** (Ajustes → Perfil de pagos; perfil "JORGE MARTINEZ ORTIZ",
     Google Play Apps, 1 usuario). La cuenta bancaria estuvo "Pendiente de verificar" hasta que Google hizo
     un microingreso y se confirmó el importe en "Gestionar métodos de pago" (✅ **verificada el 2026-09-28**,
     ver más abajo).
     **2026-09-27:** el diálogo *Verificar* dice que el ingreso con concepto **"GOOGLE"** se hizo **en torno al
     viernes 25 de septiembre** en la cuenta acabada en **6844**, y pide teclear el importe exacto. El usuario
     dice que **todavía no le ha llegado** (normal: el 25 fue viernes y el 27 es domingo, así que solo ha pasado
     un día hábil; las transferencias SEPA de Google suelen tardar 2-5 días hábiles → esperar al lunes 28 o
     martes 29). Camino: Ajustes → Perfil de pagos → *Gestionar métodos de pago* → **Verificar**.
     ⚠️ Con tres importes erróneos Google anula el ingreso y hay que pedir otro, así que **lo teclea el usuario**
     leyendo el extracto, no a ojo. ⚠️ Arriba de esa página hay un
     aviso para **registrarse en la cuota de servicio del 15 %** ("Gestionar grupo de cuentas" + aceptar
     condiciones); sin hacerlo Google aplica el 30 %.
     **2026-09-27: paso 1 HECHO** — el usuario creó el *grupo de cuentas* en Cuenta de desarrollador →
     Cuentas de desarrollador asociadas (`/associated-developer-accounts`), con su cuenta principal sola
     (no hay cuentas asociadas; la pestaña *Solicitudes* está vacía).
     ⏳ **Paso 2 pendiente y todavía NO disponible en la consola**: según la ayuda de Google
     (support.google.com/googleplay/android-developer/answer/10632485) al terminar el grupo debe aparecer al
     final de esa misma página el aviso *"Cuando termines de configurar tu grupo de cuentas, regístrate en la
     cuota de servicio del 15 %"* con el botón **"Revisar y registrarme"** → aceptar condiciones →
     **"Aceptar y registrarme"**. Ese aviso aún no sale (Google tarda en procesar el grupo recién creado) y el
     banner del Perfil de pagos sigue mostrando los dos requisitos. **Volver a mirar esa página en unas horas
     o mañana.**
  2. ✅ **0.1.9 (vc10) publicada en Prueba interna el 2026-09-26 11:38** (hacía falta una build con el permiso
     BILLING para poder crear productos). ✅ **Tres productos únicos creados el 2026-09-26 como BORRADOR**
     (`coffee_small` "Un café", `coffee_medium` "Café y tostada", `coffee_large` "Café para el equipo"; opción de
     compra `buy`, tipo Comprar, retrocompatible, "Ventas de apps digitales", 173 países). ✅ **ACTIVADOS** el
     2026-09-26 con el OK del usuario.
     ⚠️ Precios: el importe que se teclea en "Editar precios en bloque" es **sin IVA**; para que en España salga
     1,99 / 4,99 / 9,99 € hay que poner **1,64 / 4,12 / 8,26 €** de base (Google redondea a precio atractivo:
     ES 1,99 €, DE 1,99 €, UK 1,69 £, US 1,89 $; 4,99 / 4,89 / 4,29 / 4,69; 9,99 / 9,99 / 8,49 / 9,49).
     ✅ Testers: la lista **"Internos"** (contact@nezor.es, martinezortiz@gmail.com = móvil, sigfrido.mar@gmail.com =
     Chromecast) ya estaba asignada a la Prueba interna y el 2026-09-26 se marcó también en Ajustes → **Licencia
     para testing** (respuesta RESPOND_NORMALLY): las compras de esas cuentas son de prueba, no se cobran.
     Enlace para unirse a la prueba interna e instalar la 0.1.9: https://play.google.com/apps/internaltest/4701253984818511045 Tres productos, tipo
     consumible, con estos **IDs exactos**: `coffee_small` (1,99 €), `coffee_medium` (4,99 €), `coffee_large`
     (9,99 €). Nombre (es): "Un café" / "Café y tostada" / "Café para el equipo"; en inglés "A coffee" /
     "Coffee and toast" / "Coffee for the team". Descripción: "Propina voluntaria para el desarrollador. No
     desbloquea nada." Activarlos.
  3. ✅ Compradores de prueba configurados (ver punto 2).
  4. ✅ **Probado en el Chromecast el 2026-09-26**: el menú OK → ‹ Invítame a un café › lista los tres cafés con los
     precios de Play (1,99 / 4,99 / 9,99 €), OK abre la hoja de compra de Google Play ("Revisar y aceptar") y la
     compra de prueba se completó: en **Gestión de pedidos** figura "Prueba: Un café", `coffee_small`,
     GPA.3343-1251-1103-24640, Procesado, 1,99 EUR (11:42 UTC; de prueba, no se cobra).
     Cómo se instaló: la Play Store de la tele instalaba la 0.1.6 (la cuenta aún no había aceptado la invitación) y
     **no entiende el enlace de compartición interna**; se descargó el **APK universal firmado por Google** desde
     Explorador de app bundles → 10 (0.1.9) → Descargas (`~/Downloads/10.apk`, firmante CN=Android/Google) y se
     instaló con `adb install -r -i com.android.vending 10.apk` (marca a Play como instalador, lo que satisface la
     "comprobación del instalador" de la protección automática). Antes hubo que **desinstalar la build debug** de la
     tele (otra firma; se perdieron los perfiles de prueba). El APK de mi clave de subida NO sirve para probar compras.
     ⏳ Falta la misma prueba en el móvil: abrir el enlace de la prueba interna con la cuenta correspondiente,
     aceptar, instalar la 0.1.9 desde Play; menú OK → ‹ Invítame a un café › → elegir café → hoja de Play; y
     Ajustes → café. Comprobar el "¡Gracias por el café!" y que la compra sale como prueba (0 €). Verificar también
     el **fallback al QR** en un dispositivo sin Play / sin productos.
  5. ⏳ Mezclar `feature/play-billing-coffee` en main (incluye el fix de `oneTimePurchaseOfferDetailsList`) y, tras
     probar, promocionar o resubir a Producción como 0.1.9 cuando la 0.1.8 esté publicada.
- **Ficha**: no cambia. Amazon: sin cambios (sigue el QR); si algún día se quiere, Amazon IAP es otra integración.
- **Versión**: al mezclar, `versionCode 10` · `0.1.9`. Notas de versión: "Ahora puedes invitar a un café desde la
  propia app, con Google Play" / "You can now buy me a coffee right from the app, through Google Play".

## 0.1.11 — lista de ejemplo solo de radios (2026-09-29, 🎉 PUBLICADA en Play el 2026-10-01; ❌ RECHAZADA en Amazon ese mismo día)
Respuesta al rechazo de Amazon de la 0.1.8 (ver la sección de Amazon). `versionCode` **15** · `versionName`
**0.1.11**. Es la 0.1.10 (vc14) con un único cambio:
- El botón del primer arranque y de la pantalla de perfiles vacía pasa a ser **"Probar con una lista de radios
  de ejemplo"** y crea **un solo perfil**, "Demo · Radios deportivas" (`docs/playlists/sports-radio.m3u`, 14
  emisoras, streams oficiales). **Desaparece "Demo · Deporte en abierto"** y la URL de iptv-org del código
  (`SampleLists.SPORTS_TV_URL`). Los logos siguen saliendo de la base de datos de iptv-org (`api/*.json`), que
  son solo metadatos.
- Textos nuevos en los 24 idiomas (`sample_radio_button`, `sample_radio_hint`); se borra `sample_profile_sports`.
- Quien ya tenga el perfil "Demo · Deporte en abierto" creado (solo testers: la 0.1.8 no llegó a publicarse en
  ninguna tienda) lo conserva hasta que lo borre; la app no toca los perfiles existentes.
- ✅ Comprobado el 2026-09-29: compila, tests unitarios en verde, el APK debug no contiene la URL de la lista
  de iptv-org y **las 14 emisoras responden** (200/206 con audio). Lint sigue con los 2 errores de siempre
  (`app_name` y `settings_licenses_body` sin traducir), ninguno de este cambio.
- ✅ **Probada en el Chromecast el 2026-09-29 (17:50)** con la build debug (`0.1.11-debug`, instalada al lado
  de la de Play, datos vacíos): en el primer arranque sale el botón **"Probar con una lista de radios de
  ejemplo"** con su texto debajo; al pulsarlo crea **un solo perfil**, "Demo · Radios deportivas" (comprobado
  también en la base de datos: una fila), la parrilla carga **14 canales** y **Cadena SER suena** (pantalla de
  radio, "Solo audio"). Falta verla en el móvil, donde el cambio es solo de texto.
  Para conectar hizo falta el `IP:puerto` de la pantalla de Depuración inalámbrica (`adb connect`); el mDNS
  anunciaba un puerto viejo que rechazaba la conexión. No hubo que re-emparejar.
- ✅ **Mezclada en `main`** (`869929a`) y **firmada** con la clave de subida (`CN=Jorge Mtnez`, SHA-256
  `E9:9B:9E:F2:…`). Copias en `~/Downloads/easy-xtream-0.1.11-vc15/`:
  - Play: `easy-xtream-football-0.1.11-vc15.aab` (17,0 MB, md5 `478dab84…`)
  - Amazon: `easy-xtream-football-0.1.11-vc15-universal.apk` (27,2 MB, md5 `94aab177…`)
  - Comprobado en el APK de release: `versionCode 15`, sin la URL de la lista de iptv-org.
- ✅ **Amazon: enviada el 2026-09-29 (18:14)**, ver la sección de Amazon.
- ✅ **Play: ENVIADA A REVISIÓN el 2026-09-29 (~18:20)**. Producción **15 (0.1.11), lanzamiento completo
  (100 %)**, más el cambio de la ficha es-ES (párrafo IMPORTANTE de la 0.1.11). El AAB lo subió el usuario a
  mano; notas en es-ES y en-US (las de abajo, versión Play). Única advertencia: símbolos de depuración nativos.
  - Enviar **canceló y reinició la revisión** que estaba en curso desde el 26 de septiembre (aviso de Google
    *"¿Quieres reiniciar tu revisión?"*).
  - ⚠️ **Por qué al 100 % y no al 20 %**: primero se envió por fases al 20 %, y entonces Google **mantuvo
    también la 9 (0.1.8) en revisión** ("Iniciar lanzamiento completo") como versión para el 80 % restante.
    Un lanzamiento por fases no sustituye a la versión anterior del canal; solo uno completo lo hace. Con el
    botón de iptv-org dentro, esa vc9 no debía publicarse. Decisión del usuario: pasar la 0.1.11 al 100 %
    (*Producción → Gestionar la versión → Actualizar lanzamiento*), lo que **sacó la vc9 de la revisión** sin
    tener que reenviar nada. Con 11 instalaciones activas, el 20 % eran 2 dispositivos.
  - Estado final: *Resumen de publicación → Cambios en revisión*: `15 (0.1.11) · Iniciar lanzamiento
    completo` y `es-ES · Cambiar descripción completa`. En Producción, "Versión 15 (0.1.11) en revisión". La
    0.1.6 (vc7) sigue siendo la publicada. **La 0.1.8 (vc9) y la 0.1.10 (vc14) ya no se publicarán.**
  - ✅ **Prueba interna: la vc15 sustituye a la vc14 desde el 2026-09-29 (18:27)**. Versión nueva creada con
    el bundle 15 **añadido desde la biblioteca** (ya estaba subido por Producción, no hay que volver a
    subirlo), sin incluir el 14; notas breves en es-ES y en-US; *Guardar y publicar*. El canal muestra
    "Última versión: 15 (0.1.11)". Se publica al momento y **no toca la revisión de Producción**, que sigue
    igual. Ya no queda ningún canal de Play sirviendo el botón de iptv-org.
  - 🎉 **PUBLICADA EN PRODUCCIÓN el 2026-10-01** (unos 2 días de revisión). Panel de la app, pegado por el
    usuario ese día: *"No tienes cambios no publicados"*, *Última versión de producción: hace 6 horas · 100 % ·
    Teléfonos y tablets, y 3 más*, *"Se ha publicado la actualización de la aplicación"*. Comprobado en la
    ficha pública (`details?id=com.footballxtream`, HTTP 200): **versión 0.1.11**. Con ella llegan a
    Producción, de golpe, el botón de radios de ejemplo, el rediseño del reproductor en móvil y **Play Billing
    para el café** (0.1.9/0.1.10). Cifras a 28 días: 16 adquisiciones, 10 dispositivos activos al mes,
    valoración media 5,00, sin ingresos todavía. El aviso *"Se ha solucionado una infracción reciente de las
    políticas"* del panel es, salvo que la consola diga otra cosa, el del target API 36 del 2026-09-26 (ver
    la revisión del 28 de septiembre); no pide ninguna acción.
  - ✅ **Ficha en-US de Play CREADA y GUARDADA el 2026-10-01 (tarde), SIN enviar a revisión.** *Fichas de
    Play Store → Gestionar traducciones → Seleccionar idiomas → Inglés (Estados Unidos) – en-US → Aplicar*;
    nombre (20/30), descripción breve (77/80) y completa (1334/4000, párrafos sin saltos de línea duros) de
    `docs/store-listing.md`; **Guardar**. *Resumen de publicación* muestra *"Cambios que aún no se han
    enviado a revisión → Enviar 1 cambio a revisión"*. Los gráficos se heredan de es-ES. ⏳ **Falta el
    visto bueno del usuario para enviarla.** Si se quieren subir a Play las capturas nuevas (01 y 02, ver
    la sección de Amazon), mejor meterlas en ese mismo envío (hecho, ver abajo).
  - ✅ **ENVIADO A REVISIÓN el 2026-10-01 (~18:45), con el visto bueno del usuario: 5 cambios de ficha**
    (ningún cambio de versión): `en-US · Añadir idioma` y `es-ES · Cambiar capturas` de **teléfonos, tablets
    de 7", tablets de 10" y Android TV**. En las cuatro secciones quedan, en este orden,
    `02-profiles-neutral.png`, `01-channels-neutral.png`, `03-languages.png` y `04-add-profile.png` (las dos
    primeras son las capturas nuevas sin "iptv-org" ni "TDTChannels"; la ficha en-US hereda los gráficos).
    *Resumen de publicación* muestra *Cambios en revisión*; Google avisa de hasta 7 días. La app publicada
    (vc15) no cambia. ⏳ Esperar el correo de Google.
    Cómo se cambian las capturas en la consola: *Añadir recursos* de la sección abre la biblioteca lateral;
    **Subir** admite `file_upload`; cada recurso se añade a la sección activa (la azul) con la flecha de su
    fila → **Añadir**; para quitar, el botón "Quitar …" de cada miniatura (responde a `click()` por JS; las
    filas de la biblioteca no). La flecha de arriba a la izquierda del panel vuelve atrás en la vista de
    detalle, pero en la lista **cierra el panel**.
  - 🎉 **PRIMERA COMPRA REAL del café: 2026-10-01, 20:17 (18:17 UTC).** Pedido **`GPA.3387-6546-4841-82280`**,
    *Un café (Easy Xtream Football)* · `coffee_small` · **Procesado · 1,99 EUR**, **sin la etiqueta
    "Prueba:"** (los cuatro pedidos anteriores, del 26 y 27 de septiembre, sí la llevan). La hizo el usuario
    desde su móvil con martinezortiz@gmail.com y la 0.1.11 de la tienda. Play Billing queda verificado en
    Producción de punta a punta. No se ha reembolsado.
    Para que cobrara hubo que **quitar esa cuenta de la lista "Internos"** (*Ajustes → Licencia para
    testing → Internos*; es también la lista de la Prueba interna) a las ~19:00; **vuelta a añadir a las
    ~20:50**, la lista tiene otra vez 3 usuarios. contact@nezor.es no sirve para compras reales (cuenta
    del desarrollador). Ruta de los pedidos: `/console/u/1/developers/<id>/orders`.
    Ojo con el navegador: la consola exige tener iniciada la sesión de contact@nezor.es en el Chrome que
    controla Claude (queda como `/u/1/`); id de la app en las URL: `4973475173609165901`.
  - Amazon rechazó la 0.1.11 ese mismo día (ver su sección).
- ⚠️ **Capturas de la ficha, por si hay otro rechazo** (visto el 2026-09-29, **aplazado por decisión del
  usuario** hasta tener los veredictos): `docs/store-assets/screenshots/02-profiles.png`, publicada en Amazon y
  en Play, enseña perfiles llamados **"iptv-org ES"** y **"Sports iptv-org"**. `01-channels.png` muestra
  "Live sports of TDTChannels" (Teledeporte, Esport 3, Sportitalia: emisiones en abierto, menos preocupante).
  Si Amazon o Google vuelven a rechazar por contenido, es lo siguiente a cambiar: rehacerlas con nombres
  neutros ("Demo · Radios deportivas", "Mi proveedor"). No tocar la ficha mientras haya revisión en curso.
  - ✅ **PUBLICADA el 2026-10-01 a las 19:04**, 20 minutos después de enviarla (comprobado en *Actividad de
    envíos* el 2026-10-12: envío nº 10, 5 cambios, estado *Publicada*). La ficha en-US y las capturas neutras
    (teléfono, tablets 7" y 10", Android TV) ya están en la tienda. **No queda nada en revisión en Play.**
  - Notas de versión usadas en Play (viene de la 0.1.6, por eso son más largas que las de Amazon):
    ```
    <es-ES>
    • Radio: las emisoras de tu lista se ven con su logo, nombre y lo que está sonando.
    • Pausa: pulsa OK dos veces (o toca dos veces la pantalla). En pausa, un solo OK reanuda.
    • Lista de ejemplo: si aún no tienes proveedor, prueba la app con una lista de radios deportivas, que puedes borrar cuando quieras.
    • Móvil: menú del reproductor rediseñado y gestos táctiles.
    • Favoritos compartidos entre perfiles, más logos y 24 idiomas.
    • Si te gusta la app, ahora puedes invitar a un café desde Ajustes.
    </es-ES>
    <en-US>
    • Radio: stations in your playlist show their logo, name and what's on air.
    • Pause: press OK twice (or double-tap the screen). While paused, a single OK resumes.
    • Sample playlist: no provider yet? Try the app with a playlist of sports radio stations; delete it whenever you like.
    • Phone: redesigned player menu and touch gestures.
    • Favorites shared between profiles, more logos and 24 languages.
    • Like the app? You can now buy me a coffee from Settings.
    </en-US>
    ```
- **Notas de versión de 0.1.11 usadas en Amazon** (viene de la 0.1.7):
  ```
  <es-ES>
  • Radio: las emisoras de tu lista se ven con su logo, nombre y lo que está sonando.
  • Pausa: pulsa OK dos veces (o toca dos veces la pantalla). En pausa, un solo OK reanuda.
  • Lista de ejemplo: si aún no tienes proveedor, prueba la app con una lista de radios deportivas (la emisión pública de cada emisora), que puedes borrar cuando quieras.
  • Menú del reproductor rediseñado en pantallas táctiles.
  </es-ES>
  ```
  ```
  <en-US>
  • Radio: stations in your playlist show their logo, name and what's on air.
  • Pause: press OK twice (or double-tap the screen). While paused, a single OK resumes.
  • Sample playlist: no provider yet? Try the app with a playlist of sports radio stations (each station's own public stream); delete it whenever you like.
  • Player menu redesigned for touch screens.
  </en-US>
  ```

## Amazon Appstore (Fire TV) — preparado el 2026-09-22, enviada el 2026-09-23, PUBLICADA el 2026-09-26 (0.1.8 RECHAZADA el 2026-09-29 y 0.1.11 RECHAZADA el 2026-10-01)
Objetivo: que los Fire TV Stick instalen la app desde su tienda (no tienen Google Play). Cuenta de
desarrollador de Amazon gratuita. La revisión de Amazon es independiente de la de Google.
- ✅ **APK universal firmado** con la clave de subida (Amazon no usa Play App Signing; re-firma con su propio
  certificado al publicar): `app/build/outputs/apk/full/release/app-full-universal-release.apk`, 26,8 MB,
  `versionCode 8 · 0.1.7`, ABIs arm64-v8a + armeabi-v7a (+ x86 que trae FFmpeg). Se genera con
  `gradlew :app:assembleFullRelease` (en `build.gradle.kts` `isUniversalApk = true`; para Play sigue usándose
  el AAB, no le afecta). Verificado con `apksigner verify --print-certs` → `CN=Jorge Mtnez`.
- ✅ Requisitos técnicos de Fire TV que ya cumple: sin Google Play Services, `leanback` + `LEANBACK_LAUNCHER`,
  `touchscreen` no requerido, manejo completo con mando (D-pad, OK, Back, teclas multimedia), minSdk 24
  (Fire OS 6/7/8 = Android 7.1/9/11; el Stick de 2ª gen con Fire OS 5 queda fuera), targetSdk 36.
- ✅ Materiales en `docs/store-assets/`: icono **512×512** y **114×114** (`icon-114.png`, generado), capturas
  1920×1080 (4), imagen promocional 1024×500 (`feature-1024x500.png`). Textos en `docs/store-listing.md`
  (ES/EN; Amazon admite localizaciones). Política de privacidad: la misma URL de GitHub.
- ✅ Clasificación por edades: **IARC** — en el alta Amazon pide el *Global Rating ID* del correo de IARC del
  2026-09-21 (asunto *IARC Live Rating Notice: Easy Xtream Football*, de noreply@globalratings.com).
- ⚠️ **Riesgo de revisión:** Amazon es más estricta que Google con las apps IPTV ("apps que facilitan acceso a
  contenido pirata"). Defensa: la app es solo el reproductor, no incluye contenido ni listas, cada usuario pone
  su proveedor; dejarlo igual de claro que en Play en la descripción, y no usar capturas con logos de canales
  de pago reconocibles.
- ✅ **PUBLICADA (LIVE) en la Amazon Appstore el 2026-09-26** 🎉: correo "Your recent submission, Easy Xtream
  Football version 0.1.7, was successfully published and is now LIVE" con Primary / Content Policy / Functionality
  Validation en PASS. Enviada el 2026-09-23 (13:10) desde la máquina Linux con el navegador guiado por Claude.
  Cuenta de desarrollador de Amazon aprobada ese mismo día.
  ✅ **0.1.8 ENVIADA A AMAZON el 2026-09-29 (00:25 hora española, 28 sept 15:25 PDT)** como *upcoming version*.
  Estado SUBMITTED, Amazon estima publicación **antes del 2026-10-03**. Lo que se hizo (máquina Linux, navegador
  guiado por Claude): el APK universal vc9 se recompiló desde el commit `0f25b16` en un worktree temporal
  (`gradlew :app:assembleFullRelease`, misma firma `CN=Jorge Mtnez`, md5 `4690c1b0…`, copia en
  `~/Downloads/easy-xtream-0.1.8/`) y lo subió el usuario a mano; DRM No; se **borró el APK1 (vc8)** de la
  versión (Amazon avisa de que se pierde la selección de dispositivos, se rehízo); notas de versión EN/ES;
  dispositivos **Fire TV 91 / Fire tablets 12 / Automotive 0** (los 7 Fire TV excluidos son Vega OS —Stick 4K
  Select, Stick HD 2ª gen— o Fire OS 5 —Fire TV gen 1/2, Stick gen 1/2, Element 4K 2017—); pregunta nueva de
  Amazon *"Any significant changes to report?"* (ley de EE. UU. de consentimiento parental) → **None**; las
  preguntas de táctil/offline solo aparecen con Automotive marcado; párrafo IMPORTANTE nuevo en la descripción
  larga EN y ES (textareas React: se cambió el valor por el setter nativo + evento `input`, autoguardó). El
  resto (gratuita, países, público 16-17/18+, clasificación, privacidad, instrucciones de prueba, export
  compliance) se heredó de la 0.1.7.
  ❌ **0.1.8 RECHAZADA POR AMAZON el 2026-09-29 (06:55 PDT, 15:55 hora española)**, a las 15 horas de
  enviarla. Primary Validation: sin problemas · Functionality Validation: 104/104 dispositivos ·
  **Amazon Content Policy: Not compliant**: *"Your app offers pirated content within the app, promotes links
  to websites that stream pirated content, or promotes downloading via torrents"* (política *Deceptive and
  Malicious Behavior*). El informe de la consola (*Upcoming version → App status → Details*, envío
  `44345790`) dice lo mismo que el correo: ni capturas ni pasos. **La 0.1.7 sigue LIVE**: el rechazo solo
  bloquea la actualización (consola: Current version *Live*, Upcoming version *Rejected*).
  - **Causa más probable: el botón "Probar con listas de ejemplo"**, la única novedad de la 0.1.8 que toca
    contenido (la 0.1.7 pasó esta misma revisión sin él). Crea el perfil "Demo · Deporte en abierto" con
    `iptv-org categories/sports.m3u`, y esa lista, descargada el 2026-09-29, trae **440 canales de los que
    unos 89 llevan marca de televisión de pago** (Sky Sports Football y F1, ESPN, Liga de Campeones por
    Movistar Plus+, DAZN, Sony Sports Ten, Setanta, Arena Sport, Digi Sport, Match!…) y 85 se sirven desde
    una IP pelada, sin dominio del emisor. No es "deporte en abierto", que es lo que dicen la ficha y el
    texto del botón. Con la 0.1.7 esa lista solo figuraba en las instrucciones de prueba al revisor; en la
    0.1.8 la ofrece la propia app, que es justo lo que describe el motivo del rechazo.
  - **Apelar no tiene recorrido** mientras el botón apunte a esa lista: el argumento de "solo reproductor"
    ya no se sostiene si la app enlaza ella misma esos canales.
  - ⚠️ **Mismo riesgo en Play**: la 0.1.8 (vc9) que está en revisión y la 0.1.10 (vc14) de Prueba interna
    llevan el mismo botón y la misma URL (`SampleLists.SPORTS_TV_URL`).
  - **Decisión del usuario (2026-09-29): quitar la lista de TV y dejar solo las radios, en todas las
    tiendas.** Hecho en la **0.1.11 (vc15)**, ver su sección más arriba.
  - ✅ **0.1.11 (vc15) ENVIADA A AMAZON el 2026-09-29 (18:14 hora española, 09:14 PDT).** Estado SUBMITTED,
    Amazon estima publicación **antes del 2026-10-04 (09:30 PDT)**. Cómo se hizo (máquina Linux, navegador
    guiado por Claude): la versión rechazada queda en **solo lectura**, así que hay que crear otra con el menú
    de la lista de apps → **Add Upcoming Version** (release `amzn1.devportal.apprelease.f4bab9bc…`); hereda el
    APK anterior, la ficha y las instrucciones. El usuario subió a mano el APK universal vc15 (aparece como
    APK3), se marcó **DRM No**, se **borró el APK2 (vc9)** y se rehízo la selección de dispositivos:
    **Fire tablets 12 / Fire TV 91 / Automotive 0**. **Párrafo IMPORTANTE nuevo** en la descripción larga EN y
    ES, **notas de versión** de la 0.1.11 en los dos idiomas e **instrucciones de prueba reescritas**: ya no
    llevan ninguna URL de iptv-org, explican que esa lista se ha quitado en respuesta al rechazo de la 0.1.8 y
    guían al revisor por el botón *"Try with a sample radio playlist"*. Esta vez no salió la pregunta de
    *"significant changes"*.
  - ❌ **0.1.11 RECHAZADA POR AMAZON el 2026-10-01 (correo a las 8:09 hora española)**, a las ~38 horas de
    enviarla y **con el mismo motivo literal** que la 0.1.8: Primary Validation PASS · Functionality
    Validation PASS · **Content Policy Validation FAILED**: *"Your app offers pirated content within the app,
    promotes links to websites that stream pirated content, or promotes downloading via torrents"* (política
    *Deceptive and Malicious Behavior*). El correo dice que la decisión combina medios automáticos y revisión
    humana, y remite a la consola para el informe detallado (pendiente de mirar; con la 0.1.8 no decía más).
    Ese mismo día Google **aprobó y publicó** la misma vc15 en Play. La 0.1.7 debería seguir LIVE en Amazon
    (el rechazo solo bloquea la actualización; confirmarlo en la consola).
    - **Quitar la lista de iptv-org no ha bastado**, así que la causa ya no está clara. Lo que la 0.1.11
      tiene y la 0.1.7 aprobada no tenía, o que sigue a la vista del revisor:
      1. **El botón de lista de ejemplo en sí**, aunque ahora sea de radios: la app sigue *ofreciendo
         contenido ella misma* (14 emisiones de terceros para las que no hay licencia ni permiso escrito;
         talkSPORT, Fox Sports Radio o RMC narran competiciones con derechos). La 0.1.7 no ofrecía nada.
      2. **Las instrucciones de prueba** mencionan el rechazo anterior y la lista de iptv-org retirada.
      3. **Las capturas** (`02-profiles.png` con perfiles "iptv-org ES" y "Sports iptv-org";
         `01-channels.png` con "Live sports of TDTChannels"). Son las mismas con las que pasó la 0.1.7, pero
         tras un rechazo la revisión es más estricta.
      4. Menos probable: la descarga de logos de `iptv-org.github.io/api` y la mención "Channel logos from
         the iptv-org database" en Licencias.
    - **Apelación:** *Contact Us* → tipo *Appstore* → categoría *App Submission and Certification* → tema
      *App Content Policy Review Results*. Es la única vía para saber qué han visto exactamente.
    - ⚠️ **No reenviar a ciegas una tercera vez**: dos rechazos seguidos por piratería; un tercero puede
      poner en riesgo la 0.1.7 publicada o la cuenta.
    - ✅ **Decisión del usuario (2026-10-01): preguntar a Amazon y preparar una versión "solo reproductor".**
      Preparado ese día en la rama **`fix/amazon-no-sample-list`** (mezclada en `main` esa tarde):
      - **0.1.12 (vc16)**. Propiedad de Gradle nueva: `./gradlew :app:assembleFullRelease -Pstore=amazon`
        pone `BuildConfig.SAMPLE_LIST = false` y el botón de lista de ejemplo no existe (ni en el primer
        arranque ni en la pantalla de perfiles vacía). Sin la propiedad (builds de Play) todo sigue igual.
        Comprobado en el APK universal: `versionCode 16`, firma `CN=Jorge Mtnez`, y **R8 elimina la URL de
        la lista de radios del binario** (0 apariciones de `sports-radio.m3u`). Sigue estando la URL de
        logos de `iptv-org.github.io/api`, que ya iba en la 0.1.7 aprobada. 75 tests en verde.
        Copia: `~/Downloads/easy-xtream-0.1.12-vc16-amazon/` (27,2 MB), solo en la máquina Linux.
      - `docs/playlists/review-test.m3u`: lista para el revisor con 3 vídeos de licencia libre (Big Buck
        Bunny, Tears of Steel, carta de ajuste de Apple; los tres responden 200 y pasan el filtro de
        deporte de la app). La app no la ofrece; solo va en las instrucciones de prueba. La URL `raw` apunta
        a `main` (ya mezclada).
      - `docs/amazon-resubmission.md`: mensaje para Amazon (Contact Us), párrafo IMPORTANTE de la ficha
        (vuelve al de la 0.1.7), notas de versión, instrucciones de prueba y lista de comprobación.
      - ✅ **Consulta ENVIADA a Amazon el 2026-10-01 (11:48)**: caso **`22369540491`**
        (https://developer.amazon.com/support/cases/22369540491), estado *In Process: pending Amazon
        action*. Formulario *Contact Us* → Appstore → App Submission and Certification → App Content Policy
        Review Results; OS Type **FOS**, app `com.footballxtream`, dispositivos Fire TV + Fire Tablets,
        estado *Rejected*. Lo rellenó Claude en el navegador y lo envió el usuario. El texto es el de
        `docs/amazon-resubmission.md` encajado en la plantilla del campo Descripción (versión, descripción,
        documentos, capturas, notas), más la frase de que Google publicó la misma 0.1.11 ese día. La
        respuesta llega al correo de la cuenta y a *My Appstore Cases*.
      - ⏳ **2026-10-03: correo de espera de Amazon Developer Support** sobre el caso: "The issue is taking
        longer to resolve than expected. We are actively investigating your concern and will respond to you
        once we have more detailed information". No es un veredicto ni pide nada: el caso sigue abierto y
        en sus manos. No hay que contestar (y el correo es *no-reply*; para escribirles, el enlace del caso).
        Se sigue sin reenviar la 0.1.12.
      - 🎉 **2026-10-12: la 0.1.11 (vc15) está LIVE en Amazon.** Correo de Amazon Appstore Team: *"Your recent
        submission, Easy Xtream Football version 0.1.11, was successfully published and is now LIVE"*, con
        Primary / Content Policy / Functionality Validation **PASS** las tres. Es la **misma 0.1.11 que
        rechazaron el 2026-10-01**, sin reenvío por nuestra parte: la consulta del caso `22369540491` ha
        servido para que revisaran el rechazo y lo revirtieran. Comprobado en la consola el 2026-10-12: el caso
        está **Resolved** (última actualización 10/12/26) sin ningún texto de respuesta en la ficha del caso, y
        en *App List* la app sale **Live** sin *Upcoming version*. Consecuencias:
        - La 0.1.12 "solo reproductor" (`-Pstore=amazon`) **ya no hace falta reenviarla**. Se queda en `main`
          como opción de reserva por si un futuro envío vuelve a caer por lo mismo.
        - Amazon y Play tienen ahora la misma versión (0.1.11, vc15, con lista de ejemplo de radios).
        - ✅ **Capturas neutras subidas a la ficha de Amazon el 2026-10-19** (consola guiada por Claude, sobre la
          release Live `f4bab9bc…`): en *Tablet assets* y *FireTV Assets* se añadieron `01-channels.png` y
          `02-profiles.png` (copias RGB sin alfa en `docs/store-assets/amazon/`, hechas con `magick -alpha off`)
          y se borraron las dos antiguas (TDTChannels / perfiles iptv-org). Quedan 4 por sección en este orden:
          idiomas, añadir perfil, canales, perfiles. **Submit App pulsado por el usuario el 2026-10-19 (~22:50)**:
          la ficha (mismo APK vc15, solo cambian las imágenes) está en revisión; anotar aquí el resultado
          cuando llegue el correo a contact@nezor.es. El párrafo IMPORTANTE
          de la descripción ya es el de la 0.1.11 (lista de radios de ejemplo), coherente con la app Live: no
          se toca.
      - ✅ **Probada en el Chromecast el 2026-10-01 (17:20-17:50)** con la build debug de Amazon
        (`0.1.12-debug`, `-Pstore=amazon`, datos vacíos, al lado de la de Play): ni el primer arranque ni el
        final de "Connect your list" muestran el botón de ejemplo; la lista `review-test.m3u` carga y **Big
        Buck Bunny reproduce** (848x480, con audio). De paso se renombraron los tres vídeos de esa lista:
        con el prefijo común "Test N" la app los agrupaba en una sola tarjeta.
      - ✅ **Capturas rehechas** ese día en el Chromecast, en inglés: `01-channels.png` (canales inventados
        de `docs/playlists/screenshot-demo.m3u`, sin logos ni marcas) y `02-profiles.png` (perfiles "Backup
        list", "Home", "My provider", "Sports bar", "Test streams"). Ya no aparece "iptv-org" ni
        "TDTChannels". Copias RGB sin alfa para Fire TV en
        `~/Downloads/easy-xtream-0.1.12-vc16-amazon/screenshots/`. Subidas a Play el 2026-10-01 (en revisión); **en Amazon siguen las
        antiguas** hasta el reenvío.
      - ✅ **Rama mezclada en `main`** (`33e742e`), 75 tests en verde, y APK universal de Amazon recompilado
        desde `main`: vc16, `CN=Jorge Mtnez`, sin la URL de radios en el binario, md5 `1bfaba5d…`.
      - ⏳ **Falta**: la respuesta de Amazon al caso y, con el visto bueno del usuario, reenviar la 0.1.12
        (lista de comprobación en `docs/amazon-resubmission.md`). Opcional: subir las dos capturas nuevas
        también a la ficha de Play.
      - ℹ️ El Chromecast se queda con la `0.1.12-debug` de Amazon en inglés y los cinco perfiles de las
        capturas; la app de Play instalada no se ha tocado.
  Ficha publicada: https://www.amazon.com/dp/B0HKSFXX9Z (el enlace estable para compartir es
  https://www.amazon.com/gp/mas/dl/android?p=com.footballxtream ; la variante amazon.es da 404). Consola:
  https://developer.amazon.com/apps-and-games/console/apps/list.html
  - App ID `amzn1.devportal.mobileapp.07ea2808a33b46da999e14dfd0bf5184` · SKU `com.footballxtream` ·
    categoría **Movies & TV** (Amazon no tiene "Entertainment"; sin subcategoría) · gratuita · DRM **No**
    (aviso "outdated SDK version" = no lleva el Appstore SDK; es informativo).
  - Dispositivos: **Fire TV (91 modelos)** y **Fire tablets (12)**; Automotive quitado. Los Vega TV (Vega OS,
    no Android) no se pueden marcar. **Amazon ya no acepta apps para móviles Android** (desde 2025-08-20).
  - Touch: sí · offline: no · todos los países · público **16-17 y 18+** (nada de niños) · cuestionario de
    contenido propio de Amazon (no IARC, no pidió el Global Rating ID): todo "None"/"No" · privacidad: no recoge
    datos + URL de la política · idiomas marcados: los 14 de la lista de Amazon que coinciden con los 24.
  - Ficha en **inglés y español** (descripción corta, larga, 9 viñetas, palabras clave), iconos 512/114,
    4 capturas de tablet, promocional 1024×500; para Fire TV: icono 1280×720 (el banner), 4 capturas
    **aplanadas a RGB sin alfa** (Amazon rechaza transparencia en TV) y un fondo 1920×1080 generado con
    `magick` a partir de la captura de canales (desenfoque + oscurecido). Las subidas hay que hacerlas de una
    en una: en paralelo se pierden.
  - **Instrucciones de prueba** al revisor: perfil M3U con la lista pública de deportes de iptv-org
    (`https://iptv-org.github.io/iptv/categories/sports.m3u`, ~400 canales en abierto; alternativas `countries/es.m3u`
    e `index.m3u`). **No se dan credenciales Xtream del usuario** (decisión 2026-09-23).
  - Export compliance marcada (cifrado = AES-GCM del Android Keystore + HTTPS del sistema, sin librerías
    propias; además código abierto público).
  - ⏳ Esperar el correo de Amazon. Si la aprueban, anotar aquí la URL de la ficha y la fecha. Cuando salga
    una versión nueva: *Add upcoming version* en la consola, subir el APK universal nuevo y reenviar.
- Amazon **re-firma** el APK con su certificado: no afecta a nada (la app no verifica firmas ni usa licencias).

## LG Content Store (webOS) — ENVIADA A QA el 2026-10-21 (QA Requested)

> La app de TV vive en `tv/` (compartida con Samsung). Probada el 2026-10-19 en una LG 32LM637BPLA real
> (webOS 4.9 = Chrome 53): vídeo MPEG-TS, radios, teclas, puntero. Ver `tv/README.md` y la memoria de Claude.

**Lo que pide LG** (webostv.developer.lge.com/distribute/app-approval-process + plantillas oficiales
`documents_for_app_qa.zip`): cuenta gratuita en seller.lgappstv.com con la cuenta LG (martinezortiz@gmail.com),
.ipk SIN firmar (LG lo firma), icono cuadrado 400x400 opaco sin esquinas redondeadas, fondo 1920x1080, de 3 a 6
capturas 1280x720, título y descripción en inglés, y dos documentos obligatorios: la lista de comprobación
(xlsx, 53 casos + contenido) y el escenario de uso (ppt). QA en tres fases, de 3 semanas a 3 meses por vuelta.

**Listo en el repo (`docs/store-assets/lg/`):**
- `icon-400x400.png`, `background-1920x1080.jpg`, `screenshots/01..06` (1280x720, contenido de demo neutro:
  Blender + radios, en inglés; generadas con Chromium headless desde la build webOS: `tv/` + playwright).
- `self_evaluation_checklist_5.0.xlsx`: rellenada con los resultados reales en la LG (Pass / N/A + comentario).
- `ux_scenario_easy_xtream_football.pptx`: estructura de la plantilla 4.3 de LG, 18 diapositivas con cada pantalla,
  botones numerados, datos de prueba para QA (listas review-test.m3u y sports-radio.m3u por jsDelivr) y mapa de teclas.
  Portada: "App Developer Name" = Jorge Mtnez (decidido el 2026-10-19). Magic Remote: el usuario no tiene; la
  checklist y el pptx dicen que puntero y rueda se verificaron con eventos de ratón en la build webOS, no en hardware.
- Paquete: `cd tv && npm run build:store:webos` → `tv/dist/es.nezor.easyxtreamfootball_1.0.0_all.ipk` (1920x1080,
  modelos UHD) **y** `tv/dist/720/es.nezor.easyxtreamfootball_1.0.0_all.ipk` (1280x720, modelos FHD como la
  32LM637: la misma app con `resolution` 1280x720 y `html { zoom: 2/3 }`, build.mjs `--res=720`; la tienda exige
  un paquete por resolución o excluye los modelos FHD; verificado en Chromium a 1280x720, pendiente de probar en la
  LG real) (sin lista de radios de ejemplo, como la build de tienda de Samsung). **Versión webOS = 1.0.0**: Seller Lounge
  rechaza "File Version" menores que 1.0.0 (comprobado el 2026-10-19 al subir la 0.1.12), así que la numeración
  de LG va aparte de la de Android (appinfo.json `version`); el pptx y la checklist dicen 1.0.0. Hace falta
  `~/.local/webos/node_modules/.bin` en el PATH para `ares-package`.

**Hecho en la app para su QA (commits del 2026-10-19):** Magic Remote completo (hover = foco, clic = OK, rueda
= scroll de filas y listas, clic en el vídeo = menú), un solo Atrás en la pantalla de entrada sale de la app,
símbolos que faltan en la fuente de LG sustituidos, Let's Encrypt evitado (jsDelivr + proxy de logos).

**ALTA Y FORMULARIO HECHOS el 2026-10-20 (madrugada), consola guiada por Claude:**
- Cuenta Seller Lounge creada con la cuenta LG (martinezortiz@gmail.com): región Europe & CIS, vendedor "Nezor",
  nickname nezor26, correo de avisos activado. La pregunta de "App Transfer" (migrar apps de la Seller Lounge
  antigua) se contesta Cancel.
- App creada (Save as Draft) con ID `es.nezor.easyxtreamfootball`, plataforma webOS. Todos los pasos en verde:
  File Upload (ipk 1.0.0, chipset All, plataforma "2018 and later" = SDK 4.0.0, 1920x1080, File Ver 1.0.0;
  al subir el ipk preguntó por usar appinfo.json: decir NO, porque intenta usar el icono de 80 px y salta el
  aviso "only 400x400"), Images (icono 400, color de tile #39D353, 6 capturas, fondo como splash y launcher),
  Service Country Info ("Select Available Countries" = todos los países con tienda, incluida España; 21 idiomas de
  la app, LG no tiene ca/eu/gl), Display Info (título + descripción EN de 1296 bytes), Service Info
  (Entertainment > Sports, versión 1.0.0, General Audiences en los 193 países, Brasil "All" vía cuestionario,
  sin contenido adulto, keywords, contacto web+correo, política de privacidad por URL, sin datos recogidos ni
  compartidos), Feature Info (requisitos None, mando normal + Magic), Test Info (correo contact@nezor.es, pptx
  del escenario UX, nota para el tester con las dos listas de prueba, sin cuenta/voucher, sin geo-block, sin
  billing ni anuncios, player spec: HTTP progresivo + HLS, H.264/H.265, MP3/AAC/HE-AAC/AC3/EAC3), Self-check
  list (se rellena ONLINE, versión 5.0 = nuestro xlsx: 39 Pass / 14 N/A copiados por script; el xlsx no se sube).
- ⚠️ La "Note for Tester" NO se conserva al recargar la página (y sin ella Test Info no cuenta como completo y
  SUBMIT queda deshabilitado): hay que escribirla y pulsar SUBMIT en la misma sesión. Los avisos "Successfully
  Saved" son alert() nativos que bloquean la extensión de Chrome: los acepta el usuario.

**SUBMIT HECHO el 2026-10-21 (noche) → estado "QA Requested":**
- Antes del envío el SUBMIT dio dos avisos: hacía falta la descripción en ruso (Display Info, añadida) y el
  paquete solo de 1920x1080 excluía los modelos FHD → se subió un segundo ipk 1280x720 (misma app, zoom 2/3,
  `npm run build:store:webos` genera los dos). File Upload queda con dos filas, ambas 1.0.0 / SDK 4.0.0.
- La sesión caducó entre medias y hubo que reescribir la "Note for Tester" (texto en la memoria de Claude y en el
  historial; ahora menciona también los dos paquetes) y guardar Test Info antes del SUBMIT.
- Al pulsar SUBMIT aparece la página "Seller Agreement" con cuatro casillas: todas "I agree" (la 3ª permite que
  LG publique aunque haya defectos menores) → Confirm. Tras confirmar sale "QA Requested" y en la lista de apps el
  estado pasa a "App Packaging Request" (LG reempaqueta/firma el ipk antes de la QA), Last Submission 2026-10-21.
  La etiqueta rara "[TEST]U-NEXT!@#..." que aparece sobre la fila es un texto de la propia página de LG, no nuestro.
- QA: pretest → function → content; LG dice de 3 semanas a 3 meses. Avisos al correo de la cuenta LG
  (martinezortiz@gmail.com) y en la campana de la Seller Lounge; el contacto de QA es contact@nezor.es.

**Pendiente (en orden):**
1. ⏳ Esperar el veredicto de QA (estado en Applications > App list). Si rechazan, mirar Defect Info en el
   borrador y corregir; cada reenvío pasa QA entera otra vez.
2. Probar la 1.0.0 en la LG real cuando esté encendida, sobre todo el paquete 720p (`OPENSSL_ENABLE_SHA1_SIGNATURES=1
   ares-install -d lg tv/dist/720/es.nezor.easyxtreamfootball_1.0.0_all.ipk`), incluido el Atrás de salida. Si
   algo falla, corregir y reenviar (QA Requested se puede retirar desde la lista de apps).

## Samsung TV (Tizen) — 1.0.0 RECHAZADA el 2026-10-23 (pantalla negra en Tizen 7-10); 1.0.1 REENVIADA ese mismo día a los 30 grupos

> La app de TV es la misma de `tv/` que la de LG (Tizen 5.5 = Chrome 69). Probada por el usuario en el Smart Monitor
> M7 el 2026-10-18. Build de tienda: `cd tv && npm run build:store` (sin lista de ejemplo) y firmar con `tizen package`.

**Cuenta (2026-10-22, madrugada):**
- La cuenta Samsung **contact@nezor.es ya existía** y estaba bloqueada (la recuperación de contraseña pide la fecha de
  nacimiento y fallaba). Salida: en account.samsung.com → Iniciar sesión → "Verificar con correo de un uso" (código
  al correo, sin contraseña) y luego Seguridad → "Verificar con correo de un uso" (enlace) → contraseña nueva puesta
  por el usuario. El perfil tiene fecha de nacimiento 11/06/1983, nombre Jorge Martínez, país España.
- El ticket de soporte 2358729350 (creado solo el 2026-10-19 desde el flujo de recuperación, sin texto) queda
  sin contestar: ya no hace falta.
- **Seller Office (seller.samsungapps.com/tv) dado de alta** con esa cuenta: nombre Jorge Martínez y correo
  contact@nezor.es (vienen de la cuenta Samsung, no se cambian ahí), Europa/España, zona horaria Madrid, términos y
  política de privacidad aceptados por el usuario. Tipo: **Public Seller → solo puede distribuir en EE. UU.**

**Pendiente (en orden):**
1. ⏳ **Partnership para España**: Membership → Partnership exige un acuerdo previo con el Samsung Content Manager
   local. **Consulta ENVIADA por 1:1 Q&A el 2026-10-22 a las 00:41** (categoría App Management > Group management,
   título "Partnership request to distribute a free app in Spain (Europe) - Easy Xtream Football"): quién es el
   Content Manager de España y qué requisitos tiene el contrato para una app gratuita de un desarrollador
   individual. La respuesta llega al 1:1 Q&A de Seller Office y por correo a contact@nezor.es.
   ✅ **RESPONDIDA el 2026-10-22 a las 11:27** (App Management Team, correo de tvapps@samsung.com a las 13:27): hay que
   escribir a **Nagore Bilbao (n.bilbao@samsung.com)**, responsable de la región de España, que explicará cómo entrar
   en Partner Support. ✅ **Correo ENVIADO por el usuario el 2026-10-22 a las 17:57** desde **martinezortiz@gmail.com**
   (firma con contact@nezor.es): presentación de la app, App ID, estado en las otras tiendas y petición de requisitos
   de Partner Support para España/Europa. ⚠️ La respuesta de Nagore llegará al Gmail personal, no a contact@nezor.es.
2. ✅ **Certificado nuevo HECHO el 2026-10-21 (23:51)**: perfil **`nezor`** (activo) en el Certificate Manager
   (`SWT_GTK3=1 ~/tizen-studio/tools/certificate-manager/certificate-manager`), autor "Jorge Martínez" con la cuenta
   contact@nezor.es, distribuidor Public con el DUID del M7 (`XTCHCXRMXD3UE`). Ficheros en `~/SamsungCertificate/nezor/`
   (p12 protegidos con la contraseña que solo sabe el usuario). **Caducan el 2027-10-21** (Samsung los da por un año;
   se renuevan con la misma cuenta). Copia: `~/Downloads/samsung-cert-nezor-2026-10-22.tar.gz` (pendiente de pasarla
   al USB de la clave de Play y borrarla de Downloads). El perfil `exf` (cuenta del padre) ya no se usa.
   **Paquete de tienda:** `platforms/tizen/config.xml` ahora es **versión 1.0.0** (misma numeración que LG);
   `cd tv && npm run strings && EXF_SAMPLE_LIST=false node build.mjs --platform=tizen && ~/tizen-studio/tools/ide/bin/tizen package -t wgt -s nezor -o "$PWD/dist" -- "$PWD/dist/tizen"`
   → `tv/dist/exf-store-1.0.0.wgt` (157 KB, firmado por el autor nuevo, verificado en author-signature.xml).
3. ✅ **App CREADA y formulario COMPLETO el 2026-10-22 (madrugada)**: ID de Seller Office **3202610049966**, nombre de
   gestión "Easy Xtream Football", Tizen Web (wgt), idioma por defecto inglés. Secciones (todas con tic):
   - App Package: `exf-store-1.0.0.wgt` subido, Pre-Test **Pass** (v1.0.0, EXFOOTBALL.EasyXtreamFootball, API 5.5,
     privilegios internet / system info / key capture; pointer y touch "not defined/support"), fuente no compartida con
     el grupo. Estado "Ready To Submit".
   - App Images: logo transparente `icon-fg-1920x1080.png` + fondo liso `background-plain-1920x1080.jpg` (los assets 16:9
     y 1:1 los genera Samsung), icono `icon-512x423.png` (logo centrado en fondo oscuro), 4 capturas JPG de
     `docs/store-assets/samsung/screenshots/` (lista, reproductor, menú, perfiles). ⚠️ Solo hay UN input de archivo para
     capturas: cada envío va a la casilla cuyo lápiz se pulsó antes (clic al `<i>` del lápiz vía JS con `input.click`
     anulado para que no abra el selector nativo).
   - Service Info: Sports, rating ALL, los 24 idiomas (listas mat-selection-list: clic a cada opción y luego `»`), sin
     datos personales, vendedor Nezor / contact@nezor.es / repo GitHub / Jorge Martínez, teléfono y dirección puestos
     por el usuario. Sin DoC (solo hace falta para la UE).
   - Title/Description on TV: título "Easy Xtream Football", descripción EN (1185 bytes), tags
     `IPTV,player,sports,football,M3U,Xtream` (sin espacios).
   - Service Country/Region: solo **United States of America** (lo único que ofrece a un Public Seller); sin
     certificación de edad.
   - Billing Info: Free. App Feature Info: Player (vídeo H.264/HEVC/MPEG2, audio AAC/AC3/HE-AAC/MP3/DD+, MPEG-TS y
     fMP4, sin DRM, HLS + HTTP(S), AVPlay, subtítulos "Other": pistas embebidas).
   - Verification Info: **App Description** = `docs/store-assets/samsung/app_description_easy_xtream_football.pptx`
     (16 diapositivas sobre la plantilla Samsung v1.42, generado con `app_description_build.py` + el venv de
     python-pptx + las capturas con rects de LG), Geo IP block "Unuse", sin cuenta de prueba: instrucciones con las
     dos listas públicas de prueba en el campo Test Account.
   - Trucos de la consola: los diálogos de confirmación y "saved" son de la página (no bloquean); los radios y
     checkboxes son de Angular Material (clic por coordenadas o al `<input>` interno vía JS, los refs no valen); la
     pestaña se bloqueó una vez al subir archivos en App Images (recargar).
4. ✅ **QR de Ko-fi fuera de la build de Samsung (decidido el 2026-10-22):** flag `EXF_COFFEE=false` (sin pestaña Coffee
   en el menú del reproductor, sin recordatorio flotante, sin entrada en Ajustes, sin `qr.png` en el paquete). Script
   `cd tv && npm run build:store:tizen` (flags + `tizen package -s nezor` + copia a `dist/exf-store-1.0.0.wgt`).
   Paquete anterior borrado del borrador y el nuevo subido (Pre-Test Pass). Si algún día se quiere el café en
   Samsung, se quita el flag y se sube una versión nueva (pasa QA otra vez).
5. ✅ **REQUEST NEW RELEASE ENVIADO el 2026-10-22 (~00:50)**: versión 1.0.0 (paquete sin QR), los **46 grupos de
   modelos de 2022 a 2026** (Tizen 6.5 → 10.0, incluidos los "LIC"/"LIC2" de terceros), novedades EN, actualización
   Optional, nota para el verificador con las listas de prueba, "release with minor defects" = Yes. Distribute → App
   Service muestra cada grupo como **Submitted · 100 % Rollout (scheduled)**. Solo EE. UU. mientras no haya partnership.

6. ❌ **RECHAZADA en Document Review el 2026-10-23 (correo de las 00:44)** en los **30 grupos Tizen 7.0-10.0** (modelos
   2023-2026, LIC incluidos); los 16 grupos de 2022 (Tizen 6.5) no aparecen en el correo. Único defecto, CRITICAL:
   *"Infinite media loading. After App has been installed when media is launched, it displays a black screen with
   audio."* Los tres streams de `docs/playlists/review-test.m3u` siguen vivos (H.264/AAC, comprobado ese día).
   **Causa más probable:** la app no creaba el `<object type="application/avplayer">` que la guía de AVPlay pide como
   paso 1 para marcar el área de vídeo; Tizen 5.5 (el M7) pinta el plano igualmente, los modelos nuevos no.
   **Arreglo (commit `a9c4bed`, versión Tizen 1.0.1):** `tv/src/platform/tizen.ts` crea ese `object` a pantalla
   completa detrás de la UI (z-index -1, primer hijo de `body`) al abrir el primer stream, y da por terminado el
   buffering en cuanto `oncurrentplaytime` avanza aunque no llegue `onbufferingcomplete` (el "infinite loading").
   `npm run build:store:tizen` nombra el paquete con la versión del config.xml → `tv/dist/exf-store-1.0.1.wgt`.
   ✅ Probado en el M7 (Tizen 5.5) el 2026-10-23: instalado con el certificado `nezor` (hubo que desinstalar la app
   firmada con `exf`: "Author certificate not match"), perfil M3U sembrado con review-test.m3u por el inspector,
   AVPlay PLAYING, tiempo avanzando, `object` presente 1920x1080, mensaje "Cargando…" oculto. ⚠️ **No se puede
   verificar en un Tizen 7+**: no hay tele de 2023+ a mano; la confirmación real la dará la nueva revisión.

7. ✅ **1.0.1 REENVIADA el 2026-10-23 (~13:30)**: App Package → Upload (el 1.0.0 "Submitted" no se puede borrar
   mientras lo revisen los otros grupos) → `exf-store-1.0.1.wgt`, Pre-Test Pass, fuente no compartida. Verification
   Info: párrafo nuevo sobre la 1.0.1 (object de AVPlay, "Cargando" se quita al avanzar), solo la lista de demos
   como prueba principal y aclaración de que las radios no tienen vídeo a propósito (1643/4000 bytes). Request New
   Release: versión 1.0.1, **los 30 grupos rechazados** (2024-2026 + 23TV_BASIC1_LIC/LIC2; los 16 de 2022-2023 en
   "Submitted" con la 1.0.0 no son seleccionables), OS por defecto, novedades EN, nota al verificador, Optional,
   minor defects Yes, dos "I have read and confirm". El POST `package/3/distribute` devolvió 200.
   Trucos de este formulario: los grupos se filtran por **año** (mat-select) y hay que marcar Select All y pulsar
   `»` año por año (el contador "Selection: N" está en la lista derecha); el aviso rojo "There is a language in
   which content is not entered" y "Please select the OS version" son texto fijo; **Next no cambia de página**,
   despliega los apartados 4-6 debajo (Pre-Test, info, consideraciones) y luego Done + Confirm. El detalle del
   defecto está en Distribute → App Service → clic en "Rejected" (Model Group History); App Review es solo beta.

8. ℹ️ **1.0.2 preparada, NO enviada (2026-10-23, commit `0097217`):** al probar en el M7, `getCurrentStreamInfo()`
   seguía diciendo 320x184 mientras AVPlay ya emitía `PLAYER_MSG_BITRATE_CHANGE` a 6,2 Mbps (la variante 1080p):
   la imagen era buena y la línea de info mentía. Ahora el adaptador guarda ese bitrate y la info y el menú muestran
   "x.x Mbps" en lugar de la resolución falsa (probado: BipBop "1.9 Mbps", PLAYING). `tv/dist/exf-store-1.0.2.wgt`
   queda como siguiente versión para cuando Samsung resuelva la 1.0.1; no conviene reenviar ahora y reiniciar la
   revisión. También se reparó el script `build:store:tizen` (escape inválido en package.json desde a9c4bed).

**Pendiente (en orden):**
1. ⏳ Veredicto de la nueva revisión (Distribute → App Service y App Review; avisos a contact@nezor.es). Si vuelven a
   rechazar por lo mismo: pedir por 1:1 Q&A el modelo y el stream exactos, y plantearse el `<video>` HTML5 como en
   webOS para HLS (litefin #510: "AVPlay negro, HTML player bien").
2. ⏳ **Partnership para España**: correo a la Content Manager **Nagore Bilbao <n.bilbao@samsung.com>** ENVIADO el
   2026-10-22 (17:57) desde martinezortiz@gmail.com. Esperar su respuesta **en ese Gmail**. Si el **2026-10-29** no ha
   contestado, reenviar el mismo texto desde **contact@nezor.es** (la cuenta de Seller Office), actualizando la
   versión a la 1.0.1 y mencionando que es un recordatorio del correo del 22. Cuando conteste: Membership → Partnership (acuerdo
   con ella), añadir España/Europa en Service Country/Region y adjuntar la Declaración de Conformidad (EAA) en
   Service Info punto 6.
3. Tipo de cuenta (individual) por el aviso DSA. **Averiguado el 2026-10-23:** la cuenta es un grupo "Default" sin tipo
   (API `sbs/membergroup/group-join-info`: `memberGroupAccountTypeCode: DEFAULT`); My Information no tiene el campo y
   Bank Account redirige. El tipo solo se elige en **Membership → Group Information → Create Group** (nombre del
   grupo, país, Account Type Company/Individual y datos de vendedor por defecto). ✅ **GRUPO CREADO el 2026-10-23**
   (formulario rellenado por Claude con los datos de Service Info, Done pulsado por el usuario): grupo **Nezor**,
   Public, **INDIVIDUAL**, España, vendedor Nezor / contact@nezor.es / repo / Jorge Martínez / +34 / dirección de
   Valencia (API `group-join-info`: `memberGroupAccountTypeCode: INDIVIDUAL`, `type: Public`, usuario MANAGER). El
   aviso DSA queda resuelto; "Request Partnership" ya cuelga de este grupo.
4. Pasar `~/Downloads/samsung-cert-nezor-2026-10-22.tar.gz` al USB de la clave de Play y borrarla de Downloads.

## Cambios en `main` sin publicar (desde la 0.1.11 / vc15)

> La carpeta `tv/` (app web para Samsung y LG, mezclada el 2026-10-18) y `docs/logos/` no entran en el APK:
> no afectan a Play ni a Amazon. Ver `tv/README.md`.
- ✅ **2026-10-14 · recordatorio del café discreto en móvil apaisado** (merge `9586c87`): en apaisado la
  tarjeta pasa a ser pequeña y flota en la esquina inferior derecha como en la tele (taza de 40 dp, una
  línea); en vertical sigue igual; la ficha de la radio del centro (logo, nombre, "Solo audio") es más
  pequeña en móvil. La tele no cambia. Probado en el Pixel 11 Pro. Irá en la próxima versión de Play
  (la siguiente `versionCode` libre es **17**; la 16 es la 0.1.12 de Amazon, nunca enviada).

## Pendiente ⏳ (en orden)
1. **Conseguir 12 testers reales** (correos de Google) para la prueba cerrada. Es el cuello de botella.
   - Opciones: amigos/familia · un **Grupo de Google** (groups.google.com) cuyo email se pega en
     Play → Pruebas cerradas → Testers → "Grupos de Google" · comunidades de intercambio (r/androiddev,
     grupos de "closed testing" en Telegram/Discord).
2. **Enviar la versión de Prueba cerrada a revisión** (solo con visto bueno explícito).
3. **Mantener la prueba ≥ 14 días con ≥ 12 testers** (requisito de cuentas personales nuevas).
4. ✅ **Acceso a Producción concedido** (solicitado 2026-09-06, aprobado antes del 2026-09-14).
   Pasos en Play Console: Prueba y lanzamiento → Producción → Países y regiones (Todo el mundo) →
   Crear versión (añadir bundle desde la biblioteca o subir el nuevo) → notas de versión → Revisar → Enviar a revisión.
5. **(TV)** Subir banner TV + capturas TV en la ficha cuando aparezca la sección Android TV.
6. ✅ **Producción** → 0.1.5 enviada a revisión el 2026-09-14 → **PUBLICADA el 2026-09-21**. 🎉
7. ✅ **Actualización 0.1.6 + materiales de Android TV PUBLICADOS el 2026-09-25** (envío nº 7 del 21 → *Publicada*
   en Actividad de envíos). Producción: versión 7 (0.1.6), 177 países.
8. ✅ **Copia de la clave de subida fuera de ambas máquinas** (2026-09-22, `.7z` cifrado en USB offline).
   ✅ **Clave restaurada también en la máquina Linux el 2026-09-22** (ver abajo): ya se puede firmar desde
   cualquiera de las dos.

9. ✅ **0.1.8 ENVIADA A REVISIÓN el 2026-09-26 (00:05)**: Producción 9 (0.1.8), lanzamiento completo, notas es/en,
   junto con la ficha es-ES con el párrafo nuevo y "24 idiomas" (decía "8"). ⏳ Esperando a Google (hasta 7 días).
   ⚠️ **La ficha de Play solo existe en español (es-ES)**: quien la abre desde fuera la ve en español aunque la app
   tenga 24 idiomas. **Acordado con el usuario el 2026-09-27: crear la ficha en-US en cuanto Google apruebe la
   0.1.8** (no antes, para no alargar la revisión en curso, que ya incluye el cambio de la ficha es-ES).
   Cómo: Fichas de Play Store → *Gestionar traducciones* → añadir Inglés (EE. UU.) → pegar nombre, descripción
   breve y completa de `docs/store-listing.md` (con el párrafo IMPORTANTE nuevo). Los gráficos se heredan. La 0.1.6 se publicó el 2026-09-25. En Amazon: *Add upcoming version*
   con el APK universal. En ambas tiendas **cambiar la frase "NO incluye ningún canal"** por el texto nuevo de
   `docs/store-listing.md`.

10. ⏳ **0.1.9 (planificada el 2026-09-24): propina con Google Play Billing.** Sustituir el QR de Ko-fi por un
    producto consumible "Invitar a un café" (1,99 / 4,99 / 9,99 €) en la sección Café del menú del reproductor.
    Solo en la versión de Play (móvil, Android TV, Chromecast); en la de Amazon se mantiene el QR. Antes de tocar
    código: crear el **perfil de pagos** en Play Console (datos fiscales y cuenta bancaria) y los productos in-app;
    probar con compradores de prueba. Motivo: Ko-fi resulta engorroso y el enlace externo está en zona gris de la
    política de pagos de Play.

## Revisión del estado a 2026-09-28 (lunes, máquina Linux)
- ⏳ **0.1.8 sigue "En revisión"**: Actividad de envíos → envío nº 8 (2026-09-26), Producción 9 (0.1.8) +
  ficha es-ES ("Cambiar descripción completa"), estado *En revisión*. Todos los envíos anteriores (1-7)
  figuran como *Publicada*. Última publicación: 2026-09-25. Van 2 días de los 7 que avisa Google.
- ℹ️ En notificaciones hay un aviso del 2026-09-26 (23:12): *"Se ha solucionado una infracción reciente
  de las políticas"* → detalle: **"La aplicación debe estar orientada a Android 16 (nivel 36 de la API)"**,
  estado *Infracción corregida*. Era la política de target API; la 0.1.8 ya va con `targetSdk = 36`, así
  que Google la dio por corregida al procesar el envío nº 8. *Estado según las políticas* de la app y de la
  cuenta: sin problemas. No requiere ninguna acción.
- ✅ **Cuenta bancaria VERIFICADA el 2026-09-28**: el microingreso "GOOGLE" llegó ese día (segundo día hábil)
  y el usuario tecleó el importe en Perfil de pagos
  ⏳ **2026-10-13: aviso nuevo en el Perfil de pagos: "proporciona tu información fiscal de Irlanda lo antes
  posible"** (retención de impuestos). Google Ireland es quien paga a los desarrolladores europeos; el
  formulario (Ajustes → Perfil de pagos → Gestionar configuración → Información fiscal → Irlanda) pide
  residencia fiscal (España), NIF, si hay establecimiento permanente en Irlanda (no) y si se pide exención o
  tipo reducido por convenio; puede pedir un documento de residencia (certificado de la AEAT, tarjeta del NIF
  o DNI vigente con dirección; pasaporte y carné de conducir no valen). Según la ayuda de Google
  (support.google.com/product-documentation/answer/14754400) hoy no limita la cuenta ni afecta a los pagos
  ni a la retención. **Lo rellena el usuario** (datos fiscales + documento de identidad).
  **2026-10-14:** abierto en la consola: Perfil de pagos → *Añadir información fiscal* (`?place=TAX_CENTER`,
  "Gestionar información fiscal", bloques Estados Unidos / **Irlanda** / Taiwán; solo Irlanda tiene aviso).
  El botón de Irlanda abre un diálogo "Antes de empezar" (3-5 min) que exige subir un **PDF/PNG/JPEG de un
  documento de régimen fiscal** (certificado de residencia fiscal). ⏳ **Pendiente: el usuario va a sacar el
  certificado de residencia fiscal de la AEAT** (Sede Electrónica → Certificados tributarios → Residencia
  fiscal, con Cl@ve) y luego rellenar el formulario (residencia España, NIF, sin establecimiento permanente
  en Irlanda, convenio España-Irlanda).
  (`https://play.google.com/console/u/1/developers/7549371768525842906/paymentssettings`; ojo, la ruta es
  `paymentssettings`, `payments-profile` redirige a la lista de apps) → *Cómo recibes los pagos* → *Gestionar
  métodos de pago* → **Verificar**. Comprobado en la consola esa misma tarde: la cuenta acabada en 6844 ya
  sale sin el aviso "Pendiente de verificar". Del perfil de pagos solo queda la cuota del 15 %.
  **Cuota del 15 %, revisado el 2026-09-28 por la tarde:** el banner del Perfil de pagos sigue listando los dos
  requisitos y en *Cuentas de desarrollador asociadas* **sigue sin aparecer "Revisar y registrarme"**; el
  menú *Gestionar* solo tiene lo de siempre. En el DOM de esa página existe, oculto, el diálogo de registro
  ("Grupo de cuentas que se registrará (Jorge Mtnez)", botones *Cancelar* / **Confirmar y ver términos**),
  que dice *"Cuando termines de configurar tu grupo de cuentas, podrás registrarte"*: parece que se dispara
  al terminar el flujo de **Empezar** (declarar cuentas asociadas), no desde un botón suelto.
  **Confirmado con la ayuda de Google** (answer/10632485 y answer/10627869): el grupo no está "terminado"
  hasta responder a *"Cuentas de desarrollador propiedad de tu entidad legal"* → **No** (aunque solo haya
  una cuenta). El 27 solo se creó el grupo con nombre; falta ese paso. **Qué hacer:** *Cuentas de desarrollador
  asociadas* → **Empezar** → responder **No** → guardar → debería aparecer el diálogo de registro →
  **Confirmar y ver términos** → **Aceptar y registrarme**. Sin esto Google aplica el 30 %.
- ✅ **CUOTA DEL 15 % REGISTRADA el 2026-09-29.** El usuario rellenó el formulario de *Empezar* (las dos
  preguntas, "¿tu entidad legal es propietaria de otra cuenta?" y "¿otras cuentas con recursos de marca
  similares?", ambas **No**) y guardó; al momento apareció el aviso "Revisar y registrarme", aceptó los
  términos y la consola muestra *"Tu grupo de cuentas se ha registrado para obtener una cuota de servicio
  del 15 %"* y, al pie de *Cuentas de desarrollador asociadas*, "Programas y servicios en los que te has
  registrado: Cuota de servicio del 15 %". El banner del Perfil de pagos ha desaparecido y no queda ningún
  aviso en rojo (el "problema urgente con tu cuenta de pagos" del 28 era la verificación del banco).
  **El perfil de pagos está completo: banco verificado + cuota del 15 %.** Ya no queda nada pendiente de
  pagos para publicar la 0.1.10 en Producción.
- ⏳ **0.1.8 (vc9) sigue "en revisión" el 2026-09-29** (Resumen de publicación: última publicación el 25 de
  septiembre; en revisión la versión 9 y el cambio de descripción de la ficha es-ES). La 0.1.6 sigue siendo la
  disponible en Play.
- ➡️ Siguiente paso en cuanto Google apruebe la 0.1.8: mandar la **0.1.10 (vc14) a Producción por fases al
  20 %**, crear la ficha en-US y subir la 0.1.8 a Amazon como upcoming version.

## Otros TODO de calidad (no bloquean la publicación)
- ✅ **Lint sin errores (2026-10-01, mezclado en `main`).** Los dos errores de
  siempre (`app_name` y `settings_licenses_body` "sin traducir" en 23 idiomas) se arreglan marcando ambas
  cadenas `translatable="false"`: el nombre de la app no se traduce y el texto de licencias es una lista
  de nombres propios. `lintFullRelease`: 0 errores, 66 avisos; 75 tests en verde.
- ❌ **Símbolos de depuración nativos: no se puede quitar el aviso de Play.** Probado el 2026-10-01 con
  `ndk { debugSymbolLevel = "SYMBOL_TABLE" }` en release: el AAB sale **sin**
  `BUNDLE-METADATA/…debugsymbols`, porque las `.so` de FFmpeg (NextLib) y de Media3 llegan ya recortadas
  en sus AAR (sin `.symtab`) y no hay de dónde extraer símbolos. Solo se resolvería compilando FFmpeg
  desde el código. Ajuste revertido; el aviso es inofensivo.
- ✅ **Móvil: menú y mensajes rediseñados (2026-09-27).** Lo que fallaba: la capa de gestos a pantalla
  completa está por debajo de los overlays y Compose para el hit-test en el hermano de arriba, así que
  un deslizamiento que empezaba sobre la tarjeta del menú no llegaba nunca a esa capa; en vertical esa
  tarjeta de 280 dp ocupaba casi todo el ancho abajo a la izquierda y dejaba la ayuda de navegación y
  la leyenda de controles peleando por la esquina inferior derecha, donde además se ancla la tarjeta
  del café, que al dibujarse después las tapaba; y los mensajes de ayuda iban en texto pelado sobre el
  vídeo, sin la caja oscura del resto. Qué se hizo (rama `feature/mobile-menu-swipe`, 3 commits):
  **hoja inferior** a todo el ancho con asa, pestañas tocables (`menuSections` / `menuSectionIndex` /
  `selectMenuSection` nuevos en el ViewModel) y filas de 48 dp; el **Café** va dentro de la hoja; todo
  lo del borde inferior cuelga de **una sola columna**, así que no puede solaparse; los elementos
  centrados (radio, pausa) se centran en el hueco libre **medido** por encima de la hoja; la lista se
  limita al 40 % de la altura; la **leyenda pasa a fichas**, troceando la cadena existente por el
  separador `·` y pintando el gesto en verde (sin cadenas nuevas que traducir en los 24 idiomas); el
  mensaje de error también lleva caja. **La tele no cambia.** Verificado por el usuario en el Xiaomi.
  ✅ **Verificado también en el Chromecast (2026-09-27)** con la build `.debug` instalada al lado de la
  de Play: ficha del canal abajo a la izquierda, tarjeta del menú con el encabezado nuevo, leyenda de
  mando abajo a la derecha y la radio centrada, sin solapes. De paso se arregló un solape que **ya
  existía antes** en la tele: con la sección Café abierta, la tarjeta del QR se dibuja sobre la ayuda
  de navegación y la dejaba medio tapada; ahora esa ayuda se oculta mientras el QR está en pantalla.
  ✅ **Segunda tanda del 2026-09-27**, con el usuario mirando móvil y tele a la vez:
  1. **La ayuda en fichas también en la tele y en el menú.** El componente pasa a ser genérico
     (`HintChips`) y lo usan la leyenda táctil, la de mando y la ayuda de navegación.
  2. **El icono de la radio dejaba de medir 168 dp** cuando subía la hoja: el área centrada se
     reducía y las constraints lo comprimían. Ahora se mide con `wrapContentSize(unbounded = true)`,
     conserva su tamaño y solo se desplaza.
  3. **La columna inferior se dibuja DESPUÉS de los elementos centrados**, así el menú queda por
     encima (lo pidió el usuario) en vez de que la tarjeta de la radio tape las pestañas.
  4. **En la tele, ficha del canal y ayuda comparten una fila alineada abajo.** Antes la ayuda iba
     anclada a la esquina y la ficha la subía la tarjeta del menú, así que al abrirlo se desalineaban.
  5. **Misma receta visual para las dos.** La ayuda usaba 14 dp / 90 % / relleno 10 / labelMedium y la
     ficha del canal 8 dp / 60 % / 10-5 / labelSmall. Ahora la ayuda copia la de la ficha y cada
     fragmento es su propia pastilla, sin tarjeta alrededor. **Si se toca `StatsOverlay`, tocar
     también `HintChips`: van emparejados a propósito.**
- ✅ **2026-09-27 · versionCode 14 (0.1.10) PUBLICADA EN PRUEBA INTERNA (19:28)**. AAB firmado en
  `~/Downloads/easy-xtream-0.1.10-vc14/`. Lleva todo el rediseño del reproductor en móvil y los
  ajustes de la tele. Única advertencia: la de siempre, símbolos de depuración nativos (opcional).
  **Decisión de estrategia, acordada con el usuario:** no elegir todavía entre 0.1.9 y 0.1.10. La
  0.1.10 va a Prueba interna ya (se puede aunque la 0.1.8 siga en revisión, como se hizo ayer con
  vc11-13) y, cuando Google apruebe la 0.1.8, **si la 0.1.10 va limpia se manda ella directa a
  Producción y se salta la 0.1.9**: una revisión en vez de dos. Si algo falla, se publica la vc13,
  que está probada de punta a punta con una compra real. En cualquier caso, **lanzamiento por fases
  al 20 %**, por ser la primera versión en producción con pagos.
  ✅ **Verificada en el móvil el 2026-09-27** con la build firmada por Google, instalada desde el
  enlace de la prueba interna (el usuario: *"en móvil funciona como espero"*). Instalarla por USB no
  se pudo: HyperOS bloquea instalar un paquete **nuevo** por `adb` (actualizar uno ya instalado sí),
  y además tocando el APK el instalador quedaría como la app de Archivos y la facturación podría no
  encontrar los productos; **desde la Play Store queda `installerPackageName=com.android.vending`**,
  que es lo que hace falta. Ojo: Play tardó unos minutos en servir la 14, primero seguía dando la 13.
  ✅ **Verificada también en el Chromecast el 2026-09-27** (*"en la tele también la probé y de momento
  funciona como espero"*). **Las dos plataformas están validadas con la build firmada por Google, así
  que la 0.1.10 es la candidata a Producción y se salta la 0.1.9.**
  ✅ **Compra de prueba completada en el móvil con la 0.1.10** el 2026-09-27: cuarto pedido en Gestión
  de pedidos, "Prueba: Un café" (`coffee_small`), 1,99 EUR, Procesado. Al ser tester de licencias,
  Play sustituye el método de pago por la **tarjeta de prueba que siempre aprueba** y no se cobra
  nada (existe también la variante que siempre rechaza, para probar pagos fallidos).
  **La 0.1.10 queda validada de punta a punta en móvil y tele: interfaz y pago.**

  ⚠️ Al reinstalar la build `.debug` en el Chromecast se perdieron los perfiles (la base de datos
  aparece recreada y vacía). Hay que volver a pulsar "Probar con listas de ejemplo" tras cada
  reinstalación en esa tele.
  ⚠️ Para probar en el Xiaomi: HyperOS **bloquea `adb install` y la inyección de toques** (pide cuenta
  Xiaomi para "Instalar vía USB"). Se resuelve con `applicationIdSuffix = ".debug"` (nuevo en
  `build.gradle.kts`): la build de pruebas entra **al lado** de la de Play, sin desinstalar nada ni
  perder perfiles. El APK se pasa con `adb push` a `/sdcard/Download` **y hay que indexarlo**
  (`content call --uri content://media/external/file --method scan_file --arg <ruta>`) o la app
  Archivos no lo ve. La leyenda solo sale 3 veces: se reinicia con
  `adb shell run-as com.footballxtream.debug rm files/datastore/settings.preferences_pb`.
  ⚠️ Un `adb install` puede decir *Success* y dejar un APK viejo: **comparar `md5sum` del APK local con
  el de `pm path`** antes de dar por buena una prueba.
- Prueba en **hardware flojo** compatible (Fire TV Stick 3ª gen/Lite/4K con Fire OS 7, o Android TV
  reciente). El Fire Stick 2ª gen (Fire OS 5 / Android 5.1, API 22) **no es compatible** (< minSdk 24).
- (Opcional) Subir **símbolos de depuración nativos** para mejores informes de fallos.
- ✅ Microsite (`easy-xtream-football-web`) subido a GitHub y desplegado en Vercel:
  https://easy-xtream-football-web.vercel.app (auto-deploy en cada push).
  ✅ **2026-09-27**: enlace a la web personal del autor (https://martinez.place, `/es/` en español) en el pie,
  detrás de "Hecho por Jorge Mtnez" / "Made by Jorge Mtnez". Rama `feature/author-link` fusionada en `main`.
