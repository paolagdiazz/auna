package com.auna.padres

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auna.padres.data.Access
import com.auna.padres.data.AppState
import com.auna.padres.data.Contact
import com.auna.padres.data.Group
import com.auna.padres.data.Kid
import com.auna.padres.data.PERM_VIEW
import com.auna.padres.data.Passport
import com.auna.padres.data.Section
import com.auna.padres.data.Signal
import com.auna.padres.data.Store
import com.auna.padres.data.Templates
import com.auna.padres.data.newId
import com.auna.padres.cloud.CloudConfig
import com.auna.padres.cloud.CloudError
import com.auna.padres.cloud.CloudSync
import com.auna.padres.cloud.Firebase
import com.auna.padres.data.AccessRequest
import com.auna.padres.data.Alert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(FlowPreview::class)
class AunaViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val state: StateFlow<AppState> = store.state

    /** Sección a la que irá lo próximo que se cuente en «Hablar» (desde «Añadir hablando» o «Corregir»). */
    val talkContext = MutableStateFlow<TalkContext?>(null)

    // El motor de lectura en voz alta se crea la primera vez que se usa, para no retrasar el arranque.
    private val speakerLazy = lazy { Speaker(app) }
    val speaker: Speaker by speakerLazy
    val voice = VoiceInput(app)

    override fun onCleared() {
        if (speakerLazy.isInitialized()) speaker.shutdown()
        voice.cancel()
    }

    // ---------- Clave de la IA: Groq (gratis) o Claude. Solo en este móvil, fuera del JSON del pasaporte ----------
    private val secrets = app.getSharedPreferences("auna_privado", android.content.Context.MODE_PRIVATE)
    val claudeKey = MutableStateFlow(secrets.getString("claude_key", "") ?: "")
    private var cachedClaude: Pair<String, Ai>? = null

    /**
     * IA que se usa: la clave propia si la hay; si no, el puente gratuito de Aúna (con sesión iniciada).
     * Se reutiliza mientras no cambie (crearla es lo más lento de la primera llamada).
     */
    private fun claude(): Ai? {
        val key = claudeKey.value.takeIf { it.isNotBlank() }
            ?: if (CloudConfig.AI_URL.isNotBlank() && firebase.signedIn) "worker" else return null
        cachedClaude?.let { (k, c) -> if (k == key) return c }
        val ai = if (key == "worker") Ai(WorkerBackend { firebase.token() }) else Ai(key)
        return ai.also { cachedClaude = key to it }
    }

    /** ¿Hay alguna IA disponible (clave propia o puente con sesión)? */
    val aiReady: Boolean get() = claudeKey.value.isNotBlank() || (CloudConfig.AI_URL.isNotBlank() && firebase.signedIn)

    // ---------- Nube: cuenta de la familia y sincronización con la web ----------
    val firebase = Firebase(app)
    private val cloud = CloudSync(app, firebase)
    val cloudUi = MutableStateFlow(CloudUi(firebase.signedIn, firebase.email))
    private val pushed = HashMap<String, Int>()
    private var cloudAccessIds = emptySet<String>()
    private var cloudProposalIds = emptySet<String>()

    /** Entra o crea la cuenta de la familia. Devuelve null si va bien o el mensaje de error. */
    suspend fun cloudSignIn(email: String, password: String, create: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            if (create) firebase.signUp(email, password) else firebase.signIn(email, password)
            cloudUi.value = CloudUi(true, firebase.email, syncing = true)
            cachedClaude = null
            val remote = cloud.download()
            store.update { s ->
                val known = s.kids.mapNotNull { it.cloudId }.toSet()
                s.copy(kids = s.kids + remote.filter { it.cloudId !in known })
            }
            syncOnce()
            null
        } catch (e: CloudError) {
            cloudUi.value = cloudUi.value.copy(syncing = false)
            e.message
        }
    }

    fun cloudSignOut() {
        firebase.signOut()
        pushed.clear()
        cachedClaude = null
        cloudUi.value = CloudUi(false, null)
    }

    fun syncNow() { viewModelScope.launch(Dispatchers.IO) { syncOnce() } }

    private fun syncOnce() {
        if (!firebase.signedIn) return
        cloudUi.value = cloudUi.value.copy(syncing = true)
        val err = runCatching { pushAll(); pullAll() }.exceptionOrNull()
        cloudUi.value = cloudUi.value.copy(
            signedIn = firebase.signedIn, email = firebase.email, syncing = false,
            error = (err as? CloudError)?.message ?: err?.let { "No se ha podido sincronizar." },
            lastSync = if (err == null) System.currentTimeMillis() else cloudUi.value.lastSync,
        )
    }

    /** Da id en la nube y código de tarjeta a los niños que aún no los tienen. */
    private fun ensureCloudIds() {
        if (state.value.kids.any { it.cloudId == null || it.code == null }) {
            store.update { s -> s.copy(kids = s.kids.map { k -> k.copy(cloudId = k.cloudId ?: cloud.newCloudId(), code = k.code ?: cloud.newCode()) }) }
        }
    }

    /** Sube los niños que han cambiado desde la última vez. */
    private fun pushAll() {
        ensureCloudIds()
        state.value.kids.forEach { kid ->
            val h = listOf(cloud.kidPayload(kid), cloud.privatePayload(kid), cloud.cardPayload(kid) - "updatedAt").hashCode()
            if (pushed[kid.id] != h) { cloud.push(kid); pushed[kid.id] = h }
        }
    }

    /** Trae lo que han hecho otras personas (solicitudes, propuestas, avisos, consultas) y lo mezcla. */
    private fun pullAll() {
        val results = state.value.kids.filter { it.cloudId != null }.map { it.id to cloud.pull(it) }
        cloudAccessIds = results.flatMap { (_, p) -> p.access.map { it.id } }.toSet()
        cloudProposalIds = results.flatMap { (_, p) -> p.proposals.map { it.id } }.toSet()
        val ids = results.map { it.first }.toSet()
        store.update { s ->
            val byKid = results.toMap()
            s.copy(
                kids = s.kids.map { k -> byKid[k.id]?.let { k.copy(access = it.access) } ?: k },
                requests = s.requests.filter { it.kidId !in ids } + results.flatMap { it.second.requests },
                proposals = s.proposals.filter { it.kidId !in ids } + results.flatMap { it.second.proposals },
                alerts = s.alerts.filter { it.kidId !in ids } + results.flatMap { it.second.alerts },
                views = s.views.filter { it.kidId !in ids } + results.flatMap { it.second.views },
            )
        }
    }

    private fun cloudDo(block: () -> Unit) {
        if (!firebase.signedIn) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(block).onFailure { e -> cloudUi.value = cloudUi.value.copy(error = (e as? CloudError)?.message ?: "No se ha podido guardar en la nube.") }
        }
    }

    private fun kidById(id: String) = state.value.kids.firstOrNull { it.id == id }

    /** Texto para invitar a alguien a la web con el código de la tarjeta. */
    fun invitationText(kid: Kid): String? {
        val id = kid.cloudId ?: return null
        if (!firebase.signedIn) return null
        return "Hola: te invito a ver el pasaporte de ${kid.name} en Aúna. Entra en ${CloudConfig.cardUrl(id)}, " +
            "crea tu cuenta y envíame la nota. Código de su tarjeta: ${kid.code}"
    }

    fun markAlertSeen(alert: Alert) {
        store.update { s -> s.copy(alerts = s.alerts.map { if (it.id == alert.id) it.copy(seen = true) else it }) }
        kidById(alert.kidId)?.let { k -> cloudDo { cloud.markSeen(k, alert.id) } }
    }

    init {
        // Prepara el cliente en segundo plano para que la primera respuesta llegue antes.
        if (claudeKey.value.isNotBlank()) viewModelScope.launch(Dispatchers.IO) { runCatching { claude()?.warmUp() } }
        // Sube los cambios a la nube un par de segundos después de que la familia deje de tocar.
        viewModelScope.launch {
            state.drop(1).debounce(2000).collect {
                if (firebase.signedIn) withContext(Dispatchers.IO) {
                    runCatching { pushAll() }.onFailure { e -> cloudUi.value = cloudUi.value.copy(error = (e as? CloudError)?.message) }
                }
            }
        }
        // Mira cada 25 s si hay solicitudes, propuestas o avisos nuevos desde la web.
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (firebase.signedIn) syncOnce()
                delay(25_000)
            }
        }
    }

    /** Guarda la clave y comprueba que funciona. Devuelve null si va bien o el mensaje de error. */
    suspend fun saveClaudeKey(key: String): String? {
        val k = key.trim()
        if (k.isEmpty()) {
            secrets.edit().remove("claude_key").apply(); claudeKey.value = ""; return null
        }
        return try {
            Ai(k).check()
            secrets.edit().putString("claude_key", k).apply()
            claudeKey.value = k
            null
        } catch (e: AiError) {
            if (e.badKey) "Esta clave no funciona. Cópiala otra vez desde console.groq.com (API Keys)." else e.message
        }
    }

    // ---------- Hablar: voz → Claude → revisar → guardar ----------
    val talk = MutableStateFlow(TalkUi())

    fun talkTopic(topic: String) { talk.value = talk.value.copy(topic = topic) }

    fun talkStart(more: Boolean = false) {
        speaker.stop()
        talk.value = talk.value.copy(step = if (more) "rec-more" else "rec", error = null, startedAt = System.currentTimeMillis())
        voice.start(continuous = true)
    }

    fun talkStop() {
        val said = voice.stop()
        val more = talk.value.step == "rec-more"
        if (said.isBlank()) {
            val err = voice.error.value ?: "No he oído nada. Toca el micro y habla cerca del móvil."
            talk.value = talk.value.copy(step = if (more) "review" else "idle", error = err)
            return
        }
        talkProcess(said, append = more)
    }

    fun talkCancelRec() {
        voice.cancel()
        talk.value = talk.value.copy(step = if (talk.value.step == "rec-more") "review" else "idle")
    }

    fun talkWrite() { talk.value = talk.value.copy(step = "write", error = null) }
    fun talkBack() { voice.cancel(); talk.value = talk.value.copy(step = if (talk.value.findings.isNotEmpty() && talk.value.step != "review") "review" else "idle", error = null) }

    fun talkProcess(said: String, append: Boolean = false) {
        val kid = state.value.activeKid ?: return
        val ctx = talkContext.value
        val client = claude()
        if (client == null) {
            talk.value = talk.value.copy(step = if (append) "review" else "idle", pendingText = said, needsKey = true, error = null)
            return
        }
        val prevSaid = if (append) talk.value.said else ""
        talk.value = talk.value.copy(step = "thinking", said = listOf(prevSaid, said).filter { it.isNotBlank() }.joinToString("\n\n"), error = null, needsKey = false, pendingText = said)
        viewModelScope.launch {
            try {
                val question = if (append) talk.value.followUp else (ctx?.prompt ?: talk.value.topic)
                val result = client.organize(kid, said, ctx?.sectionId, question)
                val old = if (append) talk.value.findings else emptyList()
                talk.value = talk.value.copy(
                    step = "review",
                    findings = old + result.findings.map { KeptFinding(it, true) },
                    followUp = result.followUp,
                    pendingText = "",
                )
                if (state.value.readAloud && result.followUp != null) speaker.speak(result.followUp, "pregunta")
            } catch (e: AiError) {
                talk.value = talk.value.copy(step = if (append) "review" else "error", error = e.message, needsKey = e.badKey)
            }
        }
    }

    fun talkRetry() = talk.value.pendingText.takeIf { it.isNotBlank() }?.let { talkProcess(it, append = talk.value.findings.isNotEmpty()) }

    fun talkToggle(index: Int) {
        talk.value = talk.value.copy(findings = talk.value.findings.mapIndexed { i, f -> if (i == index) f.copy(kept = !f.kept) else f })
    }

    fun talkEdit(index: Int, finding: Finding) {
        talk.value = talk.value.copy(findings = talk.value.findings.mapIndexed { i, f -> if (i == index) f.copy(finding = finding) else f })
    }

    fun talkSave() {
        val kid = state.value.activeKid ?: return
        val kept = talk.value.findings.filter { it.kept }.map { it.finding }
        applyFindings(kid.id, kept)
        talkContext.value = null
        talk.value = TalkUi(step = "done", savedCount = kept.size)
    }

    fun talkReset() {
        voice.cancel()
        talkContext.value = null
        talk.value = TalkUi()
    }

    // ---------- Comprueba las respuestas ----------
    val checks = MutableStateFlow<List<Check>>(emptyList())

    fun ask(question: String) {
        val q = question.trim()
        val kid = state.value.activeKid ?: return
        if (q.isEmpty()) return
        val id = newId("q")
        checks.value = checks.value + Check(id, q, loading = true)
        val client = claude()
        if (client == null) {
            updateCheck(id) { it.copy(loading = false, error = "Para responder hace falta conectar la IA en Ajustes (o entrar en tu cuenta de Aúna).", needsKey = true) }
            return
        }
        viewModelScope.launch {
            try {
                val a = client.answer(kid, q)
                updateCheck(id) { it.copy(loading = false, answer = a) }
                if (state.value.readAloud) speaker.speak(a.text, "respuesta-$id")
            } catch (e: AiError) {
                updateCheck(id) { it.copy(loading = false, error = e.message, needsKey = e.badKey) }
            }
        }
    }

    fun retryCheck(id: String) {
        val c = checks.value.firstOrNull { it.id == id } ?: return
        checks.value = checks.value.filter { it.id != id }
        ask(c.question)
    }

    fun setVerdict(id: String, verdict: String) = updateCheck(id) { it.copy(verdict = verdict) }

    fun clearChecks() { checks.value = emptyList() }

    private fun updateCheck(id: String, change: (Check) -> Check) {
        checks.value = checks.value.map { if (it.id == id) change(it) else it }
    }

    private fun activeId() = state.value.activeKid?.id

    private fun updateKid(kidId: String, change: (Kid) -> Kid) = store.update { s ->
        s.copy(kids = s.kids.map { if (it.id == kidId) change(it) else it })
    }

    private fun updatePassport(kidId: String, change: (Passport) -> Passport) = updateKid(kidId) { it.copy(passport = change(it.passport)) }

    private fun updateGroup(kidId: String, sectionId: String, groupId: String, change: (Group) -> Group) =
        updatePassport(kidId) { p ->
            p.copy(sections = p.sections.map { s ->
                if (s.id != sectionId) s else s.copy(groups = s.groups.map { g -> if (g.id == groupId) change(g) else g })
            })
        }

    // ---------- Perfiles ----------
    fun selectKid(id: String) = store.update { it.copy(activeKidId = id) }

    fun createKid(name: String, age: String, noHabla: Boolean): String {
        val nm = name.trim().ifEmpty { "Nuevo perfil" }
        val id = newId("k")
        val ageLabel = ageLabel(age)
        store.update { it.copy(kids = it.kids + Kid(id, nm, ageLabel, passport = Passport(noHabla = noHabla)), activeKidId = id) }
        return id
    }

    fun editKid(kidId: String, name: String, age: String, noHabla: Boolean) = updateKid(kidId) {
        it.copy(name = name.trim().ifEmpty { it.name }, age = ageLabel(age), passport = it.passport.copy(noHabla = noHabla))
    }

    fun deleteKid(id: String) {
        kidById(id)?.let { k -> if (k.cloudId != null) cloudDo { cloud.deleteKid(k) } }
        deleteKidLocal(id)
    }

    private fun deleteKidLocal(id: String) = store.update { s ->
        val rest = s.kids.filter { it.id != id }
        s.copy(
            kids = rest,
            activeKidId = if (s.activeKidId == id) rest.firstOrNull()?.id else s.activeKidId,
            proposals = s.proposals.filter { it.kidId != id },
            requests = s.requests.filter { it.kidId != id },
        )
    }

    /** Copia la foto elegida al almacenamiento privado de la app y la asigna al niño. */
    fun setPhoto(kidId: String, uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(ctx.filesDir, "fotos").apply { mkdirs() }
                    val f = File(dir, "$kidId-${System.currentTimeMillis()}.jpg")
                    ctx.contentResolver.openInputStream(uri)!!.use { input -> f.outputStream().use { input.copyTo(it) } }
                    f.absolutePath
                }.getOrNull()
            } ?: return@launch
            val old = state.value.kids.firstOrNull { it.id == kidId }?.photo
            updateKid(kidId) { it.copy(photo = path) }
            if (old != null && old.startsWith(ctx.filesDir.absolutePath)) withContext(Dispatchers.IO) { File(old).delete() }
        }
    }

    // ---------- Pasaporte ----------
    fun addItem(kidId: String, sectionId: String, groupId: String, text: String) {
        if (text.isBlank()) return
        updateGroup(kidId, sectionId, groupId) { it.copy(items = it.items + text.trim()) }
    }

    fun editItem(kidId: String, sectionId: String, groupId: String, index: Int, text: String) =
        updateGroup(kidId, sectionId, groupId) { g ->
            g.copy(items = if (text.isBlank()) g.items.filterIndexed { i, _ -> i != index } else g.items.mapIndexed { i, t -> if (i == index) text.trim() else t })
        }

    fun removeItem(kidId: String, sectionId: String, groupId: String, index: Int) =
        updateGroup(kidId, sectionId, groupId) { g -> g.copy(items = g.items.filterIndexed { i, _ -> i != index }) }

    fun setYesNo(kidId: String, yes: String, no: String) = updatePassport(kidId) { it.copy(yes = yes.trim(), no = no.trim()) }

    fun saveSignal(kidId: String, signal: Signal) = updatePassport(kidId) { p ->
        if (p.signals.any { it.id == signal.id }) p.copy(signals = p.signals.map { if (it.id == signal.id) signal else it })
        else p.copy(signals = p.signals + signal)
    }

    fun removeSignal(kidId: String, id: String) = updatePassport(kidId) { p -> p.copy(signals = p.signals.filter { it.id != id }) }

    fun saveHighlight(kidId: String, index: Int?, text: String) = updatePassport(kidId) { p ->
        val t = text.trim()
        when {
            index == null -> if (t.isEmpty()) p else p.copy(highlights = p.highlights + t)
            t.isEmpty() -> p.copy(highlights = p.highlights.filterIndexed { i, _ -> i != index })
            else -> p.copy(highlights = p.highlights.mapIndexed { i, h -> if (i == index) t else h })
        }
    }

    fun addSection(kidId: String, title: String): String {
        val section = Templates.customSection(title.trim().ifEmpty { "Mi sección" })
        updatePassport(kidId) { it.copy(sections = it.sections + section) }
        return section.id
    }

    fun renameSection(kidId: String, sectionId: String, title: String) = updatePassport(kidId) { p ->
        p.copy(sections = p.sections.map { s ->
            if (s.id != sectionId || title.isBlank()) s
            else s.copy(title = title.trim(), groups = s.groups.mapIndexed { i, g -> if (i == 0 && s.custom) g.copy(title = title.trim()) else g })
        })
    }

    fun deleteSection(kidId: String, sectionId: String) = updatePassport(kidId) { p ->
        p.copy(sections = p.sections.filter { it.id != sectionId || !it.custom })
    }

    fun saveContact(kidId: String, contact: Contact) = updatePassport(kidId) { p ->
        if (p.contacts.any { it.id == contact.id }) p.copy(contacts = p.contacts.map { if (it.id == contact.id) contact else it })
        else p.copy(contacts = p.contacts + contact)
    }

    fun removeContact(kidId: String, id: String) = updatePassport(kidId) { p -> p.copy(contacts = p.contacts.filter { it.id != id }) }

    /** Guarda en el pasaporte lo que Claude ha ordenado a partir de lo que contó la familia. */
    fun applyFindings(kidId: String, findings: List<Finding>) = updatePassport(kidId) { p0 ->
        var p = p0
        findings.forEach { f ->
            p = when (f) {
                is Finding.Item -> {
                    val exists = p.sections.any { s -> s.id == f.sectionId && s.groups.any { it.id == f.groupId } }
                    if (!exists) p else p.copy(sections = p.sections.map { s ->
                        if (s.id != f.sectionId) s else s.copy(groups = s.groups.map { g ->
                            if (g.id == f.groupId && f.text !in g.items) g.copy(items = g.items + f.text) else g
                        })
                    })
                }
                is Finding.NewSignal -> p.copy(signals = p.signals + Signal(newId("sg"), f.hago, f.significa, f.hacer))
                is Finding.YesNo -> p.copy(yes = f.yes.ifBlank { p.yes }, no = f.no.ifBlank { p.no })
                is Finding.Highlight -> if (f.text in p.highlights) p else p.copy(highlights = p.highlights + f.text)
            }
        }
        p
    }

    // ---------- Accesos ----------
    fun answerRequest(requestId: String, access: Access?) {
        val req = state.value.requests.firstOrNull { it.id == requestId }
        val kid = req?.let { kidById(it.kidId) }
        if (kid?.cloudId != null) cloudDo {
            if (access != null) cloud.grant(kid, access)
            cloud.deleteRequest(kid, requestId)
        }
        answerRequestLocal(requestId, access)
    }

    private fun answerRequestLocal(requestId: String, access: Access?) = store.update { s ->
        val req = s.requests.firstOrNull { it.id == requestId } ?: return@update s
        s.copy(
            requests = s.requests.filter { it.id != requestId },
            kids = if (access == null) s.kids else s.kids.map { if (it.id == req.kidId) it.copy(access = listOf(access) + it.access) else it },
        )
    }

    fun restoreRequest(request: AccessRequest, accessId: String?) {
        val kid = kidById(request.kidId)
        if (kid?.cloudId != null) cloudDo {
            accessId?.let { cloud.revoke(kid, it) }
            cloud.restoreRequest(kid, request)
        }
        restoreRequestLocal(request, accessId)
    }

    private fun restoreRequestLocal(request: AccessRequest, accessId: String?) = store.update { s ->
        s.copy(
            requests = listOf(request) + s.requests,
            kids = s.kids.map { k -> if (k.id == request.kidId) k.copy(access = k.access.filter { it.id != accessId }) else k },
        )
    }

    fun saveAccess(kidId: String, access: Access) {
        val kid = kidById(kidId)
        if (kid?.cloudId != null && access.id in cloudAccessIds) cloudDo { cloud.grant(kid, access) }
        saveAccessLocal(kidId, access)
    }

    private fun saveAccessLocal(kidId: String, access: Access) = updateKid(kidId) { k ->
        if (k.access.any { it.id == access.id }) k.copy(access = k.access.map { if (it.id == access.id) access else it })
        else k.copy(access = k.access + access)
    }

    fun removeAccess(kidId: String, id: String) {
        val kid = kidById(kidId)
        if (kid?.cloudId != null && id in cloudAccessIds) cloudDo { cloud.revoke(kid, id) }
        updateKid(kidId) { k -> k.copy(access = k.access.filter { it.id != id }) }
    }

    fun restoreAccess(kidId: String, access: Access, index: Int) {
        val kid = kidById(kidId)
        if (kid?.cloudId != null && access.id in cloudAccessIds) cloudDo { cloud.grant(kid, access) }
        restoreAccessLocal(kidId, access, index)
    }

    private fun restoreAccessLocal(kidId: String, access: Access, index: Int) = updateKid(kidId) { k ->
        k.copy(access = k.access.toMutableList().apply { add(index.coerceIn(0, size), access) })
    }

    // ---------- Propuestas ----------
    fun decide(id: String, yes: Boolean, hago: String? = null, significa: String? = null) {
        val prop = state.value.proposals.firstOrNull { it.id == id }
        val kid = prop?.let { kidById(it.kidId) }
        if (kid?.cloudId != null && id in cloudProposalIds) cloudDo { cloud.setProposalStatus(kid, id, if (yes) "yes" else "no") }
        decideLocal(id, yes, hago, significa)
    }

    private fun decideLocal(id: String, yes: Boolean, hago: String?, significa: String?) = store.update { s ->
        val prop = s.proposals.firstOrNull { it.id == id } ?: return@update s
        val final = prop.copy(hago = hago ?: prop.hago, significa = significa ?: prop.significa, status = if (yes) "yes" else "no")
        s.copy(
            proposals = s.proposals.map { if (it.id == id) final else it },
            kids = if (!yes) s.kids else s.kids.map { k ->
                if (k.id != prop.kidId) k else k.copy(passport = k.passport.copy(signals = k.passport.signals + Signal(newId("sg"), final.hago, final.significa)))
            },
        )
    }

    // ---------- Ajustes y QR ----------
    fun setReadAloud(on: Boolean) = store.update { it.copy(readAloud = on) }
    fun setQr(kidId: String, key: String, on: Boolean) = updateKid(kidId) { it.copy(qrShow = it.qrShow + (key to on)) }

    /** Vuelve al estado inicial de la app (borra todo lo guardado en este móvil). */
    fun resetAll() {
        val dir = File(getApplication<Application>().filesDir, "fotos")
        viewModelScope.launch(Dispatchers.IO) { dir.deleteRecursively() }
        store.update { AppState() }
    }

    fun newAccess(name: String, role: String, permission: String = PERM_VIEW, until: Long? = null) =
        Access(newId("a"), name.trim(), role.trim(), permission, until)

    companion object {
        fun ageLabel(raw: String): String {
            val t = raw.trim()
            return when {
                t.isEmpty() -> ""
                t.all { it.isDigit() } -> if (t == "1") "1 año" else "$t años"
                else -> t
            }
        }
    }
}

/** Una cosa que la app ha entendido de lo que contó la familia. */
sealed class Finding {
    abstract val label: String
    data class Item(val sectionId: String, val groupId: String, val text: String, override val label: String) : Finding()
    data class NewSignal(val hago: String, val significa: String, val hacer: String) : Finding() { override val label = "Mi idioma" }
    data class YesNo(val yes: String, val no: String) : Finding() { override val label = "Sí y no" }
    data class Highlight(val text: String) : Finding() { override val label = "Lo más importante" }
}

data class TalkContext(val sectionId: String?, val prompt: String?)

/** Estado de la cuenta de la familia en la nube. */
data class CloudUi(
    val signedIn: Boolean,
    val email: String?,
    val syncing: Boolean = false,
    val error: String? = null,
    val lastSync: Long = 0,
)

data class KeptFinding(val finding: Finding, val kept: Boolean)

/** Estado de la pantalla «Hablar». step: idle · write · rec · rec-more · thinking · review · error · done */
data class TalkUi(
    val step: String = "idle",
    val topic: String? = null,
    val said: String = "",
    val findings: List<KeptFinding> = emptyList(),
    val followUp: String? = null,
    val error: String? = null,
    val needsKey: Boolean = false,
    val pendingText: String = "",
    val startedAt: Long = 0,
    val savedCount: Int = 0,
)

/** Una pregunta de «Comprueba las respuestas». verdict: null · ok · fix */
data class Check(
    val id: String,
    val question: String,
    val loading: Boolean = false,
    val answer: Answer? = null,
    val error: String? = null,
    val needsKey: Boolean = false,
    val verdict: String? = null,
)
