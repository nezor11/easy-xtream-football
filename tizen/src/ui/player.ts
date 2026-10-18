/** Full-screen player: zapping, OK menu (quality / audio / subtitles / guide / info / coffee), channel info, radio view. */
import { ChannelFolder, ChannelGroup, EpgProgram, QUALITY, QUALITY_TIERS, QualityName, availableQualities, variantAtOrBelow, variantFor } from '../core/model';
import { Player, TrackInfo } from '../core/avplay';
import { Repository } from '../core/repository';
import { Store } from '../core/storage';
import { t } from '../core/i18n';
import { h, clear } from './dom';
import { KEY, setFocusRoot, focusFirst, refocus } from './focus';
import { Screen, pop } from './screens';

type Section = 'QUALITY' | 'AUDIO' | 'SUBTITLES' | 'GUIDE' | 'INFO' | 'COFFEE';
const SECTIONS: Section[] = ['QUALITY', 'AUDIO', 'SUBTITLES', 'GUIDE', 'INFO', 'COFFEE'];

export function playerScreen(folders: ChannelFolder[], folderIndex: number, channelIndex: number, isFavoritesList: boolean): Screen {
  let fi = folderIndex, ci = channelIndex;
  let lastFi = fi, lastCi = ci;
  let group: ChannelGroup | null = null;
  let quality: QualityName | null = fixedFromMode();
  let currentQuality: QualityName = 'UNKNOWN';
  let paused = false, menuOpen = false, section: Section = 'QUALITY';
  let epg: EpgProgram[] = [];
  let lastKeyAt = 0, lastKeyCode = 0;
  let skipCount = 0;
  let legendTimer: number | null = null, coffeeTimer: number | null = null;
  let tracks: TrackInfo[] = [];
  let audioIndex = -1, subtitleIndex = -1;

  const info = h('div', { class: 'osd-info' });
  const msg = h('div', { class: 'osd-msg hidden' });
  const legend = h('div', { class: 'osd-legend hidden' });
  const radio = h('div', { class: 'radio hidden' });
  const menu = h('div', { class: 'menu hidden' });
  const coffee = h('div', { class: 'hidden gap14', style: { position: 'absolute', right: '48px', bottom: '48px', padding: '14px 18px', borderRadius: '12px', background: 'rgba(10,14,18,0.9)', display: 'flex', alignItems: 'center' } });
  const el = h('div', { class: 'screen player' }, radio, info, msg, legend, coffee, menu);

  function fixedFromMode(): QualityName | null { const m = Store.qualityMode(); return m === 'AUTO' ? null : m; }
  function channels(): ChannelGroup[] { return (folders[fi] && folders[fi].channels) || []; }
  function current(): ChannelGroup | null { return channels()[ci] || null; }

  /** Highest tier worth decoding on this panel: 4K only on an UHD panel, else FHD. */
  function panelCap(): number {
    try { if ((window as any).webapis.productinfo.isUdPanelSupported()) return QUALITY.UHD_4K.rank; } catch (e) { /* not a TV */ }
    return QUALITY.FHD.rank;
  }
  function pickVariant(g: ChannelGroup) {
    if (quality) { const v = variantFor(g, quality) || variantAtOrBelow(g, quality); currentQuality = v.quality; return v; }
    // Auto (port of PlayerViewModel.variantForEmission): cap by the panel, then the best variant the measured
    // network sustains with a 20 % margin; without a measurement, FHD or below.
    const cap = panelCap();
    const allowed = g.variants.filter((v) => QUALITY[v.quality].rank <= cap);
    const pool = allowed.length ? allowed : g.variants;
    const bandwidth = Store.bandwidthBps();
    const v = bandwidth <= 0
      ? (pool.find((x) => QUALITY[x.quality].rank <= QUALITY.FHD.rank) || pool[pool.length - 1])
      : (pool.find((x) => QUALITY[x.quality].typicalBitrateBps <= bandwidth * 0.8) || pool[pool.length - 1]);
    currentQuality = v.quality; return v;
  }
  // Buffering stalls: three within a minute on an Auto pick means the network is slower than measured.
  let stalls: number[] = [];
  function onStall(): void {
    const now = Date.now(); stalls = stalls.filter((at) => now - at < 60000); stalls.push(now);
    if (quality || stalls.length < 3 || !group) return;
    const lower = group.variants.find((v) => QUALITY[v.quality].rank < QUALITY[currentQuality].rank);
    if (!lower) return;
    const bits = QUALITY[currentQuality].typicalBitrateBps;
    Store.setBandwidthBps(Math.min(Store.bandwidthBps() || bits, bits) * 0.8);
    console.log('stepping down after stalls, bandwidth now', Store.bandwidthBps());
    stalls = []; play();
  }

  function play(): void {
    group = current();
    if (!group) { showMsg(t('player_no_channel'), true); return; }
    const variant = pickVariant(group);
    paused = false; tracks = []; audioIndex = -1; subtitleIndex = -1; epg = []; stalls = [];
    renderInfo(); showMsg('', false);
    radio.classList.toggle('hidden', !group.isRadio);
    if (group.isRadio) renderRadio();
    Player.open(variant.channel.streamUrl, {
      onReady: () => { skipCount = 0; showMsg('', false); tracks = Player.tracks(); if (menuOpen) renderMenu(); renderInfo(); if (group) Store.pushRecentChannel(group.key); },
      onBuffering: (b) => { if (b) { showMsg(t('app_loading'), false); onStall(); } else showMsg('', false); },
      onError: (e) => onPlayError(e),
      onEnded: () => onPlayError('ended'),
    });
    loadEpg();
    armCoffee();
  }

  function onPlayError(e: string): void {
    const g = group;
    const blocked = /CONNECTION_FAILED|403|401|404/.test(e);
    if (g && g.geoBlocked) { showMsg(t('player_blocked_skipping', 403), true); autoSkip(); return; }
    if (skipCount < channels().length - 1 && channels().length > 1 && blocked) { showMsg(t('player_skipping_unavailable'), true); autoSkip(); return; }
    showMsg(t('player_error_with_hint', blocked ? t('player_blocked_no_access', 403, '') : e), true);
  }
  function autoSkip(): void { skipCount++; window.setTimeout(() => { step(1, false); }, 1200); }

  function step(delta: number, manual: boolean): void {
    const n = channels().length;
    if (manual) { lastFi = fi; lastCi = ci; }
    if (n <= 1) { stepFolder(delta); return; }
    ci = (ci + delta + n) % n; play();
  }
  function stepFolder(delta: number): void { const n = folders.length; if (!n) return; fi = (fi + delta + n) % n; ci = 0; play(); }
  function jumpToLast(): void { const f = lastFi, c = lastCi; lastFi = fi; lastCi = ci; fi = Math.min(f, folders.length - 1); ci = Math.min(c, Math.max(0, channels().length - 1)); play(); }

  function changeQuality(delta: number): void {
    const g = group; if (!g) return;
    const avail = QUALITY_TIERS.filter((q) => availableQualities(g).has(q));
    if (avail.length <= 1) { showMsg(t('player_only_one_quality'), false, 1800); return; }
    const idx = Math.max(0, avail.indexOf(currentQuality));
    const next = avail[(idx + (delta > 0 ? -1 : 1) + avail.length) % avail.length]; // ▲ = higher (earlier in the list)
    quality = next; play();
  }

  async function loadEpg(): Promise<void> {
    const g = group; if (!g) return;
    try { const list = await Repository.epgFor(g); if (group === g) { epg = list; renderInfo(); if (menuOpen && section === 'GUIDE') renderMenu(); } } catch (e) { /* no guide */ }
  }

  function nowNext(): { now: EpgProgram | null; next: EpgProgram | null } {
    const now = Date.now();
    const i = epg.findIndex((p) => p.nowFlag || (p.start <= now && now < p.end));
    const cur = i >= 0 ? epg[i] : (epg[0] && epg[0].start > now ? null : epg[0] || null);
    const nxt = i >= 0 ? epg[i + 1] || null : (epg[0] && epg[0].start > now ? epg[0] : epg[1] || null);
    return { now: cur, next: nxt };
  }

  function renderInfo(): void {
    clear(info);
    if (!Store.channelInfoVisible() || !group) return;
    const video = tracks.find((x) => x.type === 'VIDEO');
    const line = h('div', { class: 'line' },
      h('span', { class: 'name', text: group.displayName }), h('span', { text: '•' }), h('span', { text: `${ci + 1}/${channels().length}` }),
      h('span', { text: '•' }), h('span', { text: '‹ ' + (quality ? QUALITY[quality].label : t('quality_auto')) + ' ›' }),
      video && video.width ? h('span', { text: '•' }) : null, video && video.width ? h('span', { text: `${video.width}x${video.height}` }) : null,
      folders[fi] && folders[fi].name && folders[fi].channels.length > 1 ? h('span', { class: 'sub', text: t('player_channel_from', folders[fi].name), style: { color: '#b9c2cc' } }) : null);
    info.appendChild(line);
    const { now, next } = nowNext();
    if (now || next) info.appendChild(h('div', { class: 'epg', text: (now ? t('epg_now', now.title) : '') + (next ? ' ' + t('epg_next_suffix', next.title) : '') }));
  }

  function renderRadio(): void {
    clear(radio); const g = group; if (!g) return;
    radio.appendChild(h('div', { class: 'box' }, g.iconUrl ? h('img', { src: g.iconUrl }) : h('div', { text: '📻', style: { fontSize: '96px' } })));
    radio.appendChild(h('div', { class: 'title', text: g.displayName }));
    radio.appendChild(h('div', { class: 'sub', text: t('radio_audio_only') }));
  }

  function showMsg(text: string, error: boolean, autoHideMs?: number): void {
    if (!text) { msg.classList.add('hidden'); return; }
    msg.textContent = text; msg.classList.toggle('error', error); msg.classList.remove('hidden');
    if (autoHideMs) window.setTimeout(() => { if (msg.textContent === text) msg.classList.add('hidden'); }, autoHideMs);
  }

  function showLegend(): void {
    if (Store.playerHintsShown() >= 3) return;
    Store.incrementPlayerHintsShown();
    clear(legend);
    for (const part of t('controls_legend').split('·')) { const [k, v] = part.split(':'); legend.appendChild(h('span', { class: 'k' }, h('b', { text: (k || '').trim() }), v ? ' ' + v.trim() : '')); }
    legend.classList.remove('hidden');
    if (legendTimer) clearTimeout(legendTimer);
    legendTimer = window.setTimeout(() => legend.classList.add('hidden'), 8000);
  }

  function armCoffee(): void {
    if (coffeeTimer) clearTimeout(coffeeTimer);
    coffee.classList.add('hidden');
    if (Store.coffeeReminderDismissed()) return;
    coffeeTimer = window.setTimeout(() => {
      clear(coffee);
      coffee.appendChild(h('img', { src: 'qr.png', class: 'qr', style: { width: '88px', height: '88px', padding: '5px' } }));
      coffee.appendChild(h('div', null, h('div', { text: t('support_title'), style: { color: '#39d353' } }), h('div', { text: t('support_entry'), style: { color: '#39d353' } }), h('div', { text: t('coffee_thanks'), style: { fontSize: '18px', color: '#39d353', marginTop: '4px' } })));
      coffee.classList.remove('hidden');
      coffeeTimer = window.setTimeout(() => coffee.classList.add('hidden'), 30000);
    }, 30000);
  }

  // --- OK menu ---
  function openMenu(): void { menuOpen = true; section = 'QUALITY'; tracks = Player.tracks(); renderMenu(); menu.classList.remove('hidden'); setFocusRoot(menu, menuKeys); focusFirst('.option'); }
  function closeMenu(): void { menuOpen = false; menu.classList.add('hidden'); setFocusRoot(el, screenKeys); }
  function sectionLabel(s: Section): string {
    return s === 'QUALITY' ? t('menu_section_quality') : s === 'AUDIO' ? t('menu_section_audio') : s === 'SUBTITLES' ? t('menu_section_subtitles') : s === 'GUIDE' ? t('menu_section_guide') : s === 'INFO' ? t('menu_section_info') : t('support_entry');
  }
  function option(label: string, selected: boolean, onPick: () => void): HTMLElement {
    return h('div', { class: 'option focusable' + (selected ? ' selected' : ''), 'data-key': label, text: (selected ? '●  ' : '○  ') + label, onclick: onPick });
  }
  function renderMenu(): void {
    clear(menu);
    const tabs = h('div', { class: 'tabs' }, ...SECTIONS.map((s) => h('div', { class: 'tab' + (s === section ? ' selected' : ''), text: sectionLabel(s) })));
    const options = h('div', { class: 'options' });
    menu.appendChild(tabs); menu.appendChild(options);
    const g = group;
    if (section === 'QUALITY' && g) {
      options.appendChild(option(t('quality_auto'), quality === null, () => { quality = null; Store.setQualityMode('AUTO'); play(); renderMenu(); refocus(); }));
      for (const q of QUALITY_TIERS.filter((x) => availableQualities(g).has(x))) options.appendChild(option(QUALITY[q].label, quality === q, () => { quality = q; play(); renderMenu(); refocus(); }));
    } else if (section === 'AUDIO') {
      const audio = tracks.filter((x) => x.type === 'AUDIO');
      if (!audio.length) options.appendChild(h('div', { class: 'text', text: t('audio_none') }));
      audio.forEach((tr, i) => options.appendChild(option((tr.language ? tr.language.toUpperCase() + ' · ' : '') + t('audio_track_n', i + 1) + (tr.codec ? ' · ' + tr.codec : ''), audioIndex === tr.index || (audioIndex < 0 && i === 0), () => { if (Player.selectTrack('AUDIO', tr.index)) audioIndex = tr.index; renderMenu(); refocus(); })));
    } else if (section === 'SUBTITLES') {
      const subs = tracks.filter((x) => x.type === 'TEXT');
      options.appendChild(option(t('subtitles_off'), subtitleIndex < 0, () => { Player.setSubtitlesEnabled(false); subtitleIndex = -1; renderMenu(); refocus(); }));
      subs.forEach((tr, i) => options.appendChild(option((tr.language ? tr.language.toUpperCase() + ' · ' : '') + t('subtitle_track_n', i + 1), subtitleIndex === tr.index, () => { if (Player.selectTrack('TEXT', tr.index)) { Player.setSubtitlesEnabled(true); subtitleIndex = tr.index; } renderMenu(); refocus(); })));
    } else if (section === 'GUIDE') {
      if (!epg.length) options.appendChild(h('div', { class: 'text', text: t('guide_none') }));
      epg.slice(0, 10).forEach((p) => options.appendChild(h('div', { class: 'option focusable', text: `${fmtTime(p.start)}  ${p.title}` })));
    } else if (section === 'INFO') {
      const visible = Store.channelInfoVisible();
      options.appendChild(option(visible ? t('info_hide') : t('info_show'), false, () => { Store.setChannelInfoVisible(!visible); renderInfo(); renderMenu(); refocus(); }));
      const v = tracks.filter((x) => x.type === 'VIDEO' || x.type === 'AUDIO').map((x) => `${x.type}: ${x.codec}${x.width ? ' ' + x.width + 'x' + x.height : ''}`).join('\n');
      if (v) options.appendChild(h('div', { class: 'text', text: v }));
    } else if (section === 'COFFEE') {
      options.appendChild(h('div', { class: 'gap18', style: { display: 'flex', alignItems: 'center' } }, h('img', { src: 'qr.png', class: 'qr', style: { width: '148px', height: '148px' } }), h('div', { class: 'text', text: t('support_message') + '\n' + t('support_site_handle'), style: { marginTop: '0' } })));
      const dismissed = Store.coffeeReminderDismissed();
      options.appendChild(option(dismissed ? t('coffee_reenable') : t('coffee_dismiss'), false, () => { Store.setCoffeeReminderDismissed(!dismissed); renderMenu(); refocus(); }));
    }
    menu.appendChild(h('div', { class: 'text', text: t('menu_nav_hint'), style: { fontSize: '18px' } }));
  }
  function fmtTime(ms: number): string { if (!ms) return '--:--'; const d = new Date(ms); return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`; }

  const menuKeys = (code: number): boolean => {
    if (code === KEY.BACK) { closeMenu(); return true; }
    if (code === KEY.LEFT || code === KEY.RIGHT) {
      const i = SECTIONS.indexOf(section); section = SECTIONS[(i + (code === KEY.RIGHT ? 1 : -1) + SECTIONS.length) % SECTIONS.length];
      renderMenu(); focusFirst('.option'); return true;
    }
    return false;
  };

  const screenKeys = (code: number): boolean => {
    const now = Date.now(); const dbl = code === lastKeyCode && now - lastKeyAt < 450; lastKeyAt = now; lastKeyCode = code;
    if (code === KEY.BACK) { return pop(); }
    if (code === KEY.LEFT || code === KEY.CH_DOWN) { if (dbl && code === KEY.LEFT) stepFolder(-1); else step(-1, true); return true; }
    if (code === KEY.RIGHT || code === KEY.CH_UP) { if (dbl && code === KEY.RIGHT) stepFolder(1); else step(1, true); return true; }
    if (code === KEY.UP) { changeQuality(1); return true; }
    if (code === KEY.DOWN) { changeQuality(-1); return true; }
    if (code === KEY.PLAY_PAUSE || code === KEY.PLAY || code === KEY.PAUSE) { togglePause(); return true; }
    if (code === KEY.STOP) { pop(); return true; }
    if (code === KEY.INFO) { Store.setChannelInfoVisible(!Store.channelInfoVisible()); renderInfo(); return true; }
    if (code === KEY.ENTER) {
      if (dbl) { togglePause(); if (menuOpen) closeMenu(); return true; }
      if (!menuOpen) openMenu();
      return true;
    }
    if (code === KEY.YELLOW) { jumpToLast(); return true; }
    return false;
  };
  function togglePause(): void { paused = !paused; if (paused) { Player.pause(); showMsg(t('player_paused'), false); } else { Player.resume(); showMsg('', false); } }

  // Long OK on the player = favorite toggle (the focus root is the screen itself).
  el.addEventListener('longpress', () => { const g = group; if (!g) return; const added = Store.toggleFavoriteChannel(g.key); showMsg(added ? t('added_to_favorites') : t('removed_from_favorites'), false, 1800); });

  return {
    el,
    onShow: () => { setFocusRoot(el, screenKeys); play(); showLegend(); },
    onHide: () => { Player.stop(); if (coffeeTimer) clearTimeout(coffeeTimer); if (legendTimer) clearTimeout(legendTimer); },
    onKey: screenKeys,
  };
}
