package com.auna.padres.data

import kotlinx.serialization.Serializable

/** Total de apartados que cuenta la barra de progreso del pasaporte. */
const val PASSPORT_TOTAL = 11

fun newId(prefix: String = "x") = prefix + System.currentTimeMillis().toString(36) + (0..999).random()

/** Un bloque de una sección: título + cosas (p. ej. «Me calma: el agua, la música…»). */
@Serializable
data class Group(
    val id: String,
    val title: String,
    val items: List<String> = emptyList(),
    val warm: Boolean = false,
)

/** «Mi idioma»: lo que hago → lo que significa → qué hacer. */
@Serializable
data class Signal(val id: String, val hago: String, val significa: String, val hacer: String = "")

@Serializable
data class Section(
    val id: String,
    val title: String,
    val groups: List<Group> = emptyList(),
    val onlyFamily: Boolean = false,
    val custom: Boolean = false,
)

@Serializable
data class Contact(val id: String, val name: String, val relation: String, val phone: String = "")

@Serializable
data class Passport(
    val noHabla: Boolean = false,
    val highlights: List<String> = emptyList(),
    val yes: String = "",
    val no: String = "",
    val signals: List<Signal> = emptyList(),
    val sections: List<Section> = Templates.baseSections(),
    val contacts: List<Contact> = emptyList(),
) {
    fun section(id: String) = sections.firstOrNull { it.id == id }
    fun group(sectionId: String, groupId: String) = section(sectionId)?.groups?.firstOrNull { it.id == groupId }

    /** Los 11 apartados que cuenta la barra de progreso. */
    fun checklist(): List<Pair<String, Boolean>> {
        val g = { s: String, gr: String -> group(s, gr)?.items?.isNotEmpty() == true }
        return listOf(
            "Quién soy" to g("sobre", "yo"),
            "Mi gente" to (g("sobre", "gente") || contacts.isNotEmpty()),
            "Cómo digo sí y no" to (yes.isNotBlank() && no.isNotBlank()),
            "Mis señales" to signals.isNotEmpty(),
            "Qué me calma" to g("calma", "calma"),
            "Qué me altera" to g("calma", "altera"),
            "Qué me gusta" to g("gusta", "gusta"),
            "Cuidado al comer" to g("comida", "cuidado"),
            "Cómo como" to g("comida", "como"),
            "Salud" to g("salud", "salud"),
            "Si tengo una crisis" to g("salud", "crisis"),
        )
    }

    val done: Int get() = checklist().count { it.second }

    /** Apartados vacíos de una sección base (para el aviso «Faltan: …»). */
    fun missingIn(sectionId: String): List<String> = when (sectionId) {
        "idioma" -> buildList {
            if (yes.isBlank() || no.isBlank()) add("cómo dice sí y no")
            if (signals.isEmpty()) add("sus señales")
        }
        else -> section(sectionId)?.takeIf { !it.custom }?.groups
            ?.filter { it.items.isEmpty() && !(sectionId == "sobre" && it.id == "gente" && contacts.isNotEmpty()) }
            ?.map { it.title.lowercase() } ?: emptyList()
    }

    fun isEmpty(sectionId: String): Boolean = when (sectionId) {
        "idioma" -> yes.isBlank() && no.isBlank() && signals.isEmpty()
        else -> section(sectionId)?.groups?.all { it.items.isEmpty() } ?: true
    }
}

/** Personas con acceso al pasaporte. `until` = fecha de caducidad (epoch ms) o null si es para siempre. */
@Serializable
data class Access(
    val id: String,
    val name: String,
    val role: String,
    val permission: String = PERM_VIEW,
    val until: Long? = null,
    val members: Int? = null,
)

const val PERM_EDIT = "editar"
const val PERM_VIEW = "ver"
const val PERM_PROPOSE = "proponer"

@Serializable
data class Proposal(
    val id: String,
    val kidId: String,
    val name: String,
    val role: String,
    val whenLabel: String,
    val said: String,
    val hago: String,
    val significa: String,
    val photo: String? = null,
    /** null = sin revisar · "yes" = añadida · "no" = descartada */
    val status: String? = null,
)

@Serializable
data class AccessRequest(
    val id: String,
    val kidId: String,
    val name: String,
    val role: String,
    val place: String = "",
    /** Nota que la persona escribió para la familia (desde la web). */
    val note: String = "",
    val email: String = "",
)

/** Aviso que llega desde la web o desde la tarjeta QR (urgencia, ubicación). */
@Serializable
data class Alert(
    val id: String,
    val kidId: String,
    val kind: String,
    val text: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val at: Long = 0,
    val seen: Boolean = false,
)

/** Quién ha consultado el pasaporte y cuándo. */
@Serializable
data class ViewLog(val kidId: String, val name: String, val at: Long)

@Serializable
data class Kid(
    val id: String,
    val name: String,
    val age: String,
    /** Foto: clave de una foto incluida («marco») o ruta de un archivo en el móvil. */
    val photo: String? = null,
    val passport: Passport = Passport(),
    val access: List<Access> = emptyList(),
    val qrShow: Map<String, Boolean> = Templates.qrDefaults(),
    /** Id del niño en la nube (null = aún no se ha subido). */
    val cloudId: String? = null,
    /** Código de la tarjeta (AUN-XXXX-XXXX) para que profes y familiares lo añadan en la web. */
    val code: String? = null,
)

/**
 * Todo lo que la familia cambia en la app. Se guarda en el móvil (JSON) y se
 * recupera al abrirla. Los valores por defecto son los datos de ejemplo.
 */
@Serializable
data class AppState(
    val kids: List<Kid> = Sample.kids(),
    val activeKidId: String? = Sample.MARCO,
    val requests: List<AccessRequest> = Sample.requests(),
    val proposals: List<Proposal> = Sample.proposals(),
    val readAloud: Boolean = false,
    val alerts: List<Alert> = emptyList(),
    val views: List<ViewLog> = emptyList(),
) {
    val activeKid: Kid? get() = kids.firstOrNull { it.id == activeKidId } ?: kids.firstOrNull()

    fun pendingProposals(kidId: String?) = proposals.filter { it.kidId == kidId && it.status == null }
    fun reviewedProposals(kidId: String?) = proposals.filter { it.kidId == kidId && it.status != null }
    fun requestsFor(kidId: String?) = requests.filter { it.kidId == kidId }
    fun unseenAlerts(kidId: String?) = alerts.filter { it.kidId == kidId && !it.seen }
}

data class QA(val q: String, val a: String, val src: String)

data class QrOption(val key: String, val label: String, val defaultOn: Boolean)
