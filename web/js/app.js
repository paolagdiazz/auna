// Aúna · web para familiares y profes. Pantallas A–K del diseño, con datos reales de Firebase.
import { html, render, useState, useEffect, useRef } from "https://cdn.jsdelivr.net/npm/htm@3.1.1/preact/standalone.module.js";
import * as fb from "./fb.js";
import { askChat } from "./ai.js";
import { Icon } from "./icons.js";

// ---------------- Utilidades ----------------
const go = (path) => { location.hash = path; };
const back = (fallback) => { if (history.length > 1) history.back(); else go(fallback); };
const initial = (s) => (s || "?").trim().charAt(0).toUpperCase();
const ROLES = ["Profe", "Monitor/a", "Logopeda", "Abuelo/a", "Tío/a", "Otro"];

function parseRoute() {
  const raw = location.hash.replace(/^#/, "") || "/";
  const [path, query = ""] = raw.split("?");
  const params = Object.fromEntries(new URLSearchParams(query));
  const parts = path.split("/").filter(Boolean);
  return { parts, params, path };
}

function fmtDate(ts) {
  const d = ts?.toDate ? ts.toDate() : ts ? new Date(ts) : null;
  return d ? `${d.getDate()}/${String(d.getMonth() + 1).padStart(2, "0")}` : "";
}

function speak(text) {
  try {
    const u = new SpeechSynthesisUtterance(text);
    u.lang = "es-ES";
    speechSynthesis.cancel();
    speechSynthesis.speak(u);
  } catch { /* sin voz en este navegador */ }
}

// ---------------- Piezas comunes ----------------
function Back({ to, label = "Volver" }) {
  return html`<button class="round" aria-label=${label} onClick=${() => back(to)}><${Icon} name="back" stroke=${2.5} /></button>`;
}

function Header({ title, subtitle, to }) {
  return html`<div class="row">
    <${Back} to=${to} />
    <div class="grow"><h1 class="t24">${title}</h1>${subtitle && html`<p class="t15 w6 muted">${subtitle}</p>`}</div>
  </div>`;
}

function Logo() {
  return html`<img src="img/logo.png" alt="Aúna" style="height:44px;width:128px;object-fit:contain;object-position:left center" />`;
}

function Nav({ current }) {
  const items = [
    ["ninos", "Mis niños", "people"],
    ["chat", "Chat general", "chat"],
    ["anadir", "Añadir niño", "plus"],
  ];
  return html`<nav class="nav" aria-label="Navegación principal">
    ${items.map(([id, label, icon]) => html`
      <a href=${"#/" + id} aria-current=${current === id ? "page" : undefined}>
        <span class="pill"><${Icon} name=${icon} color=${current === id ? "var(--green)" : "var(--muted)"} /></span>
        <span>${label}</span>
      </a>`)}
  </nav>`;
}

function Loading({ text = "Cargando…" }) {
  return html`<div class="screen center" style="justify-content:center"><div class="spinner"></div><p class="w8 muted">${text}</p></div>`;
}

function ErrorBox({ text }) {
  return text ? html`<div class="error" role="alert">${text}</div>` : null;
}

function Avatar({ name, photo, size = 44 }) {
  return html`<span class="avatar" style=${`width:${size}px;height:${size}px;border-radius:${size / 2}px;font-size:${size / 2.4}px`}>
    ${photo ? html`<img src=${photo} alt=${"Foto de " + name} />` : initial(name)}
  </span>`;
}

// ---------------- A · Tarjeta pública (quien escanea el QR) ----------------
function PublicCard({ kidId, user }) {
  const [card, setCard] = useState(undefined);
  const [view, setView] = useState("home");
  const [notice, setNotice] = useState("");
  const [locState, setLocState] = useState("");

  useEffect(() => { fb.getCard(kidId).then(setCard).catch(() => setCard(null)); }, [kidId]);
  if (card === undefined) return html`<${Loading} />`;
  if (!card) return html`<div class="screen"><${Logo} /><div class="error">Esta tarjeta no existe o la familia la ha desactivado.</div></div>`;

  const name = card.name;
  const phones = (card.contacts || []).filter((c) => c.phone);
  const call112 = async () => {
    setNotice("Avisando a su familia…");
    try { await fb.alertFamily(kidId, { kind: "112", text: `Alguien ha pulsado «Llamar al 112» en la tarjeta de ${name}.` }); setNotice("Hemos avisado a su familia."); }
    catch { setNotice("No hemos podido avisar a la familia: llámales tú."); }
    location.href = "tel:112";
  };
  const sendLocation = () => {
    if (!navigator.geolocation) { setLocState("Tu navegador no permite enviar la ubicación."); return; }
    setLocState("Buscando tu ubicación…");
    navigator.geolocation.getCurrentPosition(async (pos) => {
      try {
        await fb.alertFamily(kidId, { kind: "ubicacion", text: `Alguien está con ${name} y ha enviado su ubicación.`, lat: pos.coords.latitude, lng: pos.coords.longitude });
        setLocState("Ubicación enviada a su familia.");
      } catch { setLocState("No se ha podido enviar. Llama a su familia."); }
    }, () => setLocState("Sin permiso de ubicación. Llama a su familia."), { enableHighAccuracy: true, timeout: 15000 });
  };

  const YesNo = () => (card.yes || card.no) ? html`<div class="grid2">
      <div class="card mint"><p class="t22 w9" style="color:var(--green)">Sí</p><p class="t15 w6">${card.yes || "—"}</p></div>
      <div class="card coral"><p class="t22 w9">No</p><p class="t15 w6">${card.no || "—"}</p></div>
    </div>` : null;

  return html`<div class="screen enter">
    <div class="row"><${Logo} /><div class="spacer"></div>
      ${view !== "home" && html`<button class="btn white small" onClick=${() => setView("home")}><${Icon} name="back" size=${18} /> Atrás</button>`}
    </div>

    ${view === "home" && html`
      <div class="card" style="border:2px solid var(--border);background:var(--bg)">
        <div class="row">
          <span class="round" style="width:72px;height:72px;border-radius:36px"><img src="img/girasol.png" alt="" width="56" height="56" /></span>
          <div class="grow"><p class="t24 w9">Hola, soy ${name}</p><p class="w7 muted">${card.noHabla ? "No hablo, pero me comunico" : "Así me comunico"}</p></div>
        </div>
      </div>
      <${YesNo} />
      <p class="t18 w7 center" style="text-align:center">¿Quieres saber más de mí?</p>
      <a class="btn" href=${user ? `#/nino/${kidId}` : `#/entrar?nino=${kidId}`}>${user ? "Ver mi pasaporte" : "Iniciar sesión"}</a>
      <div class="spacer"></div>
      <p class="t22 w9" style="text-align:center">¿Alguna emergencia?</p>
      <div class="grid2">
        <button class="btn red" style="min-height:104px;flex-direction:column;border-radius:22px" onClick=${() => setView("emergencia")}><${Icon} name="warning" size=${34} color="#fff" />Emergencia</button>
        <button class="btn" style="min-height:104px;flex-direction:column;border-radius:22px" onClick=${() => setView("tutores")}><${Icon} name="people" size=${34} color="#fff" />Tutores legales</button>
      </div>`}

    ${view === "emergencia" && html`
      <button class="btn red" style="min-height:88px;font-size:26px;border-radius:22px" onClick=${call112}><${Icon} name="phone" size=${32} color="#fff" />Llamar al 112</button>
      <div class=${notice ? "ok" : "card grey"}>${notice || "Al llamar, avisamos a su familia."}</div>
      <div class="card"><p class="t13 w9 muted upper">Di esto</p><p class="t17 w8">${card.sayThis || `${name}. ${card.noHabla ? "No habla." : ""}`}</p></div>
      ${card.crisis?.length ? html`<div class="card mint2"><p class="t13 w9 upper" style="color:var(--deep)">Su familia dice</p><p class="t17 w8" style="color:var(--deep)">${card.crisis.join(". ")}.</p></div>` : null}
      ${card.food?.length ? html`<div class="card"><p class="t13 w9 muted upper">Al comer</p><p class="t17 w8">${card.food.join(", ")}.</p></div>` : null}
      ${phones.length ? html`<div class="grid2">${phones.map((c) => html`<a class="btn leaf" href=${"tel:" + c.phone.replace(/[^+\d]/g, "")}>Llamar a ${(c.relation || c.name).toLowerCase()}</a>`)}</div>` : html`<p class="muted w7">La familia aún no ha añadido teléfonos.</p>`}`}

    ${view === "tutores" && html`
      <h1>Mis tutores legales</h1>
      <div class="list">
        ${phones.length ? phones.map((c) => html`<div class="item">
          <${Avatar} name=${c.name || c.relation} />
          <div class="grow"><p class="t18 w9">${[c.relation, c.name].filter(Boolean).join(" · ")}</p><p class="t14 w7 muted">${c.phone}</p></div>
          <a class="round" style="background:var(--green);width:56px;height:56px;border-radius:28px" href=${"tel:" + c.phone.replace(/[^+\d]/g, "")} aria-label=${"Llamar a " + (c.relation || c.name)}><${Icon} name="phone" color="#fff" /></a>
        </div>`) : html`<p class="item muted w7">La familia aún no ha añadido teléfonos.</p>`}
      </div>
      <button class="btn outline" onClick=${sendLocation}><${Icon} name="pin" />Enviar mi ubicación</button>
      ${locState && html`<div class="ok">${locState}</div>`}
      <${YesNo} />`}
  </div>`;
}

// ---------------- B · Iniciar sesión ----------------
function Login({ kidId }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState("");
  const [info, setInfo] = useState("");
  const [busy, setBusy] = useState(false);
  const [kidName, setKidName] = useState("");
  useEffect(() => { if (kidId) fb.getCard(kidId).then((c) => c && setKidName(c.name)).catch(() => {}); }, [kidId]);
  const after = kidId ? `#/nino/${kidId}` : "#/ninos";

  const submit = async (e) => {
    e.preventDefault(); setErr(""); setBusy(true);
    try { await fb.login(email, password); location.hash = after; } catch (ex) { setErr(fb.humanError(ex)); }
    setBusy(false);
  };
  const google = async () => {
    setErr("");
    try { await fb.loginWithGoogle(); location.hash = after; } catch (ex) { setErr(fb.humanError(ex)); }
  };
  const forgot = async () => {
    if (!email.trim()) { setErr("Escribe tu correo y vuelve a pulsar «He olvidado mi contraseña»."); return; }
    try { await fb.resetPassword(email); setInfo("Te hemos enviado un correo para cambiar la contraseña."); setErr(""); } catch (ex) { setErr(fb.humanError(ex)); }
  };

  return html`<form class="screen enter" onSubmit=${submit}>
    ${kidId ? html`<${Header} title="Iniciar sesión" subtitle=${kidName ? `Para ver el pasaporte de ${kidName}` : ""} to=${`/c/${kidId}`} />` : html`<${Logo} /><h1>Iniciar sesión</h1>`}
    <button type="button" class="btn white" onClick=${google}>
      <svg width="20" height="20" viewBox="0 0 48 48" aria-hidden="true"><path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9.1 3.6l6.8-6.8C35.8 2.4 30.3 0 24 0 14.6 0 6.6 5.4 2.6 13.2l7.9 6.1C12.4 13.6 17.7 9.5 24 9.5z"/><path fill="#4285F4" d="M46.1 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.4c-.5 2.9-2.1 5.3-4.6 7l7.1 5.5c4.2-3.9 7.2-9.6 7.2-17z"/><path fill="#FBBC05" d="M10.5 28.7c-.5-1.4-.8-3-.8-4.7s.3-3.2.8-4.7l-7.9-6.1C1 16.4 0 20.1 0 24s1 7.6 2.6 10.8l7.9-6.1z"/><path fill="#34A853" d="M24 48c6.5 0 11.9-2.1 15.9-5.8l-7.1-5.5c-2 1.4-4.6 2.2-8.8 2.2-6.3 0-11.6-4.1-13.5-9.8l-7.9 6.1C6.6 42.6 14.6 48 24 48z"/></svg>
      Entrar con Google
    </button>
    <p class="t15 w7 muted" style="text-align:center">o con tu correo</p>
    <label class="field">Correo<input class="input" type="email" autocomplete="email" required placeholder="laura@correo.es" value=${email} onInput=${(e) => setEmail(e.target.value)} /></label>
    <label class="field">Contraseña<input class="input" type="password" autocomplete="current-password" required placeholder="••••••••" value=${password} onInput=${(e) => setPassword(e.target.value)} /></label>
    <${ErrorBox} text=${err} />
    ${info && html`<div class="ok">${info}</div>`}
    <button class="btn" disabled=${busy}>${busy ? "Entrando…" : "Entrar"}</button>
    <button type="button" class="btn ghost" onClick=${forgot}>He olvidado mi contraseña</button>
    <div class="card mint row"><p class="grow w8">¿Es tu primera vez?</p><a class="btn small" href=${kidId ? `#/registro?nino=${kidId}` : "#/registro"}>Crear cuenta</a></div>
  </form>`;
}

// ---------------- C · Crear cuenta (paso 1) ----------------
function Register({ kidId }) {
  const [f, setF] = useState({ name: "", surname: "", email: "", phone: "", password: "" });
  const [ok, setOk] = useState(false);
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault(); setErr("");
    if (f.password.length < 8) { setErr("La contraseña debe tener al menos 8 caracteres."); return; }
    if (!ok) { setErr("Tienes que aceptar el aviso de privacidad."); return; }
    setBusy(true);
    try { await fb.register(f); location.hash = kidId ? `#/nota/${kidId}` : "#/anadir"; } catch (ex) { setErr(fb.humanError(ex)); }
    setBusy(false);
  };

  return html`<form class="screen enter" onSubmit=${submit}>
    <${Header} title="Crear cuenta" subtitle="Tus datos" to=${kidId ? `/entrar?nino=${kidId}` : "/entrar"} />
    <div class="steps" aria-label="Paso 1 de 2"><span class="on"></span><span></span></div>
    <p class="t14 w8 muted">Paso 1 de 2</p>
    <div class="grid2">
      <label class="field">Nombre<input class="input" required autocomplete="given-name" placeholder="Laura" value=${f.name} onInput=${set("name")} /></label>
      <label class="field">Apellidos<input class="input" autocomplete="family-name" placeholder="Gil Ruiz" value=${f.surname} onInput=${set("surname")} /></label>
    </div>
    <label class="field">Correo<input class="input" type="email" required autocomplete="email" placeholder="laura@correo.es" value=${f.email} onInput=${set("email")} /></label>
    <label class="field">Móvil<input class="input" type="tel" autocomplete="tel" placeholder="600 000 000" value=${f.phone} onInput=${set("phone")} /></label>
    <label class="field">Contraseña<input class="input" type="password" required autocomplete="new-password" placeholder="Mínimo 8 caracteres" value=${f.password} onInput=${set("password")} /></label>
    <label class="check"><input type="checkbox" checked=${ok} onChange=${(e) => setOk(e.target.checked)} />Acepto el aviso de privacidad. Solo veré lo que la familia me permita.</label>
    <${ErrorBox} text=${err} />
    <button class="btn" disabled=${busy}>${busy ? "Creando la cuenta…" : "Siguiente"}</button>
    <p class="t14 w7 muted">Sin fotos de DNI ni selfies: la familia decide si te da acceso.</p>
  </form>`;
}

// ---------------- F · Nota para la familia (paso 2) y espera ----------------
function NoteForm({ kidId, kidName, kidAge, profile, onSent, onChange }) {
  const myName = profile?.name || fb.auth.currentUser?.displayName || "";
  const [role, setRole] = useState("Profe");
  const [note, setNote] = useState(`Hola, soy ${myName}. Me gustaría poder consultar el pasaporte de ${kidName} para ayudarle.`);
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const send = async () => {
    setErr(""); setBusy(true);
    try {
      await fb.sendRequest(kidId, { kidName, kidAge, role, note, myName: [profile?.name, profile?.surname].filter(Boolean).join(" ") || myName });
      onSent(role);
    } catch (ex) { setErr(fb.humanError(ex)); }
    setBusy(false);
  };
  return html`
    <div class="card row">
      <${Avatar} name=${kidName} size=${52} />
      <div class="grow"><p class="t18 w9">Para la familia de ${kidName}</p><p class="t15 w6 muted">Ellos deciden si te dan acceso</p></div>
      ${onChange && html`<button class="btn white small" onClick=${onChange}>Cambiar</button>`}
    </div>
    <p class="t15 w8 muted">¿Quién eres para ${kidName}?</p>
    <div class="wrap">${ROLES.map((r) => html`<button class="choice" aria-pressed=${role === r} onClick=${() => setRole(r)}>${r}</button>`)}</div>
    <label class="field">Tu nota<textarea class="input" value=${note} onInput=${(e) => setNote(e.target.value)} maxlength="1500"></textarea></label>
    <${ErrorBox} text=${err} />
    <button class="btn" onClick=${send} disabled=${busy || !note.trim()}>${busy ? "Enviando…" : "Enviar a la familia"}</button>`;
}

function Note({ kidId, profile }) {
  const [card, setCard] = useState(undefined);
  const [sentRole, setSentRole] = useState(null);
  useEffect(() => { fb.getCard(kidId).then(setCard).catch(() => setCard(null)); }, [kidId]);
  if (card === undefined) return html`<${Loading} />`;
  if (!card) return html`<div class="screen"><${Header} title="Crear cuenta" to="/anadir" /><div class="error">No encontramos a este niño.</div></div>`;
  return html`<div class="screen enter">
    <${Header} title="Crear cuenta" subtitle="Nota para la familia" to="/ninos" />
    <div class="steps"><span class="on"></span><span class="on"></span></div>
    ${!sentRole ? html`<p class="t14 w8 muted">Paso 2 de 2</p>
      <${NoteForm} kidId=${kidId} kidName=${card.name} kidAge=${card.age} profile=${profile} onSent=${setSentRole} />`
      : html`<${Waiting} role=${sentRole} kidName=${card.name} />`}
  </div>`;
}

function Waiting({ role, kidName }) {
  return html`
    <div class="card center col" style="padding:28px 16px;gap:12px">
      <img src="img/girasol.png" alt="" width="88" height="88" />
      <h2>Esperando a la familia</h2>
      <p class="muted w7">Aparecerá ${kidName} en «Mis niños» en cuanto te acepten.</p>
    </div>
    <div class="list">
      ${[["Cuenta creada", true], [`Nota enviada · ${role}`, true], ["La familia lo revisa", false]].map(([t, done]) => html`
        <div class="item"><span class="avatar" style=${done ? "" : "background:var(--line)"}>${done ? html`<${Icon} name="check" color="var(--green)" stroke=${2.8} />` : "…"}</span><p class="w8 grow">${t}</p></div>`)}
    </div>
    <a class="btn outline" href="#/ninos">Ir a mis niños</a>`;
}

// ---------------- G · Mis niños ----------------
function Ninos({ profile }) {
  const [links, setLinks] = useState(null);
  const [kids, setKids] = useState({});
  useEffect(() => fb.watchLinks(setLinks), []);
  useEffect(() => {
    if (!links) return;
    links.forEach(async (l) => {
      const kid = await fb.getKid(l.kidId);
      const access = kid ? await fb.getMyAccess(l.kidId) : null;
      const request = kid ? null : await fb.getMyRequest(l.kidId);
      setKids((k) => ({ ...k, [l.kidId]: { kid, access, request, loaded: true } }));
    });
  }, [links]);

  const name = profile?.name || fb.auth.currentUser?.displayName?.split(" ")[0] || "";
  return html`<div class="screen with-nav enter">
    <div class="row"><${Logo} /><div class="spacer"></div>
      <button class="round" aria-label="Cerrar sesión" onClick=${() => fb.logout()}><${Icon} name="logout" /></button>
    </div>
    <h1 style="font-size:30px">Hola${name ? ", " + name : ""}</h1>
    <a class="card row" style="background:var(--leaf)" href="#/chat">
      <span class="round" style="width:56px;height:56px;border-radius:28px"><${Icon} name="chat" color="var(--green)" size=${26} /></span>
      <div class="grow"><p class="t22 w9">Chat general</p><p class="w7" style="color:var(--deep)">Pregunta por tus niños</p></div>
    </a>
    <p class="t14 w8 muted upper">Tus niños</p>
    ${links === null ? html`<div class="spinner"></div>` : links.length === 0 ? html`
      <div class="card center col" style="gap:10px;padding:24px 16px">
        <img src="img/girasol.png" alt="" width="72" height="72" />
        <p class="w9 t18">Aún no tienes niños</p>
        <p class="muted w7">Escanea su tarjeta QR o escribe el código de su tarjeta.</p>
        <a class="btn small" href="#/anadir">Añadir niño</a>
      </div>` : links.map((l) => {
        const st = kids[l.kidId];
        const kid = st?.kid;
        if (kid) {
          const until = st.access?.until ? `acceso hasta el ${fmtDate(st.access.until)}` : "acceso siempre";
          const chips = (kid.passport?.highlights || []).slice(0, 3);
          return html`<a class="card col" href=${`#/nino/${l.kidId}`} style="gap:10px">
            <div class="row"><${Avatar} name=${kid.name} photo=${kid.photo} size=${52} />
              <div class="grow"><p class="t22 w9">${kid.name}</p><p class="t14 w7 muted">${[kid.age, until].filter(Boolean).join(" · ")}</p></div>
              <${Icon} name="next" color="var(--green)" stroke=${2.5} />
            </div>
            ${chips.length ? html`<div class="wrap">${chips.map((c) => html`<span class="chip">${c}</span>`)}</div>` : null}
          </a>`;
        }
        return html`<div class="card row" style="opacity:${st?.loaded ? 1 : 0.6}">
          <${Avatar} name=${l.kidName} size=${52} />
          <div class="grow"><p class="t22 w9">${l.kidName}</p><p class="t14 w7 muted">${l.kidAge || ""}</p>
            <p class="t14 w8" style="color:var(--forest)">${!st?.loaded ? "Comprobando…" : st.request ? "Esperando a su familia" : "La familia no ha dado acceso"}</p></div>
          ${st?.loaded && !st.request && html`<button class="btn white small" onClick=${() => fb.removeLink(l.kidId)}>Quitar</button>`}
        </div>`;
      })}
    <p class="t14 w7 muted" style="text-align:center">Las familias ven quién consulta y cuándo.</p>
    <${Nav} current="ninos" />
  </div>`;
}

/** Niños con acceso (pasaporte + ficha), para la ficha y el chat. */
function useMyKids() {
  const [state, setState] = useState(null);
  useEffect(() => fb.watchLinks(async (links) => {
    const out = [];
    for (const l of links) {
      const kid = await fb.getKid(l.kidId);
      if (kid) out.push({ id: l.kidId, kid, card: await fb.getCard(l.kidId).catch(() => null), role: l.role });
    }
    setState(out);
  }), []);
  return state;
}

// ---------------- H · Chat general ----------------
const KID_COLORS = [["var(--mint)", "var(--green)"], ["var(--grey)", "var(--ink)"], ["var(--mint2)", "var(--deep)"]];

function Chat({ profile }) {
  const kids = useMyKids();
  const [log, setLog] = useState(() => { try { return JSON.parse(sessionStorage.getItem("auna-chat") || "[]"); } catch { return []; } });
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const end = useRef(null);
  useEffect(() => { try { sessionStorage.setItem("auna-chat", JSON.stringify(log.slice(-40))); } catch {} end.current?.scrollIntoView({ behavior: "smooth" }); }, [log]);

  const myName = [profile?.name, profile?.surname].filter(Boolean).join(" ") || fb.auth.currentUser?.displayName || "";
  const colorOf = (name) => {
    if (name === "Todos") return ["var(--green)", "#fff"];
    const i = (kids || []).findIndex((k) => k.kid.name === name);
    return KID_COLORS[(i < 0 ? 0 : i) % KID_COLORS.length];
  };

  const ask = async (q) => {
    const text = q.trim();
    if (!text || busy) return;
    setDraft(""); setBusy(true);
    setLog((l) => [...l, { t: "me", text }]);
    try {
      if (!kids?.length) throw new Error("Aún no tienes acceso a ningún niño. Añade uno y espera a que su familia te acepte.");
      const r = await askChat(text, kids);
      let extra = "";
      if (r.tipo === "apunte" && r.apunte?.nino) {
        const k = kids.find((x) => x.id === r.apunte.nino);
        try {
          await fb.propose(r.apunte.nino, { said: r.apunte.dijo || text, hago: r.apunte.hago || text, significa: r.apunte.significa || "", fromName: myName, role: k?.role || "" });
        } catch { extra = "No tienes permiso para proponer cambios a esta familia: pídeselo a ella."; }
      }
      setLog((l) => [...l, { t: "bot", blocks: r.bloques || [], src: r.fuente || "", noKnow: r.tipo === "no_se", extra }]);
    } catch (e) {
      setLog((l) => [...l, { t: "bot", blocks: [{ nino: "Todos", lineas: [e.message] }], src: "", error: true }]);
    }
    setBusy(false);
  };

  const urgent = async () => {
    setLog((l) => [...l, { t: "bot", urgent: true, blocks: [{ nino: "Todos", lineas: ["Urgencia: llama al 112.", "Avisamos a las familias."] }], src: "Respuesta fija, sin IA" }]);
    for (const k of kids || []) { fb.alertFamily(k.id, { kind: "urgencia", text: `${myName} ha pulsado «Urgencia» en el chat.`, fromName: myName }).catch(() => {}); }
  };

  const names = (kids || []).map((k) => k.kid.name);
  const chips = names.length ? [
    "Mañana vamos a la playa",
    `¿Qué hago si ${names[0]} se pone nervioso?`,
    "¿Quién tiene alguna alergia o crisis?",
    `Apunta: ${names[0]} hoy ha señalado la puerta para salir`,
  ] : [];

  return html`<div class="screen with-nav enter" style="gap:12px">
    <div class="row">
      <div class="grow"><h1>Chat general</h1><p class="t15 w7 muted">${kids === null ? "…" : names.join(" y ") || "Sin niños todavía"}</p></div>
      <button class="btn red small" onClick=${urgent}><${Icon} name="warning" color="#fff" size=${20} />Urgencia</button>
    </div>
    <p class="info">Respondo solo con lo que han contado sus familias.</p>
    ${log.map((m) => m.t === "me" ? html`<p class="bubble-me">${m.text}</p>` : html`
      <div class=${"bubble-bot" + (m.urgent || m.error ? " urgent" : "")}>
        ${m.blocks.map((b) => { const [bg, fg] = colorOf(b.nino); return html`<div class="col">
          <span class="tag" style=${`align-self:flex-start;background:${bg};color:${fg}`}>${b.nino}</span>
          ${b.lineas.map((ln) => html`<p class="t17 w7">${ln}</p>`)}</div>`; })}
        ${m.extra && html`<p class="t15 w8" style="color:var(--coral)">${m.extra}</p>`}
        ${m.src && html`<p class="t13 w7 muted row" style="gap:6px"><${Icon} name="book" size=${14} />${m.src}</p>`}
        ${m.urgent && html`<a class="btn red small" href="tel:112">Llamar al 112</a>`}
        ${m.noKnow && html`<a class="btn white small" href="#/ninos">Ver mis niños</a>`}
      </div>`)}
    ${busy && html`<div class="bubble-bot row"><div class="spinner"></div><p class="w7 muted">Buscando en sus pasaportes…</p></div>`}
    <div ref=${end}></div>
    <div class="spacer"></div>
    ${!log.length && chips.length ? html`<div class="col">${chips.map((c) => html`<button class="btn white" style="justify-content:flex-start;min-height:48px;font-size:16px" onClick=${() => ask(c)}>${c}</button>`)}</div>` : null}
    <form class="row" onSubmit=${(e) => { e.preventDefault(); ask(draft); }}>
      <input class="input" style="border-radius:26px" placeholder="¿Qué está pasando?" aria-label="Escribe tu pregunta" value=${draft} onInput=${(e) => setDraft(e.target.value)} />
      <button class="round" style="background:var(--green);width:52px;height:52px;border-radius:26px" aria-label="Enviar" disabled=${busy}><${Icon} name="send" color="#fff" /></button>
    </form>
    <${Nav} current="chat" />
  </div>`;
}

// ---------------- I · Lo esencial de un niño ----------------
function Ficha({ kidId, profile }) {
  const [data, setData] = useState(undefined);
  useEffect(() => {
    (async () => {
      const kid = await fb.getKid(kidId);
      if (!kid) { setData(null); return; }
      const card = await fb.getCard(kidId).catch(() => null);
      setData({ kid, card });
      fb.logView(kidId, [profile?.name, profile?.surname].filter(Boolean).join(" ") || fb.auth.currentUser?.email || "");
    })().catch(() => setData(null));
  }, [kidId]);
  if (data === undefined) return html`<${Loading} />`;
  if (data === null) return html`<div class="screen enter"><${Header} title="Pasaporte" to="/ninos" />
    <div class="error">Aún no tienes acceso. La familia tiene que aceptar tu nota.</div>
    <a class="btn" href=${`#/nota/${kidId}`}>Enviar una nota a la familia</a></div>`;

  const { kid, card } = data;
  const p = kid.passport || {};
  const group = (sid, gid) => (p.sections || []).find((s) => s.id === sid)?.groups?.find((g) => g.id === gid)?.items || [];
  const calma = group("calma", "calma"), altera = group("calma", "altera");
  const important = [p.noHabla || kid.noHabla ? "No hablo, pero me comunico." : null, ...(p.highlights || [])].filter(Boolean);
  const others = (p.sections || []).filter((s) => !["calma", "idioma"].includes(s.id) && (s.groups || []).some((g) => g.items?.length));

  return html`<div class="screen enter">
    <div class="row"><${Back} to="/ninos" /><${Avatar} name=${kid.name} photo=${kid.photo} size=${48} />
      <div class="grow"><h1>${kid.name}</h1><p class="t15 w7 muted">Lo esencial</p></div>
      <button class="round" style="background:var(--mint)" aria-label="Escuchar" onClick=${() => speak(important.join(" "))}><${Icon} name="volume" color="var(--green)" /></button>
    </div>
    ${important.length ? html`<div class="card mint2 col" style="gap:8px">
      <p class="row t15 w9" style="color:var(--deep);gap:8px"><${Icon} name="star" color="var(--dot)" size=${20} />Lo más importante</p>
      ${important.map((t) => html`<p class="t17 w8">${t}</p>`)}</div>` : null}
    ${(p.yes || p.no) && html`<div class="grid2">
      <div class="card mint"><p class="t22 w9" style="color:var(--green)">Sí</p><p class="t15 w6">${p.yes || "—"}</p></div>
      <div class="card coral"><p class="t22 w9">No</p><p class="t15 w6">${p.no || "—"}</p></div></div>`}
    ${p.signals?.length ? html`<p class="t14 w8 muted upper">Mi idioma</p>
      ${p.signals.map((s) => html`<div class="card col"><p class="t17 w8">${s.hago}</p><span class="chip warm" style="align-self:flex-start">${s.significa}</span>${s.hacer && html`<p class="t15 w6 muted">✓ ${s.hacer}</p>`}</div>`)}` : null}
    ${(calma.length || altera.length) ? html`<div class="grid2">
      <div class="card mint"><p class="t14 w9 upper" style="color:var(--green)">Me calma</p><p class="t15 w7">${calma.join(", ") || "—"}</p></div>
      <div class="card mint2"><p class="t14 w9 upper" style="color:var(--deep)">Me altera</p><p class="t15 w7">${altera.join(", ") || "—"}</p></div></div>` : null}
    ${card?.crisis?.length ? html`<div class="card coral"><p class="t14 w9 upper">Si tengo una crisis</p><p class="t17 w8">${card.crisis.join(". ")}.</p></div>` : null}
    ${others.map((s) => html`<div class="card col"><p class="t14 w9 muted upper">${s.title}</p>
      ${s.groups.filter((g) => g.items?.length).map((g) => html`<div class="col"><p class="t13 w8 muted">${g.title}</p><div class="wrap">${g.items.map((i) => html`<span class=${"chip" + (g.warm ? " warm" : "")}>${i}</span>`)}</div></div>`)}</div>`)}
    <div class="grid2">
      <a class="btn soft" href=${`#/nino/${kidId}/pictos`}><${Icon} name="hand" />Pictos</a>
      <a class="btn" href="#/chat"><${Icon} name="chat" color="#fff" />Preguntar</a>
    </div>
    <p class="t14 w7 muted" style="text-align:center">Ves lo que su familia comparte contigo.</p>
  </div>`;
}

// ---------------- J · Pictos ----------------
// Palabras para buscar pictogramas de ARASAAC según lo que el niño puede querer decir.
const PICTO_WORDS = [
  ["ruido", "Mucho ruido"], ["dolor", "Me duele"], ["hambre", "Tengo hambre"], ["sed", "Tengo sed"],
  ["salir", "Quiero salir"], ["baño", "Quiero ir al baño"], ["dormir", "Tengo sueño"], ["nervioso", "Estoy nervioso"],
];

function pictoList(kid) {
  const text = (kid.passport?.signals || []).map((s) => (s.significa + " " + s.hago).toLowerCase()).join(" ");
  const fromSignals = PICTO_WORDS.filter(([w]) => text.includes(w.slice(0, 4)));
  const base = PICTO_WORDS.slice(0, 5);
  const seen = new Set();
  return [...fromSignals, ...base].filter(([w]) => !seen.has(w) && seen.add(w)).slice(0, 6);
}

function PictoImg({ word }) {
  const [src, setSrc] = useState(null);
  useEffect(() => {
    const key = "picto-" + word;
    const cached = sessionStorage.getItem(key);
    if (cached) { setSrc(cached); return; }
    fetch(`https://api.arasaac.org/v1/pictograms/es/search/${encodeURIComponent(word)}`)
      .then((r) => r.json()).then((arr) => {
        const id = arr?.[0]?._id;
        if (id) { const url = `https://static.arasaac.org/pictograms/${id}/${id}_300.png`; sessionStorage.setItem(key, url); setSrc(url); }
      }).catch(() => {});
  }, [word]);
  return src ? html`<img src=${src} alt="" />` : html`<span class="avatar" style="width:96px;height:96px;border-radius:20px"><${Icon} name="hand" size=${40} color="var(--green)" /></span>`;
}

function Pictos({ kidId }) {
  const [kid, setKid] = useState(undefined);
  const [picked, setPicked] = useState("");
  useEffect(() => { fb.getKid(kidId).then(setKid).catch(() => setKid(null)); }, [kidId]);
  if (kid === undefined) return html`<${Loading} />`;
  if (!kid) return html`<div class="screen"><${Header} title="Pictos" to="/ninos" /><div class="error">Aún no tienes acceso.</div></div>`;
  const pick = (label) => { setPicked(label); speak(label); };
  return html`<div class="screen enter">
    <${Header} title=${`¿Qué quieres, ${kid.name}?`} subtitle="Enséñale la pantalla" to=${`/nino/${kidId}`} />
    <div class="pictos">${pictoList(kid).map(([w, label]) => html`
      <button class="picto" aria-pressed=${picked === label} onClick=${() => pick(label)}><${PictoImg} word=${w} />${label}</button>`)}</div>
    <div class="grid2">
      <button class="btn" style="min-height:88px;font-size:28px;border-radius:22px" onClick=${() => pick("Sí")}>SÍ</button>
      <button class="btn red" style="min-height:88px;font-size:28px;border-radius:22px" onClick=${() => pick("No")}>NO</button>
    </div>
    ${picked ? html`<div class="card mint row"><p class="grow t18 w9">${kid.name}: «${picked}»</p><a class="btn small" href="#/chat">¿Qué hago?</a></div>`
      : html`<p class="t15 w7 muted" style="text-align:center">Toca lo que señale. Se dice en voz alta.</p>`}
    <p class="t13 w6 muted" style="text-align:center">Pictogramas de ARASAAC (Sergio Palao, Gobierno de Aragón), licencia CC BY-NC-SA.</p>
  </div>`;
}

// ---------------- K · Añadir niño ----------------
function Anadir({ profile }) {
  const [step, setStep] = useState("find");
  const [code, setCode] = useState("");
  const [found, setFound] = useState(null);
  const [err, setErr] = useState("");
  const [role, setRole] = useState("");
  const [scanning, setScanning] = useState(false);
  const video = useRef(null);

  const openKid = async (kidId) => {
    const card = await fb.getCard(kidId);
    if (!card) throw new Error("No encontramos a ese niño.");
    setFound({ kidId, name: card.name, age: card.age });
    setStep("note");
  };
  const search = async () => {
    setErr("");
    try {
      const r = await fb.findByCode(code);
      if (!r) { setErr("No hay ninguna tarjeta con ese código. Revísalo."); return; }
      await openKid(r.kidId);
    } catch (e) { setErr(fb.humanError(e)); }
  };

  const scan = async () => {
    setErr("");
    if (!("BarcodeDetector" in window)) { setErr("Este navegador no puede leer QR. Abre la cámara del móvil, escanea la tarjeta y pulsa «Iniciar sesión», o escribe el código."); return; }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
      setScanning(true);
      setTimeout(async () => {
        const v = video.current; if (!v) return;
        v.srcObject = stream; await v.play();
        const det = new window.BarcodeDetector({ formats: ["qr_code"] });
        const stop = () => { stream.getTracks().forEach((t) => t.stop()); setScanning(false); };
        const tick = async () => {
          if (!v.srcObject) return;
          try {
            const codes = await det.detect(v);
            const raw = codes?.[0]?.rawValue || "";
            const m = raw.match(/#\/c\/([A-Za-z0-9_-]+)/);
            if (m) { stop(); try { await openKid(m[1]); } catch (e) { setErr(e.message); } return; }
            if (raw) { stop(); setErr("Ese QR no es de una tarjeta de Aúna con enlace. Escribe el código de la tarjeta."); return; }
          } catch { /* sigue intentando */ }
          requestAnimationFrame(tick);
        };
        tick();
      }, 50);
    } catch { setErr("Sin permiso de cámara. Escribe el código de la tarjeta."); }
  };

  return html`<div class="screen with-nav enter">
    <h1>Añadir niño</h1>
    ${step === "find" && html`
      ${scanning ? html`<video ref=${video} class="scan" playsinline muted></video>` : html`
      <button class="btn" style="min-height:120px;flex-direction:column;border-radius:22px" onClick=${scan}><${Icon} name="scan" size=${40} color="#fff" />Escanear su QR</button>`}
      <p class="t15 w7 muted" style="text-align:center">o con el código de su tarjeta</p>
      <div class="row">
        <label class="field grow">Código<input class="input" placeholder="AUN-XXXX-XXXX" autocapitalize="characters" value=${code} onInput=${(e) => setCode(e.target.value)} /></label>
        <button class="btn small" style="align-self:flex-end;min-height:52px" onClick=${search} disabled=${!code.trim()}>Buscar</button>
      </div>
      <${ErrorBox} text=${err} />
      <p class="t14 w7 muted">Su familia recibirá tu nota y decidirá si te da acceso.</p>`}
    ${step === "note" && found && html`<${NoteForm} kidId=${found.kidId} kidName=${found.name} kidAge=${found.age} profile=${profile}
        onSent=${(r) => { setRole(r); setStep("sent"); }} onChange=${() => { setFound(null); setStep("find"); }} />`}
    ${step === "sent" && found && html`<div class="card center col" style="padding:28px 16px;gap:12px">
        <span class="avatar" style="width:88px;height:88px;border-radius:44px"><${Icon} name="check" size=${44} color="var(--green)" stroke=${2.8} /></span>
        <h2>Nota enviada</h2><p class="muted w7">${found.name} aparecerá en tu lista cuando su familia te acepte (${role}).</p></div>
      <a class="btn" href="#/ninos">Volver a mis niños</a>`}
    <${Nav} current="anadir" />
  </div>`;
}

// ---------------- Sin configurar ----------------
function Setup() {
  return html`<div class="screen"><${Logo} /><h1>Falta configurar Firebase</h1>
    <p class="w7 muted">Pega el bloque firebaseConfig en <b>web/config.js</b> (Firebase › Configuración del proyecto › Tus apps).</p></div>`;
}

// ---------------- App y rutas ----------------
function App() {
  const [route, setRoute] = useState(parseRoute());
  const [user, setUser] = useState(undefined);
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    const onHash = () => { setRoute(parseRoute()); window.scrollTo(0, 0); };
    addEventListener("hashchange", onHash);
    return () => removeEventListener("hashchange", onHash);
  }, []);
  useEffect(() => fb.watchAuth(async (u) => {
    setUser(u);
    if (u) { await fb.ensureProfile(u).catch(() => {}); setProfile(await fb.getProfile(u.uid).catch(() => null)); }
    else setProfile(null);
  }), []);

  if (!fb.configured) return html`<${Setup} />`;
  const { parts, params } = route;
  const [a, b, c] = parts;

  // Pública: la tarjeta del QR se ve sin iniciar sesión.
  if (a === "c" && b) return html`<${PublicCard} key=${b} kidId=${b} user=${user} />`;
  if (user === undefined) return html`<${Loading} />`;

  if (!user) {
    if (a === "registro") return html`<${Register} kidId=${params.nino} />`;
    return html`<${Login} kidId=${params.nino || (a === "nino" ? b : undefined)} />`;
  }
  if (a === "entrar" || a === "registro") { go(params.nino ? `/nino/${params.nino}` : "/ninos"); return null; }
  if (a === "nota" && b) return html`<${Note} key=${b} kidId=${b} profile=${profile} />`;
  if (a === "nino" && b && c === "pictos") return html`<${Pictos} key=${b} kidId=${b} />`;
  if (a === "nino" && b) return html`<${Ficha} key=${b} kidId=${b} profile=${profile} />`;
  if (a === "chat") return html`<${Chat} profile=${profile} />`;
  if (a === "anadir") return html`<${Anadir} profile=${profile} />`;
  return html`<${Ninos} profile=${profile} />`;
}

render(html`<${App} />`, document.getElementById("app"));
