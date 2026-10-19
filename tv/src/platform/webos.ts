/**
 * LG webOS: the HTML5 video element plays MPEG-TS over HTTP through the TV's media pipeline (H.264/HEVC,
 * MPEG audio, AC-3/E-AC-3 per LG's format specs). Keys: Back 461, colours 403-406, channel ± as PageUp/Down,
 * media keys as on the standard RCU. webOSTV.js (www/webOSTVjs) exposes webOS.platformBack() to leave.
 */
import { PlayerEvents, TrackInfo, TvPlatform, TvPlayer } from './index';

let video: HTMLVideoElement | null = null;
let current: PlayerEvents = {};
let ready = false;

function element(): HTMLVideoElement {
  if (video) return video;
  video = document.createElement('video');
  video.id = 'exf-video';
  video.autoplay = true;
  video.setAttribute('playsinline', '');
  Object.assign(video.style, { position: 'absolute', left: '0', top: '0', width: '1920px', height: '1080px', background: '#000', objectFit: 'contain', zIndex: '-1' });
  video.addEventListener('playing', () => { if (current.onBuffering) current.onBuffering(false); if (!ready) { ready = true; if (current.onReady) current.onReady(); } });
  video.addEventListener('waiting', () => { if (current.onBuffering) current.onBuffering(true); });
  video.addEventListener('canplay', () => { if (current.onBuffering) current.onBuffering(false); });
  video.addEventListener('timeupdate', () => { if (current.onTime) current.onTime(video!.currentTime * 1000); });
  video.addEventListener('ended', () => { if (current.onEnded) current.onEnded(); });
  // Picture size known or changed after playback started (e.g. an HLS variant switch): refresh the info line.
  video.addEventListener('resize', () => { if (ready && current.onReady) current.onReady(); });
  video.addEventListener('error', () => {
    const err = video!.error;
    const code = err ? err.code : 0;
    // MEDIA_ERR_NETWORK (2) and MEDIA_ERR_SRC_NOT_SUPPORTED (4) are what a dead or blocked stream produces.
    if (current.onError) current.onError(code === 2 ? 'CONNECTION_FAILED' : code === 4 ? 'SRC_NOT_SUPPORTED' : 'MEDIA_ERR_' + code);
  });
  document.body.insertBefore(video, document.body.firstChild);
  return video;
}

function toTracks(list: any, type: 'AUDIO' | 'TEXT'): TrackInfo[] {
  const out: TrackInfo[] = [];
  if (!list) return out;
  for (let i = 0; i < list.length; i++) out.push({ index: i, type, language: String(list[i].language || ''), codec: '' });
  return out;
}

const player: TvPlayer = {
  available(): boolean { return typeof document !== 'undefined' && !!document.createElement('video').canPlayType; },
  open(url: string, events: PlayerEvents): void {
    this.stop();
    current = events; ready = false;
    const v = element();
    v.src = url;
    v.load();
    const p = v.play();
    if (p && p.catch) p.catch((e: any) => { if (current.onError && !ready) current.onError('play: ' + (e && e.message ? e.message : e)); });
  },
  stop(): void {
    if (!video) return;
    try { video.pause(); } catch (e) { /* idle */ }
    video.removeAttribute('src');
    try { video.load(); } catch (e) { /* idle */ }
    current = {};
  },
  pause(): void { if (video) video.pause(); },
  resume(): void { if (video) { const p = video.play(); if (p && p.catch) p.catch(() => {}); } },
  state(): string { if (!video || !video.src) return 'NONE'; return video.paused ? 'PAUSED' : 'PLAYING'; },
  tracks(): TrackInfo[] {
    if (!video) return [];
    const v = video as any;
    const vt: TrackInfo[] = video.videoWidth ? [{ index: 0, type: 'VIDEO', language: '', codec: '', width: video.videoWidth, height: video.videoHeight }] : [];
    return [...vt, ...toTracks(v.audioTracks, 'AUDIO'), ...toTracks(v.textTracks, 'TEXT')];
  },
  currentTracks(): TrackInfo[] {
    if (!video) return [];
    const info: TrackInfo[] = [];
    if (video.videoWidth) info.push({ index: 0, type: 'VIDEO', language: '', codec: '', width: video.videoWidth, height: video.videoHeight });
    const a = (video as any).audioTracks;
    if (a) for (let i = 0; i < a.length; i++) if (a[i].enabled) info.push({ index: i, type: 'AUDIO', language: String(a[i].language || ''), codec: '' });
    return info;
  },
  selectTrack(type: 'AUDIO' | 'TEXT', index: number): boolean {
    if (!video) return false;
    const list = type === 'AUDIO' ? (video as any).audioTracks : video.textTracks;
    if (!list || index < 0 || index >= list.length) return false;
    for (let i = 0; i < list.length; i++) {
      if (type === 'AUDIO') list[i].enabled = i === index; else list[i].mode = i === index ? 'showing' : 'disabled';
    }
    return true;
  },
  setSubtitlesEnabled(on: boolean): void {
    if (!video) return;
    const list = video.textTracks;
    if (!on) for (let i = 0; i < list.length; i++) list[i].mode = 'disabled';
  },
};

export const WebosPlatform: TvPlatform = {
  name: 'webos',
  keys: { BACK: 461, EXIT: 461, PLAY_PAUSE: 10252, PLAY: 415, PAUSE: 19, STOP: 413, CH_UP: 33, CH_DOWN: 34, RED: 403, GREEN: 404, YELLOW: 405, BLUE: 406, IME_DONE: 13, IME_CANCEL: 461, INFO: 457 },
  player,
  registerKeys(): void { /* webOS delivers these keys without registration */ },
  exitApp(): void {
    const w = (window as any).webOS;
    try { if (w && w.platformBack) { w.platformBack(); return; } } catch (e) { /* fall through */ }
    try { window.close(); } catch (e) { /* desktop */ }
  },
  isUdPanel(): boolean {
    // webOS reports the panel through webOS.deviceInfo (webOSTV.js); 4K panels report uhd: true.
    let uhd = false;
    try { const w = (window as any).webOS; if (w && w.deviceInfo) w.deviceInfo((info: any) => { uhd = !!(info && (info.uhd || info.uhd8K)); }); } catch (e) { /* unknown */ }
    return uhd;
  },
  hasPointer: true,
};
