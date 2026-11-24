// Conexión con Firebase (cuentas y base de datos) y todas las lecturas/escrituras de la web.
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js";
import {
  getAuth, onAuthStateChanged, signInWithEmailAndPassword, createUserWithEmailAndPassword,
  sendPasswordResetEmail, updateProfile, signOut, GoogleAuthProvider, signInWithPopup,
  signInWithRedirect, getRedirectResult,
} from "https://www.gstatic.com/firebasejs/11.0.2/firebase-auth.js";
import {
  getFirestore, doc, getDoc, setDoc, addDoc, deleteDoc, collection, onSnapshot, serverTimestamp,
} from "https://www.gstatic.com/firebasejs/11.0.2/firebase-firestore.js";
import { firebaseConfig } from "../config.js";

export const configured = !!firebaseConfig;
const app = configured ? initializeApp(firebaseConfig) : null;
export const auth = app ? getAuth(app) : null;
const db = app ? getFirestore(app) : null;
if (auth) auth.languageCode = "es";

// ---------- Cuenta ----------
export function watchAuth(cb) {
  if (!auth) { cb(null); return () => {}; }
  getRedirectResult(auth).catch(() => {});
  return onAuthStateChanged(auth, cb);
}

export async function login(email, password) {
  await signInWithEmailAndPassword(auth, email.trim(), password);
}

export async function loginWithGoogle() {
  const provider = new GoogleAuthProvider();
  try {
    const res = await signInWithPopup(auth, provider);
    await ensureProfile(res.user);
  } catch (e) {
    // En algunos móviles las ventanas emergentes están bloqueadas: se usa la redirección.
    if (e?.code === "auth/popup-blocked" || e?.code === "auth/operation-not-supported-in-this-environment") {
      await signInWithRedirect(auth, provider);
    } else throw e;
  }
}

export async function register({ name, surname, email, phone, password }) {
  const cred = await createUserWithEmailAndPassword(auth, email.trim(), password);
  await updateProfile(cred.user, { displayName: `${name.trim()} ${surname.trim()}`.trim() });
  await setDoc(doc(db, "users", cred.user.uid), {
    name: name.trim(), surname: surname.trim(), email: email.trim(), phone: phone.trim(), createdAt: serverTimestamp(),
  });
}

export async function ensureProfile(user) {
  const ref = doc(db, "users", user.uid);
  const snap = await getDoc(ref);
  if (!snap.exists()) {
    const [name, ...rest] = (user.displayName || user.email || "").split(" ");
    await setDoc(ref, { name: name || "", surname: rest.join(" "), email: user.email || "", phone: "", createdAt: serverTimestamp() });
  }
}

export async function getProfile(uid) {
  const snap = await getDoc(doc(db, "users", uid));
  return snap.exists() ? snap.data() : null;
}

export const resetPassword = (email) => sendPasswordResetEmail(auth, email.trim());
export const logout = () => signOut(auth);
export const idToken = () => auth.currentUser.getIdToken();

// ---------- Niños ----------
/** Ficha pública de emergencia (la del QR). Cualquiera puede leerla. */
export async function getCard(kidId) {
  const snap = await getDoc(doc(db, "kids", kidId, "emergency", "card"));
  return snap.exists() ? snap.data() : null;
}

/** Busca un niño por el código de su tarjeta (AUN-XXXX-XXXX). */
export async function findByCode(code) {
  const clean = code.trim().toUpperCase().replace(/\s+/g, "");
  const snap = await getDoc(doc(db, "codes", clean));
  return snap.exists() ? snap.data() : null;
}

/** Pasaporte compartido (solo si la familia ha dado acceso). Devuelve null si no hay permiso. */
export async function getKid(kidId) {
  try {
    const snap = await getDoc(doc(db, "kids", kidId));
    return snap.exists() ? { id: kidId, ...snap.data() } : null;
  } catch (e) {
    if (e?.code === "permission-denied") return null;
    throw e;
  }
}

export async function getMyAccess(kidId) {
  try {
    const snap = await getDoc(doc(db, "kids", kidId, "access", auth.currentUser.uid));
    return snap.exists() ? snap.data() : null;
  } catch { return null; }
}

export async function getMyRequest(kidId) {
  try {
    const snap = await getDoc(doc(db, "kids", kidId, "requests", auth.currentUser.uid));
    return snap.exists() ? snap.data() : null;
  } catch { return null; }
}

/** Lista de niños que he pedido o tengo (en tiempo real). */
export function watchLinks(cb) {
  return onSnapshot(collection(db, "users", auth.currentUser.uid, "links"), (qs) => {
    cb(qs.docs.map((d) => ({ kidId: d.id, ...d.data() })));
  }, () => cb([]));
}

/** Envía la nota a la familia pidiendo acceso. */
export async function sendRequest(kidId, { kidName, kidAge, role, note, myName }) {
  const uid = auth.currentUser.uid;
  await setDoc(doc(db, "kids", kidId, "requests", uid), {
    name: myName, email: auth.currentUser.email || "", role, note: note.trim(), createdAt: serverTimestamp(),
  });
  await setDoc(doc(db, "users", uid, "links", kidId), { kidName, kidAge: kidAge || "", role, createdAt: serverTimestamp() });
}

export const removeLink = (kidId) => deleteDoc(doc(db, "users", auth.currentUser.uid, "links", kidId));

/** Propuesta para la familia (p. ej. «Apunta: hoy ha señalado la puerta»). */
export async function propose(kidId, { said, hago, significa, fromName, role }) {
  await addDoc(collection(db, "kids", kidId, "proposals"), {
    fromUid: auth.currentUser.uid, fromName, role, said, hago, significa, status: null, createdAt: serverTimestamp(),
  });
}

/** Aviso a la familia (urgencia o ubicación). Funciona también sin sesión, desde la tarjeta. */
export async function alertFamily(kidId, { kind, text, fromName = "", lat = null, lng = null }) {
  await addDoc(collection(db, "kids", kidId, "alerts"), { kind, text, fromName, lat, lng, createdAt: serverTimestamp(), seen: false });
}

/** Registro de consulta: la familia ve quién ha mirado el pasaporte y cuándo. */
export async function logView(kidId, name) {
  try {
    await addDoc(collection(db, "kids", kidId, "views"), { uid: auth.currentUser.uid, name, at: serverTimestamp() });
  } catch { /* no es crítico */ }
}

/** Traduce los errores de Firebase a mensajes para la persona. */
export function humanError(e) {
  const code = e?.code || "";
  const map = {
    "auth/invalid-credential": "El correo o la contraseña no son correctos.",
    "auth/wrong-password": "La contraseña no es correcta.",
    "auth/user-not-found": "No hay ninguna cuenta con ese correo.",
    "auth/invalid-email": "Ese correo no parece válido.",
    "auth/email-already-in-use": "Ya hay una cuenta con ese correo. Inicia sesión.",
    "auth/weak-password": "La contraseña debe tener al menos 8 caracteres.",
    "auth/too-many-requests": "Demasiados intentos. Espera un poco y vuelve a probar.",
    "auth/network-request-failed": "No hay conexión a internet.",
    "auth/popup-closed-by-user": "Has cerrado la ventana de Google antes de terminar.",
    "auth/unauthorized-domain": "Esta dirección no está autorizada en Firebase (Authentication › Dominios autorizados).",
    "permission-denied": "No tienes permiso para ver esto. La familia tiene que darte acceso.",
    "unavailable": "No hay conexión a internet.",
  };
  return map[code] || e?.message || "Algo ha fallado. Vuelve a intentarlo.";
}
