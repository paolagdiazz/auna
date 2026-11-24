package com.auna.padres

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.auna.padres.data.Kid
import com.auna.padres.data.Passport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Lectura en voz alta con el motor de voz del móvil (en español). */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null
    private val _speaking = MutableStateFlow<String?>(null)
    /** Clave de lo que se está leyendo ahora (o null). */
    val speaking: StateFlow<String?> = _speaking

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        tts.language = Locale.forLanguageTag("es-ES")
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { if (_speaking.value == utteranceId) _speaking.value = null }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { _speaking.value = null }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { if (_speaking.value == utteranceId) _speaking.value = null }
        })
        ready = true
        pending?.let { speak(it, _speaking.value ?: "auna") }
        pending = null
    }

    fun speak(text: String, key: String = "auna") {
        if (text.isBlank()) return
        _speaking.value = key
        if (!ready) { pending = text; return }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, key)
    }

    /** Si ya se está leyendo [key], para; si no, empieza a leer. */
    fun toggle(text: String, key: String) {
        if (_speaking.value == key) stop() else speak(text, key)
    }

    fun stop() {
        pending = null
        tts.stop()
        _speaking.value = null
    }

    fun shutdown() = tts.shutdown()

    companion object {
        fun sectionText(kid: Kid, sectionId: String): String {
            val p = kid.passport
            val s = p.section(sectionId) ?: return ""
            val parts = mutableListOf(s.title + ".")
            if (sectionId == "idioma") {
                if (p.yes.isNotBlank()) parts += "Para decir sí: ${p.yes}."
                if (p.no.isNotBlank()) parts += "Para decir no: ${p.no}."
                p.signals.forEach { sg ->
                    parts += "${sg.hago}: ${sg.significa}." + if (sg.hacer.isNotBlank()) " ${sg.hacer}" else ""
                }
            }
            s.groups.filter { it.items.isNotEmpty() }.forEach { g -> parts += "${g.title}: ${g.items.joinToString(", ")}." }
            if (sectionId == "sobre") p.contacts.forEach { c -> parts += "${c.relation}: ${c.name}." }
            if (parts.size == 1) parts += "Aún está vacía."
            return parts.joinToString(" ")
        }

        fun passportText(kid: Kid): String {
            val p: Passport = kid.passport
            val intro = buildString {
                append("Hola, soy ${kid.name}.")
                if (kid.age.isNotBlank()) append(" Tengo ${kid.age}.")
                if (p.noHabla) append(" No hablo, pero me comunico.")
                if (p.highlights.isNotEmpty()) append(" Lo más importante: ${p.highlights.joinToString(". ")}.")
            }
            return (listOf(intro) + p.sections.filter { !p.isEmpty(it.id) }.map { sectionText(kid, it.id) }).joinToString(" ")
        }
    }
}
