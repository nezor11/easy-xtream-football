/**
 * Samsung Tizen: AVPlay (webapis.avplay) draws the picture on a plane behind the (transparent) page, through
 * an `<object type="application/avplayer">` element that marks the display area (Samsung's AVPlay guide, step 1).
 * Tizen 5.5 (2020 Smart Monitor) shows the plane even without that element; the 2023+ models (Tizen 7.0 and
 * later) do not: Seller Office rejected 1.0.0 with "black screen with audio" on every Tizen 7-10 model group.
 */
import { PlayerEvents, TrackInfo, TvPlatform, TvPlayer } from './index';

declare const webapis: any;

function hasAvplay(): boolean { return typeof webapis !== 'undefined' && !!webapis.avplay; }

let surface: HTMLElement | null = null;
/**
 * Variant bitrate from PLAYER_MSG_BITRATE_CHANGE. On an adaptive HLS stream getCurrentStreamInfo() keeps
 * reporting the size of the first variant (320x184 while 1080p plays, seen on Tizen 5.5), so the info line
 * shows this instead of a stale resolution.
 */
let variantBitrate = 0;
/** The full-screen AVPlay object, behind the UI (`#app` is positioned, so a negative z-index keeps it underneath). */
function ensureSurface(): void {
  if (surface || typeof document === 'undefined') return;
  surface = document.createElement('object');
  surface.id = 'exf-avplayer';
  surface.setAttribute('type', 'application/avplayer');
  Object.assign(surface.style, { position: 'absolute', left: '0', top: '0', width: '1920px', height: '1080px', zIndex: '-1' });
  document.body.insertBefore(surface, document.body.firstChild);
}

function parseTrack(t: any): TrackInfo {
  let extra: any = {}; try { extra = JSON.parse(t.extra_info); } catch (e) { /* ignore */ }
  return { index: Number(t.index), type: t.type, language: String(extra.language || extra.track_lang || ''), codec: String(extra.fourCC || extra.codec || extra.format || ''), width: extra.Width ? Number(extra.Width) : undefined, height: extra.Height ? Number(extra.Height) : undefined };
}

const player: TvPlayer = {
  available(): boolean { return hasAvplay(); },

  open(url: string, events: PlayerEvents): void {
    if (!hasAvplay()) { if (events.onError) events.onError('AVPlay unavailable'); return; }
    this.stop();
    ensureSurface();
    webapis.avplay.open(url);
    webapis.avplay.setDisplayRect(0, 0, 1920, 1080);
    try { webapis.avplay.setDisplayMethod('PLAYER_DISPLAY_MODE_AUTO_ASPECT_RATIO'); } catch (e) { /* older firmware */ }
    try { webapis.avplay.setStreamingProperty('ADAPTIVE_INFO', 'STARTBITRATE=HIGHEST'); } catch (e) { /* not adaptive */ }
    let ready = false, buffering = false, lastTime = -1;
    variantBitrate = 0;
    const setBuffering = (b: boolean) => { if (buffering === b) return; buffering = b; if (events.onBuffering) events.onBuffering(b); };
    webapis.avplay.setListener({
      onbufferingstart: () => { setBuffering(true); },
      onbufferingprogress: () => {},
      onbufferingcomplete: () => { setBuffering(false); if (!ready) { ready = true; if (events.onReady) events.onReady(); } },
      // Some firmware never reports onbufferingcomplete for a live stream: playback advancing is proof enough.
      oncurrentplaytime: (t: number) => {
        if (t > lastTime && lastTime >= 0) setBuffering(false);
        lastTime = t;
        if (!ready && t > 0) { ready = true; if (events.onReady) events.onReady(); }
        if (events.onTime) events.onTime(t);
      },
      onerror: (e: any) => { if (events.onError) events.onError(String(e)); },
      onevent: (name: string, data: string) => {
        // A single-variant playlist reports 99999999 ("unknown"): keep the real size from the stream info then.
        if (name === 'PLAYER_MSG_BITRATE_CHANGE') { const m = /BITRATE:(\d+)/.exec(String(data)); if (m) { const b = Number(m[1]); variantBitrate = b > 0 && b < 50e6 ? b : 0; if (ready && events.onReady) events.onReady(); } }
      },
      onstreamcompleted: () => { if (events.onEnded) events.onEnded(); },
      ondrmevent: () => {},
    });
    webapis.avplay.prepareAsync(() => {
      try { webapis.avplay.play(); } catch (e) { if (events.onError) events.onError('play: ' + e); }
    }, (e: any) => { if (events.onError) events.onError(String(e)); });
  },

  stop(): void {
    if (!hasAvplay()) return;
    try { webapis.avplay.stop(); } catch (e) { /* idle */ }
    try { webapis.avplay.close(); } catch (e) { /* idle */ }
  },
  pause(): void { try { webapis.avplay.pause(); } catch (e) { /* ignore */ } },
  resume(): void { try { webapis.avplay.play(); } catch (e) { /* ignore */ } },
  state(): string { try { return webapis.avplay.getState(); } catch (e) { return 'NONE'; } },
  tracks(): TrackInfo[] { try { return (webapis.avplay.getTotalTrackInfo() as any[]).map(parseTrack); } catch (e) { return []; } },
  bufferAheadMs(): number { return -1; }, // AVPlay has no buffered-range API
  currentTracks(): TrackInfo[] {
    try {
      return (webapis.avplay.getCurrentStreamInfo() as any[]).map(parseTrack).map((t) => (t.type === 'VIDEO' && variantBitrate > 0 ? { ...t, width: undefined, height: undefined, bitrateBps: variantBitrate } : t));
    } catch (e) { return []; }
  },
  selectTrack(type: 'AUDIO' | 'TEXT', index: number): boolean { try { webapis.avplay.setSelectTrack(type, index); return true; } catch (e) { return false; } },
  setSubtitlesEnabled(on: boolean): void { try { webapis.avplay.setSilentSubtitle(!on); } catch (e) { /* ignore */ } },
};

export const TizenPlatform: TvPlatform = {
  name: 'tizen',
  keys: { BACK: 10009, EXIT: 10182, PLAY_PAUSE: 10252, PLAY: 415, PAUSE: 19, STOP: 413, CH_UP: 427, CH_DOWN: 428, RED: 403, GREEN: 404, YELLOW: 405, BLUE: 406, IME_DONE: 65376, IME_CANCEL: 65385, INFO: 457 },
  player,
  registerKeys(): void {
    try { (window as any).tizen.tvinputdevice.registerKeyBatch(['ChannelUp', 'ChannelDown', 'MediaPlayPause', 'MediaPlay', 'MediaPause', 'MediaStop', 'ColorF0Red', 'ColorF1Green', 'ColorF2Yellow', 'ColorF3Blue', 'Info']); } catch (e) { /* not on a TV */ }
  },
  exitApp(): void { try { (window as any).tizen.application.getCurrentApplication().exit(); } catch (e) { /* desktop */ } },
  isUdPanel(): boolean { try { return !!(window as any).webapis.productinfo.isUdPanelSupported(); } catch (e) { return false; } },
  hasPointer: false,
};
