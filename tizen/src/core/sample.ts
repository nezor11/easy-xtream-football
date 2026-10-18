/** The sample radio playlist offered when there is no profile yet (see the Android SampleLists). */
export const SPORTS_RADIO_URL = 'https://raw.githubusercontent.com/nezor11/easy-xtream-football/main/docs/playlists/sports-radio.m3u';
/** Store builds hide the sample list (a store rejected the app over content it offered itself): `npm run build:store`. */
declare const __EXF_SAMPLE_LIST__: boolean;
export const SAMPLE_LIST: boolean = typeof __EXF_SAMPLE_LIST__ === 'boolean' ? __EXF_SAMPLE_LIST__ : true;
