package com.auna.padres.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.auna.padres.AunaViewModel
import com.auna.padres.Check
import com.auna.padres.TalkContext
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.BackHeader
import com.auna.padres.ui.CircleIconButton
import com.auna.padres.ui.Girasol
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.Navi
import com.auna.padres.ui.Routes
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna
import com.auna.padres.ui.theme.Nunito

/** «Comprueba las respuestas»: la familia pregunta como si fuera la profe y Claude responde con el pasaporte. */
@Composable
fun ProbarScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val checks by vm.checks.collectAsState()
    val listening by vm.voice.listening.collectAsState()
    val heard by vm.voice.text.collectAsState()
    val partial by vm.voice.partial.collectAsState()
    val kid = s.activeKid
    val name = kid?.name ?: "el niño"
    val context = LocalContext.current
    var typed by rememberSaveable { mutableStateOf("") }
    var dictating by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(checks.size) { if (checks.isNotEmpty()) listState.animateScrollToItem(checks.lastIndex) }
    // Al terminar el dictado, lo reconocido pasa al cuadro de texto.
    LaunchedEffect(listening) {
        if (!listening && dictating) {
            dictating = false
            val t = heard.trim()
            if (t.isNotEmpty()) typed = t
        }
    }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) { dictating = true; vm.voice.start(continuous = false) } }
    val startDictation = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            dictating = true; vm.voice.start(continuous = false)
        } else mic.launch(Manifest.permission.RECORD_AUDIO)
    }
    val send = { q: String -> if (q.isNotBlank()) { vm.ask(q); typed = "" } }

    val suggestions = listOf(
        "¿Cómo sé si a $name le duele algo?",
        "¿Qué hago si se pone nervioso?",
        "¿Qué puede comer y cómo?",
    )

    Column(Modifier.fillMaxSize()) {
        BackHeader(
            "Comprueba las respuestas", { navi.back() },
            Modifier.padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 8.dp),
            titleSize = 22, subtitle = "Así le contesta a la profe",
        ) {
            if (checks.isNotEmpty()) CircleIconButton(Icons.Trash, "Borrar la conversación", { vm.clearChecks() }, iconColor = Auna.Muted, stroke = 2.2f)
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Gutter, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (checks.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Girasol(88.dp)
                        Txt("Pregunta como si fueras la profe", 20, 800, align = TextAlign.Center)
                        Txt("Responde solo con lo que pone en el pasaporte de $name.", 15, 600, Auna.Muted, align = TextAlign.Center)
                    }
                }
            }
            items(checks, key = { it.id }) { c ->
                Exchange(
                    c,
                    onOk = { vm.setVerdict(c.id, "ok") },
                    onFix = {
                        vm.setVerdict(c.id, "fix")
                        vm.talkReset()
                        vm.talkContext.value = TalkContext(null, "La profe preguntó: «${c.question}». ¿Cómo es de verdad? Cuéntamelo.")
                        navi.tab(Routes.HABLAR)
                    },
                    onRetry = { vm.retryCheck(c.id) },
                    onKey = { navi.open(Routes.AJUSTES) },
                )
            }
        }

        if (checks.isEmpty()) {
            Column(Modifier.padding(start = Gutter, end = Gutter, top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { q ->
                    AButton({ send(q) }, Modifier.fillMaxWidth(), bg = Auna.White, border = Auna.Border, radius = 16.dp, minHeight = 48.dp, arrangement = Arrangement.Start) {
                        Txt(q, 16, 800)
                    }
                }
            }
        }

        Row(
            Modifier.padding(start = Gutter, end = Gutter, top = 12.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Auna.White)
                    .border(if (listening) 2.dp else 1.dp, if (listening) Auna.Green else Auna.Border, RoundedCornerShape(26.dp))
                    .padding(start = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircleIconButton(
                    if (listening) Icons.Close else Icons.Mic,
                    if (listening) "Parar el dictado" else "Preguntar hablando",
                    { if (listening) vm.voice.stop() else startDictation() },
                    bg = if (listening) Auna.Mint else Auna.White, size = 44.dp, iconColor = Auna.Green, stroke = 2.2f,
                )
                val style = TextStyle(fontFamily = Nunito, fontSize = 16.sp, color = Auna.Ink)
                val shown = if (listening) listOf(heard, partial).filter { it.isNotBlank() }.joinToString(" ") else typed
                BasicTextField(
                    value = shown,
                    onValueChange = { if (!listening) typed = it },
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(Auna.Green),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(typed) }),
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Escribe una pregunta" },
                    decorationBox = { inner ->
                        Box {
                            if (shown.isEmpty()) Text(if (listening) "Te escucho…" else "Otra pregunta…", style = style.copy(color = Auna.Muted, fontWeight = FontWeight(600)))
                            inner()
                        }
                    },
                )
            }
            CircleIconButton(Icons.Send, "Enviar", { send(typed) }, bg = Auna.Green, size = 52.dp, iconColor = Auna.White, stroke = 2.4f)
        }
    }
}

@Composable
private fun Exchange(c: Check, onOk: () -> Unit, onFix: () -> Unit, onRetry: () -> Unit, onKey: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Txt(
                c.question, 17, 700,
                modifier = Modifier
                    .widthIn(max = maxWidth * 0.8f)
                    .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                    .background(Auna.Mint)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Girasol(40.dp)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                    .background(if (c.error != null) Auna.CoralBg else Auna.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    c.loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Auna.Green, strokeWidth = 2.5.dp)
                        Txt("Buscando en el pasaporte…", 16, 700, Auna.Muted)
                    }
                    c.error != null -> {
                        Txt(c.error, 16, 800, Auna.Coral, lineHeight = 1.35)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (c.needsKey) AButton(onKey, radius = 14.dp, minHeight = 44.dp) { Txt("Añadir la clave", 15, 900, Auna.White) }
                            AButton(onRetry, bg = Auna.White, border = Auna.Coral, radius = 14.dp, minHeight = 44.dp) { Txt("Reintentar", 15, 800, Auna.Coral) }
                        }
                    }
                    c.answer != null -> {
                        Txt(c.answer.text, 17, 700, lineHeight = 1.4)
                        val src = if (!c.answer.known) "El pasaporte aún no lo dice" else c.answer.sources.joinToString(" y ").ifBlank { "Pasaporte" }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AIcon(Icons.Book, Auna.Muted, 14.dp, 2.4f)
                            Txt(src, 13, 700, Auna.Muted)
                        }
                    }
                }
            }
        }
        if (c.answer != null) {
            when (c.verdict) {
                null -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AButton(onOk, Modifier.weight(1f), minHeight = 56.dp, arrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        AIcon(Icons.Check, Auna.White, 22.dp, 2.8f)
                        Txt("Está bien", 17, 900, Auna.White)
                    }
                    AButton(onFix, Modifier.weight(1f), bg = Auna.White, border = Auna.Green, minHeight = 56.dp, arrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        AIcon(Icons.Pencil, Auna.Green, 22.dp, 2.4f)
                        Txt(if (c.answer.known) "Corregir" else "Contárselo", 17, 900, Auna.Green)
                    }
                }
                "ok" -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(
                        Modifier.card(Auna.Mint, 16.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AIcon(Icons.Check, Auna.Green, 18.dp, 2.8f)
                        Txt("¡Gracias! Respuesta correcta", 16, 800, Auna.Green)
                    }
                }
                else -> Txt("Lo has corregido en «Hablar»", 14, 700, Auna.Muted, Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
        }
    }
}
