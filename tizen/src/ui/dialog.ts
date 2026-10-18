/** Modal with a title and a column of actions (remote-driven). */
import { h } from './dom';
import { setFocusRoot, focusFirst, KEY, refocus } from './focus';
import { currentScreen } from './screens';

export interface DialogAction { label: string; onPick: () => void; danger?: boolean }

let restore: (() => void) | null = null;

export function confirmDialog(title: string, actions: DialogAction[], body?: string): void {
  const list = h('div', { class: 'list' });
  const dlg = h('div', { class: 'dialog' }, h('div', { class: 'box' }, h('div', { class: 'title', text: title }), body ? h('div', { class: 'text', text: body, style: { color: '#b9c2cc', marginBottom: '16px', whiteSpace: 'pre-wrap' } }) : null, list));
  const close = () => { if (dlg.parentNode) dlg.parentNode.removeChild(dlg); if (restore) { restore(); restore = null; } };
  for (const a of actions) {
    list.appendChild(h('div', { class: 'item focusable', text: a.label, style: a.danger ? { color: '#ff8a8a' } : undefined, onclick: () => { close(); a.onPick(); } }));
  }
  document.body.appendChild(dlg);
  const prev = currentScreen();
  restore = () => { if (prev) { setFocusRoot(prev.el, prev.onKey); refocus(); } };
  setFocusRoot(dlg, (code) => { if (code === KEY.BACK) { close(); return true; } return false; });
  focusFirst();
}
