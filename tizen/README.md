# Easy Xtream Football para Samsung TV (Tizen)

Versión web de la app para teles y monitores Samsung con Tizen 5.5 o posterior. Misma lógica de datos que la
app Android (portada a TypeScript: parser M3U, parser de nombres y filtro de deporte, agrupación, cliente
Xtream, guía XMLTV) y los mismos 24 idiomas (convertidos desde `app/src/main/res/values*/strings.xml` en cada
compilación). Reproduce con AVPlay, el reproductor nativo de Samsung. Sin facturación: el café va con el QR.

## Estructura
- `src/core/`: modelo, parsers, repositorio, almacenamiento (localStorage), i18n, envoltorio de AVPlay.
- `src/ui/`: motor de foco para el mando (`focus.ts`), pila de pantallas, perfiles, añadir perfil, canales,
  reproductor, ajustes.
- `www/`: `config.xml`, `index.html`, `style.css`, icono y QR; `app.js` y `strings.json` se generan.
- `scripts/strings.py`: strings.xml → strings.json. `tests/`: pruebas de los parsers (`node --test tests/`,
  con `EXF_M3U=<lista>` prueba una lista real).

## Compilar, probar e instalar
```
npm install
npm run build                       # strings.json + www/app.js (esbuild, target chrome69)
npm run check                       # tsc
python3 -m http.server 8788 --directory www   # prueba en un navegador de escritorio (todo menos el vídeo)
T=~/tizen-studio
$T/tools/sdb connect <ip-tele>:26101
$T/tools/ide/bin/tizen package -t wgt -s exf -o "$PWD/dist" -- "$PWD/www"
cp "dist/Easy Xtream Football.wgt" dist/exf.wgt
$T/tools/ide/bin/tizen install -n dist/exf.wgt -s <ip-tele>:26101
$T/tools/sdb -s <ip-tele>:26101 shell 0 was_execute EXFOOTBALL.EasyXtreamFootball
```
El fondo de la página es transparente a propósito: AVPlay pinta el vídeo en un plano por detrás de la página.
Las teclas de color, canal ±, reproducción e Info se registran con `tizen.tvinputdevice`.
