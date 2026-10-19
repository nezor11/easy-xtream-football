/** Samsung Tizen: AVPlay (webapis.avplay) draws the picture on a plane behind the (transparent) page. */
import { PlayerEvents, TrackInfo, TvPlatform, TvPlayer } from './index';

declare const webapis: any;

function hasAvplay(): boolean { return typeof webapis !== 'undefined' && !!webapis.avplay; }

function parseTrack(t: any): TrackInfo {
  let extra: any = {}; try { extra = JSON.parse(t.extra_info); } catch (e) { /* ignore */ }
  return { index: Number(t.index), type: t.type, language: String(extra.language || extra.track_lang || ''), codec: String(extra.fourCC || extra.codec || extra.format || ''), width: extra.Width ? Number(extra.Width) : undefined, height: extra.Height ? Number(extra.Height) : undefined };
}

const player: TvPlayer = {
  available(): boolean { return hasAvplay(); },

  open(url: string, events: PlayerEvents): void {
    if (!hasAvplay()) { if (events.onError) events.onError('AVPlay unavailable'); return; }
    this.stop();
    webapis.avplay.open(url);
    webapis.avplay.setDisplayRect(0, 0, 1920, 1080);
    try { webapis.avplay.setDisplayMethod('PLAYER_DISPLAY_MODE_AUTO_ASPECT_RATIO'); } catch (e) { /* older firmware */ }
    try { webapis.avplay.setStreamingProperty('ADAPTIVE_INFO', 'STARTBITRATE=HIGHEST'); } catch (e) { /* not adaptive */ }
    let ready = false;
    webapis.avplay.setListener({
      onbufferingstart: () => { if (events.onBuffering) events.onBuffering(true); },
      onbufferingprogress: () => {},
      onbufferingcomplete: () => { if (events.onBuffering) events.onBuffering(false); if (!ready) { ready = true; if (events.onReady) events.onReady(); } },
      oncurrentplaytime: (t: number) => { if (!ready && t > 0) { ready = true; if (events.onReady) events.onReady(); } if (events.onTime) events.onTime(t); },
      onerror: (e: any) => { if (events.onError) events.onError(String(e)); },
      onevent: () => {},
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
  currentTracks(): TrackInfo[] { try { return (webapis.avplay.getCurrentStreamInfo() as any[]).map(parseTrack); } catch (e) { return []; } },
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
