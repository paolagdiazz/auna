// Aúna · puente de IA (Cloudflare Worker, plan gratuito).
// Guarda la clave de Groq en secreto y solo responde a personas con sesión en Aúna (token de Firebase).
//
// Variables del Worker (Cloudflare › Workers › auna-ia › Settings › Variables and Secrets):
//   GROQ_API_KEY         (Secret)  tu clave de Groq, empieza por gsk_
//   FIREBASE_PROJECT_ID  (Text)    el projectId de Firebase, p. ej. "auna-1234"
//
// Petición: POST con cabecera «Authorization: Bearer <token de Firebase>» y cuerpo
//   { system, user, schema, maxTokens }  →  respuesta { json }  o  { error } con código HTTP.

const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
const MODEL = "openai/gpt-oss-120b";
const JWKS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const MAX_INPUT_CHARS = 40000;

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Access-Control-Max-Age": "86400",
};

const reply = (status, body) => new Response(JSON.stringify(body), { status, headers: { ...CORS, "Content-Type": "application/json; charset=utf-8" } });

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS });
    if (request.method !== "POST") return reply(405, { error: "Solo POST." });
    if (!env.GROQ_API_KEY || !env.FIREBASE_PROJECT_ID) return reply(500, { error: "El puente de IA no está configurado (faltan GROQ_API_KEY o FIREBASE_PROJECT_ID)." });

    // 1. ¿Tiene sesión en Aúna?
    const token = (request.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
    try {
      await verifyFirebaseToken(token, env.FIREBASE_PROJECT_ID);
    } catch (e) {
      return reply(401, { error: "Tienes que iniciar sesión en Aúna." });
    }

    // 2. Petición válida y no demasiado grande.
    let body;
    try { body = await request.json(); } catch { return reply(400, { error: "Petición no válida." }); }
    const { system, user, schema } = body || {};
    if (typeof system !== "string" || typeof user !== "string" || typeof schema !== "object" || !schema) return reply(400, { error: "Petición no válida." });
    if (system.length + user.length > MAX_INPUT_CHARS) return reply(413, { error: "El texto es demasiado largo." });
    const maxTokens = Math.min(Number(body.maxTokens) || 2000, 3000);

    // 3. Groq con salida JSON estricta; si no la acepta, modo JSON simple con el esquema en las instrucciones.
    const messages = [{ role: "system", content: system }, { role: "user", content: user }];
    let res = await groq(env, {
      model: MODEL, messages, max_completion_tokens: maxTokens, reasoning_effort: "low",
      response_format: { type: "json_schema", json_schema: { name: "respuesta", strict: true, schema } },
    });
    if (res.status === 400) {
      res = await groq(env, {
        model: MODEL, max_completion_tokens: maxTokens,
        messages: [{ role: "system", content: `${system}\n\nResponde solo con un objeto JSON que cumpla este esquema:\n${JSON.stringify(schema)}` }, { role: "user", content: user }],
        response_format: { type: "json_object" },
      });
    }
    const data = await res.json().catch(() => ({}));
    if (res.status === 429) return reply(429, { error: "La IA gratuita ha llegado a su límite por ahora. Espera un minuto y vuelve a intentarlo." });
    if (!res.ok) return reply(502, { error: `La IA ha dado un error (${res.status}). ${data?.error?.message || ""}`.trim() });

    const choice = data?.choices?.[0];
    if (choice?.finish_reason === "length") return reply(502, { error: "La respuesta era demasiado larga. Prueba con algo más corto." });
    try {
      return reply(200, { json: JSON.parse(choice?.message?.content || "") });
    } catch {
      return reply(502, { error: "No he entendido la respuesta de la IA. Vuelve a intentarlo." });
    }
  },
};

function groq(env, payload) {
  return fetch(GROQ_URL, {
    method: "POST",
    headers: { Authorization: `Bearer ${env.GROQ_API_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

// ---------- Verificación del token de Firebase (RS256) ----------
let jwksCache = { keys: null, until: 0 };

async function getKeys() {
  if (jwksCache.keys && Date.now() < jwksCache.until) return jwksCache.keys;
  const res = await fetch(JWKS_URL);
  const { keys } = await res.json();
  const maxAge = Number((res.headers.get("Cache-Control") || "").match(/max-age=(\d+)/)?.[1] || 3600);
  jwksCache = { keys, until: Date.now() + maxAge * 1000 };
  return keys;
}

function b64urlToBytes(s) {
  const b64 = s.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(s.length / 4) * 4, "=");
  return Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
}

async function verifyFirebaseToken(token, projectId) {
  const [h, p, sig] = token.split(".");
  if (!h || !p || !sig) throw new Error("token");
  const header = JSON.parse(new TextDecoder().decode(b64urlToBytes(h)));
  const payload = JSON.parse(new TextDecoder().decode(b64urlToBytes(p)));
  if (header.alg !== "RS256") throw new Error("alg");
  const jwk = (await getKeys()).find((k) => k.kid === header.kid);
  if (!jwk) throw new Error("kid");
  const key = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const ok = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, b64urlToBytes(sig), new TextEncoder().encode(`${h}.${p}`));
  if (!ok) throw new Error("firma");
  const now = Math.floor(Date.now() / 1000);
  if (payload.aud !== projectId || payload.iss !== `https://securetoken.google.com/${projectId}`) throw new Error("proyecto");
  if (!payload.sub || payload.exp < now || payload.iat > now + 300) throw new Error("caducado");
  return payload;
}
