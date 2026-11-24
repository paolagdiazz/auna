package com.auna.padres

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/**
 * Groq (API compatible con OpenAI, con plan gratuito). Usa salida JSON estricta con esquema;
 * si el modelo no la acepta, repite una vez en modo JSON simple con el esquema en las instrucciones.
 */
class GroqBackend(private val apiKey: String) : AiBackend {
    override val label = "Groq"

    override fun check() {
        val (code, body) = request("GET", "$BASE/models", null)
        if (code != 200) throw errorFor(code, body)
    }

    override fun json(system: String, user: String, schema: Map<String, Any>, maxTokens: Long): JsonObject {
        val messages = listOf(mapOf("role" to "system", "content" to system), mapOf("role" to "user", "content" to user))
        // El plan gratuito limita los tokens por minuto: respuestas acotadas y razonamiento corto.
        val limit = minOf(maxTokens, 3000L)
        val strict = mapOf(
            "model" to MODEL,
            "messages" to messages,
            "max_completion_tokens" to limit,
            "reasoning_effort" to "low",
            "response_format" to mapOf(
                "type" to "json_schema",
                "json_schema" to mapOf("name" to "respuesta", "strict" to true, "schema" to schema),
            ),
        )
        var (code, body) = request("POST", "$BASE/chat/completions", toJson(strict).toString())
        if (code == 400) {
            // Respaldo: modo JSON simple, con el esquema descrito en las instrucciones.
            val loose = mapOf(
                "model" to MODEL,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to system + "\n\nResponde solo con un objeto JSON que cumpla este esquema:\n" + toJson(schema)),
                    mapOf("role" to "user", "content" to user),
                ),
                "max_completion_tokens" to limit,
                "response_format" to mapOf("type" to "json_object"),
            )
            val retry = request("POST", "$BASE/chat/completions", toJson(loose).toString())
            code = retry.first; body = retry.second
        }
        if (code != 200) throw errorFor(code, body)
        val choice = runCatching { Json.parseToJsonElement(body).jsonObject["choices"]!!.jsonArray[0].jsonObject }
            .getOrElse { throw AiError("No he entendido la respuesta de Groq. Vuelve a intentarlo.") }
        if (choice.str("finish_reason") == "length") throw AiError("La respuesta era demasiado larga. Prueba con algo más corto.")
        val content = runCatching { choice["message"]!!.jsonObject["content"]!!.jsonPrimitive.content }.getOrDefault("")
        return runCatching { Json.parseToJsonElement(content).jsonObject }
            .getOrElse { throw AiError("No he entendido la respuesta de Groq. Vuelve a intentarlo.") }
    }

    private fun request(method: String, url: String, body: String?): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20_000
            conn.readTimeout = 90_000
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            return code to text
        } finally {
            conn.disconnect()
        }
    }

    private fun errorFor(code: Int, body: String): AiError {
        val detail = runCatching { Json.parseToJsonElement(body).jsonObject["error"]!!.jsonObject.str("message") }.getOrDefault("")
        return when (code) {
            401 -> AiError("La clave de Groq no es válida. Revísala en Ajustes.", badKey = true)
            403 -> AiError("Esta clave de Groq no tiene permiso. Revísala en Ajustes.", badKey = true)
            429 -> AiError("Has llegado al límite gratuito de Groq por ahora. Espera un minuto (o hasta mañana) y vuelve a intentarlo.")
            in 500..599 -> AiError("Groq tiene un problema ahora mismo ($code). Vuelve a intentarlo en un rato.")
            else -> AiError("Groq ha dado un error ($code)." + if (detail.isNotBlank()) " $detail" else "")
        }
    }

    companion object {
        const val BASE = "https://api.groq.com/openai/v1"
        const val MODEL = "openai/gpt-oss-120b"

        /** Convierte mapas, listas y valores de Kotlin a JSON. */
        fun toJson(v: Any?): JsonElement = when (v) {
            null -> JsonNull
            is JsonElement -> v
            is Map<*, *> -> JsonObject(v.entries.associate { (k, value) -> k.toString() to toJson(value) })
            is List<*> -> JsonArray(v.map { toJson(it) })
            is Boolean -> JsonPrimitive(v)
            is Number -> JsonPrimitive(v)
            else -> JsonPrimitive(v.toString())
        }
    }
}
