package com.auna.padres

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.auna.padres.data.AppState
import com.auna.padres.data.CustomSection
import com.auna.padres.data.Kid
import com.auna.padres.data.Store
import kotlinx.coroutines.flow.StateFlow

class AunaViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val state: StateFlow<AppState> = store.state

    // Perfiles
    fun selectKid(id: String) = store.update { it.copy(activeKidId = id) }

    fun createKid(name: String, age: String): String {
        val nm = name.trim().ifEmpty { "Nuevo perfil" }
        val id = "k" + System.currentTimeMillis()
        store.update { it.copy(kids = it.kids + Kid(id, nm, "$age años", 0), activeKidId = id) }
        return nm
    }

    fun deleteKid(id: String) = store.update { s ->
        val rest = s.kids.filter { it.id != id }
        s.copy(
            kids = rest,
            activeKidId = if (s.activeKidId == id) rest.firstOrNull()?.id else s.activeKidId,
            customSections = s.customSections.filter { it.kidId != id },
        )
    }

    // Pasaporte
    fun addSection(kidId: String, title: String) = store.update { s ->
        s.copy(customSections = s.customSections + CustomSection("c" + System.currentTimeMillis(), kidId, title))
    }

    // Accesos
    fun answerRequest(answer: String) = store.update { it.copy(request = answer) }
    fun setDuration(d: String) = store.update { it.copy(requestDuration = d) }
    fun setRole(r: String) = store.update { it.copy(requestRole = r) }
    fun removeAccess(id: String) = store.update { it.copy(removedAccess = it.removedAccess + id) }
    fun undoRequest() = store.update { it.copy(request = "pending", removedAccess = it.removedAccess - "pedro") }

    // Propuestas
    fun decide(id: String, yes: Boolean) = store.update { it.copy(proposals = it.proposals + (id to if (yes) "yes" else "no")) }

    // Ajustes y QR
    fun setWhatsapp(on: Boolean) = store.update { it.copy(whatsapp = on) }
    fun setReadAloud(on: Boolean) = store.update { it.copy(readAloud = on) }
    fun setQr(key: String, on: Boolean) = store.update { it.copy(qrShow = it.qrShow + (key to on)) }

    /** Vuelve al estado inicial de la app (borra todo lo guardado en este móvil). */
    fun resetAll() = store.update { AppState() }
}
