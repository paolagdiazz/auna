package com.auna.padres

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pasa la voz a texto con el reconocimiento de voz del móvil (español de España).
 * En modo continuo vuelve a escuchar tras cada pausa hasta que se llama a [stop].
 */
class VoiceInput(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    private var continuous = false
    private var active = false
    /** Errores seguidos sin oír nada; evita reintentar sin fin. */
    private var strikes = 0
    private var askedDownload = false

    private val _text = MutableStateFlow("")
    /** Todo lo reconocido hasta ahora (frases ya cerradas). */
    val text: StateFlow<String> = _text
    private val _partial = MutableStateFlow("")
    /** Lo que se está reconociendo en este momento. */
    val partial: StateFlow<String> = _partial
    private val _level = MutableStateFlow(0f)
    /** Volumen de la voz, 0..1, para la onda. */
    val level: StateFlow<Float> = _level
    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(continuous: Boolean, keepText: Boolean = false) {
        stopInternal()
        if (!keepText) _text.value = ""
        _partial.value = ""
        _error.value = null
        this.continuous = continuous
        active = true
        strikes = 0
        if (!available()) {
            _error.value = "Este móvil no tiene reconocimiento de voz. Puedes escribirlo."
            active = false
            return
        }
        listen()
    }

    /** Para de escuchar y devuelve todo el texto reconocido. */
    fun stop(): String {
        active = false
        val all = join(_text.value, _partial.value)
        _text.value = all
        _partial.value = ""
        stopInternal()
        return all
    }

    fun cancel() {
        active = false
        stopInternal()
        _text.value = ""
        _partial.value = ""
    }

    private fun intent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
    }

    private fun listen() {
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        r.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
        }
        r.startListening(intent)
        _listening.value = true
    }

    private fun stopInternal() {
        recognizer?.run { runCatching { cancel() }; runCatching { destroy() } }
        recognizer = null
        _listening.value = false
        _level.value = 0f
    }

    private fun join(a: String, b: String) = listOf(a.trim(), b.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) { _level.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { _level.value = 0f }
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                _partial.value = it
                if (it.isNotBlank()) strikes = 0
            }
        }

        override fun onResults(results: Bundle?) {
            val best = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (best.isNotBlank()) strikes = 0
            _text.value = join(_text.value, best.ifBlank { _partial.value })
            _partial.value = ""
            if (active && continuous) listen() else { active = false; stopInternal() }
        }

        override fun onError(error: Int) {
            when (error) {
                // Silencio o nada entendido: en modo continuo seguimos escuchando.
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    if (_partial.value.isNotBlank()) { _text.value = join(_text.value, _partial.value); _partial.value = "" }
                    if (active && continuous) { listen(); return }
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> _error.value = "Aúna necesita permiso para usar el micrófono."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> _error.value = "El reconocimiento de voz necesita conexión a internet."
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                    // Suele ser que falta el paquete de español sin conexión: pedimos que se descargue y seguimos.
                    if (!askedDownload && android.os.Build.VERSION.SDK_INT >= 33) {
                        askedDownload = true
                        runCatching { recognizer?.triggerModelDownload(intent()) }
                    }
                    if (active && continuous && ++strikes <= 3) { stopInternal(); listen(); return }
                    if (_text.value.isBlank() && _partial.value.isBlank()) _error.value = "Falta el español en el reconocimiento de voz del móvil. Revisa la conexión o descárgalo en Ajustes del móvil › Sistema › Idiomas."
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
                    if (active && continuous && ++strikes <= 3) { stopInternal(); listen(); return }
                else -> _error.value = "No he podido escucharte. Vuelve a intentarlo."
            }
            active = false
            stopInternal()
        }
    }
}
