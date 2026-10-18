/**
 * Remote-control focus for a TV page: spatial navigation between `.focusable` elements inside the
 * active container (nearest element in the pressed direction), OK → click, long OK → `longpress` event.
 */
import { platform } from '../platform';

/** Arrows, OK and digits are universal; the rest come from the platform's key map. */
export const KEY = { LEFT: 37, UP: 38, RIGHT: 39, DOWN: 40, ENTER: 13, ...platform.keys };

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
    // Text fields get DOM focus (and the TV keyboard) only on OK, see installKeyboard.
    if (document.activeElement && document.activeElement !== document.body && document.activeElement !== el) (document.activeElement as HTMLElement).blur();
    if (scroll) keepVisible(el);
    el.dispatchEvent(new CustomEvent('focused', { bubbles: true }));
  }
}

/** Scrolls horizontal card rows and vertical lists (overflow: hidden) so the focused element stays in view. */
function keepVisible(el: HTMLElement): void {
  const row = el.closest<HTMLElement>('.cards');
  if (row) {
    const r = el.getBoundingClientRect(), c = row.getBoundingClientRect();
    if (r.right > c.right - 20) row.scrollLeft += r.right - c.right + 40;
    else if (r.left < c.left + 20) row.scrollLeft -= c.left - r.left + 40;
  }
  const col = el.closest<HTMLElement>('.rows, .options, .list');
  if (col) {
    const r = el.getBoundingClientRect(), c = col.getBoundingClientRect();
    if (r.bottom > c.bottom - 10) col.scrollTop += r.bottom - c.bottom + 30;
    else if (r.top < c.top + 10) col.scrollTop -= c.top - r.top + 30;
  }
}

export function focusFirst(selector?: string): void {
  const list = focusables();
  const el = selector ? list.find((e) => e.matches(selector)) || list[0] : list[0];
  setFocus(el || null);
}

/** Restores focus to the current element if still visible, else to the first focusable. */
/**
 * Restores focus after a re-render: the same element if still attached, else the new element with the
 * same `data-key` (in the same row when rows repeat keys), else the first focusable matching `selector`.
 */
export function refocus(selector?: string): void {
  const list = focusables();
  if (focused && list.includes(focused)) { setFocus(focused); return; }
  const key = focused && focused.dataset ? focused.dataset.key : undefined;
  if (key) {
    const rowTitle = (el: HTMLElement): string => { const h2 = el.closest('.row') && el.closest('.row')!.querySelector('h2'); return h2 && h2.firstChild ? h2.firstChild.textContent || '' : ''; };
    const wanted = rowTitle(focused!);
    const same = list.filter((el) => el.dataset.key === key);
    const el = same.find((e) => rowTitle(e) === wanted) || same[0];
    if (el) { setFocus(el); return; }
  }
  focusFirst(selector);
}

function move(dir: number): boolean {
  const list = focusables();
  if (list.length === 0) return false;
  if (!focused || !list.includes(focused)) { setFocus(list[0]); return true; }
  const a = focused.getBoundingClientRect();
  const ax = (a.left + a.right) / 2, ay = (a.top + a.bottom) / 2;
  let best: HTMLElement | null = null, bestScore = Infinity;
  // ◀ ▶ inside a horizontal row stay in that row (and wrap at its ends, below); ▲ ▼ change rows.
  const row = dir === KEY.LEFT || dir === KEY.RIGHT ? focused.closest<HTMLElement>('.cards:not(.wrap)') : null;
  for (const el of list) {
    if (el === focused) continue;
    if (row && !row.contains(el)) continue;
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
  // End of a horizontal row: wrap around to the other end (rows are circular, like channel zapping).
  if (row) {
    const items = list.filter((el) => row.contains(el));
    if (items.length > 1) { setFocus(dir === KEY.RIGHT ? items[0] : items[items.length - 1]); return true; }
  }
  return false;
}

export function installKeyboard(): void {
  // Keep the logical focus in step with DOM focus (mouse clicks on a desktop, IME focus changes on TV).
  document.addEventListener('focusin', (ev) => { const el = ev.target as HTMLElement; if (el && el.classList && el.classList.contains('focusable') && el !== focused) setFocus(el, false); });
  document.addEventListener('keydown', (ev) => {
    const code = ev.keyCode;
    const inInput = document.activeElement && document.activeElement.tagName === 'INPUT';
    if (inInput && (code === KEY.ENTER || code === KEY.IME_DONE || code === KEY.IME_CANCEL || code === KEY.BACK)) {
      (document.activeElement as HTMLElement).blur(); ev.preventDefault(); return; // leave edit mode, keep the logical focus
    }
    if (inInput && (code === KEY.LEFT || code === KEY.RIGHT)) return; // caret movement inside the field
    if (keyHandler && keyHandler(code, ev)) { ev.preventDefault(); return; }
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
      if (!wasLong && focused) { if (focused.tagName === 'INPUT') (focused as HTMLInputElement).focus(); else focused.click(); }
    }
  });
  platform.registerKeys();
  // A pointer (LG's Magic Remote, a mouse on a desktop) focuses what it hovers, so OK then acts on it.
  document.addEventListener('mouseover', (ev) => { const target = ev.target as HTMLElement; const el = target && target.closest ? target.closest<HTMLElement>('.focusable') : null; if (el && el !== focused && (!root || root.contains(el))) setFocus(el, false); });
}
