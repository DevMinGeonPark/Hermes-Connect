import { glyphs } from "./assets/ionicons/glyphs.js?v=6";

// Official Ionicons geometry; only black paint is normalized to currentColor.
export function icon(name, cls = "", variant = "outline") {
  const key = variant === "filled" ? `${name}-filled` : name;
  const glyph = glyphs[key];
  if (!glyph) throw new Error(`Unknown preview icon: ${key}`);
  return `<svg class="icon ${cls}" data-icon="${name}" data-variant="${variant}" viewBox="0 0 512 512" fill="currentColor" aria-hidden="true" focusable="false">${glyph}</svg>`;
}
