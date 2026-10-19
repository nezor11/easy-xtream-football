/** APIs missing on the oldest TVs we target (webOS 4.x = Chrome 53). Syntax is lowered by esbuild; APIs are not. */
if (!String.prototype.padStart) {
  String.prototype.padStart = function (len: number, fill?: string): string {
    const pad = fill === undefined ? ' ' : String(fill);
    let s = String(this);
    while (s.length < len && pad) s = pad.slice(0, len - s.length) + s;
    return s;
  };
}
if (!String.prototype.padEnd) {
  String.prototype.padEnd = function (len: number, fill?: string): string {
    const pad = fill === undefined ? ' ' : String(fill);
    let s = String(this);
    while (s.length < len && pad) s = s + pad.slice(0, len - s.length);
    return s;
  };
}
export {};
