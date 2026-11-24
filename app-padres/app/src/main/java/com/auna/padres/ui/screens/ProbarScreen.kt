package com.auna.padres.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auna.padres.data.Sample
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

/** «Comprueba las respuestas»: la familia pregunta como si fuera la profe. */
@Composable
fun ProbarScreen(navi: Navi) {
    var q by rememberSaveable { mutableStateOf(-1) }
    var verdict by rememberSaveable { mutableStateOf<String?>(null) }
    var typed by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        BackHeader(
            "Comprueba las respuestas", { navi.back() },
            Modifier.padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 8.dp),
            titleSize = 22, subtitle = "Así le contesta a la profe",
        )

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (q < 0) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Girasol(88.dp)
                    Txt("Pregunta como si fueras la profe", 20, 800, align = TextAlign.Center)
                }
            } else {
                val cur = Sample.qa[q]
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Txt(
                        cur.q, 17, 700,
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
                            .background(Auna.White)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Txt(cur.a, 17, 700, lineHeight = 1.4)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AIcon(Icons.Book, Auna.Muted, 14.dp, 2.4f)
                            Txt(cur.src, 13, 700, Auna.Muted)
                        }
                    }
                }

                when (verdict) {
                    null -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AButton({ verdict = "ok" }, Modifier.weight(1f), minHeight = 64.dp, arrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                            AIcon(Icons.Check, Auna.White, 22.dp, 2.8f)
                            Txt("Está bien", 17, 900, Auna.White)
                        }
                        AButton({ verdict = "fix" }, Modifier.weight(1f), bg = Auna.White, border = Auna.Green, minHeight = 64.dp,
                            arrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                            AIcon(Icons.Pencil, Auna.Green, 22.dp, 2.4f)
                            Txt("Corregir", 17, 900, Auna.Green)
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
                    else -> Row(
                        Modifier
                            .fillMaxWidth()
                            .card(Auna.MintStrong, 18.dp)
                            .clickable(role = Role.Button) { navi.tab(Routes.HABLAR) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(Auna.Leaf), contentAlignment = Alignment.Center) {
                            AIcon(Icons.Mic, Auna.Green, 22.dp, 2.2f)
                        }
                        Txt("Cuéntame cómo es", 17, 800, Auna.DeepGreen, Modifier.weight(1f))
                        AIcon(Icons.ChevronRight, Auna.DeepGreen, 22.dp, 2.5f)
                    }
                }
            }
        }

        Column(Modifier.padding(start = Gutter, end = Gutter, top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Sample.qa.forEachIndexed { i, item ->
                AButton(
                    { q = i; verdict = null },
                    Modifier.fillMaxWidth(),
                    bg = Auna.White, border = if (i == q) Auna.Green else Auna.Border,
                    radius = 16.dp, minHeight = 48.dp,
                    arrangement = Arrangement.Start,
                ) { Txt(item.q, 16, 800) }
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
                    .border(1.dp, Auna.Border, RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AIcon(Icons.Mic, Auna.Green, 22.dp, 2.2f)
                val style = TextStyle(fontFamily = Nunito, fontSize = 16.sp, color = Auna.Ink)
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(Auna.Green),
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Escribe una pregunta" },
                    decorationBox = { inner ->
                        Box {
                            if (typed.isEmpty()) Text("Otra pregunta…", style = style.copy(color = Auna.Muted, fontWeight = FontWeight(600)))
                            inner()
                        }
                    },
                )
            }
            CircleIconButton(Icons.Send, "Enviar", {}, bg = Auna.Green, size = 52.dp, iconColor = Auna.White, stroke = 2.4f)
        }
    }
}
