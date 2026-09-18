import { glyphs } from "./assets/phosphor/glyphs.js?v=5";

// Official Phosphor regular assets; retain their original paths and proportions.
export function icon(name, cls = "") {
  const glyph = glyphs[name];
  if (!glyph) throw new Error(`Unknown preview icon: ${name}`);
  return `<svg class="icon ${cls}" viewBox="0 0 256 256" fill="currentColor" aria-hidden="true" focusable="false">${glyph}</svg>`;
}
