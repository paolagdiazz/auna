// Configuración pública de Aúna. Nada de esto es secreto: la seguridad está en las reglas de Firestore
// (firebase/firestore.rules) y la clave de la IA vive solo en el puente de Cloudflare.

// Pega aquí el bloque firebaseConfig de: Firebase › Configuración del proyecto › Tus apps › Aúna web.
export const firebaseConfig = null;

// Dirección del puente de IA en Cloudflare (worker/auna-ia.js), p. ej. "https://auna-ia.tu-cuenta.workers.dev".
export const AI_URL = "";
