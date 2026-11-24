package com.auna.padres

import com.auna.padres.cloud.CloudConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * IA a través del puente gratuito de Cloudflare (worker/auna-ia.js): la clave de Groq vive allí,
 * así que la familia no tiene que pegar ninguna clave. Solo funciona con la sesión de Aúna iniciada.
 */
class WorkerBackend(private val token: () -> String) : AiBackend {
    override val label = "la IA de Aúna"

    override fun check() { token() }

    override fun json(system: String, user: String, schema: Map<String, Any>, maxTokens: Long): JsonObject {
        val body = JsonObject(mapOf(
            "system" to JsonPrimitive(system),
            "user" to JsonPrimitive(user),
            "schema" to GroqBackend.toJson(schema),
            "maxTokens" to JsonPrimitive(maxTokens),
        ))
        val conn = URL(CloudConfig.AI_URL).openConnection() as HttpURLConnection
        val (code, text) = try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 90_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${token()}")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val c = conn.responseCode
            c to ((if (c in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "")
        } finally { conn.disconnect() }
        val o = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        if (code != 200) throw AiError(o.str("error").ifBlank { "La IA ha dado un error ($code)." }, badKey = code == 401)
        return runCatching { o["json"]!!.jsonObject }.getOrElse { throw AiError("No he entendido la respuesta de la IA. Vuelve a intentarlo.") }
    }
}
