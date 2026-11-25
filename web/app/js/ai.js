// Chat general: la IA responde solo con lo que han contado las familias.
// Las preguntas van al puente de Cloudflare (worker/auna-ia.js), que guarda la clave de Groq en secreto.
import { AI_URL } from "../config.js";
import { idToken } from "./fb.js";

/** Texto del pasaporte compartido de un niño, para dárselo a la IA. */
export function passportText(kid, card) {
  const p = kid?.passport || {};
  const out = [];
  out.push(`${kid?.name || card?.name}${kid?.age ? ` (${kid.age})` : ""}.${p.noHabla || card?.noHabla ? " No habla, pero se comunica." : ""}`);
  if (p.highlights?.length) out.push(`Lo más importante: ${p.highlights.join(". ")}.`);
  if (p.yes || p.no) out.push(`Sí: ${p.yes || "?"}. No: ${p.no || "?"}.`);
  (p.signals || []).forEach((s) => out.push(`Señal: ${s.hago} → ${s.significa}.${s.hacer ? " Qué hacer: " + s.hacer : ""}`));
  (p.sections || []).forEach((s) => (s.groups || []).forEach((g) => {
    if (g.items?.length) out.push(`${s.title} › ${g.title}: ${g.items.join(", ")}.`);
  }));
  if (card?.crisis?.length) out.push(`Si tiene una crisis: ${card.crisis.join(". ")}.`);
  if (card?.health?.length) out.push(`Salud (visible para quien cuida): ${card.health.join(", ")}.`);
  return out.join("\n");
}

const SCHEMA = (kidIds) => ({
  type: "object",
  additionalProperties: false,
  required: ["tipo", "bloques", "fuente", "apunte"],
  properties: {
    tipo: { type: "string", enum: ["respuesta", "apunte", "no_se"] },
    bloques: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        required: ["nino", "lineas"],
        properties: {
          nino: { type: "string", description: "Nombre del niño o «Todos»." },
          lineas: { type: "array", items: { type: "string" } },
        },
      },
    },
    fuente: { type: "string", description: "Secciones del pasaporte usadas, p. ej. «Mi idioma · de su familia». Vacío si no aplica." },
    apunte: {
      type: "object",
      additionalProperties: false,
      required: ["nino", "dijo", "hago", "significa"],
      properties: {
        nino: { type: "string", enum: [...kidIds, ""] },
        dijo: { type: "string", description: "Lo que ha observado quien escribe, tal cual." },
        hago: { type: "string", description: "La conducta, en primera persona del niño (p. ej. «Señalo la puerta»)." },
        significa: { type: "string", description: "Lo que puede significar, si se sabe. Vacío si no." },
      },
    },
  },
});

/**
 * Pregunta a la IA. `kids` = [{ id, kid, card }] de los niños con acceso.
 * Devuelve { tipo, bloques, fuente, apunte }.
 */
export async function askChat(question, kids) {
  if (!AI_URL) throw new Error("Falta configurar el puente de IA (AI_URL en config.js).");
  const system = [
    "Eres el asistente de Aúna para profes, monitores y familiares de niños con discapacidad que no hablan.",
    "Responde SOLO con lo que dicen sus pasaportes (abajo). No inventes, no diagnostiques, no des consejos médicos que no estén escritos.",
    "No añadas objetos, actividades ni consejos que la familia no haya mencionado: si no está escrito, no lo digas.",
    "Agrupa la respuesta por niño, con frases cortas y prácticas: qué significa y qué hacer. Usa «Todos» si aplica a todos.",
    "Si el mensaje empieza por «Apunta», «Anota» o cuenta algo nuevo que ha observado quien escribe, usa tipo=apunte, rellena «apunte» con el id del niño y pon en «bloques» una línea diciendo que se ha enviado a su familia para que lo apruebe, y en «apunte.hago» la conducta sin la palabra «Apunta».",
    "Si los pasaportes no dicen nada, usa tipo=no_se y di que pregunten a su familia.",
    "Si hay riesgo para la salud, recuerda llamar al 112 y a la familia.",
    "En «apunte» pon cadenas vacías cuando no sea un apunte.",
    "Español de España, sencillo.",
  ].join("\n");
  const user = kids.map((k) => `[id ${k.id}]\n${passportText(k.kid, k.card)}`).join("\n\n") + `\n\nMensaje: «${question.trim()}»`;
  const token = await idToken();
  let res;
  try {
    res = await fetch(AI_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ system, user, schema: SCHEMA(kids.map((k) => k.id)), maxTokens: 2500 }),
    });
  } catch {
    throw new Error("No hay conexión a internet.");
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || `La IA ha dado un error (${res.status}).`);
  return data.json;
}
