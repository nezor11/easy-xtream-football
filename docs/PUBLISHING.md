# Publicación en Google Play — estado y pasos

Checklist vivo del proceso de publicación de **Easy Xtream Football**.
Última actualización: 2026-09-21.

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

## Amazon Appstore (Fire TV) — preparado el 2026-09-22
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
- ⏳ **Pasos (el usuario, en developer.amazon.com):** 1) crear la cuenta de desarrollador (gratis; requiere
  aceptar el acuerdo de distribución) → 2) *Apps & Services → Add New App → Android* → 3) subir el APK
  universal → 4) *Device support*: Fire TV (todos los modelos compatibles que ofrezca) y, si se quiere, Fire
  tablets y móviles Android → 5) *Content rating*: IARC con el Global Rating ID → 6) ficha: título, descripciones
  corta/larga, palabras clave, categoría Entretenimiento, icono 512 y 114, capturas, imagen promocional,
  política de privacidad, correo de soporte → 7) enviar. Revisión: días. Anotar aquí el resultado.
- Amazon **re-firma** el APK con su certificado: no afecta a nada (la app no verifica firmas ni usa licencias).

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
7. ✅ **Actualización 0.1.6** firmada y **enviada a revisión el 2026-09-21**, junto con los materiales de
   Android TV; factor de forma Android TV ya activo. ⏳ Esperando aprobación de Google.
8. ✅ **Copia de la clave de subida fuera de ambas máquinas** (2026-09-22, `.7z` cifrado en USB offline).
   ✅ **Clave restaurada también en la máquina Linux el 2026-09-22** (ver abajo): ya se puede firmar desde
   cualquiera de las dos.

## Otros TODO de calidad (no bloquean la publicación)
- Prueba en **hardware flojo** compatible (Fire TV Stick 3ª gen/Lite/4K con Fire OS 7, o Android TV
  reciente). El Fire Stick 2ª gen (Fire OS 5 / Android 5.1, API 22) **no es compatible** (< minSdk 24).
- (Opcional) Subir **símbolos de depuración nativos** para mejores informes de fallos.
- ✅ Microsite (`easy-xtream-football-web`) subido a GitHub y desplegado en Vercel:
  https://easy-xtream-football-web.vercel.app (auto-deploy en cada push).
