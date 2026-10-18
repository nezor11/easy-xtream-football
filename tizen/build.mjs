// Bundles src/app.ts into www/app.js for the Tizen 5.5 webview (Chromium 69 → ES2017, no modules).
import * as esbuild from 'esbuild';
const watch = process.argv.includes('--watch');
const options = {
  entryPoints: ['src/app.ts'],
  bundle: true,
  format: 'iife',
  target: ['chrome69'],
  outfile: 'www/app.js',
  sourcemap: false,
  minify: false,
  legalComments: 'none',
  define: { __EXF_LOG_URL__: JSON.stringify(process.env.EXF_LOG_URL ?? ''), __EXF_SAMPLE_LIST__: process.env.EXF_SAMPLE_LIST === 'false' ? 'false' : 'true' },
};
if (watch) {
  const ctx = await esbuild.context(options);
  await ctx.watch();
  console.log('watching…');
} else {
  await esbuild.build(options);
  console.log('built www/app.js');
}
