package com.auna.padres.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.auna.padres.data.Access
import com.auna.padres.data.AccessRequest
import com.auna.padres.data.Alert
import com.auna.padres.data.Kid
import com.auna.padres.data.Passport
import com.auna.padres.data.Proposal
import com.auna.padres.data.Templates
import com.auna.padres.data.ViewLog
import com.auna.padres.ui.photoRes
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Calendar

/** Lo que llega de la nube para un niño. */
data class Pulled(
    val requests: List<AccessRequest>,
    val access: List<Access>,
    val proposals: List<Proposal>,
    val alerts: List<Alert>,
    val views: List<ViewLog>,
)

/**
 * Sincroniza la app de padres con Firestore (mismo esquema que lee la web):
 *  kids/{id}                 pasaporte compartido (sin secciones «Solo familia»)
 *  kids/{id}/private/family  secciones «Solo familia»
 *  kids/{id}/emergency/card  ficha pública del QR
 *  codes/{código}            código de la tarjeta → niño
 * y trae solicitudes, accesos, propuestas, avisos y consultas.
 */
class CloudSync(private val context: Context, private val fb: Firebase) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val rnd = SecureRandom()

    fun newCloudId(): String {
        val abc = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..20).map { abc[rnd.nextInt(abc.length)] }.joinToString("")
    }

    fun newCode(): String {
        val abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        fun part() = (1..4).map { abc[rnd.nextInt(abc.length)] }.joinToString("")
        return "AUN-${part()}-${part()}"
    }

    /** Lo que se sube del niño; también sirve para saber si ha cambiado. */
    fun kidPayload(kid: Kid): Map<String, Any?> {
        val shared = kid.passport.copy(sections = kid.passport.sections.filter { !it.onlyFamily })
        return mapOf(
            "owners" to listOf(fb.uid),
            "name" to kid.name,
            "age" to kid.age,
            "noHabla" to kid.passport.noHabla,
            "photo" to photoData(kid.photo),
            "passport" to json.encodeToJsonElement(Passport.serializer(), shared),
            "code" to kid.code,
        )
    }

    fun privatePayload(kid: Kid): Map<String, Any?> = mapOf(
        "sections" to json.encodeToJsonElement(
            kotlinx.serialization.builtins.ListSerializer(com.auna.padres.data.Section.serializer()),
            kid.passport.sections.filter { it.onlyFamily },
        ),
    )

    /** Ficha pública de emergencia: solo lo marcado en «Qué se muestra». */
    fun cardPayload(kid: Kid): Map<String, Any?> {
        val p = kid.passport
        val on = { k: String -> kid.qrShow[k] ?: Templates.qrDefaults()[k] ?: false }
        val crisis = if (on("crisis")) p.group("salud", "crisis")?.items.orEmpty() else emptyList()
        val health = if (on("salud")) p.group("salud", "salud")?.items.orEmpty() else emptyList()
        val sayThis = buildString {
            append(kid.name)
            if (kid.age.isNotBlank()) append(", ${kid.age}")
            append(".")
            if (p.noHabla) append(" No habla.")
            val epi = p.group("salud", "salud")?.items.orEmpty().filter { it.lowercase().contains("epilep") }
            if (on("crisis") && epi.isNotEmpty()) append(" Tiene epilepsia.")
            if (health.isNotEmpty()) append(" Salud: ${health.joinToString(", ")}.")
        }
        return mapOf(
            "name" to kid.name,
            "age" to if (on("nombre")) kid.age else "",
            "noHabla" to (on("nombre") && p.noHabla),
            "yes" to if (on("sino")) p.yes else "",
            "no" to if (on("sino")) p.no else "",
            "sayThis" to sayThis,
            "crisis" to crisis,
            "food" to if (on("crisis")) p.group("comida", "cuidado")?.items.orEmpty() else emptyList(),
            "health" to health,
            "contacts" to if (on("familia")) p.contacts.filter { it.phone.isNotBlank() }.map { mapOf("relation" to it.relation, "name" to it.name, "phone" to it.phone) } else emptyList<Any>(),
            "updatedAt" to Timestamp(System.currentTimeMillis()),
        )
    }

    /** Sube (o actualiza) un niño. El niño debe tener ya cloudId y code. */
    fun push(kid: Kid) {
        val id = kid.cloudId ?: return
        fb.set("kids/$id", kidPayload(kid) + ("updatedAt" to Timestamp(System.currentTimeMillis())))
        fb.set("kids/$id/private/family", privatePayload(kid))
        fb.set("kids/$id/emergency/card", cardPayload(kid))
        kid.code?.let { fb.set("codes/$it", mapOf("kidId" to id, "name" to kid.name, "age" to kid.age)) }
    }

    fun deleteKid(kid: Kid) {
        val id = kid.cloudId ?: return
        runCatching { fb.delete("kids/$id/emergency/card") }
        kid.code?.let { runCatching { fb.delete("codes/$it") } }
        runCatching { fb.delete("kids/$id/private/family") }
        fb.delete("kids/$id")
    }

    /** Niños de esta familia que están en la nube (para un móvil nuevo). */
    fun download(): List<Kid> = fb.myKids().mapNotNull { (id, d) ->
        runCatching {
            val shared = json.decodeFromJsonElement(Passport.serializer(), Firebase.toJson(d["passport"]))
            val privateSections = runCatching {
                val priv = fb.get("kids/$id/private/family")
                json.decodeFromJsonElement(
                    kotlinx.serialization.builtins.ListSerializer(com.auna.padres.data.Section.serializer()),
                    Firebase.toJson(priv?.get("sections") ?: emptyList<Any>()),
                )
            }.getOrDefault(emptyList())
            val passport = shared.copy(sections = shared.sections + privateSections)
            Kid(
                id = "k$id", name = d["name"] as? String ?: "", age = d["age"] as? String ?: "",
                photo = null, passport = passport, cloudId = id, code = d["code"] as? String,
            )
        }.getOrNull()
    }

    /** Trae de la nube lo que otras personas han hecho para este niño. */
    fun pull(kid: Kid): Pulled {
        val id = kid.cloudId ?: return Pulled(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val requests = fb.list("kids/$id/requests").map { (uid, d) ->
            AccessRequest(uid, kid.id, d.str("name").ifBlank { d.str("email") }, d.str("role"), "", d.str("note"), d.str("email"))
        }
        val access = fb.list("kids/$id/access").map { (uid, d) ->
            Access(uid, d.str("name"), d.str("role"), d.str("permission").ifBlank { "ver" }, (d["until"] as? Timestamp)?.millis)
        }
        val proposals = fb.list("kids/$id/proposals").map { (pid, d) ->
            Proposal(
                id = pid, kidId = kid.id, name = d.str("fromName"), role = d.str("role"),
                whenLabel = whenLabel((d["createdAt"] as? Timestamp)?.millis), said = d.str("said"),
                hago = d.str("hago"), significa = d.str("significa"), status = d["status"] as? String,
            )
        }
        val alerts = fb.list("kids/$id/alerts").map { (aid, d) ->
            Alert(aid, kid.id, d.str("kind"), d.str("text"), d["lat"] as? Double, d["lng"] as? Double, (d["createdAt"] as? Timestamp)?.millis ?: 0, d["seen"] == true)
        }.sortedByDescending { it.at }
        val views = fb.list("kids/$id/views").map { (_, d) ->
            ViewLog(kid.id, d.str("name"), (d["at"] as? Timestamp)?.millis ?: 0)
        }.sortedByDescending { it.at }.take(20)
        return Pulled(requests, access, proposals, alerts, views)
    }

    // ---------- Acciones de la familia ----------
    fun grant(kid: Kid, access: Access) {
        val id = kid.cloudId ?: return
        fb.set("kids/$id/access/${access.id}", mapOf(
            "name" to access.name, "role" to access.role, "permission" to access.permission,
            "until" to access.until?.let { Timestamp(it) }, "grantedAt" to Timestamp(System.currentTimeMillis()),
        ))
    }

    fun revoke(kid: Kid, accessId: String) { kid.cloudId?.let { fb.delete("kids/$it/access/$accessId") } }

    fun deleteRequest(kid: Kid, uid: String) { kid.cloudId?.let { fb.delete("kids/$it/requests/$uid") } }

    fun restoreRequest(kid: Kid, r: AccessRequest) {
        kid.cloudId?.let { fb.set("kids/$it/requests/${r.id}", mapOf("name" to r.name, "role" to r.role, "note" to r.note, "email" to r.email, "createdAt" to Timestamp(System.currentTimeMillis()))) }
    }

    fun setProposalStatus(kid: Kid, proposalId: String, status: String) {
        kid.cloudId?.let { fb.update("kids/$it/proposals/$proposalId", mapOf("status" to status)) }
    }

    fun markSeen(kid: Kid, alertId: String) {
        kid.cloudId?.let { fb.update("kids/$it/alerts/$alertId", mapOf("seen" to true)) }
    }

    // ---------- Fotos ----------
    /** Foto pequeña (160 px, JPEG) como data URL, para que la web la enseñe sin almacenamiento de pago. */
    private val photoCache = HashMap<String, String?>()

    fun photoData(key: String?): String? {
        if (key == null) return null
        photoCache[key]?.let { return it }
        val bmp = runCatching {
            val res = photoRes(key)
            if (res != null) BitmapFactory.decodeResource(context.resources, res)
            else BitmapFactory.decodeFile(key, BitmapFactory.Options().apply { inSampleSize = 4 })
        }.getOrNull() ?: return null
        val side = minOf(bmp.width, bmp.height)
        val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        val small = Bitmap.createScaledBitmap(square, 160, 160, true)
        val out = ByteArrayOutputStream()
        small.compress(Bitmap.CompressFormat.JPEG, 72, out)
        val data = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        photoCache[key] = data
        return data
    }

    private fun whenLabel(millis: Long?): String {
        if (millis == null) return ""
        val c = Calendar.getInstance(); val today = c.get(Calendar.DAY_OF_YEAR); val year = c.get(Calendar.YEAR)
        c.timeInMillis = millis
        return when {
            c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == today -> "hoy"
            c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == today - 1 -> "ayer"
            else -> "${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}"
        }
    }

    private fun Map<String, Any?>.str(k: String) = this[k] as? String ?: ""
}
