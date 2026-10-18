/** Profile picker: cards per saved profile, add, long-press to edit/delete, settings and language. */
import { Profile } from '../core/model';
import { Store } from '../core/storage';
import { t } from '../core/i18n';
import { h, clear } from './dom';
import { KEY, refocus } from './focus';
import { Screen, push, pop } from './screens';
import { addProfileScreen } from './addProfile';
import { settingsScreen, languageScreen } from './settings';
import { confirmDialog } from './dialog';

export function profilesScreen(onOpen: (p: Profile) => void): Screen {
  const grid = h('div', { class: 'cards wrap', style: { flexWrap: 'wrap', marginTop: '40px', maxHeight: '700px' } });
  const el = h('div', { class: 'screen' },
    h('div', { class: 'brand' }, h('img', { class: 'logo', src: 'icon.png' }), h('div', null, h('h1', { text: t('app_name') }), h('div', { class: 'tagline', text: t('brand_tagline') }))),
    h('div', { class: 'topbar' },
      h('div', { class: 'btn icon focusable', text: '⚙', title: t('settings_title'), onclick: () => push(settingsScreen()) }),
      h('div', { class: 'btn icon focusable', text: '🌐', title: t('settings_language'), onclick: () => push(languageScreen()) })),
    grid,
    h('div', { class: 'hint', text: t('profiles_longpress_hint') }));

  function typeLabel(p: Profile): string { return p.type === 'M3U' ? t('profile_type_m3u') : p.type === 'DIRECT' ? t('profile_type_direct') : t('profile_type_xtream'); }

  function render(): void {
    clear(grid);
    for (const p of Store.profiles()) {
      const card = h('div', { class: 'card focusable', onclick: () => { Store.setLastProfileId(p.id); onOpen(p); } },
        h('div', { class: 'monogram', text: (p.name.trim()[0] || '?').toUpperCase() }),
        h('div', { class: 'name', text: p.name }), h('div', { class: 'sub', text: typeLabel(p) }));
      card.addEventListener('longpress', () => profileMenu(p));
      grid.appendChild(card);
    }
    grid.appendChild(h('div', { class: 'card focusable', onclick: () => push(addProfileScreen(null, () => { pop(); render(); refocus(); })) },
      h('div', { class: 'monogram', text: '+' }), h('div', { class: 'name', text: t('add_profile') })));
  }

  function profileMenu(p: Profile): void {
    confirmDialog(p.name, [
      { label: t('action_edit'), onPick: () => push(addProfileScreen(p, () => { pop(); render(); refocus(); })) },
      { label: t('action_delete'), danger: true, onPick: () => { Store.deleteProfile(p.id); render(); refocus(); } },
      { label: t('action_cancel'), onPick: () => {} },
    ]);
  }

  return { el, onShow: render, initialFocus: '.card', onKey: (code) => { if (code === KEY.BACK) { exitApp(); return true; } return false; } };
}

let backArmedAt = 0;
export function exitApp(): void {
  // Two quick Backs exit, like the Android app.
  const now = Date.now();
  if (now - backArmedAt < 2500) { try { (window as any).tizen.application.getCurrentApplication().exit(); } catch (e) { /* desktop */ } return; }
  backArmedAt = now;
  toast(t('press_back_to_exit'));
}

let toastEl: HTMLElement | null = null, toastTimer: number | null = null;
export function toast(text: string, ms = 2500): void {
  if (!toastEl) { toastEl = h('div', { class: 'osd-msg', style: { top: 'auto', bottom: '60px', transform: 'translateX(-50%)', zIndex: '50' } }); }
  toastEl.textContent = text;
  if (!toastEl.parentNode) document.body.appendChild(toastEl);
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => { if (toastEl && toastEl.parentNode) toastEl.parentNode.removeChild(toastEl); }, ms);
}
