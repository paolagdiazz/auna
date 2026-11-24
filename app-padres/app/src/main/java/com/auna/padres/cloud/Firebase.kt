package com.auna.padres.cloud

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Error de la nube con un mensaje que se puede enseñar a la familia. */
class CloudError(message: String, val code: Int = 0) : Exception(message)

/** Marca de tiempo para guardar en Firestore. */
data class Timestamp(val millis: Long)

/**
 * Cliente mínimo de Firebase por REST: cuentas (correo y contraseña) y Firestore.
 * Sin SDK de Google: solo HTTPS, con las mismas reglas de seguridad que la web.
 */
class Firebase(context: Context) {
    private val prefs = context.getSharedPreferences("auna_privado", Context.MODE_PRIVATE)
    private var idToken: String? = null
    private var tokenUntil = 0L

    val uid: String? get() = prefs.getString("cloud_uid", null)
    val email: String? get() = prefs.getString("cloud_email", null)
    val signedIn: Boolean get() = CloudConfig.enabled && uid != null && prefs.getString("cloud_refresh", null) != null

    // ---------- Cuenta ----------
    fun signIn(email: String, password: String) = authCall("accounts:signInWithPassword", email, password)
    fun signUp(email: String, password: String) = authCall("accounts:signUp", email, password)

    fun signOut() {
        prefs.edit().remove("cloud_uid").remove("cloud_email").remove("cloud_refresh").apply()
        idToken = null
    }

    private fun authCall(method: String, email: String, password: String) {
        val body = JsonObject(mapOf("email" to JsonPrimitive(email.trim()), "password" to JsonPrimitive(password), "returnSecureToken" to JsonPrimitive(true)))
        val (code, text) = http("POST", "https://identitytoolkit.googleapis.com/v1/$method?key=${CloudConfig.API_KEY}", body.toString(), null)
        if (code != 200) throw CloudError(authMessage(text), code)
        val o = Json.parseToJsonElement(text).jsonObject
        saveSession(o.s("localId"), o.s("email"), o.s("refreshToken"), o.s("idToken"), o.s("expiresIn").toLongOrNull() ?: 3600)
    }

    private fun saveSession(uid: String, email: String, refresh: String, token: String, expiresIn: Long) {
        prefs.edit().putString("cloud_uid", uid).putString("cloud_email", email).putString("cloud_refresh", refresh).apply()
        idToken = token
        tokenUntil = System.currentTimeMillis() + (expiresIn - 120) * 1000
    }

    /** Token de sesión válido (se renueva solo). */
    fun token(): String {
        idToken?.let { if (System.currentTimeMillis() < tokenUntil) return it }
        val refresh = prefs.getString("cloud_refresh", null) ?: throw CloudError("No has iniciado sesión.", 401)
        val (code, text) = http(
            "POST", "https://securetoken.googleapis.com/v1/token?key=${CloudConfig.API_KEY}",
            "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refresh, "UTF-8"), null, form = true,
        )
        if (code != 200) { if (code == 400) signOut(); throw CloudError("La sesión ha caducado. Vuelve a entrar en Ajustes.", code) }
        val o = Json.parseToJsonElement(text).jsonObject
        saveSession(o.s("user_id"), email ?: "", o.s("refresh_token"), o.s("id_token"), o.s("expires_in").toLongOrNull() ?: 3600)
        return idToken!!
    }

    // ---------- Firestore ----------
    private val base get() = "https://firestore.googleapis.com/v1/projects/${CloudConfig.PROJECT_ID}/databases/(default)/documents"

    /** Crea o sustituye un documento. */
    fun set(path: String, data: Map<String, Any?>) {
        val body = JsonObject(mapOf("fields" to fields(data)))
        val (code, text) = http("PATCH", "$base/$path", body.toString(), token())
        if (code !in 200..299) throw CloudError(dbMessage(code, text), code)
    }

    /** Cambia solo algunos campos de un documento que ya existe. */
    fun update(path: String, data: Map<String, Any?>) {
        val mask = data.keys.joinToString("&") { "updateMask.fieldPaths=" + URLEncoder.encode(it, "UTF-8") }
        val body = JsonObject(mapOf("fields" to fields(data)))
        val (code, text) = http("PATCH", "$base/$path?$mask&currentDocument.exists=true", body.toString(), token())
        if (code !in 200..299) throw CloudError(dbMessage(code, text), code)
    }

    fun delete(path: String) {
        val (code, text) = http("DELETE", "$base/$path", null, token())
        if (code !in 200..299 && code != 404) throw CloudError(dbMessage(code, text), code)
    }

    /** Documentos de una colección: lista de (id, datos). */
    fun list(path: String): List<Pair<String, Map<String, Any?>>> {
        val out = mutableListOf<Pair<String, Map<String, Any?>>>()
        var pageToken: String? = null
        do {
            val url = "$base/$path?pageSize=300" + (pageToken?.let { "&pageToken=" + URLEncoder.encode(it, "UTF-8") } ?: "")
            val (code, text) = http("GET", url, null, token())
            if (code == 404) return out
            if (code != 200) throw CloudError(dbMessage(code, text), code)
            val o = Json.parseToJsonElement(text).jsonObject
            o["documents"]?.jsonArray?.forEach { d ->
                val doc = d.jsonObject
                out += doc.s("name").substringAfterLast('/') to decodeFields(doc["fields"]?.jsonObject)
            }
            pageToken = o["nextPageToken"]?.jsonPrimitive?.content
        } while (pageToken != null)
        return out
    }

    /** Niños de los que soy familia (owners contiene mi uid). */
    fun myKids(): List<Pair<String, Map<String, Any?>>> {
        val me = uid ?: return emptyList()
        val query = """{"structuredQuery":{"from":[{"collectionId":"kids"}],"where":{"fieldFilter":{"field":{"fieldPath":"owners"},"op":"ARRAY_CONTAINS","value":{"stringValue":"$me"}}}}}"""
        val (code, text) = http("POST", "$base:runQuery", query, token())
        if (code != 200) throw CloudError(dbMessage(code, text), code)
        return Json.parseToJsonElement(text).jsonArray.mapNotNull { row ->
            val doc = row.jsonObject["document"]?.jsonObject ?: return@mapNotNull null
            doc.s("name").substringAfterLast('/') to decodeFields(doc["fields"]?.jsonObject)
        }
    }

    fun get(path: String): Map<String, Any?>? {
        val (code, text) = http("GET", "$base/$path", null, token())
        if (code == 404) return null
        if (code != 200) throw CloudError(dbMessage(code, text), code)
        return decodeFields(Json.parseToJsonElement(text).jsonObject["fields"]?.jsonObject)
    }

    // ---------- HTTP ----------
    private fun http(method: String, url: String, body: String?, bearer: String?, form: Boolean = false): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        try {
            // HttpURLConnection no admite PATCH: se usa la cabecera de Google para sobrescribir el método.
            if (method == "PATCH") { conn.requestMethod = "POST"; conn.setRequestProperty("X-HTTP-Method-Override", "PATCH") } else conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            if (bearer != null) conn.setRequestProperty("Authorization", "Bearer $bearer")
            conn.setRequestProperty("Content-Type", if (form) "application/x-www-form-urlencoded" else "application/json")
            if (body != null) { conn.doOutput = true; conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) } }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return code to (stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "")
        } catch (e: java.io.IOException) {
            throw CloudError("No hay conexión a internet.")
        } finally {
            conn.disconnect()
        }
    }

    private fun authMessage(text: String): String {
        val msg = runCatching { Json.parseToJsonElement(text).jsonObject["error"]!!.jsonObject.s("message") }.getOrDefault("")
        return when {
            msg.startsWith("EMAIL_EXISTS") -> "Ya hay una cuenta con ese correo. Pulsa «Entrar»."
            msg.startsWith("INVALID_LOGIN_CREDENTIALS") || msg.startsWith("INVALID_PASSWORD") || msg.startsWith("EMAIL_NOT_FOUND") -> "El correo o la contraseña no son correctos."
            msg.startsWith("WEAK_PASSWORD") -> "La contraseña debe tener al menos 8 caracteres."
            msg.startsWith("INVALID_EMAIL") -> "Ese correo no parece válido."
            msg.startsWith("TOO_MANY_ATTEMPTS") -> "Demasiados intentos. Espera un poco."
            msg.startsWith("OPERATION_NOT_ALLOWED") -> "Activa «Correo electrónico/contraseña» en Firebase › Authentication."
            else -> "No se ha podido entrar ($msg)."
        }
    }

    private fun dbMessage(code: Int, text: String): String = when (code) {
        401 -> "La sesión ha caducado. Vuelve a entrar en Ajustes."
        403 -> "Firebase no lo permite: revisa que las reglas de seguridad estén publicadas."
        else -> "Error de la nube ($code). " + runCatching { Json.parseToJsonElement(text).jsonObject["error"]!!.jsonObject.s("message") }.getOrDefault("")
    }

    companion object {
        private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

        /** Kotlin → valores de Firestore. */
        fun value(v: Any?): JsonElement = when (v) {
            null -> obj("nullValue" to JsonNull)
            is Timestamp -> obj("timestampValue" to JsonPrimitive(synchronized(iso) { iso.format(Date(v.millis)) }))
            is String -> obj("stringValue" to JsonPrimitive(v))
            is Boolean -> obj("booleanValue" to JsonPrimitive(v))
            is Int, is Long -> obj("integerValue" to JsonPrimitive(v.toString()))
            is Number -> obj("doubleValue" to JsonPrimitive(v.toDouble()))
            is Map<*, *> -> obj("mapValue" to obj("fields" to fields(v.entries.associate { it.key.toString() to it.value })))
            is List<*> -> obj("arrayValue" to obj("values" to JsonArray(v.map { value(it) })))
            is JsonElement -> fromJson(v)
            else -> obj("stringValue" to JsonPrimitive(v.toString()))
        }

        /** JSON normal (p. ej. el pasaporte serializado) → valores de Firestore. */
        private fun fromJson(e: JsonElement): JsonElement = when (e) {
            is JsonNull -> obj("nullValue" to JsonNull)
            is JsonPrimitive -> when {
                e.isString -> obj("stringValue" to e)
                e.booleanOrNull != null -> obj("booleanValue" to e)
                e.longOrNull != null -> obj("integerValue" to JsonPrimitive(e.content))
                else -> obj("doubleValue" to JsonPrimitive(e.doubleOrNull ?: 0.0))
            }
            is JsonArray -> obj("arrayValue" to obj("values" to JsonArray(e.map { fromJson(it) })))
            is JsonObject -> obj("mapValue" to obj("fields" to JsonObject(e.mapValues { fromJson(it.value) })))
        }

        fun fields(data: Map<String, Any?>) = JsonObject(data.mapValues { value(it.value) })

        /** Valores de Firestore → Kotlin (mapas, listas, String, Long, Double, Boolean, Timestamp). */
        fun decodeFields(f: JsonObject?): Map<String, Any?> = f?.mapValues { decode(it.value.jsonObject) } ?: emptyMap()

        private fun decode(v: JsonObject): Any? {
            val (type, raw) = v.entries.first()
            return when (type) {
                "stringValue" -> raw.jsonPrimitive.content
                "booleanValue" -> raw.jsonPrimitive.booleanOrNull
                "integerValue" -> raw.jsonPrimitive.content.toLongOrNull()
                "doubleValue" -> raw.jsonPrimitive.doubleOrNull
                "timestampValue" -> Timestamp(runCatching { parseIso(raw.jsonPrimitive.content) }.getOrDefault(0L))
                "mapValue" -> decodeFields(raw.jsonObject["fields"]?.jsonObject)
                "arrayValue" -> raw.jsonObject["values"]?.jsonArray?.map { decode(it.jsonObject) } ?: emptyList<Any?>()
                else -> null
            }
        }

        /** Firestore devuelve fechas con 0, 3, 6 o 9 decimales. */
        private fun parseIso(s: String): Long {
            val m = Regex("""(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(\.\d+)?Z""").find(s) ?: return 0L
            val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val ms = m.groupValues[2].drop(1).padEnd(3, '0').take(3).toIntOrNull() ?: 0
            return f.parse(m.groupValues[1])!!.time + ms
        }

        /** Mapa decodificado → JSON normal (para leer el pasaporte con kotlinx). */
        fun toJson(v: Any?): JsonElement = when (v) {
            null -> JsonNull
            is Map<*, *> -> JsonObject(v.entries.associate { it.key.toString() to toJson(it.value) })
            is List<*> -> JsonArray(v.map { toJson(it) })
            is Boolean -> JsonPrimitive(v)
            is Number -> JsonPrimitive(v)
            is Timestamp -> JsonPrimitive(v.millis)
            else -> JsonPrimitive(v.toString())
        }

        private fun obj(vararg p: Pair<String, JsonElement>) = JsonObject(p.toMap())
        private fun JsonObject.s(k: String) = runCatching { this[k]!!.jsonPrimitive.content }.getOrDefault("")
    }
}
