// Iconos de trazo del diseño (viewBox 24×24), los mismos que en la app de padres.
import { html } from "https://cdn.jsdelivr.net/npm/htm@3.1.1/preact/standalone.module.js";

const P = {
  back: ["M15 6l-6 6 6 6"],
  next: ["M9 6l6 6-6 6"],
  close: ["M6 6l12 12M18 6 6 18"],
  check: ["M5 12l5 5L20 7"],
  plus: ["M12 5v14M5 12h14"],
  send: ["M5 12h14M13 6l6 6-6 6"],
  chat: ["M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z"],
  people: ["M2.5 20c.8-3.5 3.4-5.5 6.5-5.5s5.7 2 6.5 5.5", "M17 14c2.4 0 4 1.6 4.5 4.5"],
  scan: ["M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M7 12h10"],
  warning: ["M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z", "M12 9v4M12 17h.01"],
  pin: ["M12 21s-7-6.1-7-11.5a7 7 0 0 1 14 0C19 14.9 12 21 12 21z"],
  eye: ["M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"],
  lock: ["M8 11V7a4 4 0 0 1 8 0v4"],
  book: ["M4 5a2 2 0 0 1 2-2h12v18H6a2 2 0 0 1-2-2z"],
  hand: ["M18 11V6a2 2 0 0 0-4 0v5", "M14 10V4a2 2 0 0 0-4 0v6", "M10 10.5V6a2 2 0 0 0-4 0v8", "M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.9-6-2.4l-3.6-3.6a2 2 0 0 1 2.8-2.8L7 15"],
  star: ["M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5L2.5 9.4l6.6-.9z"],
  logout: ["M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3", "M10 17l-5-5 5-5M5 12h11"],
  volume: ["M11 5 6 9H2v6h4l5 4z", "M15.5 8.5a5 5 0 0 1 0 7"],
};
const CIRCLES = { people: [[9, 8, 3.5], [17, 9, 2.5]], eye: [[12, 12, 3]], pin: [[12, 9.5, 2.5]] };
const RECTS = { lock: [[5, 11, 14, 10, 2]] };
const PHONE = "M6.6 10.8a15.1 15.1 0 0 0 6.6 6.6l2.2-2.2a1 1 0 0 1 1-.25 11.4 11.4 0 0 0 3.6.57 1 1 0 0 1 1 1V20a1 1 0 0 1-1 1A17 17 0 0 1 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1c0 1.25.2 2.45.57 3.57a1 1 0 0 1-.25 1z";

/** Icono decorativo; la etiqueta accesible va en el botón que lo contiene. */
export function Icon({ name, size = 22, color = "currentColor", stroke = 2.2 }) {
  if (name === "phone") {
    return html`<svg width=${size} height=${size} viewBox="0 0 24 24" aria-hidden="true"><path d=${PHONE} fill=${color} /></svg>`;
  }
  const filled = name === "star";
  return html`<svg width=${size} height=${size} viewBox="0 0 24 24" fill=${filled ? color : "none"} stroke=${filled ? "none" : color}
      stroke-width=${stroke} stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    ${(P[name] || []).map((d) => html`<path d=${d} />`)}
    ${(CIRCLES[name] || []).map(([cx, cy, r]) => html`<circle cx=${cx} cy=${cy} r=${r} />`)}
    ${(RECTS[name] || []).map(([x, y, w, h, rx]) => html`<rect x=${x} y=${y} width=${w} height=${h} rx=${rx} />`)}
  </svg>`;
}
