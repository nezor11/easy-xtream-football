/** Settings: language, clear cache, coffee reminder, open-source licenses. */
import { Store } from '../core/storage';
import { Repository } from '../core/repository';
import { t, SUPPORTED_TAGS, applyLanguage, languageTag } from '../core/i18n';
import { h, clear } from './dom';
import { KEY, refocus } from './focus';
import { Screen, push, pop } from './screens';
import { confirmDialog } from './dialog';
import { toast } from './profiles';

const LANGUAGE_NAMES: Record<string, string> = { en: 'English', es: 'Español', ca: 'Català', eu: 'Euskara', gl: 'Galego', pt: 'Português', fr: 'Français', it: 'Italiano', de: 'Deutsch', tr: 'Türkçe', pl: 'Polski', ar: 'العربية', id: 'Bahasa Indonesia', vi: 'Tiếng Việt', ro: 'Română', el: 'Ελληνικά', hr: 'Hrvatski', 'sr-Latn': 'Srpski', th: 'ไทย', 'zh-TW': '繁體中文', ru: 'Русский', nl: 'Nederlands', sq: 'Shqip', hi: 'हिन्दी' };

export function settingsScreen(): Screen {
  const list = h('div', { class: 'settings', style: { width: '1100px', marginTop: '30px' } });
  const el = h('div', { class: 'screen' }, h('h1', { text: t('settings_title') }), list);
  function item(label: string, value: string, onclick: () => void): HTMLElement { return h('div', { class: 'item focusable', onclick }, h('span', { text: label }), h('span', { class: 'value', text: value })); }
  function render(): void {
    clear(list);
    list.appendChild(item(t('settings_language'), Store.language() ? LANGUAGE_NAMES[Store.language()] || Store.language() : t('language_system'), () => push(languageScreen())));
    list.appendChild(item(t('settings_clear_cache'), '', () => { Repository.clearCache(); toast(t('settings_cache_cleared')); }));
    const dismissed = Store.coffeeReminderDismissed();
    list.appendChild(item(t('support_entry'), dismissed ? t('coffee_reenable') : t('coffee_dismiss'), () => confirmDialog(t('support_title'), [
      { label: dismissed ? t('coffee_reenable') : t('coffee_dismiss'), onPick: () => { Store.setCoffeeReminderDismissed(!dismissed); render(); refocus(); } },
      { label: t('action_cancel'), onPick: () => {} },
    ], t('support_message') + '\n' + t('support_site_handle'))));
    list.appendChild(item(t('settings_open_source'), 'GPL-3.0', () => confirmDialog(t('settings_open_source'), [{ label: t('action_accept'), onPick: () => {} }], t('settings_licenses_body'))));
  }
  return { el, onShow: render, onKey: (code) => code === KEY.BACK ? pop() : false };
}

export function languageScreen(): Screen {
  const list = h('div', { class: 'list', style: { width: '900px', marginTop: '30px', maxHeight: '860px' } });
  const el = h('div', { class: 'screen' }, h('h1', { text: t('settings_language') }), list);
  function render(): void {
    clear(list);
    const chosen = Store.language();
    for (const tag of SUPPORTED_TAGS) {
      list.appendChild(h('div', { class: 'item focusable' + (tag === chosen ? ' selected' : ''), text: (tag === chosen ? '●  ' : '○  ') + (tag ? LANGUAGE_NAMES[tag] || tag : t('language_system')), onclick: () => { Store.setLanguage(tag); applyLanguage(tag); pop(); (window as any).__exfRerender && (window as any).__exfRerender(); } }));
    }
    void languageTag();
  }
  return { el, onShow: render, initialFocus: '.selected', onKey: (code) => code === KEY.BACK ? pop() : false };
}
