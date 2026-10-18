/** Add / edit a profile: Xtream, M3U playlist or direct link. Validates against the server before saving. */
import { Profile, ProfileType } from '../core/model';
import { Store } from '../core/storage';
import { Repository, XtreamAuthError, HttpError } from '../core/repository';
import { M3uFormatError } from '../core/repository';
import { t } from '../core/i18n';
import { h, clear } from './dom';
import { KEY, setFocus, focusFirst } from './focus';
import { Screen, pop } from './screens';
import { SAMPLE_LIST, SPORTS_RADIO_URL } from '../core/sample';

export function addProfileScreen(existing: Profile | null, onSaved: (p: Profile) => void): Screen {
  let type: ProfileType = existing ? existing.type : 'XTREAM';
  const values = { name: existing ? existing.name : '', serverUrl: existing ? existing.serverUrl : '', username: existing ? existing.username : '', password: existing ? existing.password : '', m3uUrl: existing ? existing.m3uUrl : '' };
  let busy = false;
  const form = h('div', { style: { width: '1100px', marginTop: '24px' } });
  const errorEl = h('div', { class: 'error' });
  const tabs = h('div', { class: 'gap12', style: { display: 'flex', marginTop: '28px' } });
  const el = h('div', { class: 'screen' }, h('h1', { text: existing ? t('add_edit_profile') : t('add_connect_list') }), tabs, form, errorEl);

  function tab(label: string, value: ProfileType): HTMLElement {
    return h('div', { class: 'chip focusable' + (type === value ? ' selected' : ''), text: label, onclick: () => { type = value; render(); focusFirst('.chip.selected'); } });
  }
  function field(label: string, key: keyof typeof values, helper?: string, password = false): HTMLElement {
    const input = h('input', { type: password ? 'password' : 'text', class: 'focusable', value: values[key], oninput: (e: Event) => { values[key] = (e.target as HTMLInputElement).value; } }) as HTMLInputElement;
    input.addEventListener('keydown', (e) => { if (e.keyCode === KEY.IME_DONE || e.keyCode === KEY.ENTER) { input.blur(); } });
    return h('div', { class: 'field' }, h('label', { text: label }), input, helper ? h('div', { class: 'helper', text: helper }) : null);
  }
  function render(): void {
    clear(tabs); clear(form); errorEl.textContent = '';
    tabs.appendChild(tab(t('profile_type_xtream'), 'XTREAM')); tabs.appendChild(tab(t('profile_type_m3u'), 'M3U')); tabs.appendChild(tab(t('profile_type_direct'), 'DIRECT'));
    const desc = type === 'XTREAM' ? t('add_desc_xtream') : type === 'M3U' ? t('add_desc_m3u') : t('add_desc_direct');
    form.appendChild(h('div', { class: 'helper', text: desc, style: { fontSize: '22px', marginTop: '20px' } }));
    if (type === 'XTREAM') {
      form.appendChild(field(t('field_server_url'), 'serverUrl', t('helper_server')));
      form.appendChild(field(t('field_username'), 'username'));
      form.appendChild(field(t('field_password'), 'password', undefined, true));
    } else {
      form.appendChild(field(type === 'M3U' ? t('field_m3u_url') : t('field_stream_url'), 'm3uUrl', type === 'M3U' ? t('helper_m3u') : t('helper_direct')));
    }
    form.appendChild(field(t('field_profile_name'), 'name'));
    const save = h('div', { class: 'btn primary focusable', text: existing ? t('btn_save_changes') : t('btn_save_enter'), style: { marginTop: '30px' }, onclick: () => submit(save) });
    form.appendChild(save);
    if (!existing && SAMPLE_LIST && Store.profiles().length === 0) {
      // Its own line, well apart from the save button (inline-flex buttons would otherwise sit side by side).
      form.appendChild(h('div', { style: { marginTop: '40px' } }, h('div', { class: 'btn focusable', text: '▶  ' + t('sample_radio_button'), onclick: () => {
        const p = Store.saveProfile({ name: t('sample_profile_radio'), type: 'M3U', serverUrl: '', username: '', password: '', m3uUrl: SPORTS_RADIO_URL });
        onSaved(p);
      } })));
      form.appendChild(h('div', { class: 'helper', text: t('sample_radio_hint'), style: { marginTop: '10px' } }));
    }
  }

  async function submit(button: HTMLElement): Promise<void> {
    if (busy) return;
    busy = true; errorEl.textContent = ''; const label = button.textContent; button.textContent = t('btn_connecting');
    try {
      let name = values.name.trim();
      if (type === 'XTREAM') {
        const profile = { name, serverUrl: values.serverUrl.trim(), username: values.username.trim(), password: values.password.trim() };
        await Repository.validateXtream(profile);
        if (!name) name = hostOf(profile.serverUrl);
      } else if (type === 'M3U') {
        await Repository.validateM3u(values.m3uUrl.trim());
        if (!name) name = t('default_m3u_name');
      } else {
        if (!/^https?:\/\//i.test(values.m3uUrl.trim())) throw new Error('bad_url');
        if (!name) name = hostOf(values.m3uUrl);
      }
      const saved = Store.saveProfile({ id: existing ? existing.id : undefined, name, type, serverUrl: type === 'XTREAM' ? values.serverUrl.trim() : '', username: type === 'XTREAM' ? values.username.trim() : '', password: type === 'XTREAM' ? values.password.trim() : '', m3uUrl: type !== 'XTREAM' ? values.m3uUrl.trim() : '' });
      onSaved(saved);
    } catch (e: any) {
      errorEl.textContent = describe(e);
      setFocus(button);
    } finally { busy = false; button.textContent = label; }
  }

  function describe(e: any): string {
    if (e instanceof XtreamAuthError) return t('error_xtream_auth');
    if (e instanceof M3uFormatError) return t('error_m3u_load');
    if (e instanceof HttpError) return type === 'XTREAM' ? t('error_xtream_http', e.status) : t('error_m3u_load');
    if (e && e.message === 'bad_response') return t('error_xtream_bad_response');
    if (type === 'XTREAM') return t('error_xtream_unreachable', hostOf(values.serverUrl));
    return t('error_m3u_load');
  }

  return { el, onShow: render, initialFocus: 'input', onKey: (code) => {
    if (code === KEY.BACK) { if (document.activeElement && document.activeElement.tagName === 'INPUT') { (document.activeElement as HTMLElement).blur(); return true; } return pop(); }
    return false;
  } };
}

function hostOf(url: string): string {
  const m = /^(?:https?:\/\/)?([^/:?#]+)/i.exec(url.trim());
  return m ? m[1] : url.trim();
}
