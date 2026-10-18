// Dev tool: how many channels of a playlist get a logo from docs/logos/sports.json. Usage: node scripts/logo-coverage.mjs <playlist.m3u>
import { readFileSync } from 'node:fs';
import * as esbuild from 'esbuild';
const b = await esbuild.build({ stdin: { contents: `export * from './src/core/m3u'; export * from './src/core/grouping'; export * from './src/core/names'; export * as logos from './src/core/logos';`, resolveDir: process.cwd(), loader: 'ts' }, bundle: true, format: 'esm', write: false, target: ['node22'] });
const mod = await import('data:text/javascript;base64,' + Buffer.from(b.outputFiles[0].text).toString('base64'));
globalThis.localStorage = { getItem: () => null, setItem: () => {} };
const doc = JSON.parse(readFileSync('../docs/logos/sports.json', 'utf8'));
globalThis.fetch = async () => ({ ok: true, status: 200, text: async () => JSON.stringify(doc) });
const S = process.argv[2];
const channels = mod.M3uParser.parse(readFileSync(S, 'utf8'));
const groups = mod.buildGroups(channels);
await mod.logos.ensureLogoMap();
let byName = 0;
const enriched = groups.map((g) => { const u = g.iconUrl || mod.logos.logoFor(g.displayName); if (u && !g.iconUrl) byName++; return u ? { ...g, iconUrl: u } : g; });
const filled = mod.fillFromFolderSiblings(enriched);
console.log(`groups ${groups.length}, with own logo ${groups.filter(g=>g.iconUrl).length}, by name ${byName}, after siblings ${filled.filter(g=>g.iconUrl).length}`);
console.log('sample:', filled.filter(g=>g.iconUrl).slice(0,12).map(g=>g.displayName+' -> '+g.iconUrl.replace('https://i.imgur.com/','i:')).join(' | '));
console.log('missing:', filled.filter(g=>!g.iconUrl).slice(0,25).map(g=>g.displayName).join(' | '));
