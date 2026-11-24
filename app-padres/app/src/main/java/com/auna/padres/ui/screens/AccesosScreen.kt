package com.auna.padres.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.data.Sample
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.ChoiceButton
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.Initial
import com.auna.padres.ui.PrimaryButton
import com.auna.padres.ui.Tag
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna

@Composable
fun AccesosScreen(vm: AunaViewModel) {
    val s by vm.state.collectAsState()
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var custom by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Txt("Accesos", 26, 900)

        if (s.requestPending) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Auna.White)
                    .border(3.dp, Auna.CoralBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Initial("P", 52.dp, Auna.MintStrong, Auna.DeepGreen, 22)
                    Column(Modifier.weight(1f)) {
                        Txt("Pedro Ruiz", 18, 900)
                        Txt("Profe sustituto · CEE San José", 15, 600, Auna.Muted)
                    }
                    Tag("NUEVA", Auna.CoralBg, Auna.Coral, fontSize = 12, weight = 900, hPad = 8.dp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AButton({ vm.answerRequest("no") }, Modifier.weight(1f), bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 52.dp, hPadding = 6.dp) {
                        Txt("No", 15, 800)
                    }
                    AButton({ vm.answerRequest("day") }, Modifier.weight(1f), bg = Auna.White, border = Auna.Green, radius = 14.dp, minHeight = 52.dp, hPadding = 6.dp) {
                        Txt("Solo hoy", 15, 800, Auna.Green)
                    }
                    AButton(
                        { custom = !custom },
                        Modifier.weight(1f).semantics { stateDescription = if (custom) "Abierto" else "Cerrado" },
                        bg = Auna.CoralBg, border = Auna.CoralBorder, radius = 14.dp, minHeight = 52.dp, hPadding = 6.dp,
                    ) { Txt("Personalizar", 15, 800, Auna.Coral) }
                }
                if (custom) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Auna.Line))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Txt("¿Cuánto tiempo?", 15, 800, Auna.Muted)
                        Sample.durations.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { d ->
                                    ChoiceButton(
                                        d, s.requestDuration == d, { vm.setDuration(d) }, Modifier.weight(1f),
                                        selBg = Auna.CoralBg, selBorder = Auna.Coral, borderColor = Auna.CoralBorder,
                                    )
                                }
                            }
                        }
                        Txt("¿Qué puede hacer?", 15, 800, Auna.Muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Sample.roles.forEach { (id, label) ->
                                ChoiceButton(
                                    label, s.requestRole == id, { vm.setRole(id) }, Modifier.weight(1f),
                                    selBg = Auna.CoralBg, selBorder = Auna.Coral, borderColor = Auna.CoralBorder,
                                )
                            }
                        }
                        PrimaryButton(
                            "Dar acceso", { vm.answerRequest("custom"); custom = false },
                            Modifier.fillMaxWidth(), height = 52.dp, fontSize = 16, radius = 14.dp,
                        )
                    }
                }
            }
        } else {
            val toast = when (s.request) {
                "day" -> "Pedro puede verlo hoy"
                "custom" -> "Pedro puede verlo: ${s.requestDuration}"
                else -> "Solicitud rechazada"
            }
            Row(
                Modifier.fillMaxWidth().card(Auna.Mint, 16.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AIcon(Icons.Check, Auna.Green, 18.dp, 2.8f)
                Txt(toast, 16, 800, Auna.Green, Modifier.weight(1f))
                AButton({ vm.undoRequest(); custom = false }, bg = Auna.CoralBg, radius = 20.dp, minHeight = 40.dp, hPadding = 14.dp) {
                    Txt("Deshacer", 15, 900, Auna.Coral)
                }
            }
        }

        Column(Modifier.fillMaxWidth().card(Auna.White, 22.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
            s.accessPeople().forEach { p ->
                val isOpen = open == p.id
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 72.dp)
                            .clickable(role = Role.Button) { open = if (isOpen) null else p.id }
                            .semantics { stateDescription = if (isOpen) "Abierto" else "Cerrado" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Initial(p.initial, 44.dp, if (p.warn) Auna.MintStrong else Auna.Mint, if (p.warn) Auna.DeepGreen else Auna.Green)
                        Column(Modifier.weight(1f)) {
                            Txt(p.name, 17, 800)
                            Txt(p.meta, 14, 700, if (p.warn) Auna.Forest else Auna.Muted)
                        }
                        AIcon(if (isOpen) Icons.ChevronUp else Icons.ChevronDown, Auna.Muted, 20.dp, 2.5f)
                    }
                    if (isOpen) {
                        Row(Modifier.padding(start = 56.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AButton({}, bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 44.dp, hPadding = 14.dp) {
                                Txt("Cambiar", 15, 800)
                            }
                            AButton({ vm.removeAccess(p.id); open = null }, bg = Auna.White, border = Auna.Forest, radius = 14.dp, minHeight = 44.dp, hPadding = 14.dp) {
                                Txt("Quitar acceso", 15, 800, Auna.Forest)
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Auna.Line))
                }
            }
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Auna.Line), contentAlignment = Alignment.Center) {
                    AIcon(Icons.Scan, Auna.Ink, 20.dp, 2.2f)
                }
                Column(Modifier.weight(1f)) {
                    Txt("Quien escanee el QR", 17, 800)
                    Txt("Solo emergencia", 14, 700, Auna.Muted)
                }
            }
        }

        PrimaryButton("Invitar", {}, Modifier.fillMaxWidth(), radius = 18.dp, icon = Icons.Plus)
    }
}
