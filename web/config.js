// Configuración pública de Aúna. Nada de esto es secreto: la seguridad está en las reglas de Firestore
// (firebase/firestore.rules) y la clave de la IA vive solo en el puente de Cloudflare.

// Firebase › Configuración del proyecto › Tus apps › Aúna web.
export const firebaseConfig = {
  apiKey: "AIzaSyBPD5J3uoZAuqcgmf5KOWUGaRAoBWRQ7jQ",
  authDomain: "auna-9c51c.firebaseapp.com",
  projectId: "auna-9c51c",
  storageBucket: "auna-9c51c.firebasestorage.app",
  messagingSenderId: "604002674280",
  appId: "1:604002674280:web:c8edb189b5b0018caafddd",
};

// Puente de IA en Cloudflare (worker/auna-ia.js).
export const AI_URL = "https://auna-ia.paolagamberodiaz.workers.dev";
