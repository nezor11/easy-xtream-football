/** Channel grid: header, filters, sections (live now / favorites / recents / favorite folders / football / more sports), folder view. */
import { ChannelFolder, ChannelGroup, QUALITY_MODES, availableQualities, fixedQuality, isSingle } from '../core/model';
import { logoRetryUrl } from '../core/logos';
import { foldIntoFolders } from '../core/grouping';
import { Repository } from '../core/repository';
import { Store } from '../core/storage';
import { t, plural } from '../core/i18n';
import { h, clear, icon } from './dom';
import { confirmDialog } from './dialog';
import { KEY, refocus, focusFirst, setFocus } from './focus';
import { Screen, push, pop } from './screens';
import { playerScreen } from './player';
import { toast } from './profiles';

export function channelsScreen(): Screen {
  let folders: ChannelFolder[] = [];
  let groups: ChannelGroup[] = [];
  let opened: ChannelFolder | null = null;
  let filtersOpen = false;
  let reorderKey: string | null = null; // favorite being moved with ◀ ▶
  let query = '';
  let liveNow: Array<{ group: ChannelGroup; title: string }> = [];
  let liveNowToken = 0;
  const header = h('h1', null);
  const sub = h('h2', { style: { marginTop: '6px' } });
  const filters = h('div', { class: 'hidden gap12', style: { display: 'flex', marginTop: '18px' } });
  const rows = h('div', { class: 'rows' });
  const topbar = h('div', { class: 'topbar' },
    h('div', { class: 'btn icon focusable', title: t('action_filters'), onclick: () => { filtersOpen = !filtersOpen; filters.classList.toggle('hidden', !filtersOpen); renderRows(); if (filtersOpen) focusFirst('.chip'); } }, icon('search')));
  const el = h('div', { class: 'screen' }, header, sub, topbar, filters, rows);

  function sourceTitle(): string { const n = Repository.activeProfileName(); return n ? t('live_sports_of', n) : t('live_sports'); }

  function renderFilters(): void {
    clear(filters);
    const mode = Store.qualityMode();
    for (const m of QUALITY_MODES) {
      filters.appendChild(h('div', { class: 'chip focusable' + (m === mode ? ' selected' : ''), text: m === 'AUTO' ? t('quality_auto') : m.replace('UHD_4K', '4K'), onclick: () => { Store.setQualityMode(m); renderFilters(); renderRows(); refocus(); } }));
    }
    const search = h('input', { type: 'text', class: 'chip focusable', placeholder: t('search_field_label'), value: query, style: { width: '320px', background: '#141b22', color: '#e6eaee' }, oninput: (e: Event) => { query = (e.target as HTMLInputElement).value; renderRows(); } }) as HTMLInputElement;
        filters.appendChild(search);
    filters.appendChild(h('div', { class: 'chip focusable', onclick: () => load(true) }, icon('reload'), ' ' + t('action_reload').replace(/^↻\s*/, '')));
  }

  function card(group: ChannelGroup | null, folder: ChannelFolder | null, onclick: () => void, onlong?: () => void): HTMLElement {
    const name = group ? group.displayName : folder!.name;
    const icon = group ? group.iconUrl : folder!.iconUrl;
    const isRadio = group ? !!group.isRadio : !!folder!.isRadio;
    const geo = group ? !!group.geoBlocked : !!folder!.geoBlocked;
    const country = group ? group.country : folder!.country;
    const fav = group ? Store.favoriteChannelKeys().includes(group.key) : Store.favoriteFolders().includes(folder!.name);
    const c = h('div', { class: 'card focusable' + (group && group.key === reorderKey ? ' reorder' : ''), 'data-key': group ? group.key : '', onclick },
      icon ? h('img', { class: 'logo', src: icon, onerror: (e: Event) => { const img = e.target as HTMLImageElement; const retry = logoRetryUrl(img.src, 240); if (retry) img.src = retry; else img.replaceWith(h('div', { class: 'monogram', text: name.slice(0, 2).toUpperCase() })); } }) : h('div', { class: 'monogram', text: name.slice(0, 2).toUpperCase() }),
      h('div', { class: 'name', text: name }),
      folder && !isSingle(folder) ? h('div', { class: 'sub', text: plural('channels_count', folder.channels.length) }) : (group ? h('div', { class: 'sub', text: qualityLabel(group) }) : null),
      h('div', { class: 'badge', text: [isRadio ? '📻' : '', country ? country.toUpperCase() : '', geo ? '🔒' : ''].filter(Boolean).join(' ') }),
      fav ? h('div', { class: 'star', text: '★' }) : null,
      folder && !isSingle(folder) ? h('div', { class: 'arrow', text: '▸' }) : null);
    if (onlong) c.addEventListener('longpress', onlong);
    return c;
  }
  function qualityLabel(g: ChannelGroup): string {
    const q = Array.from(availableQualities(g)).filter((x) => x !== 'UNKNOWN');
    return q.map((x) => x.replace('UHD_4K', '4K')).join(' · ');
  }

  function row(title: string, items: HTMLElement[]): HTMLElement {
    return h('div', { class: 'row' }, h('h2', null, title, h('span', { class: 'count', text: String(items.length) })), h('div', { class: 'cards' }, ...items));
  }

  function findGroup(key: string): ChannelGroup | undefined { return groups.find((g) => g.key === key); }

  function playFolder(folder: ChannelFolder, index: number): void {
    const all = folders.length ? folders : [folder];
    const fi = Math.max(0, all.indexOf(folder));
    push(playerScreen(all, fi, index, false));
  }
  function playList(list: ChannelGroup[], index: number, isFavorites: boolean): void {
    push(playerScreen([{ name: '', iconUrl: null, isFootball: false, channels: list }], 0, index, isFavorites));
  }

  function renderRows(): void {
    clear(rows); rows.scrollTop = 0;
    rows.style.top = filtersOpen ? '230px' : '150px';
    if (opened) {
      rows.appendChild(h('div', { class: 'row' }, h('h2', null, opened.name, h('span', { class: 'count', text: String(opened.channels.length) }), h('span', { style: { fontSize: '18px', marginLeft: '20px' }, text: t('folder_back_hint') })),
        h('div', { class: 'cards wrap', style: { flexWrap: 'wrap' } }, ...opened.channels.map((g, i) => card(g, null, () => playFolder(opened!, i), () => toggleFavChannel(g))))));
      return;
    }
    const mode = Store.qualityMode(); const fixed = fixedQuality(mode);
    let visible = fixed ? folders.filter((f) => f.channels.some((g) => { const a = availableQualities(g); return a.has(fixed) || a.has('UNKNOWN'); })) : folders;
    const needle = query.trim().toLowerCase();
    if (needle) visible = visible.filter((f) => f.name.toLowerCase().includes(needle));
    const favFolderNames = Store.favoriteFolders();
    const isFav = (f: ChannelFolder) => favFolderNames.includes(f.name);
    const favoriteFolders = visible.filter(isFav), football = visible.filter((f) => f.isFootball && !isFav(f)), other = visible.filter((f) => !f.isFootball && !isFav(f));
    const folderCard = (f: ChannelFolder) => card(isSingle(f) ? f.channels[0] : null, isSingle(f) ? null : f, () => { if (isSingle(f)) playFolder(f, 0); else { opened = f; renderRows(); focusFirst('.card'); } }, () => { if (isSingle(f)) toggleFavChannel(f.channels[0]); else { Store.toggleFavoriteFolder(f.name); renderRows(); refocus(); } });
    if (!needle) {
      if (liveNow.length) rows.appendChild(row(t('now_live'), liveNow.map((x, i) => card(x.group, null, () => playList(liveNow.map((y) => y.group), i, false)))));
      const favs = Store.favoriteChannelKeys().map(findGroup).filter((g): g is ChannelGroup => !!g);
      if (favs.length) {
        const favRow = row(t('section_favorite_channels'), favs.map((g, i) => card(g, null, () => playList(favs, i, true), () => favoriteMenu(g))));
        const pos = reorderKey ? favs.findIndex((g) => g.key === reorderKey) : -1;
        if (pos >= 0) favRow.querySelector('h2')!.appendChild(h('span', { style: { fontSize: '18px', marginLeft: '20px', color: '#ffd54f' }, text: t('reorder_a11y_position', pos + 1, favs.length) + ' · ◀ ▶ · OK' }));
        rows.appendChild(favRow);
      }
      const recents = Store.recentChannelKeys().map(findGroup).filter((g): g is ChannelGroup => !!g);
      if (recents.length) rows.appendChild(row(t('section_recent'), recents.map((g, i) => card(g, null, () => playList(recents, i, false), () => toggleFavChannel(g)))));
    }
    if (favoriteFolders.length) rows.appendChild(row(t('section_favorite_folders'), favoriteFolders.map(folderCard)));
    if (football.length) rows.appendChild(row(t('section_football'), football.map(folderCard)));
    if (other.length) rows.appendChild(row(t('section_more_sports'), other.map(folderCard)));
    if (!visible.length) rows.appendChild(h('div', { class: 'center' }, h('div', { text: needle ? t('no_results') : t('no_sports_channels') })));
  }

  /** Long OK on a favorite: move it (then ◀ ▶ keep moving until OK or Back) or drop it. */
  function favoriteMenu(g: ChannelGroup): void {
    confirmDialog(g.displayName, [
      { label: t('action_move_left'), onPick: () => moveFavorite(g.key, -1) },
      { label: t('action_move_right'), onPick: () => moveFavorite(g.key, 1) },
      { label: t('action_remove_favorite'), onPick: () => toggleFavChannel(g), danger: true },
    ]);
  }
  function moveFavorite(key: string, delta: number): void {
    reorderKey = key; Store.moveFavoriteChannel(key, delta); renderRows();
    const card = rows.querySelector<HTMLElement>('.card[data-key="' + key.replace(/"/g, '\\"') + '"]');
    if (card) setFocus(card); else refocus();
  }
  function endReorder(): void { if (!reorderKey) return; const key = reorderKey; reorderKey = null; renderRows(); const card = rows.querySelector<HTMLElement>('.card[data-key="' + key.replace(/"/g, '\\"') + '"]'); if (card) setFocus(card); else refocus(); }

  function toggleFavChannel(g: ChannelGroup): void {
    const added = Store.toggleFavoriteChannel(g.key);
    toast(added ? t('added_to_favorites') : t('removed_from_favorites'));
    renderRows(); refocus();
  }

  async function load(force = false): Promise<void> {
    clear(rows); rows.appendChild(h('div', { class: 'center' }, h('div', { class: 'spinner' }), h('div', { text: t('loading_channels') })));
    header.textContent = sourceTitle(); sub.textContent = '';
    try {
      groups = await Repository.loadLiveGroups(force);
      folders = foldIntoFolders(groups);
      sub.textContent = plural('channels_count', groups.length);
      renderRows(); focusFirst('.card');
      refreshLiveNow();
      // Logos arrive later (the map may need a download): re-render in place, keeping the focus.
      const loaded = groups;
      Repository.enrichLogos(groups).then((enriched) => { if (groups !== loaded || enriched === loaded) return; groups = enriched; folders = foldIntoFolders(groups); renderRows(); refocus(); });
    } catch (e) {
      clear(rows);
      rows.appendChild(h('div', { class: 'center' }, h('div', { text: t('error_load_channels') }), h('div', { class: 'btn primary focusable', text: t('action_retry'), onclick: () => load(true) })));
      focusFirst();
    }
  }

  async function refreshLiveNow(): Promise<void> {
    const token = ++liveNowToken;
    const keys = Array.from(new Set([...Store.recentChannelKeys(), ...Store.favoriteChannelKeys()]));
    const list = keys.map(findGroup).filter((g): g is ChannelGroup => !!g);
    const out: Array<{ group: ChannelGroup; title: string }> = [];
    for (const g of list) {
      const p = await Repository.nowProgram(g);
      if (token !== liveNowToken) return;
      if (p) out.push({ group: g, title: p.title });
    }
    if (out.length !== liveNow.length) { liveNow = out; renderRows(); refocus(); } else liveNow = out;
  }

  const unsubscribe = Store.onChange(() => { /* favorites/recents changed in the player */ });

  return {
    el,
    onShow: () => { renderFilters(); if (!groups.length) load(); else { renderRows(); refocus('.card'); refreshLiveNow(); } },
    onHide: () => { unsubscribe(); },
    initialFocus: '.card',
    onKey: (code) => {
      if (reorderKey) {
        if (code === KEY.LEFT || code === KEY.RIGHT) { moveFavorite(reorderKey, code === KEY.LEFT ? -1 : 1); return true; }
        if (code === KEY.ENTER || code === KEY.BACK) { endReorder(); return true; }
        if (code === KEY.UP || code === KEY.DOWN) endReorder();
      }
      if (code === KEY.BACK) {
        if (document.activeElement && document.activeElement.tagName === 'INPUT') { (document.activeElement as HTMLElement).blur(); return true; }
        if (opened) { opened = null; renderRows(); focusFirst('.card'); return true; }
        if (filtersOpen) { filtersOpen = false; filters.classList.add('hidden'); renderRows(); focusFirst('.card'); return true; }
        return pop();
      }
      return false;
    },
  };
}
