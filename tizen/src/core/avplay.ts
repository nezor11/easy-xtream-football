/** Thin wrapper over Samsung's AVPlay (webapis.avplay) with the events the player screen needs. */
declare const webapis: any;

export interface TrackInfo { index: number; type: 'AUDIO' | 'TEXT' | 'VIDEO'; language: string; codec: string; width?: number; height?: number; }
export interface PlayerEvents {
  onReady?: () => void;            // first frame / buffering complete
  onBuffering?: (buffering: boolean) => void;
  onError?: (error: string) => void;
  onEnded?: () => void;
  onTime?: (ms: number) => void;
}

function hasAvplay(): boolean { return typeof webapis !== 'undefined' && !!webapis.avplay; }

export const Player = {
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

  /** Tracks of the current stream (empty until playing). */
  tracks(): TrackInfo[] {
    try {
      return (webapis.avplay.getTotalTrackInfo() as any[]).map((t) => {
        let extra: any = {}; try { extra = JSON.parse(t.extra_info); } catch (e) { /* ignore */ }
        return { index: Number(t.index), type: t.type, language: String(extra.language || extra.track_lang || ''), codec: String(extra.fourCC || extra.codec || extra.format || ''), width: extra.Width ? Number(extra.Width) : undefined, height: extra.Height ? Number(extra.Height) : undefined };
      });
    } catch (e) { return []; }
  },
  currentTracks(): TrackInfo[] {
    try {
      return (webapis.avplay.getCurrentStreamInfo() as any[]).map((t) => {
        let extra: any = {}; try { extra = JSON.parse(t.extra_info); } catch (e) { /* ignore */ }
        return { index: Number(t.index), type: t.type, language: String(extra.language || extra.track_lang || ''), codec: String(extra.fourCC || extra.codec || extra.format || ''), width: extra.Width ? Number(extra.Width) : undefined, height: extra.Height ? Number(extra.Height) : undefined };
      });
    } catch (e) { return []; }
  },
  selectTrack(type: 'AUDIO' | 'TEXT', index: number): boolean {
    try { webapis.avplay.setSelectTrack(type, index); return true; } catch (e) { return false; }
  },
  setSubtitlesEnabled(on: boolean): void { try { webapis.avplay.setSilentSubtitle(!on); } catch (e) { /* ignore */ } },
};
