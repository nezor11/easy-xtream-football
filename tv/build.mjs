// Bundles src/app.ts for a TV platform and assembles its package directory.
//   node build.mjs                      → Tizen bundle into www/app.js (desktop testing, Tizen packaging)
//   node build.mjs --platform=webos     → dist/webos/ with appinfo.json, ready for ares-package
//   node build.mjs --platform=tizen     → dist/tizen/ with config.xml, ready for tizen package
// Env: EXF_LOG_URL (remote console for debugging), EXF_SAMPLE_LIST=false (store build).
import * as esbuild from 'esbuild';
import { cpSync, mkdirSync, readFileSync, rmSync, writeFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const args = Object.fromEntries(process.argv.slice(2).filter((a) => a.startsWith('--')).map((a) => { const [k, v] = a.slice(2).split('='); return [k, v ?? true]; }));
const platform = args.platform || 'tizen';
const watch = !!args.watch;
const toDist = !!args.platform;
const outDir = toDist ? join('dist', platform) : 'www';

// Tizen 5.5 = Chrome 69; webOS 4.x (2018-2019 TVs) = Chrome 53, webOS 6 = Chrome 79. The lowest target serves all:
// esbuild lowers async/await, object spread and `??` for it; the few missing APIs are in src/polyfills.ts.
const options = {
  entryPoints: ['src/app.ts'],
  bundle: true,
  format: 'iife',
  target: ['chrome53'],
  outfile: join(outDir, 'app.js'),
  sourcemap: false,
  minify: false,
  legalComments: 'none',
  define: {
    __EXF_PLATFORM__: JSON.stringify(platform),
    __EXF_LOG_URL__: JSON.stringify(process.env.EXF_LOG_URL ?? ''),
    __EXF_SAMPLE_LIST__: process.env.EXF_SAMPLE_LIST === 'false' ? 'false' : 'true',
  },
};

const PLATFORM_SCRIPTS = {
  tizen: '<script src="$WEBAPIS/webapis/webapis.js"></script>',
  webos: '',
};

function assemble() {
  if (!toDist) return;
  rmSync(outDir, { recursive: true, force: true });
  mkdirSync(outDir, { recursive: true });
  for (const f of ['style.css', 'icon.png', 'qr.png', 'strings.json']) cpSync(join('www', f), join(outDir, f));
  const html = readFileSync(join('www', 'index.html'), 'utf8').replace('<!--PLATFORM_SCRIPTS-->', PLATFORM_SCRIPTS[platform] ?? '');
  writeFileSync(join(outDir, 'index.html'), html);
  const extra = join('platforms', platform);
  if (existsSync(extra)) cpSync(extra, outDir, { recursive: true });
}

assemble();
if (watch) {
  const ctx = await esbuild.context(options);
  await ctx.watch();
  console.log('watching…');
} else {
  await esbuild.build(options);
  console.log(`built ${options.outfile} (${platform})`);
}
