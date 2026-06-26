# Publicación en Google Play — estado y pasos

Checklist vivo del proceso de publicación de **Easy Xtream Football**.
Última actualización: 2026-06-23.

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

## Pendiente ⏳ (en orden)
1. **Conseguir 12 testers reales** (correos de Google) para la prueba cerrada. Es el cuello de botella.
   - Opciones: amigos/familia · un **Grupo de Google** (groups.google.com) cuyo email se pega en
     Play → Pruebas cerradas → Testers → "Grupos de Google" · comunidades de intercambio (r/androiddev,
     grupos de "closed testing" en Telegram/Discord).
2. **Enviar la versión de Prueba cerrada a revisión** (solo con visto bueno explícito).
3. **Mantener la prueba ≥ 14 días con ≥ 12 testers** (requisito de cuentas personales nuevas).
4. **Solicitar acceso a Producción** (se desbloquea al cumplir 12 testers / 14 días) y responder las
   preguntas sobre la prueba cerrada.
5. **(TV)** Subir banner TV + capturas TV en la ficha cuando aparezca la sección Android TV.
6. **Producción** → enviar a revisión → publicada. 🎉

## Otros TODO de calidad (no bloquean la publicación)
- Prueba en **hardware flojo** compatible (Fire TV Stick 3ª gen/Lite/4K con Fire OS 7, o Android TV
  reciente). El Fire Stick 2ª gen (Fire OS 5 / Android 5.1, API 22) **no es compatible** (< minSdk 24).
- (Opcional) Subir **símbolos de depuración nativos** para mejores informes de fallos.
- Microsite (`easy-xtream-football-web`): pendiente de subir a GitHub + desplegar en Vercel.
