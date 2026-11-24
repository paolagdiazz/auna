package com.auna.padres.data

import kotlinx.serialization.Serializable

/** Total de secciones que cuenta la barra de progreso del pasaporte (8/11). */
const val PASSPORT_TOTAL = 11

@Serializable
data class Kid(
    val id: String,
    val name: String,
    val age: String,
    val done: Int,
    /** Clave de la foto incluida en la app (ver `photoRes`). Null = foto pendiente. */
    val photo: String? = null,
)

@Serializable
data class CustomSection(val id: String, val kidId: String, val title: String)

/**
 * Todo lo que la familia cambia en la app. Se guarda en el móvil (JSON) y se
 * recupera al abrirla. Los valores por defecto son los datos de ejemplo del diseño.
 */
@Serializable
data class AppState(
    val kids: List<Kid> = Sample.kids,
    val activeKidId: String? = Sample.kids.first().id,
    val customSections: List<CustomSection> = emptyList(),
    /** Solicitud de acceso de Pedro: pending · day · custom · no */
    val request: String = "pending",
    val requestDuration: String = "1 mes",
    val requestRole: String = "Ver",
    val removedAccess: Set<String> = emptySet(),
    /** Propuestas revisadas: id → "yes" | "no" */
    val proposals: Map<String, String> = emptyMap(),
    val whatsapp: Boolean = true,
    val readAloud: Boolean = false,
    val qrShow: Map<String, Boolean> = Sample.qrOptions.associate { it.key to it.defaultOn },
) {
    val activeKid: Kid? get() = kids.firstOrNull { it.id == activeKidId } ?: kids.firstOrNull()

    val pendingProposals: List<Proposal> get() = Sample.proposals.filter { it.id !in proposals }

    val requestPending: Boolean get() = request == "pending"

    fun sectionsFor(kidId: String) = customSections.filter { it.kidId == kidId }

    /** Señales de «Mi idioma»: las del ejemplo + las propuestas aceptadas. */
    fun signalsFor(kidId: String): List<Signal> {
        if (kidId != Sample.MARCO) return emptyList()
        val accepted = Sample.proposals.filter { proposals[it.id] == "yes" }.map { Signal(it.hago, it.significa, "") }
        return Sample.signals + accepted
    }

    fun accessPeople(): List<AccessPerson> {
        val base = Sample.access.toMutableList()
        when (request) {
            "day" -> base.add(0, AccessPerson("pedro", "P", "Pedro · profe", "Solo hoy", warn = true))
            "custom" -> base.add(
                0,
                AccessPerson(
                    "pedro", "P", "Pedro · profe",
                    requestDuration + if (requestRole == "Ver") "" else " · puede proponer",
                    warn = true,
                ),
            )
        }
        return base.filter { it.id !in removedAccess }
    }
}

data class Signal(val hago: String, val significa: String, val hacer: String)

data class Group(val title: String, val items: List<String>, val warm: Boolean = false)

data class PassportSection(
    val title: String,
    val groups: List<Group> = emptyList(),
    val onlyFamily: Boolean = false,
    val pending: String? = null,
)

data class AccessPerson(val id: String, val initial: String, val name: String, val meta: String, val warn: Boolean = false)

data class Proposal(
    val id: String,
    val initial: String,
    val photo: String?,
    val name: String,
    val role: String,
    val `when`: String,
    val said: String,
    val section: String,
    val hago: String,
    val significa: String,
)

data class QrOption(val key: String, val label: String, val defaultOn: Boolean)

data class QA(val q: String, val a: String, val src: String)
