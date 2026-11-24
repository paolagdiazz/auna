package com.auna.padres.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.data.Sample
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AField
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.ChoiceButton
import com.auna.padres.ui.CircleIconButton
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
import kotlin.math.PI
import kotlin.math.cos

/**
 * Hablar → Revisar → Guardar. La grabación está simulada (no usa el micrófono):
 * al terminar se muestra lo que la app «ha entendido» con los ejemplos del diseño.
 */
@Composable
fun HablarScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val kid = s.activeKid
    var step by rememberSaveable { mutableStateOf("idle") }
    var topic by rememberSaveable { mutableStateOf("comunica") }
    var text by rememberSaveable { mutableStateOf("") }
    var sec by rememberSaveable { mutableIntStateOf(0) }
    var removed by remember { mutableStateOf(setOf<String>()) }

    if (kid == null) {
        Column(Modifier.fillMaxSize().padding(Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt("Hablar", 26, 900)
            NoKid({ navi.tab(Routes.PERFILES) })
        }
        return
    }

    LaunchedEffect(step) {
        if (step == "rec") {
            while (true) {
                delay(1000)
                sec += 1
            }
        }
    }

    BackHandler(enabled = step != "idle") { step = "idle" }

    val prompt = Sample.topics.first { it.first == topic }.third.replace("{n}", kid.name)
    val start = { sec = 0; step = "rec" }

    when (step) {
        "idle" -> Idle(kid.name, topic, prompt, onTopic = { topic = it }, onStart = start, onWrite = { step = "write" })
        "write" -> Write(
            name = kid.name, prompt = prompt, text = text,
            onText = { text = it },
            onBack = { step = "idle" },
            onStarter = { st -> text = (if (text.isNotEmpty()) "$text " else "") + st.replace("…", " ") },
            onSpeak = start,
            onSend = { step = "review" },
        )
        "rec" -> Recording(sec, onStop = { step = "review" })
        "review" -> Review(
            removed = removed,
            onToggle = { id -> removed = if (id in removed) removed - id else removed + id },
            onSpeak = start,
            onSave = { step = "done" },
        )
        else -> Done(
            kept = Sample.reviewItems.count { it.first !in removed },
            onPassport = { navi.tab(Routes.PASAPORTE) },
            onAgain = { step = "idle"; removed = emptySet(); sec = 0; text = "" },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Idle(name: String, topic: String, prompt: String, onTopic: (String) -> Unit, onStart: () -> Unit, onWrite: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Txt("Hablar de $name", 26, 900)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Sample.topics.forEach { (id, label, _) ->
                ChoiceButton(label, id == topic, { onTopic(id) }, height = 44.dp, radius = 22.dp, fontSize = 16)
            }
        }
        PromptCard(prompt, 44, 18)
        Column(
            Modifier.fillMaxWidth().heightIn(min = 260.dp),
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
            AButton(onWrite, bg = Auna.White, border = Auna.Border, radius = 24.dp, minHeight = 48.dp, hPadding = 20.dp,
                arrangement = Arrangement.spacedBy(8.dp)) {
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
private fun Write(
    name: String,
    prompt: String,
    text: String,
    onText: (String) -> Unit,
    onBack: () -> Unit,
    onStarter: (String) -> Unit,
    onSpeak: () -> Unit,
    onSend: () -> Unit,
) {
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
            onValueChange = onText,
            placeholder = "Escribe como si se lo contaras a una amiga…",
            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 200.dp).semantics { contentDescription = "Lo que quieres contar" },
            fontSize = 18, weight = 600, radius = 20.dp, minHeight = 200.dp, bg = Auna.White,
            singleLine = false, contentAlignment = Alignment.TopStart, padding = 16.dp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Sample.starters.forEach { st ->
                AButton({ onStarter(st) }, bg = Auna.White, border = Auna.Border, radius = 22.dp, minHeight = 44.dp, hPadding = 14.dp) {
                    Txt(st, 15, 800)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AButton(onSpeak, Modifier.width(56.dp), bg = Auna.Leaf, hPadding = 0.dp, contentDescription = "Mejor hablar") {
                AIcon(Icons.Mic, Auna.Green, 24.dp, 2.2f)
            }
            PrimaryButton("Enviar", onSend, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Recording(sec: Int, onStop: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "onda")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "fase",
    )
    val heights = listOf(36, 60, 44, 72, 40, 64, 30)

    Column(
        Modifier.fillMaxSize().padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Auna.Dot))
            Txt("Escuchando…", 16, 800, Auna.Muted)
        }
        Txt("%02d:%02d".format(sec / 60, sec % 60), 56, 900)
        Row(Modifier.height(72.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            heights.forEachIndexed { i, h ->
                val p = ((phase - i * 0.15f / 0.9f) % 1f + 1f) % 1f
                val scale = 0.4f + 0.6f * (0.5f - 0.5f * cos(2 * PI * p).toFloat())
                Box(
                    Modifier
                        .width(8.dp)
                        .height(h.dp)
                        .graphicsLayer { scaleY = scale }
                        .clip(RoundedCornerShape(4.dp))
                        .background(Auna.Green),
                )
            }
        }
        Txt(
            "\"Cuando se tira de la oreja y llora, casi siempre es que le duele algo…\"",
            17, 400, Auna.Muted, Modifier.fillMaxWidth().card(Auna.White, 20.dp).padding(16.dp), lineHeight = 1.4,
        )
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
private fun Review(removed: Set<String>, onToggle: (String) -> Unit, onSpeak: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Txt("Esto he entendido", 26, 900, modifier = Modifier.padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 8.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Sample.reviewItems.forEach { (id, tag, txt) ->
                val off = id in removed
                Column(
                    Modifier.fillMaxWidth().alpha(if (off) 0.45f else 1f).card(Auna.White, 20.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Tag(tag, Auna.Mint, Auna.Green)
                    Txt(txt, 17, 700, lineHeight = 1.35)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AButton({ onToggle(id) }, bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 44.dp) {
                            Txt(if (off) "Recuperar" else "Quitar", 15, 800)
                        }
                        AButton({}, bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 44.dp) { Txt("Editar", 15, 800) }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().card(Auna.MintStrong, 20.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Girasol(40.dp)
                Txt("¿Hay algo que le dé miedo?", 17, 800, Auna.DeepGreen, Modifier.weight(1f))
                CircleIconButton(Icons.Mic, "Responder hablando", onSpeak, bg = Auna.Leaf, iconColor = Auna.Green, stroke = 2.2f)
            }
        }
        PrimaryButton(
            "Guardar en el pasaporte", onSave,
            Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 16.dp),
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
