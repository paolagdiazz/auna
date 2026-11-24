package com.auna.padres.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.auna.padres.AunaViewModel
import com.auna.padres.Ai
import com.auna.padres.Finding
import com.auna.padres.KeptFinding
import com.auna.padres.TalkUi
import com.auna.padres.data.Kid
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AField
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.ChoiceButton
import com.auna.padres.ui.CircleIconButton
import com.auna.padres.ui.EditSheet
import com.auna.padres.ui.Field
import com.auna.padres.ui.Girasol
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.Navi
import com.auna.padres.ui.NoKid
import com.auna.padres.ui.PrimaryButton
import com.auna.padres.ui.Routes
import com.auna.padres.ui.SecondaryButton
import com.auna.padres.ui.Tag
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna
import kotlinx.coroutines.delay

private data class Topic(val id: String, val label: String, val prompt: String)

private fun topics(name: String) = listOf(
    Topic("comunica", "Cómo se comunica", "¿Qué hace $name cuando le duele algo o quiere algo?"),
    Topic("comida", "Comida", "¿Cómo come $name? ¿Necesita ayuda?"),
    Topic("calma", "Qué le calma", "¿Qué le ayuda a tranquilizarse? ¿Y qué le altera?"),
    Topic("salud", "Salud", "¿Qué debe saber alguien sobre su salud?"),
)

/** Hablar → Claude ordena lo que cuentas → revisas → se guarda en el pasaporte. */
@Composable
fun HablarScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val t by vm.talk.collectAsState()
    val ctx by vm.talkContext.collectAsState()
    val kid = s.activeKid
    val context = LocalContext.current
    var lastKid by rememberSaveable { mutableStateOf(kid?.id) }
    var wantMore by remember { mutableStateOf(false) }
    var permError by remember { mutableStateOf<String?>(null) }

    if (kid == null) {
        Column(Modifier.fillMaxSize().padding(Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt("Hablar", 26, 900)
            NoKid({ navi.tab(Routes.PERFILES) })
        }
        return
    }
    LaunchedEffect(kid.id) { if (lastKid != kid.id) { lastKid = kid.id; vm.talkReset() } }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { permError = null; vm.talkStart(wantMore) } else permError = "Sin permiso de micrófono no puedo escucharte. Puedes escribirlo."
    }
    val startRec: (Boolean) -> Unit = { more ->
        wantMore = more
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.talkStart(more)
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    BackHandler(enabled = t.step != "idle") {
        when (t.step) {
            "rec", "rec-more" -> vm.talkCancelRec()
            "review", "done" -> vm.talkReset()
            else -> vm.talkBack()
        }
    }

    val topicList = topics(kid.name)
    val sectionTitle = ctx?.sectionId?.let { kid.passport.section(it)?.title }
    val prompt = ctx?.prompt
        ?: sectionTitle?.let { "Cuéntame sobre «$it»: lo que sepas, como se lo contarías a una amiga." }
        ?: topicList.firstOrNull { it.prompt == t.topic }?.prompt
        ?: topicList.first().prompt
    if (t.topic == null) LaunchedEffect(Unit) { vm.talkTopic(topicList.first().prompt) }

    AnimatedContent(
        targetState = t.step.let { if (it == "rec-more") "rec" else it },
        transitionSpec = { (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 8 }) togetherWith fadeOut(tween(120)) },
        label = "hablar",
    ) { step ->
        Box(Modifier.fillMaxSize().background(Auna.Bg)) {
            when (step) {
                "idle" -> Idle(
                    kid = kid, t = t, topicList = topicList, prompt = prompt, sectionTitle = sectionTitle,
                    permError = permError,
                    onClearSection = { vm.talkContext.value = null },
                    onTopic = { vm.talkTopic(it) },
                    onStart = { startRec(false) },
                    onWrite = { vm.talkWrite() },
                    onKey = { navi.open(Routes.AJUSTES) },
                    onRetry = { vm.talkRetry() },
                )
                "write" -> Write(kid.name, prompt, t.pendingText, onBack = { vm.talkBack() }, onSpeak = { startRec(false) }, onSend = { vm.talkProcess(it) })
                "rec" -> Recording(vm, t, more = t.step == "rec-more", onStop = { vm.talkStop() }, onCancel = { vm.talkCancelRec() })
                "thinking" -> Thinking(t.pendingText)
                "error" -> ErrorView(t, onRetry = { vm.talkRetry() }, onKey = { navi.open(Routes.AJUSTES) }, onBack = { vm.talkBack() })
                "review" -> Review(
                    vm, kid, t,
                    onMore = { startRec(true) },
                    onKey = { navi.open(Routes.AJUSTES) },
                    onSave = { vm.talkSave() },
                    onDiscard = { vm.talkReset() },
                )
                else -> Done(t.savedCount, onPassport = { vm.talkReset(); navi.tab(Routes.PASAPORTE) }, onAgain = { vm.talkReset() })
            }
        }
    }
}

@Composable
private fun KeyNeeded(onKey: () -> Unit, onRetry: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth().card(Auna.CoralBg, 20.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Txt("Falta conectar la IA", 18, 900, Auna.Coral)
        Txt(
            "Para ordenar lo que cuentas, Aúna usa una IA. En Ajustes, entra en la cuenta de la familia o pega una clave (la de Groq es gratis). Lo que has contado no se pierde.",
            15, 700, Auna.Coral, lineHeight = 1.35,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Añadir la clave", onKey, Modifier.weight(1f), height = 48.dp, fontSize = 15, radius = 14.dp)
            if (onRetry != null) SecondaryButton("Reintentar", onRetry, Modifier.weight(1f), height = 48.dp, fontSize = 15, radius = 14.dp)
        }
    }
}

@Composable
private fun ErrorCard(text: String) {
    Row(
        Modifier.fillMaxWidth().card(Auna.CoralBg, 18.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AIcon(Icons.Alert, Auna.Coral, 22.dp, 2.2f)
        Txt(text, 15, 800, Auna.Coral, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Idle(
    kid: Kid,
    t: TalkUi,
    topicList: List<Topic>,
    prompt: String,
    sectionTitle: String?,
    permError: String?,
    onClearSection: () -> Unit,
    onTopic: (String) -> Unit,
    onStart: () -> Unit,
    onWrite: () -> Unit,
    onKey: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Txt("Hablar de ${kid.name}", 26, 900)
        if (sectionTitle != null) {
            Row(
                Modifier.card(Auna.Mint, 22.dp).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Txt("Para «$sectionTitle»", 16, 800, Auna.Green)
                CircleIconButton(Icons.Close, "Quitar la sección", onClearSection, bg = Auna.Mint, size = 40.dp, iconSize = 16.dp, iconColor = Auna.Green)
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                topicList.forEach { tp ->
                    ChoiceButton(tp.label, tp.prompt == t.topic, { onTopic(tp.prompt) }, height = 44.dp, radius = 22.dp, fontSize = 16)
                }
            }
        }
        PromptCard(prompt, 44, 18)
        if (t.needsKey) KeyNeeded(onKey, if (t.pendingText.isNotBlank()) onRetry else null)
        (permError ?: t.error)?.let { ErrorCard(it) }
        Column(
            Modifier.fillMaxWidth().heightIn(min = 240.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .size(156.dp)
                    .clip(CircleShape)
                    .background(Auna.MintStrong)
                    .clickable(role = Role.Button, onClick = onStart)
                    .semantics { contentDescription = "Empezar a hablar" }
                    .padding(14.dp)
                    .clip(CircleShape)
                    .background(Auna.Leaf),
                contentAlignment = Alignment.Center,
            ) { AIcon(Icons.Mic, Auna.Green, 56.dp, 2f) }
            Txt("Toca y habla", 18, 800)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AButton(onWrite, bg = Auna.White, border = Auna.Border, radius = 24.dp, minHeight = 48.dp, hPadding = 20.dp, arrangement = Arrangement.spacedBy(8.dp)) {
                AIcon(Icons.Pencil, Auna.Green, 20.dp, 2.2f)
                Txt("Prefiero escribir", 16, 800, Auna.Green)
            }
        }
    }
}

@Composable
private fun PromptCard(prompt: String, girasol: Int, fontSize: Int, bg: androidx.compose.ui.graphics.Color = Auna.White) {
    Row(
        Modifier.fillMaxWidth().card(bg, 20.dp).padding(horizontal = 16.dp, vertical = if (girasol > 40) 16.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Girasol(girasol.dp)
        Txt(prompt, fontSize, 800, lineHeight = 1.3, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Write(name: String, prompt: String, initial: String, onBack: () -> Unit, onSpeak: () -> Unit, onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    Column(
        Modifier.fillMaxSize().padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIconButton(Icons.ChevronLeft, "Volver", onBack)
            Txt("Escribir sobre $name", 24, 900)
        }
        PromptCard(prompt, 40, 17)
        AField(
            value = text,
            onValueChange = { text = it },
            placeholder = "Escribe como si se lo contaras a una amiga…",
            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 160.dp).semantics { contentDescription = "Lo que quieres contar" },
            fontSize = 18, weight = 600, radius = 20.dp, minHeight = 160.dp, bg = Auna.White,
            singleLine = false, contentAlignment = Alignment.TopStart, padding = 16.dp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Cuando se tira de…", "Le calma…", "No le gusta…", "Si tiene una crisis…").forEach { st ->
                AButton(
                    { text = (if (text.isNotEmpty()) "$text " else "") + st.replace("…", " ") },
                    bg = Auna.White, border = Auna.Border, radius = 22.dp, minHeight = 44.dp, hPadding = 14.dp,
                ) { Txt(st, 15, 800) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AButton(onSpeak, Modifier.width(56.dp), bg = Auna.Leaf, hPadding = 0.dp, contentDescription = "Mejor hablar") {
                AIcon(Icons.Mic, Auna.Green, 24.dp, 2.2f)
            }
            PrimaryButton("Enviar", { if (text.isNotBlank()) onSend(text) }, Modifier.weight(1f).alpha(if (text.isBlank()) 0.5f else 1f))
        }
    }
}

@Composable
private fun Recording(vm: AunaViewModel, t: TalkUi, more: Boolean, onStop: () -> Unit, onCancel: () -> Unit) {
    val text by vm.voice.text.collectAsState()
    val partial by vm.voice.partial.collectAsState()
    val level by vm.voice.level.collectAsState()
    val error by vm.voice.error.collectAsState()
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(t.startedAt) {
        while (true) { elapsed = (System.currentTimeMillis() - t.startedAt) / 1000; delay(250) }
    }
    val heights = listOf(36, 60, 44, 72, 40, 64, 30)
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(120); tick++ } }

    Column(
        Modifier.fillMaxSize().padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Auna.Dot))
            Txt(if (more) "Escuchando tu respuesta…" else "Escuchando…", 16, 800, Auna.Muted, Modifier.weight(1f))
            AButton(onCancel, bg = Auna.White, border = Auna.Border, radius = 20.dp, minHeight = 40.dp, hPadding = 14.dp) { Txt("Cancelar", 15, 800) }
        }
        Txt("%02d:%02d".format(elapsed / 60, elapsed % 60), 56, 900)
        Row(Modifier.height(72.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            heights.forEachIndexed { i, h ->
                val wobble = 0.55f + 0.45f * (((tick + i * 3) % 7) / 6f)
                val target = (0.18f + 0.82f * level * wobble).coerceIn(0.12f, 1f)
                val scale by animateFloatAsState(target, tween(120), label = "barra")
                Box(Modifier.width(8.dp).height((h * scale).dp).clip(RoundedCornerShape(4.dp)).background(Auna.Green))
            }
        }
        val live = listOf(text, partial).filter { it.isNotBlank() }.joinToString(" ")
        Txt(
            if (live.isBlank()) "Habla con normalidad. Puedes hacer pausas." else "«$live»",
            17, if (live.isBlank()) 600 else 700, if (live.isBlank()) Auna.Muted else Auna.Ink,
            Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 80.dp).card(Auna.White, 20.dp).padding(16.dp).verticalScroll(rememberScrollState()),
            lineHeight = 1.4,
        )
        error?.let { ErrorCard(it) }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Auna.Mint)
                .clickable(role = Role.Button, onClick = onStop)
                .semantics { contentDescription = "Terminar" }
                .padding(12.dp)
                .clip(CircleShape)
                .background(Auna.Green),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Auna.White)) }
        Txt("Terminar", 18, 800)
    }
}

@Composable
private fun Thinking(said: String) {
    Column(
        Modifier.fillMaxSize().padding(Gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(120.dp), color = Auna.Leaf, trackColor = Auna.Mint, strokeWidth = 8.dp)
            Girasol(72.dp)
        }
        Txt("Ordenando lo que has contado…", 20, 900, align = TextAlign.Center)
        if (said.isNotBlank()) {
            Txt("«${said.trim()}»", 16, 600, Auna.Muted, Modifier.fillMaxWidth().card(Auna.White, 20.dp).padding(16.dp), lineHeight = 1.4)
        }
    }
}

@Composable
private fun ErrorView(t: TalkUi, onRetry: () -> Unit, onKey: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Gutter),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIconButton(Icons.ChevronLeft, "Volver", onBack)
            Txt("No se ha podido ordenar", 22, 900)
        }
        t.error?.let { ErrorCard(it) }
        if (t.needsKey) KeyNeeded(onKey, onRetry)
        if (t.pendingText.isNotBlank()) {
            Txt("Lo que has contado (no se pierde):", 15, 800, Auna.Muted)
            Txt("«${t.pendingText.trim()}»", 16, 600, Auna.Ink, Modifier.fillMaxWidth().card(Auna.White, 20.dp).padding(16.dp), lineHeight = 1.4)
        }
        if (!t.needsKey) PrimaryButton("Reintentar", onRetry, Modifier.fillMaxWidth())
    }
}

private fun findingText(f: Finding): String = when (f) {
    is Finding.Item -> f.text
    is Finding.NewSignal -> "${f.hago} → ${f.significa}" + if (f.hacer.isNotBlank()) ". ${f.hacer}" else ""
    is Finding.YesNo -> listOfNotNull(f.yes.takeIf { it.isNotBlank() }?.let { "Sí: $it" }, f.no.takeIf { it.isNotBlank() }?.let { "No: $it" }).joinToString(" · ")
    is Finding.Highlight -> f.text
}

@Composable
private fun Review(vm: AunaViewModel, kid: Kid, t: TalkUi, onMore: () -> Unit, onKey: () -> Unit, onSave: () -> Unit, onDiscard: () -> Unit) {
    var editing by remember { mutableStateOf<Int?>(null) }
    var showSaid by remember { mutableStateOf(false) }
    val kept = t.findings.count { it.kept }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Txt("Esto he entendido", 26, 900, modifier = Modifier.weight(1f))
            AButton(onDiscard, bg = Auna.Bg, radius = 20.dp, minHeight = 40.dp, hPadding = 10.dp) { Txt("Descartar", 15, 800, Auna.Forest) }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (t.findings.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().card(Auna.White, 20.dp).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Txt("No he encontrado nada nuevo para el pasaporte.", 17, 800)
                    Txt("Prueba a contar algo concreto: qué hace ${kid.name}, qué significa y qué hay que hacer.", 15, 600, Auna.Muted, lineHeight = 1.35)
                }
            }
            t.findings.forEachIndexed { i, kf -> FindingCard(kf, onToggle = { vm.talkToggle(i) }, onEdit = { editing = i }) }

            t.error?.let { ErrorCard(it) }
            if (t.needsKey) KeyNeeded(onKey, null)

            t.followUp?.let { q ->
                Row(
                    Modifier.fillMaxWidth().card(Auna.MintStrong, 20.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Girasol(40.dp)
                    Txt(q, 17, 800, Auna.DeepGreen, Modifier.weight(1f))
                    CircleIconButton(Icons.Mic, "Responder hablando", onMore, bg = Auna.Leaf, iconColor = Auna.Green, stroke = 2.2f)
                }
            }

            if (t.said.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button) { showSaid = !showSaid }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AIcon(if (showSaid) Icons.ChevronUp else Icons.ChevronDown, Auna.Muted, 18.dp, 2.5f)
                    Txt(if (showSaid) "Ocultar lo que has dicho" else "Ver lo que has dicho", 15, 800, Auna.Muted)
                }
                if (showSaid) Txt("«${t.said.trim()}»", 15, 600, Auna.Muted, Modifier.fillMaxWidth().card(Auna.White, 18.dp).padding(14.dp), lineHeight = 1.4)
            }
        }
        PrimaryButton(
            if (kept == 0) "Nada que guardar" else if (kept == 1) "Guardar 1 cosa en el pasaporte" else "Guardar $kept cosas en el pasaporte",
            { if (kept > 0) onSave() },
            Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 16.dp).alpha(if (kept == 0) 0.5f else 1f),
        )
    }

    editing?.let { i ->
        val kf = t.findings.getOrNull(i) ?: return@let
        FindingEditor(kid, kf.finding, onDismiss = { editing = null }) { vm.talkEdit(i, it); editing = null }
    }
}

@Composable
private fun FindingCard(kf: KeptFinding, onToggle: () -> Unit, onEdit: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().alpha(if (kf.kept) 1f else 0.45f).card(Auna.White, 20.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tag(kf.finding.label, Auna.Mint, Auna.Green)
        Txt(findingText(kf.finding), 17, 700, lineHeight = 1.35)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AButton(onToggle, bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 44.dp) {
                Txt(if (kf.kept) "Quitar" else "Recuperar", 15, 800)
            }
            AButton(onEdit, bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 44.dp) { Txt("Editar", 15, 800) }
        }
    }
}

/** Editar una cosa entendida antes de guardarla (texto y, si es un apartado, dónde va). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FindingEditor(kid: Kid, f: Finding, onDismiss: () -> Unit, onSave: (Finding) -> Unit) {
    when (f) {
        is Finding.Item -> {
            val targets = Ai.targets(kid)
            var target by remember { mutableStateOf("${f.sectionId}/${f.groupId}") }
            EditSheet(
                title = "Editar",
                fields = listOf(Field("Texto", f.text)),
                onDismiss = onDismiss,
                onSave = { (txt) ->
                    val (sec, grp) = target.split("/", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                    if (txt.isNotBlank()) onSave(Finding.Item(sec, grp, txt.trim(), targets[target] ?: f.label))
                },
                extra = {
                    Txt("¿Dónde va?", 15, 800, Auna.Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        targets.forEach { (key, label) -> ChoiceButton(label, key == target, { target = key }, height = 44.dp, fontSize = 14) }
                    }
                },
            )
        }
        is Finding.NewSignal -> EditSheet(
            title = "Editar señal",
            fields = listOf(Field("Lo que hace", f.hago), Field("Qué significa", f.significa), Field("Qué hacer", f.hacer, multiline = true)),
            onDismiss = onDismiss,
            onSave = { (h, sg, hc) -> if (h.isNotBlank() && sg.isNotBlank()) onSave(Finding.NewSignal(h.trim(), sg.trim(), hc.trim())) },
        )
        is Finding.YesNo -> EditSheet(
            title = "Cómo dice sí y no",
            fields = listOf(Field("Para decir «sí»", f.yes), Field("Para decir «no»", f.no)),
            onDismiss = onDismiss,
            onSave = { (y, n) -> onSave(Finding.YesNo(y.trim(), n.trim())) },
        )
        is Finding.Highlight -> EditSheet(
            title = "Lo más importante",
            fields = listOf(Field("Texto", f.text)),
            onDismiss = onDismiss,
            onSave = { (txt) -> if (txt.isNotBlank()) onSave(Finding.Highlight(txt.trim())) },
        )
    }
}

@Composable
private fun Done(kept: Int, onPassport: () -> Unit, onAgain: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Spacer(Modifier.height(80.dp))
        Box(Modifier.size(120.dp).clip(CircleShape).background(Auna.Mint), contentAlignment = Alignment.Center) {
            AIcon(Icons.Check, Auna.Green, 60.dp, 2.6f)
        }
        Txt("¡Guardado!", 28, 900)
        Txt(
            if (kept == 1) "1 cosa nueva en el pasaporte" else "$kept cosas nuevas en el pasaporte",
            18, 600, Auna.Muted, align = TextAlign.Center,
        )
        Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Ver pasaporte", onPassport, Modifier.fillMaxWidth())
            SecondaryButton("Contar algo más", onAgain, Modifier.fillMaxWidth())
        }
    }
}
