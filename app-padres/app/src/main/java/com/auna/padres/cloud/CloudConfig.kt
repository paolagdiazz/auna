package com.auna.padres.cloud

/**
 * Configuración pública de la nube de Aúna (la misma que web/config.js).
 * Nada de esto es secreto: la seguridad está en las reglas de Firestore.
 */
object CloudConfig {
    /** firebaseConfig.apiKey */
    const val API_KEY = "AIzaSyBPD5J3uoZAuqcgmf5KOWUGaRAoBWRQ7jQ"
    /** firebaseConfig.projectId */
    const val PROJECT_ID = "auna-9c51c"
    /** Dirección de la web (GitHub Pages). */
    const val WEB_URL = "https://paolagdiazz.github.io/auna/app/"
    /** Puente de IA en Cloudflare (worker/auna-ia.js). Vacío = la app usa una clave propia. */
    const val AI_URL = "https://auna-ia.paolagamberodiaz.workers.dev"

    val enabled: Boolean get() = API_KEY.isNotBlank() && PROJECT_ID.isNotBlank()

    fun cardUrl(cloudId: String) = "${WEB_URL}#/c/$cloudId"
}
