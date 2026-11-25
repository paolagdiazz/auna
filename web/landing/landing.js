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

// Mapa de validación. Para añadir a alguien, copia una línea de ejemplo y quita las //.
// nombre: quién es (va en negrita). frase: lo que pone el bocadillo.
// cita: true si la frase es algo que nos dijeron (sale entre comillas).
// lado: dónde sale el bocadillo respecto al pin ("arriba", "abajo", "izq" o "der").
// Vale cualquier sitio de la provincia. Las coordenadas se sacan en Google Maps: clic derecho sobre el sitio y se copian (lat, lon).
const VALIDADORES = [
  { lat: 36.5610, lon: -4.6162, nombre: "Colegio Salliver · Fuengirola", frase: "Centro privado interesado", lado: "arriba" },
  // { lat: 36.7213, lon: -4.4214, nombre: "Madre de Marco", frase: "Por fin no tengo que repetirlo todo cada curso.", cita: true, lado: "arriba" },
];
(function mapa() {
  const caja = document.getElementById("pines");
  const vacio = document.getElementById("mapa-vacio");
  if (!caja) return;
  // Misma proyección que el dibujo: los límites del mapa vienen en el atributo data-box del SVG.
  const svg = document.querySelector(".mapa-svg");
  const [LON0, LON1, LAT0, LAT1] = svg.dataset.box.split(",").map(Number);
  const [, , W, H] = svg.getAttribute("viewBox").split(" ").map(Number);
  const C = Math.cos(((LAT0 + LAT1) / 2) * Math.PI / 180), K = W / ((LON1 - LON0) * C);
  const pinSvg = '<svg viewBox="0 0 30 38" aria-hidden="true"><path d="M15 37c-1-4-13-13-13-22a13 13 0 0 1 26 0c0 9-12 18-13 22z"/><circle cx="15" cy="15" r="5" fill="#fff"/></svg>';
  VALIDADORES.forEach((v, i) => {
    const x = (v.lon - LON0) * C * K, y = (LAT1 - v.lat) * K;
    const pin = document.createElement("div");
    pin.className = `pin ${v.lado || "arriba"}`;
    pin.style.left = `${(x / W) * 100}%`;
    pin.style.top = `${(y / H) * 100}%`;
    pin.style.setProperty("--d", `${0.3 + i * 0.6}s`);
    pin.innerHTML = `<span class="pin-punto">${pinSvg}</span><div class="bocadillo"><strong></strong>${v.cita ? "<q></q>" : "<p></p>"}</div>`;
    pin.querySelector(".bocadillo strong").textContent = v.nombre || "";
    pin.querySelector(".bocadillo q, .bocadillo p").textContent = v.frase;
    caja.appendChild(pin);
  });
  vacio.hidden = VALIDADORES.length > 0;
  const sec = document.getElementById("mapa");
  new IntersectionObserver((es, o) => es.forEach((e) => {
    if (e.isIntersecting) { sec.classList.add("play"); o.disconnect(); }
  }), { threshold: 0.6 }).observe(sec);
})();

// Cada diapositiva recibe la clase «play» cuando ocupa casi toda la pantalla (para sus animaciones).
document.querySelectorAll("main > section.slide").forEach((sec) => new IntersectionObserver((es, o) => es.forEach((e) => {
  if (e.isIntersecting) { sec.classList.add("play"); o.disconnect(); }
}), { threshold: 0.6 }).observe(sec));

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
const CORREO = "contacto@auna.es";
const form = document.getElementById("form");
const note = document.getElementById("form-note");
form.addEventListener("submit", (e) => {
  e.preventDefault();
  if (!CORREO) { note.textContent = "Falta poner el correo de contacto del equipo."; return; }
  const d = new FormData(form);
  const body = `${d.get("mensaje")}\n\n— ${d.get("nombre")} (${d.get("correo")})`;
  location.href = `mailto:${CORREO}?subject=${encodeURIComponent("Aúna · mensaje de " + d.get("nombre"))}&body=${encodeURIComponent(body)}`;
  note.textContent = "Se ha abierto tu correo con el mensaje listo para enviar.";
});

// Modo presentación: espacio o → van a la siguiente diapositiva marcada con data-p;
// mayús+espacio o ← vuelven. Si hay secciones de la web en medio, se salta con un telón.
const paradas = [...document.querySelectorAll("main > section[data-p]")];
const suave = matchMedia("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth";
const telon = document.createElement("div");
telon.className = "telon";
document.body.appendChild(telon);
let saltando = false;
// Desplazamiento propio (0,7 s con aceleración suave) para que cada paso dure siempre lo mismo.
function deslizar(y, fin) {
  const raiz = document.documentElement;
  const y0 = scrollY, dy = y - y0, t0 = performance.now(), dur = 700;
  raiz.style.scrollBehavior = "auto";
  const paso = (t) => {
    const k = Math.min(1, (t - t0) / dur);
    const e = k < 0.5 ? 4 * k * k * k : 1 - Math.pow(-2 * k + 2, 3) / 2;
    scrollTo(0, y0 + dy * e);
    if (k < 1) requestAnimationFrame(paso);
    else { raiz.style.scrollBehavior = ""; fin && fin(); }
  };
  requestAnimationFrame(paso);
}
function indiceActual() {
  // La diapositiva cuyo borde superior está más cerca de lo alto de la pantalla.
  let mejor = 0, dist = Infinity;
  paradas.forEach((el, i) => { const d = Math.abs(el.getBoundingClientRect().top); if (d < dist) { dist = d; mejor = i; } });
  return { i: mejor, exacto: dist < 8, arriba: paradas[mejor].getBoundingClientRect().top < 0 };
}
function irA(dir) {
  if (saltando) return;
  const pos = indiceActual();
  let i;
  if (dir > 0) i = pos.exacto || pos.arriba ? pos.i + 1 : pos.i;
  else i = pos.exacto || !pos.arriba ? pos.i - 1 : pos.i;
  if (i < 0 || i >= paradas.length) return;
  const el = paradas[i];
  const seguidas = el.previousElementSibling === paradas[i - 1] && dir > 0 || el.nextElementSibling === paradas[i + 1] && dir < 0;
  const y = el.offsetTop;
  if (suave === "instant") { scrollTo({ top: y, behavior: "instant" }); return; }
  if (seguidas) { saltando = true; deslizar(y, () => (saltando = false)); return; }
  saltando = true;
  telon.classList.add("on");
  setTimeout(() => {
    scrollTo({ top: y, behavior: "instant" });
    requestAnimationFrame(() => { telon.classList.remove("on"); setTimeout(() => (saltando = false), 300); });
  }, 300);
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
  document.body.classList.add("presenta");
  irA(dir);
});
// Al volver a usar el ratón o el dedo, se sale del modo presentación y vuelve la cabecera.
["wheel", "touchstart", "mousedown"].forEach((t) => addEventListener(t, () => document.body.classList.remove("presenta"), { passive: true }));
