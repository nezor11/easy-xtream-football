/**
 * Remote-control focus for a TV page: spatial navigation between `.focusable` elements inside the
 * active container (nearest element in the pressed direction), OK → click, long OK → `longpress` event.
 */
export const KEY = {
  LEFT: 37, UP: 38, RIGHT: 39, DOWN: 40, ENTER: 13, BACK: 10009, EXIT: 10182,
  PLAY_PAUSE: 10252, PLAY: 415, PAUSE: 19, STOP: 413, CH_UP: 427, CH_DOWN: 428, RED: 403, GREEN: 404, YELLOW: 405, BLUE: 406,
  IME_DONE: 65376, IME_CANCEL: 65385, INFO: 457,
};

let root: HTMLElement | null = null;
let focused: HTMLElement | null = null;
let keyHandler: ((code: number, ev: KeyboardEvent) => boolean) | null = null;
let okDownAt = 0, okLong = false, okTimer: number | null = null;
const LONG_MS = 600;

export function setFocusRoot(el: HTMLElement | null, handler?: (code: number, ev: KeyboardEvent) => boolean): void {
  root = el; keyHandler = handler || null; focused = null;
}

export function current(): HTMLElement | null { return focused; }

function focusables(): HTMLElement[] {
  if (!root) return [];
  const all = Array.from(root.querySelectorAll<HTMLElement>('.focusable'));
  return all.filter((el) => el.offsetParent !== null && !el.classList.contains('disabled'));
}

export function setFocus(el: HTMLElement | null, scroll = true): void {
  if (focused && focused !== el) focused.classList.remove('focus');
  focused = el;
  if (el) {
    el.classList.add('focus');
    if (el.tagName === 'INPUT') (el as HTMLInputElement).focus(); else if (document.activeElement && document.activeElement !== document.body) (document.activeElement as HTMLElement).blur();
    if (scroll) keepVisible(el);
    el.dispatchEvent(new CustomEvent('focused', { bubbles: true }));
  }
}

/** Scrolls horizontal card rows and vertical lists so the focused element stays in view (translate-based). */
function keepVisible(el: HTMLElement): void {
  const row = el.closest<HTMLElement>('.cards');
  if (row) {
    const r = el.getBoundingClientRect(), c = row.getBoundingClientRect();
    const cur = parseFloat(row.dataset.tx || '0');
    let tx = cur;
    if (r.right > c.right - 20) tx = cur - (r.right - c.right + 40);
    if (r.left < c.left + 20) tx = Math.min(0, cur + (c.left - r.left + 40));
    if (tx !== cur) { row.dataset.tx = String(tx); row.style.transform = `translateX(${tx}px)`; }
  }
  const col = el.closest<HTMLElement>('.rows, .options, .list');
  if (col) {
    const r = el.getBoundingClientRect(), c = col.getBoundingClientRect();
    const cur = parseFloat(col.dataset.ty || '0');
    let ty = cur;
    if (r.bottom > c.bottom - 10) ty = cur - (r.bottom - c.bottom + 30);
    if (r.top < c.top + 10) ty = Math.min(0, cur + (c.top - r.top + 30));
    if (ty !== cur) { col.dataset.ty = String(ty); col.style.transform = `translateY(${ty}px)`; }
  }
}

export function focusFirst(selector?: string): void {
  const list = focusables();
  const el = selector ? list.find((e) => e.matches(selector)) || list[0] : list[0];
  setFocus(el || null);
}

/** Restores focus to the current element if still visible, else to the first focusable. */
export function refocus(): void {
  if (focused && focusables().includes(focused)) setFocus(focused); else focusFirst();
}

function move(dir: number): boolean {
  const list = focusables();
  if (list.length === 0) return false;
  if (!focused || !list.includes(focused)) { setFocus(list[0]); return true; }
  const a = focused.getBoundingClientRect();
  const ax = (a.left + a.right) / 2, ay = (a.top + a.bottom) / 2;
  let best: HTMLElement | null = null, bestScore = Infinity;
  for (const el of list) {
    if (el === focused) continue;
    const b = el.getBoundingClientRect();
    const bx = (b.left + b.right) / 2, by = (b.top + b.bottom) / 2;
    const dx = bx - ax, dy = by - ay;
    let primary: number, secondary: number;
    if (dir === KEY.LEFT) { primary = -dx; secondary = Math.abs(dy); if (b.right > a.left + 4) continue; }
    else if (dir === KEY.RIGHT) { primary = dx; secondary = Math.abs(dy); if (b.left < a.right - 4) continue; }
    else if (dir === KEY.UP) { primary = -dy; secondary = Math.abs(dx); if (b.bottom > a.top + 4) continue; }
    else { primary = dy; secondary = Math.abs(dx); if (b.top < a.bottom - 4) continue; }
    if (primary <= 0) continue;
    // Prefer elements overlapping on the secondary axis; weigh distance along the main axis most.
    const overlap = dir === KEY.LEFT || dir === KEY.RIGHT ? Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top) : Math.min(a.right, b.right) - Math.max(a.left, b.left);
    const score = primary + secondary * (overlap > 0 ? 0.3 : 2.5);
    if (score < bestScore) { bestScore = score; best = el; }
  }
  if (best) { setFocus(best); return true; }
  return false;
}

export function installKeyboard(): void {
  // Keep the logical focus in step with DOM focus (mouse clicks on a desktop, IME focus changes on TV).
  document.addEventListener('focusin', (ev) => { const el = ev.target as HTMLElement; if (el && el.classList && el.classList.contains('focusable') && el !== focused) setFocus(el, false); });
  document.addEventListener('keydown', (ev) => {
    const code = ev.keyCode;
    const inInput = document.activeElement && document.activeElement.tagName === 'INPUT';
    if (keyHandler && keyHandler(code, ev)) { ev.preventDefault(); return; }
    if (inInput && (code === KEY.LEFT || code === KEY.RIGHT)) return; // caret movement inside the field
    if (code === KEY.LEFT || code === KEY.RIGHT || code === KEY.UP || code === KEY.DOWN) {
      if (inInput && (code === KEY.UP || code === KEY.DOWN)) (document.activeElement as HTMLElement).blur();
      if (move(code)) ev.preventDefault();
    } else if (code === KEY.ENTER) {
      if (inInput) return; // the IME handles OK inside a text field
      if (!okDownAt) { okDownAt = Date.now(); okLong = false; okTimer = window.setTimeout(() => { okLong = true; if (focused) focused.dispatchEvent(new CustomEvent('longpress', { bubbles: true })); }, LONG_MS); }
      ev.preventDefault();
    }
  });
  document.addEventListener('keyup', (ev) => {
    if (ev.keyCode === KEY.ENTER && okDownAt) {
      if (okTimer) clearTimeout(okTimer);
      const wasLong = okLong; okDownAt = 0; okLong = false;
      if (!wasLong && focused) focused.click();
    }
  });
  try { (window as any).tizen.tvinputdevice.registerKeyBatch(['ChannelUp', 'ChannelDown', 'MediaPlayPause', 'MediaPlay', 'MediaPause', 'MediaStop', 'ColorF0Red', 'ColorF1Green', 'ColorF2Yellow', 'ColorF3Blue', 'Info']); } catch (e) { /* not on a TV */ }
}
