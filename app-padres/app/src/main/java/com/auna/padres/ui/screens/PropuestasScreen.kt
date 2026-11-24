package com.auna.padres.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.data.Proposal
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.BackHeader
import com.auna.padres.ui.EditSheet
import com.auna.padres.ui.Field
import com.auna.padres.ui.Girasol
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.Initial
import com.auna.padres.ui.Navi
import com.auna.padres.ui.Photo
import com.auna.padres.ui.PrimaryButton
import com.auna.padres.ui.Routes
import com.auna.padres.ui.Tag
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna

/** Propuestas de cambios que hacen profes y terapeutas. La familia decide. */
@Composable
fun PropuestasScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val kidId = s.activeKid?.id
    val pending = s.pendingProposals(kidId)
    val done = s.reviewedProposals(kidId)
    var editing by remember { mutableStateOf<Proposal?>(null) }

    Column(Modifier.fillMaxSize()) {
        BackHeader(
            "Propuestas", { navi.back() },
            Modifier.padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 8.dp),
        ) {
            if (pending.isNotEmpty()) {
                Tag("${pending.size} por revisar", Auna.CoralBg, Auna.Coral, fontSize = 14, weight = 900, radius = 12.dp, hPad = 12.dp, vPad = 4.dp)
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            pending.forEach { p ->
                ProposalCard(p, onNo = { vm.decide(p.id, false) }, onEdit = { editing = p }, onYes = { vm.decide(p.id, true) })
            }

            if (pending.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().card(Auna.White, 24.dp).padding(horizontal = 16.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Girasol(88.dp)
                    Txt("Todo al día", 22, 900)
                    PrimaryButton("Ver pasaporte", { navi.tab(Routes.PASAPORTE) }, height = 52.dp, fontSize = 17, radius = 16.dp)
                }
            }

            if (done.isNotEmpty()) {
                Txt("Revisadas", 13, 900, Auna.Muted, upper = true)
                done.forEach { d ->
                    val yes = d.status == "yes"
                    val color = if (yes) Auna.Green else Auna.Muted
                    Row(
                        Modifier.fillMaxWidth().card(if (yes) Auna.Mint else Auna.Line, 16.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Txt(d.hago, 16, 800, color, Modifier.weight(1f))
                        Txt(if (yes) "Añadida" else "Descartada", 14, 900, color)
                    }
                }
            }
        }
    }

    editing?.let { p ->
        EditSheet(
            title = "Editar propuesta",
            fields = listOf(Field("Lo que hace", p.hago), Field("Qué significa", p.significa)),
            onDismiss = { editing = null },
            saveLabel = "Añadir a Mi idioma",
            onSave = { (h, sg) ->
                if (h.isNotBlank() && sg.isNotBlank()) vm.decide(p.id, true, h.trim(), sg.trim())
                editing = null
            },
        )
    }
}

@Composable
private fun ProposalCard(p: Proposal, onNo: () -> Unit, onEdit: () -> Unit, onYes: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().card(Auna.White, 24.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (p.photo != null) Photo(p.photo, p.name, 44.dp, ring = null) else Initial(p.name.take(1))
            Column(Modifier.weight(1f)) {
                Txt(p.name, 17, 900)
                Txt("${p.role} · ${p.whenLabel}", 14, 700, Auna.Muted)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AIcon(Icons.Eye, Auna.Muted, 16.dp, 2.4f)
                Txt("Ha visto", 13, 900, Auna.Muted, upper = true)
            }
            Txt(
                "\"${p.said}\"", 17, 700, lineHeight = 1.35,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp))
                    .background(Auna.GreyCard)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AIcon(Icons.ArrowDown, Auna.Balance, 24.dp, 2.6f) }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AIcon(Icons.Passport, Auna.Green, 16.dp, 2.4f)
                Txt("Así quedaría en Mi idioma", 13, 900, Auna.Green, upper = true)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(2.dp, Auna.Mint, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Txt(p.hago, 17, 800)
                Tag(p.significa, Auna.MintStrong, Auna.DeepGreen, fontSize = 15)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AButton(onNo, Modifier.weight(1f), bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 52.dp, hPadding = 6.dp) {
                Txt("No", 15, 800)
            }
            AButton(onEdit, Modifier.weight(1f), bg = Auna.White, border = Auna.Green, radius = 14.dp, minHeight = 52.dp, hPadding = 6.dp) {
                Txt("Editar", 15, 800, Auna.Green)
            }
            AButton(onYes, Modifier.weight(1f), radius = 14.dp, minHeight = 52.dp, hPadding = 6.dp) {
                Txt("Añadir", 15, 800, Auna.White)
            }
        }
    }
}
