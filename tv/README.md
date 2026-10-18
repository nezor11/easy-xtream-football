# Easy Xtream Football para Samsung TV (Tizen) y LG (webOS)

Versión web de la app para teles y monitores Samsung con Tizen 5.5 o posterior. Misma lógica de datos que la
app Android (portada a TypeScript: parser M3U, parser de nombres y filtro de deporte, agrupación, cliente
Xtream, guía XMLTV) y los mismos 24 idiomas (convertidos desde `app/src/main/res/values*/strings.xml` en cada
compilación). Reproduce con AVPlay, el reproductor nativo de Samsung. Sin facturación: el café va con el QR.

## Estructura
- `src/core/`: modelo, parsers, repositorio, almacenamiento (localStorage), i18n, envoltorio de AVPlay.
- `src/ui/`: motor de foco para el mando (`focus.ts`), pila de pantallas, perfiles, añadir perfil, canales,
  reproductor, ajustes.
- `src/platform/`: lo que cambia entre marcas (reproductor AVPlay o vídeo HTML5, teclas del mando, salida,
  panel UHD). `build.mjs --platform=tizen|webos` elige uno y deja en `dist/<plataforma>/` el directorio listo
  para empaquetar, con `platforms/tizen/config.xml` o `platforms/webos/appinfo.json` e iconos.
- `www/`: `index.html`, `style.css`, icono y QR; `app.js` y `strings.json` se generan.
- `scripts/strings.py`: strings.xml → strings.json. `tests/`: pruebas de los parsers (`npm test`,
  con `EXF_M3U=<lista>` prueba una lista real). `scripts/logo-coverage.mjs <lista>`: cuántos canales de una
  lista reciben logo del mapa `docs/logos/sports.json` (lo genera `scripts/build-logo-map.py` en la raíz y
  lo refresca cada semana la acción `logo-map.yml`; la app lo descarga y lo cachea 7 días en localStorage).

## Compilar, probar e instalar
```
npm install
npm run build                       # strings.json + www/app.js (esbuild, target chrome69)
npm run check                       # tsc
python3 -m http.server 8788 --directory www   # prueba en un navegador de escritorio (todo menos el vídeo)
# Samsung
node build.mjs --platform=tizen
T=~/tizen-studio
$T/tools/sdb connect <ip-tele>:26101
$T/tools/ide/bin/tizen package -t wgt -s exf -o "$PWD/dist" -- "$PWD/dist/tizen"
cp "dist/Easy Xtream Football.wgt" dist/exf.wgt
$T/tools/ide/bin/tizen install -n dist/exf.wgt -s <ip-tele>:26101
$T/tools/sdb -s <ip-tele>:26101 shell 0 was_execute EXFOOTBALL.EasyXtreamFootball

# LG (CLI: npm install @webos-tools/cli; simulador: webOS TV Simulator de webostv.developer.lge.com)
node build.mjs --platform=webos
ares-package dist/webos -o dist                              # → dist/es.nezor.easyxtreamfootball_<v>_all.ipk
ares-launch -s 26 -sp <dir del simulador> "$PWD/dist/webos"   # en el simulador (sin empaquetar)
ares-setup-device && ares-install -d <tele> dist/*.ipk && ares-launch -d <tele> es.nezor.easyxtreamfootball
```
En LG el vídeo va por el elemento `<video>` del sistema (MPEG-TS, H.264/HEVC, MPEG audio, Dolby); el
simulador NO reproduce como una tele, solo sirve para la interfaz. Atrás es el keyCode 461
(`disableBackHistoryAPI` en `appinfo.json`); el puntero del Magic Remote enfoca lo que sobrevuela.
El fondo de la página es transparente a propósito: AVPlay pinta el vídeo en un plano por detrás de la página.
Las teclas de color, canal ±, reproducción e Info se registran con `tizen.tvinputdevice`.

## Depurar en la tele
- Tizen 5.5 es Chrome 69: sin `inset`, sin `gap` en flexbox, sin `trimStart`. El CSS usa márgenes entre hermanos.
- `sdb shell 0 debug EXFOOTBALL.EasyXtreamFootball` lanza la app con el inspector y devuelve un puerto;
  `sdb forward tcp:P tcp:P` y `http://127.0.0.1:P/json` dan el websocket del DevTools Protocol
  (`Runtime.evaluate` funciona; `Page.captureScreenshot` se cuelga). Las teclas del mando se simulan
  despachando `KeyboardEvent` con el `keyCode` de Tizen (Atrás 10009).
- `console.log` no llega a `sdb dlog`: compilando con `EXF_LOG_URL=http://<pc>:8787/log npm run build` la
  app reenvía la consola y los errores por POST a ese servidor.
- Los campos de texto solo entran en edición (teclado de Samsung) al pulsar OK; Atrás o Listo salen.
