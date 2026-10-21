/**
 * The sample radio playlist offered when there is no profile yet (see the Android SampleLists). Served through
 * jsDelivr rather than raw.githubusercontent.com: GitHub raw uses Let's Encrypt and 2019 TVs (webOS 4.x) do not
 * trust the ISRG Root X1, so every request fails with status 0.
 */
export const SPORTS_RADIO_URL = 'https://cdn.jsdelivr.net/gh/nezor11/easy-xtream-football@main/docs/playlists/sports-radio.m3u';
/** Store builds hide the sample list (a store rejected the app over content it offered itself): `npm run build:store`. */
declare const __EXF_SAMPLE_LIST__: boolean;
export const SAMPLE_LIST: boolean = typeof __EXF_SAMPLE_LIST__ === 'boolean' ? __EXF_SAMPLE_LIST__ : true;
/** Samsung's seller terms take 30 % of any revenue "arising from the app": the Samsung store build hides the coffee card (`EXF_COFFEE=false`). */
declare const __EXF_COFFEE__: boolean;
export const COFFEE: boolean = typeof __EXF_COFFEE__ === 'boolean' ? __EXF_COFFEE__ : true;
