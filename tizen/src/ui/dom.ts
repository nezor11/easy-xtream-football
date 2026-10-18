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
