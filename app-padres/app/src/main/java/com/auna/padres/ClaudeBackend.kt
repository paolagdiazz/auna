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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Duration

/** Claude (API de Anthropic, de pago) con el SDK oficial y salida JSON estructurada. */
class ClaudeBackend(apiKey: String) : AiBackend {
    override val label = "Claude"
    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .timeout(Duration.ofSeconds(90))
        .build()

    /** Carga por adelantado las clases del SDK (JSON y modelos) sin hacer ninguna llamada. */
    override fun warmUp() {
        JsonOutputFormat.Schema.builder().putAdditionalProperty("type", JsonValue.from(mapOf("a" to listOf(1)))).build()
        MessageCreateParams.builder().model(MODEL).maxTokens(1).addUserMessage("x").build()
    }

    override fun check() = mapErrors { client.models().retrieve(MODEL); Unit }

    override fun json(system: String, user: String, schema: Map<String, Any>, maxTokens: Long): JsonObject = mapErrors {
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
        if (stop == StopReason.REFUSAL) throw AiError("Claude no ha podido responder a esto. Prueba a decirlo de otra forma.")
        if (stop == StopReason.MAX_TOKENS) throw AiError("La respuesta era demasiado larga. Prueba con algo más corto.")
        val text = msg.content().mapNotNull { b -> b.text().map { it.text() }.orElse(null) }.joinToString("")
        runCatching { Json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw AiError("No he entendido la respuesta de Claude. Vuelve a intentarlo.") }
    }

    private fun <T> mapErrors(block: () -> T): T = try {
        block()
    } catch (e: AiError) {
        throw e
    } catch (e: UnauthorizedException) {
        throw AiError("La clave de Claude no es válida. Revísala en Ajustes.", badKey = true)
    } catch (e: PermissionDeniedException) {
        throw AiError("Esta clave de Claude no tiene permiso. Revísala en Ajustes.", badKey = true)
    } catch (e: RateLimitException) {
        throw AiError("Claude está muy ocupado ahora mismo. Espera un momento y vuelve a intentarlo.")
    } catch (e: AnthropicServiceException) {
        throw AiError("Claude ha dado un error (${e.statusCode()}). Vuelve a intentarlo.")
    } catch (e: AnthropicIoException) {
        throw AiError("No hay conexión a internet. Revisa la conexión y vuelve a intentarlo.")
    }

    companion object {
        const val MODEL = "claude-opus-5-5"
    }
}
