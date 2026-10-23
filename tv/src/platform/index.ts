/**
 * What differs between TV platforms: the media player, the remote's key codes, how to exit, what the
 * panel can show. The build picks one with `__EXF_PLATFORM__` (see build.mjs); everything else is shared.
 */
import { TizenPlatform } from './tizen';
import { WebosPlatform } from './webos';

export interface TrackInfo { index: number; type: 'AUDIO' | 'TEXT' | 'VIDEO'; language: string; codec: string; width?: number; height?: number;
  /** Bitrate of the variant actually playing (adaptive streams), when the platform reports it. */
  bitrateBps?: number; }
export interface PlayerEvents {
  onReady?: () => void;            // first frame / buffering complete
  onBuffering?: (buffering: boolean) => void;
  onError?: (error: string) => void;
  onEnded?: () => void;
  onTime?: (ms: number) => void;
}
export interface TvPlayer {
  available(): boolean;
  open(url: string, events: PlayerEvents): void;
  stop(): void;
  pause(): void;
  resume(): void;
  state(): string;
  tracks(): TrackInfo[];
  currentTracks(): TrackInfo[];
  /** Milliseconds of media already downloaded beyond the playhead, or -1 when the platform cannot tell. */
  bufferAheadMs(): number;
  selectTrack(type: 'AUDIO' | 'TEXT', index: number): boolean;
  setSubtitlesEnabled(on: boolean): void;
}
/** Remote key codes that differ per platform (arrows, OK and digits are the same everywhere). */
export interface KeyMap {
  BACK: number; EXIT: number; PLAY_PAUSE: number; PLAY: number; PAUSE: number; STOP: number;
  CH_UP: number; CH_DOWN: number; RED: number; GREEN: number; YELLOW: number; BLUE: number;
  IME_DONE: number; IME_CANCEL: number; INFO: number;
}
export interface TvPlatform {
  name: 'tizen' | 'webos';
  keys: KeyMap;
  player: TvPlayer;
  /** Registers the extra remote keys the app wants to receive (no-op where not needed). */
  registerKeys(): void;
  exitApp(): void;
  /** True when the panel is UHD (4K worth decoding); false when unknown. */
  isUdPanel(): boolean;
  /** The Magic Remote pointer (webOS) focuses what it hovers; other platforms have no pointer. */
  hasPointer: boolean;
}

declare const __EXF_PLATFORM__: string;
export const platform: TvPlatform = typeof __EXF_PLATFORM__ === 'string' && __EXF_PLATFORM__ === 'webos' ? WebosPlatform : TizenPlatform;
