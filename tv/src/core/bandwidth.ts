/**
 * Network speed estimate for the Auto quality. AVPlay exposes no byte counter, so the estimate comes from
 * downloads we do anyway: the playlist (1 MB+, but a dynamic playlist is paced by the provider's server, so it
 * is only a lower bound) and a static file from a CDN (the logo map), timed between progress events to leave
 * out the connection setup. Probes only raise the estimate; repeated buffering in the player lowers it.
 */
import { Store } from './storage';
import { LOGO_MAP_URL } from './logos';

const PROBE_MIN_BYTES = 300_000;
const PROBE_STALE_MS = 60 * 60 * 1000; // Wi-Fi conditions change: re-measure after an hour and start from the new value

/** Records a measured rate; keeps the best of the recent probes (a slow server must not hide a fast network). */
export function recordBandwidth(bps: number, what: string): void {
  if (!(bps > 0)) return;
  const current = Store.bandwidthBps();
  const stale = Date.now() - Store.bandwidthAt() > PROBE_STALE_MS;
  const next = stale ? bps : Math.max(current, bps);
  Store.setBandwidthBps(next);
  console.log('bandwidth ' + what + ' ' + (bps / 1e6).toFixed(1) + ' Mbps → using ' + (next / 1e6).toFixed(1) + ' Mbps');
}

/** Downloads the logo map (static, CDN) at most hourly to measure the rate between the first and last bytes. */
export function probeBandwidthIfStale(): void {
  if (Date.now() - Store.bandwidthAt() < PROBE_STALE_MS && Store.bandwidthBps() > 0) return;
  try {
    const xhr = new XMLHttpRequest();
    let firstAt = 0, firstLoaded = 0;
    xhr.open('GET', LOGO_MAP_URL + '?probe=' + Date.now(), true);
    xhr.onprogress = (e) => { if (!firstAt) { firstAt = Date.now(); firstLoaded = e.loaded; } };
    xhr.onload = () => {
      const ms = Date.now() - firstAt, bytes = xhr.response ? String(xhr.response).length - firstLoaded : 0;
      if (firstAt && ms > 50 && bytes >= PROBE_MIN_BYTES) recordBandwidth(bytes * 8 * 1000 / ms, 'cdn probe');
    };
    xhr.send();
  } catch (e) { /* no network: nothing to record */ }
}
