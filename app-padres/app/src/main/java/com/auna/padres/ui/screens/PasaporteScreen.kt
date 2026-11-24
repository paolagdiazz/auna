package com.auna.padres.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.data.Group
import com.auna.padres.data.Kid
import com.auna.padres.data.PASSPORT_TOTAL
import com.auna.padres.data.PassportSection
import com.auna.padres.data.Sample
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.AunaIcon
import com.auna.padres.ui.CircleIconButton
import com.auna.padres.ui.Girasol
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.KidPhoto
import com.auna.padres.ui.Navi
import com.auna.padres.ui.NoKid
import com.auna.padres.ui.PrimaryButton
import com.auna.padres.ui.Routes
import com.auna.padres.ui.Tag
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna

private val sectionIcons = mapOf(
    "sobre" to Icons.OpenBook,
    "idioma" to Icons.Hand,
    "calma" to Icons.Headphones,
    "gusta" to Icons.Heart,
    "comida" to Icons.Utensils,
    "salud" to Icons.Health,
)

private val highlightIcons = mapOf("utensils" to Icons.Utensils, "mute" to Icons.Mute, "smile" to Icons.Smile)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasaporteScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val kid = s.activeKid
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var sheet by rememberSaveable { mutableStateOf(false) }

    if (kid == null) {
        Column(Modifier.fillMaxSize().padding(Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt("Pasaporte", 26, 900)
            NoKid({ navi.tab(Routes.PERFILES) })
        }
        return
    }

    val isMarco = kid.id == Sample.MARCO
    val custom = s.sectionsFor(kid.id)

    BackHandler(enabled = open != null) { open = null }

    val current = open
    if (current == null) {
        PassportGrid(
            kid = kid,
            isMarco = isMarco,
            customTiles = custom.map { it.id to it.title },
            onOpen = { open = it },
            onAdd = { sheet = true },
        )
    } else {
        val section: PassportSection = when {
            isMarco && Sample.marcoSections.containsKey(current) -> Sample.marcoSections.getValue(current)
            Sample.sectionTitles.containsKey(current) -> PassportSection(Sample.sectionTitles.getValue(current))
            else -> PassportSection(custom.firstOrNull { it.id == current }?.title ?: "")
        }
        val signals = if (current == "idioma") s.signalsFor(kid.id) else emptyList()
        SectionDetail(
            section = section,
            isIdioma = current == "idioma" && isMarco,
            signals = signals,
            onBack = { open = null },
            onTalk = { navi.tab(Routes.HABLAR) },
        )
    }

    if (sheet) {
        val used = custom.map { it.title }
        ModalBottomSheet(
            onDismissRequest = { sheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Auna.Bg,
            scrimColor = Auna.Scrim,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = null,
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt("Añadir sección", 22, 900, modifier = Modifier.weight(1f))
                    CircleIconButton(Icons.Close, "Cerrar", { sheet = false }, size = 44.dp, iconSize = 20.dp)
                }
                Sample.extraSectionOptions.filter { it !in used }.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { title ->
                            AButton(
                                { vm.addSection(kid.id, title); sheet = false },
                                Modifier.weight(1f),
                                bg = Auna.White, border = Auna.Border, minHeight = 64.dp,
                            ) { Txt(title, 17, 800) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                AButton(
                    { vm.addSection(kid.id, "Mi sección"); sheet = false },
                    Modifier.fillMaxWidth(),
                    bg = Auna.Bg, border = Auna.Balance, dashed = true,
                ) { Txt("Otra, con mi nombre", 17, 900, Auna.Green) }
            }
        }
    }
}

@Composable
private fun PassportGrid(
    kid: Kid,
    isMarco: Boolean,
    customTiles: List<Pair<String, String>>,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Gutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("Pasaporte", 26, 900, modifier = Modifier.weight(1f))
            CircleIconButton(Icons.VolumeFull, "Escuchar el pasaporte", {}, iconColor = Auna.Green, stroke = 2.2f)
        }

        Row(
            Modifier.fillMaxWidth().card(Auna.GreyCard, 26.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KidPhoto(kid, 100.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt(kid.name, 28, 900, lineHeight = 1.1)
                Txt(kid.age, 16, 700, Auna.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isMarco) Tag("No hablo", Auna.White, Auna.Ink)
                    Tag("${kid.done}/$PASSPORT_TOTAL", Auna.White, Auna.Ink)
                }
            }
        }

        if (isMarco) {
            Column(
                Modifier.fillMaxWidth().card(Auna.MintStrong, 22.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AIcon(Icons.Star, Auna.Dot, 20.dp)
                    Txt("Lo más importante", 16, 900, Auna.DeepGreen)
                }
                Sample.highlights.forEach { (icon, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AIcon(highlightIcons.getValue(icon), Auna.DeepGreen, 22.dp, 2f)
                        Txt(text, 17, 800)
                    }
                }
            }
        }

        val tiles = Sample.baseSections.map { Triple(it, Sample.sectionTitles.getValue(it), sectionIcons.getValue(it)) } +
            customTiles.map { Triple(it.first, it.second, Icons.Sparkle) }
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (id, title, icon) ->
                    SectionTile(
                        title = title,
                        icon = icon,
                        highlighted = id == "idioma",
                        badge = if (id == "salud" && isMarco) "Falta" else null,
                        modifier = Modifier.weight(1f),
                    ) { onOpen(id) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        PrimaryButton("Añadir sección", onAdd, Modifier.fillMaxWidth(), height = 64.dp, icon = Icons.Plus)
    }
}

@Composable
private fun SectionTile(title: String, icon: AunaIcon, highlighted: Boolean, badge: String?, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .height(104.dp)
            .clip(shape)
            .background(Auna.White)
            .then(if (highlighted) Modifier.border(2.dp, Auna.Leaf, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            AIcon(icon, Auna.Green, 30.dp, 2f)
            Txt(title, 15, 800, align = TextAlign.Center)
        }
        if (badge != null) {
            Box(Modifier.align(Alignment.TopEnd)) {
                Tag(badge, Auna.MintStrong, Auna.DeepGreen, fontSize = 12, weight = 900, radius = 8.dp, hPad = 7.dp, vPad = 1.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionDetail(
    section: PassportSection,
    isIdioma: Boolean,
    signals: List<com.auna.padres.data.Signal>,
    onBack: () -> Unit,
    onTalk: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleIconButton(Icons.ChevronLeft, "Volver", onBack)
            Txt(section.title, 24, 900, modifier = Modifier.weight(1f))
            CircleIconButton(Icons.VolumeFull, "Escuchar", {}, bg = Auna.Mint, iconColor = Auna.Green, stroke = 2.2f)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = Gutter, end = Gutter, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (section.onlyFamily) {
                Row(
                    Modifier.card(Auna.Ink, 14.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AIcon(Icons.Lock, Auna.White, 16.dp, 2.4f)
                    Txt("Solo familia", 14, 800, Auna.White)
                }
            }

            val empty = !isIdioma && section.groups.isEmpty()

            if (isIdioma) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f).card(Auna.Mint, 18.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Txt("Sí", 20, 900, Auna.Green)
                        Txt("Sonrío y toco tu mano", 15, 600)
                    }
                    Column(Modifier.weight(1f).card(Auna.CoralBg, 18.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Txt("No", 20, 900, Auna.Coral)
                        Txt("Giro la cabeza", 15, 600, Auna.Coral)
                    }
                }
                signals.forEach { sg ->
                    Column(
                        Modifier.fillMaxWidth().card(Auna.White, 18.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Txt(sg.hago, 17, 800)
                        Tag(sg.significa, Auna.MintStrong, Auna.DeepGreen, fontSize = 15)
                        if (sg.hacer.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AIcon(Icons.Check, Auna.Green, 18.dp, 2.6f, Modifier.padding(top = 2.dp))
                                Txt(sg.hacer, 15, 600, Auna.Muted)
                            }
                        }
                    }
                }
            }

            section.groups.forEach { g -> GroupCard(g) }

            if (empty) {
                Column(
                    Modifier.fillMaxWidth().card(Auna.White, 22.dp).padding(horizontal = 16.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Girasol(72.dp)
                    Txt("Aún está vacía", 19, 900)
                }
            }

            if (section.pending != null) {
                Row(
                    Modifier.fillMaxWidth().card(Auna.MintStrong, 20.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Auna.Dot))
                    Txt(section.pending, 16, 800, Auna.DeepGreen, Modifier.weight(1f))
                }
            }

            AButton(
                onTalk,
                Modifier.fillMaxWidth().padding(top = 4.dp),
                bg = Auna.Bg, border = Auna.Balance, dashed = true,
            ) {
                AIcon(Icons.Mic, Auna.Green, 22.dp, 2.2f)
                Txt("Añadir hablando", 17, 800, Auna.Green)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupCard(g: Group) {
    Column(
        Modifier.fillMaxWidth().card(Auna.White, 20.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Txt(g.title, 14, 800, Auna.Muted, upper = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            g.items.forEach { item ->
                Tag(
                    item,
                    if (g.warm) Auna.MintStrong else Auna.Mint,
                    if (g.warm) Auna.DeepGreen else Auna.Green,
                    fontSize = 16, weight = 700, radius = 14.dp, hPad = 14.dp, vPad = 8.dp,
                )
            }
        }
    }
}
