// Aúna · animaciones de la página de presentación.
document.documentElement.classList.add("js");

// Títulos por líneas: envuelve cada línea para que suba desde detrás de su máscara.
document.querySelectorAll(".lines > span").forEach((line, i) => {
  const inner = document.createElement("span");
  inner.className = "in";
  inner.style.transitionDelay = `${i * 0.12}s`;
  while (line.firstChild) inner.appendChild(line.firstChild);
  line.appendChild(inner);
});

// Aparecer al entrar en pantalla (con un pequeño escalonado entre hermanos).
const io = new IntersectionObserver((entries) => {
  entries.forEach((e) => {
    if (!e.isIntersecting) return;
    const el = e.target;
    const siblings = [...el.parentElement.children].filter((c) => c.classList.contains("reveal"));
    const idx = Math.max(0, siblings.indexOf(el));
    el.style.transitionDelay = `${Math.min(idx, 6) * 0.08}s`;
    el.classList.add("visto");
    io.unobserve(el);
  });
}, { threshold: 0.18, rootMargin: "0px 0px -8% 0px" });
document.querySelectorAll(".reveal, .lines").forEach((el) => io.observe(el));

// Antes y ahora: la secuencia empieza cuando la diapositiva ocupa casi toda la pantalla.
const antes = document.getElementById("antes");
if (antes) new IntersectionObserver((es, o) => es.forEach((e) => {
  if (e.isIntersecting) { antes.classList.add("play"); o.disconnect(); }
}), { threshold: 0.6 }).observe(antes);

// Cabecera: se esconde al bajar y vuelve al subir. Marca la sección activa.
const cabecera = document.querySelector(".top");
let lastY = scrollY;
addEventListener("scroll", () => {
  const y = scrollY;
  cabecera.classList.toggle("hidden", y > lastY && y > 300);
  lastY = y;
}, { passive: true });
const links = [...document.querySelectorAll(".menu a")];
const secObs = new IntersectionObserver((entries) => {
  entries.forEach((e) => {
    if (e.isIntersecting) links.forEach((a) => a.classList.toggle("on", a.getAttribute("href") === `#${e.target.id}`));
  });
}, { rootMargin: "-45% 0px -50% 0px" });
document.querySelectorAll("main > section[id]").forEach((s) => secObs.observe(s));

// Menú en el móvil.
const burger = document.querySelector(".burger");
const mm = document.getElementById("menu-movil");
burger.addEventListener("click", () => {
  const open = burger.getAttribute("aria-expanded") !== "true";
  burger.setAttribute("aria-expanded", String(open));
  mm.hidden = !open;
});
mm.addEventListener("click", (e) => { if (e.target.tagName === "A") { mm.hidden = true; burger.setAttribute("aria-expanded", "false"); } });

// 1 · La app: el móvil fijo cambia de pantalla con un círculo que se abre desde el centro.
const screens = [...document.querySelectorAll(".screen")];
const dots = [...document.querySelectorAll(".dots li")];
let current = 1;
function show(step) {
  if (step === current) return;
  screens.forEach((s) => s.classList.remove("prev"));
  screens.find((s) => +s.dataset.step === current)?.classList.add("prev");
  screens.forEach((s) => s.classList.toggle("on", +s.dataset.step === step));
  dots.forEach((d, i) => d.classList.toggle("on", i === step - 1));
  current = step;
}
const steps = [...document.querySelectorAll(".step")];
// El paso activo es el más cercano al centro de la pantalla.
function pickStep() {
  const mid = innerHeight / 2;
  let best = null, dist = Infinity;
  steps.forEach((s) => {
    const r = s.getBoundingClientRect();
    const d = Math.abs(r.top + r.height / 2 - mid);
    if (d < dist) { dist = d; best = s; }
  });
  if (!best) return;
  steps.forEach((s) => s.classList.toggle("on", s === best));
  show(+best.dataset.step);
}
let ticking = false;
addEventListener("scroll", () => {
  if (ticking) return;
  ticking = true;
  requestAnimationFrame(() => { pickStep(); ticking = false; });
}, { passive: true });
pickStep();

// Hilo de puntos: se va dibujando según bajas (máscara que crece sobre un trazo punteado).
const hilo = document.querySelector(".hilo");
const path = document.getElementById("hilo-path");
if (hilo && path) {
  const ns = "http://www.w3.org/2000/svg";
  const defs = document.createElementNS(ns, "defs");
  const mask = document.createElementNS(ns, "mask");
  mask.id = "hilo-mask";
  const draw = path.cloneNode();
  draw.removeAttribute("id");
  draw.setAttribute("stroke", "#fff");
  draw.style.strokeWidth = "3";
  mask.appendChild(draw);
  defs.appendChild(mask);
  hilo.prepend(defs);
  path.setAttribute("mask", "url(#hilo-mask)");
  path.style.strokeDasharray = "0.1 2.2";
  path.style.strokeWidth = "3.5";
  const len = draw.getTotalLength();
  draw.style.strokeDasharray = `${len}`;
  const update = () => {
    const max = document.documentElement.scrollHeight - innerHeight;
    const p = Math.min(1, Math.max(0, scrollY / max));
    draw.style.strokeDashoffset = `${len * (1 - p)}`;
  };
  update();
  addEventListener("scroll", update, { passive: true });
  addEventListener("resize", update);
}

// Demo «¿Qué quiere decir Marco?»: la respuesta florece desde el girasol.
const DEMO = [
  ["Le duele algo", "Enséñale los pictos del cuerpo y avisa a su madre."],
  ["Hay demasiado ruido", "Llévale a un sitio tranquilo y ponle su música."],
  ["Tiene hambre o sed", "Enséñale los pictos de comida y bebida."],
  ["Quiere salir", "Si se puede, sal con él."],
];
const out = document.querySelector(".demo-out");
document.querySelectorAll(".demo-chips button").forEach((b) => b.addEventListener("click", () => {
  document.querySelectorAll(".demo-chips button").forEach((x) => x.setAttribute("aria-pressed", String(x === b)));
  const [sig, hacer] = DEMO[+b.dataset.i];
  document.getElementById("demo-sig").textContent = sig;
  document.getElementById("demo-hacer").textContent = hacer;
  out.classList.remove("flip"); void out.offsetWidth; out.classList.add("flip");
}));

// Contacto: sin servidor, el mensaje se abre en el correo de la persona.
const CORREO = ""; // ← pon aquí el correo de contacto
const form = document.getElementById("form");
const note = document.getElementById("form-note");
if (CORREO) { const t = document.getElementById("correo-txt"); t.textContent = CORREO; t.classList.remove("placeholder"); }
form.addEventListener("submit", (e) => {
  e.preventDefault();
  if (!CORREO) { note.textContent = "Falta poner el correo de contacto del equipo."; return; }
  const d = new FormData(form);
  const body = `${d.get("mensaje")}\n\n— ${d.get("nombre")} (${d.get("correo")})`;
  location.href = `mailto:${CORREO}?subject=${encodeURIComponent("Aúna · mensaje de " + d.get("nombre"))}&body=${encodeURIComponent(body)}`;
  note.textContent = "Se ha abierto tu correo con el mensaje listo para enviar.";
});

// Teclado: espacio o → avanzan a la siguiente parada; mayús+espacio o ← vuelven.
// Las paradas son cada sección y, dentro de «La app», cada paso del móvil.
const paradas = [...document.querySelectorAll("main > section, .step, .pie")];
const suave = matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth";
function destino(el) {
  const r = el.getBoundingClientRect();
  if (el.classList.contains("step")) return scrollY + r.top + r.height / 2 - innerHeight / 2;
  return scrollY + r.top;
}
let ultimo = { y: 0, t: -1e9 }; // si pulsas seguido, cuenta desde la parada a la que ya vamos
function irA(dir) {
  const max = document.documentElement.scrollHeight - innerHeight;
  const ys = paradas.filter((el) => !el.closest(".app") || el.classList.contains("step") || el === document.getElementById("app"))
    .map((el) => Math.min(max, Math.max(0, Math.round(destino(el)))))
    .sort((a, b) => a - b)
    .filter((v, i, arr) => i === 0 || v - arr[i - 1] >= innerHeight * 0.3);
  const y = performance.now() - ultimo.t < 900 ? ultimo.y : scrollY;
  const t = dir > 0 ? ys.find((v) => v > y + 8) : [...ys].reverse().find((v) => v < y - 8);
  if (t === undefined) return;
  ultimo = { y: t, t: performance.now() };
  scrollTo({ top: t, behavior: suave });
}
addEventListener("keydown", (e) => {
  if (e.defaultPrevented || e.altKey || e.ctrlKey || e.metaKey) return;
  const el = document.activeElement;
  if (el && (el.isContentEditable || /^(INPUT|TEXTAREA|SELECT|BUTTON)$/.test(el.tagName))) return;
  let dir = 0;
  if (e.key === " " || e.code === "Space") dir = e.shiftKey ? -1 : 1;
  else if (e.key === "ArrowRight" && !e.shiftKey) dir = 1;
  else if (e.key === "ArrowLeft" && !e.shiftKey) dir = -1;
  if (!dir) return;
  e.preventDefault();
  irA(dir);
});
