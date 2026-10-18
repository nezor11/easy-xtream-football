/** Remote log for on-device debugging: mirrors console and errors to a PC via POST.
 *  Enabled only when the build defines __EXF_LOG_URL__ (see build.mjs, EXF_LOG_URL env). */
declare const __EXF_LOG_URL__: string;

export function installRemoteLog(): void {
  const url = typeof __EXF_LOG_URL__ === 'string' ? __EXF_LOG_URL__ : '';
  if (!url) return;
  const send = (level: string, parts: unknown[]): void => {
    const text = parts.map((p) => {
      if (p instanceof Error) return `${p.name}: ${p.message}\n${p.stack ?? ''}`;
      if (typeof p === 'string') return p;
      try { return JSON.stringify(p); } catch { return String(p); }
    }).join(' ');
    try {
      const xhr = new XMLHttpRequest();
      xhr.open('POST', url, true);
      xhr.send(`[${level}] ${text}`);
    } catch { /* ignore */ }
  };
  for (const level of ['log', 'info', 'warn', 'error'] as const) {
    const orig = console[level].bind(console);
    console[level] = (...args: unknown[]) => { orig(...args); send(level, args); };
  }
  window.addEventListener('error', (e) => send('uncaught', [`${e.message} @ ${e.filename}:${e.lineno}:${e.colno}`, e.error]));
  window.addEventListener('unhandledrejection', (e: PromiseRejectionEvent) => send('unhandled', [e.reason]));
  send('log', ['remote log on', navigator.userAgent]);
}
