# Publicación en Google Play — estado y pasos

Checklist vivo del proceso de publicación de **Easy Xtream Football**.
Última actualización: 2026-09-16.

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
- **Ramas preparadas (NO mezclar hasta que la app esté publicada)**, hechas el 2026-09-16:
  - `feature/play-url` (este repo): `docs/portfolio-card.md` con la URL definitiva de Play en vez de `{{PLAY_URL}}`.
  - `feature/play-store-link` (repo `easy-xtream-football-web`): botón "Descargar en Google Play" en el hero y
    enlace en el footer; lint y build OK. Al mezclar en `main`, Vercel despliega solo.

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
6. ✅ **Producción** → 0.1.5 enviada a revisión el 2026-09-14 → ⏳ esperando aprobación → publicada. 🎉
7. **Actualización 0.1.6** firmada a Producción en cuanto la 0.1.5 esté publicada.

## Otros TODO de calidad (no bloquean la publicación)
- Prueba en **hardware flojo** compatible (Fire TV Stick 3ª gen/Lite/4K con Fire OS 7, o Android TV
  reciente). El Fire Stick 2ª gen (Fire OS 5 / Android 5.1, API 22) **no es compatible** (< minSdk 24).
- (Opcional) Subir **símbolos de depuración nativos** para mejores informes de fallos.
- ✅ Microsite (`easy-xtream-football-web`) subido a GitHub y desplegado en Vercel:
  https://easy-xtream-football-web.vercel.app (auto-deploy en cada push).
