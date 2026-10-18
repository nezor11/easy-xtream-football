// Runs the ported parsers over a real playlist (path in EXF_M3U) and prints what the Android app would show.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as esbuild from 'esbuild';

const bundle = await esbuild.build({
  stdin: { contents: `export * from './src/core/m3u'; export * from './src/core/grouping'; export * from './src/core/names'; export * as logos from './src/core/logos';`, resolveDir: process.cwd(), loader: 'ts' },
  bundle: true, format: 'esm', write: false, target: ['node22'],
});
const mod = await import('data:text/javascript;base64,' + Buffer.from(bundle.outputFiles[0].text).toString('base64'));

test('channel name parser', () => {
  assert.equal(mod.ChannelNameParser.quality('LaLigaTVHD'), 'HD');
  assert.equal(mod.ChannelNameParser.quality('M+ LaLiga 4K'), 'UHD_4K');
  assert.equal(mod.ChannelNameParser.baseName('ES| DAZN LaLiga HD'), 'DAZN LaLiga');
  assert.equal(mod.ChannelNameParser.groupKey('DAZN LaLiga FHD'), mod.ChannelNameParser.groupKey('|ES| DAZN LaLiga HD 🇪🇸'));
  assert.ok(mod.ChannelNameParser.isSports('Eurosport 1', null));
  assert.ok(!mod.ChannelNameParser.isSports('Transport TV', 'Documentales'));
  assert.ok(mod.ChannelNameParser.isFootball('DAZN LaLiga', null));
  assert.equal(mod.ChannelNameParser.folderKey('Eurosport 1'), 'eurosp');
  assert.equal(mod.ChannelNameParser.folderKey('M+ LaLiga'), 'mlalig');
  assert.ok(mod.ChannelNameParser.isRadioCategory('Radios deportivas'));
  assert.ok(!mod.ChannelNameParser.isRadioCategory('Radiotelevisión'));
});

test('real playlist', { skip: !process.env.EXF_M3U }, () => {
  const text = readFileSync(process.env.EXF_M3U, 'utf8');
  const t0 = Date.now();
  const channels = mod.M3uParser.parse(text);
  const groups = mod.buildGroups(channels);
  const folders = mod.foldIntoFolders(groups);
  console.log(`live channels ${channels.length}, sports groups ${groups.length}, folders ${folders.length}, ${Date.now() - t0} ms`);
  console.log('football folders:', folders.filter((f) => f.isFootball).slice(0, 8).map((f) => `${f.name} (${f.channels.length})`).join(' · '));
  assert.ok(groups.length > 0);
});

test('logo matching candidates (port of LogoMatchingTest)', () => {
  const c = mod.logos.candidates;
  assert.equal(c('Canal Deporte 2')[0], 'canaldeporte2');
  assert.ok(c('Canal Sport 1').includes('canalsports1'));
  assert.ok(c('Canal Sports').includes('canalsport'));
  assert.ok(c('Canal Deporte TV').includes('canaldeporte'));
  const keys = c('Pais Canal Deporte Norte Alternate');
  assert.ok(keys.indexOf('paiscanaldeportenorte') < keys.indexOf('canaldeportenortealternate'));
  assert.ok(keys.includes('paiscanaldeporte'));
  assert.ok(!keys.includes('canaldeportenorte'));
  assert.ok(!c('Canal Deporte 2').includes('canal'));
  assert.ok(!c('Canal Deporte 2').includes('deporte2'));
  assert.ok(!c('Pais TV Sports').includes('tvsports'));
  assert.ok(!c('Pais TV Sports').includes('tvsport'));
  assert.ok(c('Pais Canal Deporte 2').includes('canaldeporte2'));
  assert.equal(mod.logos.normalize('Rádio Ñandú 1!'), 'rádioñandú1');
});
