/** Screen switching: one active screen at a time, each with its own focus root and key handler. */
import { setFocusRoot, focusFirst } from './focus';
import { clear } from './dom';

export interface Screen {
  el: HTMLElement;
  /** Return true when the key was consumed. */
  onKey?: (code: number, ev: KeyboardEvent) => boolean;
  onShow?: () => void;
  onHide?: () => void;
  /** CSS selector of the element to focus first. */
  initialFocus?: string;
}

const stack: Screen[] = [];
const container = () => document.getElementById('app')!;

function activate(s: Screen): void {
  clear(container());
  container().appendChild(s.el);
  setFocusRoot(s.el, s.onKey);
  if (s.onShow) s.onShow();
  focusFirst(s.initialFocus);
}

export function push(s: Screen): void {
  const top = stack[stack.length - 1];
  if (top && top.onHide) top.onHide();
  stack.push(s); activate(s);
}
export function replace(s: Screen): void {
  const top = stack.pop();
  if (top && top.onHide) top.onHide();
  stack.push(s); activate(s);
}
export function pop(): boolean {
  if (stack.length <= 1) return false;
  const top = stack.pop()!;
  if (top.onHide) top.onHide();
  activate(stack[stack.length - 1]);
  return true;
}
export function reset(s: Screen): void {
  stack.forEach((x) => { if (x.onHide) x.onHide(); });
  stack.length = 0; stack.push(s); activate(s);
}
export function depth(): number { return stack.length; }
export function currentScreen(): Screen | undefined { return stack[stack.length - 1]; }
