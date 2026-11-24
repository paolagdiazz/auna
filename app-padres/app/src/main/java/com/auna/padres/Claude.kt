package com.auna.padres

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.auna.padres.data.Kid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration

/** Error que se puede enseñar tal cual a la familia. */
class ClaudeError(message: String, val badKey: Boolean = false) : Exception(message)

data class Organized(val findings: List<Finding>, val followUp: String?)

data class Answer(val text: String, val sources: List<String>, val known: Boolean)

/**
 * Llamadas a Claude (API de Anthropic) con la clave que la familia guarda en Ajustes.
 * Solo se envía el pasaporte del niño activo y lo que se acaba de contar o preguntar.
 */
class Claude(apiKey: String) {
    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .timeout(Duration.ofSeconds(90))
        .build()

    /** Comprueba que la clave funciona (no gasta tokens). */
    suspend fun check() = call { client.models().retrieve(MODEL); Unit }

    /** Ordena en el pasaporte lo que la familia ha contado. */
    suspend fun organize(kid: Kid, said: String, focusSectionId: String?, topic: String?): Organized = call {
        val targets = targets(kid)
        val schema = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "required" to listOf("items", "senales", "si", "no", "importante", "pregunta"),
            "properties" to mapOf(
                "items" to mapOf(
                    "type" to "array",
                    "description" to "Cosas nuevas para los apartados del pasaporte.",
                    "items" to mapOf(
                        "type" to "object",
                        "additionalProperties" to false,
                        "required" to listOf("apartado", "texto"),
                        "properties" to mapOf(
                            "apartado" to mapOf("type" to "string", "enum" to targets.keys.toList()),
                            "texto" to mapOf("type" to "string", "description" to "Entrada corta (2-8 palabras), en primera persona del niño cuando tenga sentido."),
                        ),
                    ),
                ),
                "senales" to mapOf(
                    "type" to "array",
                    "description" to "Conductas con significado: lo que hace, lo que significa y qué debe hacer quien le cuida.",
                    "items" to mapOf(
                        "type" to "object",
                        "additionalProperties" to false,
                        "required" to listOf("hago", "significa", "hacer"),
                        "properties" to mapOf(
                            "hago" to mapOf("type" to "string"),
                            "significa" to mapOf("type" to "string"),
                            "hacer" to mapOf("type" to "string", "description" to "Vacío si la familia no lo ha dicho."),
                        ),
                    ),
                ),
                "si" to mapOf("type" to "string", "description" to "Cómo dice «sí». Vacío si no se ha dicho."),
                "no" to mapOf("type" to "string", "description" to "Cómo dice «no». Vacío si no se ha dicho."),
                "importante" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "description" to "Solo avisos críticos de seguridad nuevos (alergias, atragantamiento, crisis). Normalmente vacío."),
                "pregunta" to mapOf("type" to "string", "description" to "Una pregunta corta y cálida para seguir completando el pasaporte, sobre algo que aún falta. Vacía si no hace falta."),
            ),
        )
        val system = """
            Ayudas a una familia a rellenar el «pasaporte» de su hijo o hija: una ficha para que profes, monitores y sanitarios le entiendan y le cuiden.
            La familia te cuenta cosas hablando (transcripción automática, puede tener errores) o por escrito.
            Tu trabajo: convertir lo que cuenta en entradas cortas y claras, cada una en su apartado.
            Reglas:
            - Usa solo lo que la familia ha dicho. No inventes ni supongas nada.
            - No repitas lo que ya está en el pasaporte actual.
            - Las conductas con significado («cuando se tira de la oreja es que le duele algo») van en «senales», no en «items».
            - Escribe en español de España, sencillo, sin diagnósticos que la familia no haya dicho.
            - Si lo contado no aporta nada para el pasaporte, devuelve las listas vacías.
        """.trimIndent()
        val focus = focusSectionId?.let { id -> kid.passport.section(id)?.title }
        val user = buildString {
            appendLine("Niño/a: ${kid.name}${if (kid.age.isNotBlank()) ", ${kid.age}" else ""}${if (kid.passport.noHabla) ", no habla" else ""}.")
            if (focus != null) appendLine("La familia está completando la sección «$focus».")
            if (topic != null) appendLine("Le preguntamos: «$topic»")
            appendLine()
            appendLine("Apartados disponibles:")
            targets.forEach { (key, label) -> appendLine("- $key: $label") }
            appendLine()
            appendLine("Pasaporte actual:")
            appendLine(Speaker.passportText(kid))
            appendLine()
            appendLine("Lo que ha contado la familia:")
            append("«${said.trim()}»")
        }
        val json = ask(system, user, schema, maxTokens = 4000)
        val findings = mutableListOf<Finding>()
        json["items"]?.jsonArray?.forEach { el ->
            val o = el.jsonObject
            val key = o.str("apartado")
            val text = o.str("texto").trim()
            val (sec, grp) = key.split("/", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            if (text.isNotEmpty() && key in targets) findings += Finding.Item(sec, grp, text, targets.getValue(key))
        }
        json["senales"]?.jsonArray?.forEach { el ->
            val o = el.jsonObject
            if (o.str("hago").isNotBlank() && o.str("significa").isNotBlank()) {
                findings += Finding.NewSignal(o.str("hago").trim(), o.str("significa").trim(), o.str("hacer").trim())
            }
        }
        val si = json.str("si"); val no = json.str("no")
        if (si.isNotBlank() || no.isNotBlank()) findings += Finding.YesNo(si.trim(), no.trim())
        json["importante"]?.jsonArray?.forEach { el ->
            val t = el.jsonPrimitive.content.trim()
            if (t.isNotEmpty()) findings += Finding.Highlight(t)
        }
        Organized(findings, json.str("pregunta").trim().ifEmpty { null })
    }

    /** Responde como lo haría el pasaporte, solo con lo que pone en él. */
    suspend fun answer(kid: Kid, question: String): Answer = call {
        val schema = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "required" to listOf("sabe", "respuesta", "fuentes"),
            "properties" to mapOf(
                "sabe" to mapOf("type" to "boolean", "description" to "true si el pasaporte contiene la información para responder."),
                "respuesta" to mapOf("type" to "string", "description" to "1 a 3 frases prácticas, dirigidas a quien cuida."),
                "fuentes" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "description" to "Títulos de las secciones del pasaporte usadas."),
            ),
        )
        val system = """
            Eres el pasaporte de ${kid.name}. Una profe, monitor o sanitario te pregunta algo para cuidarle.
            Responde solo con lo que dice el pasaporte. No inventes ni des consejos médicos que no estén escritos.
            Habla de ${kid.name} en tercera persona, con frases cortas y prácticas: qué significa y qué hacer.
            Si el pasaporte no dice nada sobre eso, pon sabe=false y explica que la familia aún no lo ha contado.
            Si hay riesgo para su salud, recuerda llamar al 112 y a la familia.
        """.trimIndent()
        val user = "Pasaporte:\n${Speaker.passportText(kid)}\n\nPregunta: «${question.trim()}»"
        val json = ask(system, user, schema, maxTokens = 2000)
        Answer(
            text = json.str("respuesta"),
            sources = json["fuentes"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            known = runCatching { json["sabe"]!!.jsonPrimitive.boolean }.getOrDefault(true),
        )
    }

    private fun ask(system: String, user: String, schema: Map<String, Any>, maxTokens: Long): JsonObject {
        val format = JsonOutputFormat.builder()
            .schema(JsonOutputFormat.Schema.builder().apply { schema.forEach { (k, v) -> putAdditionalProperty(k, JsonValue.from(v)) } }.build())
            .build()
        fun params(withFallback: Boolean) = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(maxTokens)
            .system(system)
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).format(format).build())
            .apply {
                if (withFallback) {
                    // Si el modelo rechaza la petición, la API la reintenta sola con otro modelo.
                    putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                }
            }
            .addUserMessage(user)
            .build()
        val msg = try {
            client.messages().create(params(withFallback = true))
        } catch (e: BadRequestException) {
            // Por si esta cuenta no admite la opción de respaldo: se repite una vez sin ella.
            client.messages().create(params(withFallback = false))
        }
        val stop = msg.stopReason().orElse(null)
        if (stop == StopReason.REFUSAL) throw ClaudeError("Claude no ha podido responder a esto. Prueba a decirlo de otra forma.")
        if (stop == StopReason.MAX_TOKENS) throw ClaudeError("La respuesta era demasiado larga. Prueba con algo más corto.")
        val text = msg.content().mapNotNull { b -> b.text().map { it.text() }.orElse(null) }.joinToString("")
        return runCatching { Json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw ClaudeError("No he entendido la respuesta de Claude. Vuelve a intentarlo.") }
    }

    private suspend fun <T> call(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: ClaudeError) {
            throw e
        } catch (e: UnauthorizedException) {
            throw ClaudeError("La clave de Claude no es válida. Revísala en Ajustes.", badKey = true)
        } catch (e: PermissionDeniedException) {
            throw ClaudeError("Esta clave de Claude no tiene permiso. Revísala en Ajustes.", badKey = true)
        } catch (e: RateLimitException) {
            throw ClaudeError("Claude está muy ocupado ahora mismo. Espera un momento y vuelve a intentarlo.")
        } catch (e: AnthropicServiceException) {
            throw ClaudeError("Claude ha dado un error (${e.statusCode()}). Vuelve a intentarlo.")
        } catch (e: AnthropicIoException) {
            throw ClaudeError("No hay conexión a internet. Revisa la conexión y vuelve a intentarlo.")
        } catch (e: Exception) {
            throw ClaudeError("No se ha podido hablar con Claude. Revisa la conexión y vuelve a intentarlo.")
        }
    }

    companion object {
        const val MODEL = "claude-opus-5-5"

        /** «sección/apartado» → nombre legible, para que Claude sepa dónde va cada cosa. */
        fun targets(kid: Kid): Map<String, String> = buildMap {
            kid.passport.sections.forEach { s ->
                s.groups.forEach { g -> put("${s.id}/${g.id}", if (s.custom) s.title else "${s.title} › ${g.title}") }
            }
        }
    }
}

private fun JsonObject.str(key: String) = runCatching { this[key]!!.jsonPrimitive.content }.getOrDefault("")
