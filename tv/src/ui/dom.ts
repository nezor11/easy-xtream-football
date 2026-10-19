/** Tiny DOM helpers. */
export type Child = Node | string | null | undefined | false;
export function h(tag: string, attrs: Record<string, any> | null, ...children: Child[]): HTMLElement {
  const el = document.createElement(tag);
  if (attrs) for (const k of Object.keys(attrs)) {
    const v = attrs[k];
    if (v === null || v === undefined || v === false) continue;
    if (k === 'class') el.className = String(v);
    else if (k === 'text') el.textContent = String(v);
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2).toLowerCase(), v);
    else if (k === 'style' && typeof v === 'object') Object.assign(el.style, v);
    else if (k.startsWith('data-')) el.setAttribute(k, String(v));
    else (el as any)[k] = v;
  }
  for (const c of children) {
    if (c === null || c === undefined || c === false) continue;
    el.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
  }
  return el;
}
export function clear(el: Element): void { while (el.firstChild) el.removeChild(el.firstChild); }

const ICONS: Record<string, string> = {
  search: '<circle cx="10" cy="10" r="6.5"/><line x1="15" y1="15" x2="21" y2="21"/>',
  reload: '<path d="M20 12a8 8 0 1 1-2.3-5.7"/><polyline points="20 3 20 9 14 9"/>',
  // Gear (Feather "settings", MIT): TV fonts often lack U+2699.
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
};
/** Inline SVG icon (stroke = currentColor), 1em square. */
export function icon(name: string): HTMLElement {
  const span = document.createElement('span');
  span.className = 'icon-' + name;
  span.style.display = 'inline-block'; span.style.width = '1em'; span.style.height = '1em'; span.style.verticalAlign = '-0.15em';
  span.innerHTML = `<svg viewBox="0 0 24 24" width="100%" height="100%" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">${ICONS[name] || ''}</svg>`;
  return span;
}
