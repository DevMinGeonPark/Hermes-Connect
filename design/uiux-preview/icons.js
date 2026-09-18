const paths = {
  compose: "M14 4H4v16h16V10 M20 2l2 2-11 11-4 1 1-4Z",
  list: "M9 6h12 M9 12h12 M9 18h12 M3 6h.01 M3 12h.01 M3 18h.01",
  computer: "M3 4h18v13H3z M8 21h8 M12 17v4",
  chat: "M21 11.5a8.4 8.4 0 0 1-.9 3.8A8.5 8.5 0 0 1 12.5 20a8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.7A8.4 8.4 0 0 1 4 11.5 8.5 8.5 0 0 1 8.7 3.9a8.4 8.4 0 0 1 3.8-.9H13a8.5 8.5 0 0 1 8 8v.5Z",
  grid: "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z",
  link: "M10 13a5 5 0 0 0 7 .1l3-3a5 5 0 0 0-7-7l-2 2 M14 11a5 5 0 0 0-7-.1l-3 3a5 5 0 0 0 7 7l2-2",
  arrow: "M7 17 17 7 M7 7h10v10",
  chevron: "m9 5 7 7-7 7",
  down: "m6 9 6 6 6-6",
  plus: "M12 5v14 M5 12h14",
  send: "m5 12 7-7 7 7 M12 5v15",
  mic: "M9 5a3 3 0 0 1 6 0v7a3 3 0 0 1-6 0V5Z M5 10v2a7 7 0 0 0 14 0v-2 M12 19v3 M8 22h8",
  file: "M14 2H5v20h14V7z M14 2v6h5 M8 12h8 M8 16h6",
  folder: "M3 5h6l2 3h10v12H3z",
  clock: "M12 8v5l3 2 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z",
  check: "m5 12 4 4L19 6",
  close: "m6 6 12 12 M6 18 18 6",
  terminal: "m4 6 6 6-6 6 M12 19h8",
  sliders:
    "M4 21v-7 M4 10V3 M12 21v-9 M12 8V3 M20 21v-5 M20 12V3 M1 14h6 M9 8h6 M17 16h6",
  shield: "M12 2 3 6v6c0 5 9 10 9 10s9-5 9-10V6Z m-5 10 3 3 5-6",
  menu: "M4 6h16 M4 12h16 M4 18h16",
  search: "M21 21l-5-5 M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z",
  dots: "M5 12h.01 M12 12h.01 M19 12h.01",
  pause: "M8 5v14 M16 5v14",
  play: "m7 3 14 9-14 9Z",
  bell: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9 M10 21h4",
  activity: "M2 12h4l3-8 6 16 3-8h4",
  external: "M15 3h6v6 M21 3l-9 9 M9 3H3v18h18v-6",
  copy: "M9 9h12v12H9z M5 15H3V3h12v2",
  book: "M12 5v16 M2 3c5 0 7 0 10 2 3-2 5-2 10-2v16c-5 0-7 0-10 2-3-2-5-2-10-2Z",
  moon: "M21 13a9 9 0 1 1-10-10 7 7 0 0 0 10 10Z",
  wifi: "M2 8a16 16 0 0 1 20 0 M5 12a11 11 0 0 1 14 0 M9 16a5 5 0 0 1 6 0 M12 20h.01",
  branch:
    "M6 3v12a6 6 0 0 0 12 0V9 M6 9h7a5 5 0 0 0 5-5 M3 3a3 3 0 1 0 6 0 M15 4a3 3 0 1 0 6 0",
  spark: "m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5Z",
  back: "m14 5-7 7 7 7",
  warning: "M12 3 2 21h20Z M12 9v5 M12 18h.01",
  volume: "m11 5-6 4H2v6h3l6 4Z M15 8a6 6 0 0 1 0 8 M18 5a10 10 0 0 1 0 14",
};
export function icon(name, cls = "") {
  return `<svg class="icon ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${(
    paths[name] || paths.spark
  )
    .split(" | ")
    .map((d) => `<path d="${d}"/>`)
    .join("")}</svg>`;
}
