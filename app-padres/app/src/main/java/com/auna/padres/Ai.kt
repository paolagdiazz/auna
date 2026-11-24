package com.auna.padres

import com.auna.padres.data.Kid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Error que se puede enseñar tal cual a la familia. */
class AiError(message: String, val badKey: Boolean = false) : Exception(message)

data class Organized(val findings: List<Finding>, val followUp: String?)

data class Answer(val text: String, val sources: List<String>, val known: Boolean)

/** Un proveedor de IA: recibe instrucciones, texto y un esquema JSON, y devuelve el JSON. */
interface AiBackend {
    /** Nombre para enseñar a la familia («Groq», «Claude»). */
    val label: String
    /** Comprueba que la clave funciona (sin gastar tokens). Lanza [AiError] si no. */
    fun check()
    /** Llamada bloqueante; lanza [AiError] con un mensaje claro si algo falla. */
    fun json(system: String, user: String, schema: Map<String, Any>, maxTokens: Long): JsonObject
    fun warmUp() {}
}

/**
 * La IA de la app. Elige el proveedor según la clave que la familia pega en Ajustes:
 * las de Groq empiezan por «gsk_» (gratis), las de Claude por «sk-ant-».
 * Solo se envía el pasaporte del niño activo y lo que se acaba de contar o preguntar.
 */
class Ai(apiKey: String) {
    val backend: AiBackend = if (isGroqKey(apiKey)) GroqBackend(apiKey) else ClaudeBackend(apiKey)

    fun warmUp() = backend.warmUp()

    suspend fun check() = call { backend.check() }

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
        val json = backend.json(system, user, schema, maxTokens = 4000)
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
        val json = backend.json(system, user, schema, maxTokens = 2000)
        Answer(
            text = json.str("respuesta"),
            sources = json["fuentes"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            known = runCatching { json["sabe"]!!.jsonPrimitive.boolean }.getOrDefault(true),
        )
    }

    private suspend fun <T> call(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: AiError) {
            throw e
        } catch (e: java.io.IOException) {
            throw AiError("No hay conexión a internet. Revisa la conexión y vuelve a intentarlo.")
        } catch (e: Exception) {
            throw AiError("No se ha podido hablar con ${backend.label}. Vuelve a intentarlo.")
        }
    }

    companion object {
        fun isGroqKey(key: String) = key.trim().startsWith("gsk_")

        fun providerName(key: String) = if (isGroqKey(key)) "Groq" else "Claude"

        /** «sección/apartado» → nombre legible, para que la IA sepa dónde va cada cosa. */
        fun targets(kid: Kid): Map<String, String> = buildMap {
            kid.passport.sections.forEach { s ->
                s.groups.forEach { g -> put("${s.id}/${g.id}", if (s.custom) s.title else "${s.title} › ${g.title}") }
            }
        }
    }
}

internal fun JsonObject.str(key: String) = runCatching { this[key]!!.jsonPrimitive.content }.getOrDefault("")
