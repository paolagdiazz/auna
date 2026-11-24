package com.auna.padres.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Guarda el estado de la app en un JSON dentro del almacenamiento privado del
 * móvil. Este archivo no se sube a ningún sitio.
 */
class Store(context: Context) {
    private val file = File(context.filesDir, "auna_estado_v2.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        // Los StateFlow se fusionan: solo se escribe el último estado pendiente, en orden.
        scope.launch { _state.drop(1).collect { save(it) } }
    }

    fun update(change: (AppState) -> AppState) = _state.update(change)

    private fun load(): AppState =
        runCatching { json.decodeFromString(AppState.serializer(), file.readText()) }.getOrElse { AppState() }

    private fun save(s: AppState) {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(AppState.serializer(), s))
            tmp.renameTo(file)
        }
    }
}
