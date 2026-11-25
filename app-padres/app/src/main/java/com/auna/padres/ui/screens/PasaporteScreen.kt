package com.auna.padres.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.Speaker
import com.auna.padres.TalkContext
import com.auna.padres.data.Contact
import com.auna.padres.data.Group
import com.auna.padres.data.Kid
import com.auna.padres.data.PASSPORT_TOTAL
import com.auna.padres.data.Section
import com.auna.padres.data.Signal
import com.auna.padres.data.Templates
import com.auna.padres.data.newId
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.AunaIcon
import com.auna.padres.ui.CircleIconButton
import com.auna.padres.ui.EditSheet
import com.auna.padres.ui.Field
import com.auna.padres.ui.Girasol
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.KidEditor
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

/** Icono de «Lo más importante» según de qué hable la frase. */
internal fun highlightIcon(text: String): AunaIcon {
    val t = text.lowercase()
    return when {
        listOf("com", "trit", "beb", "alerg", "atragant").any { it in t } -> Icons.Utensils
        listOf("ruido", "grit", "oído", "sonido").any { it in t } -> Icons.Mute
        listOf("ríe", "rio", "río", "sonr", "content", "llor").any { it in t } -> Icons.Smile
        listOf("epilep", "crisis", "medic", "salud").any { it in t } -> Icons.Health
        else -> Icons.Star
    }
}

/** Lo que se está editando ahora mismo en la hoja inferior. */
private sealed class Ed {
    data class Item(val sectionId: String, val groupId: String, val index: Int?) : Ed()
    data object YesNo : Ed()
    data class Sig(val id: String?) : Ed()
    data class Highlight(val index: Int?) : Ed()
    data class Person(val id: String?) : Ed()
    data object NewSection : Ed()
    data class RenameSection(val sectionId: String) : Ed()
    data object Profile : Ed()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasaporteScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val speaking by vm.speaker.speaking.collectAsState()
    val kid = s.activeKid
    // Al cambiar de niño, el pasaporte vuelve a la cuadrícula.
    var open by rememberSaveable(kid?.id) { mutableStateOf<String?>(null) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Ed?>(null) }

    if (kid == null) {
        Column(Modifier.fillMaxSize().padding(Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt("Pasaporte", 26, 900)
            NoKid({ navi.tab(Routes.PERFILES) })
        }
        return
    }

    // Si se borra la sección abierta, volvemos a la cuadrícula.
    if (open != null && kid.passport.section(open!!) == null) open = null
    BackHandler(enabled = open != null) { open = null }

    AnimatedContent(
        targetState = open,
        transitionSpec = {
            val forward = targetState != null
            val spec = tween<androidx.compose.ui.unit.IntOffset>(280, easing = FastOutSlowInEasing)
            (slideInHorizontally(spec) { if (forward) it else -it / 4 }) togetherWith
                (slideOutHorizontally(spec) { if (forward) -it / 4 else it })
        },
        label = "pasaporte",
    ) { current ->
        Box(Modifier.fillMaxSize().background(Auna.Bg)) {
            if (current == null) {
                PassportGrid(
                    kid = kid,
                    speaking = speaking == "pasaporte",
                    onListen = { vm.speaker.toggle(Speaker.passportText(kid), "pasaporte") },
                    onOpen = { open = it },
                    onAdd = { sheet = true },
                    onEdit = { editing = it },
                )
            } else {
                val section = kid.passport.section(current)
                if (section != null) {
                    SectionDetail(
                        kid = kid,
                        section = section,
                        speaking = speaking == "sec-$current",
                        onListen = { vm.speaker.toggle(Speaker.sectionText(kid, current), "sec-$current") },
                        onBack = { open = null },
                        onTalk = {
                            vm.talkContext.value = TalkContext(section.id, null)
                            navi.tab(Routes.HABLAR)
                        },
                        onEdit = { editing = it },
                    )
                }
            }
        }
    }

    if (sheet) {
        val used = kid.passport.sections.map { it.title }
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
                Templates.extraSectionOptions.filter { it !in used }.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { title ->
                            AButton(
                                { open = vm.addSection(kid.id, title); sheet = false },
                                Modifier.weight(1f),
                                bg = Auna.White, border = Auna.Border, minHeight = 64.dp,
                            ) { Txt(title, 17, 800) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                AButton(
                    { sheet = false; editing = Ed.NewSection },
                    Modifier.fillMaxWidth(),
                    bg = Auna.Bg, border = Auna.Balance, dashed = true,
                ) { Txt("Otra, con mi nombre", 17, 900, Auna.Green) }
            }
        }
    }

    editing?.let { ed -> Editor(vm, kid, ed, onDone = { editing = null }, onOpenSection = { open = it }) }
}

@Composable
private fun Editor(vm: AunaViewModel, kid: Kid, ed: Ed, onDone: () -> Unit, onOpenSection: (String) -> Unit) {
    val p = kid.passport
    when (ed) {
        is Ed.Item -> {
            val g = p.group(ed.sectionId, ed.groupId) ?: return onDone()
            val old = ed.index?.let { g.items.getOrNull(it) }
            EditSheet(
                title = if (old == null) "Añadir a «${g.title}»" else g.title,
                fields = listOf(Field("Texto", old ?: "", "Escribe aquí")),
                onDismiss = onDone,
                onSave = { (t) ->
                    if (ed.index == null) vm.addItem(kid.id, ed.sectionId, ed.groupId, t) else vm.editItem(kid.id, ed.sectionId, ed.groupId, ed.index, t)
                    onDone()
                },
                onDelete = ed.index?.let { i -> { vm.removeItem(kid.id, ed.sectionId, ed.groupId, i); onDone() } },
            )
        }
        Ed.YesNo -> EditSheet(
            title = "Cómo dice sí y no",
            fields = listOf(Field("Para decir «sí»", p.yes, "Ej.: Sonrío y toco tu mano"), Field("Para decir «no»", p.no, "Ej.: Giro la cabeza")),
            onDismiss = onDone,
            onSave = { (y, n) -> vm.setYesNo(kid.id, y, n); onDone() },
        )
        is Ed.Sig -> {
            val old = ed.id?.let { id -> p.signals.firstOrNull { it.id == id } }
            EditSheet(
                title = if (old == null) "Nueva señal" else "Señal",
                fields = listOf(
                    Field("Lo que hace", old?.hago ?: "", "Ej.: Se tira de la oreja y llora"),
                    Field("Qué significa", old?.significa ?: "", "Ej.: Le duele algo"),
                    Field("Qué hacer (opcional)", old?.hacer ?: "", "Ej.: Enséñale los pictos del cuerpo", multiline = true),
                ),
                onDismiss = onDone,
                onSave = { (h, sg, hc) ->
                    if (h.isNotBlank() && sg.isNotBlank()) vm.saveSignal(kid.id, Signal(old?.id ?: newId("sg"), h.trim(), sg.trim(), hc.trim()))
                    onDone()
                },
                onDelete = old?.let { o -> { vm.removeSignal(kid.id, o.id); onDone() } },
            )
        }
        is Ed.Highlight -> {
            val old = ed.index?.let { p.highlights.getOrNull(it) }
            EditSheet(
                title = "Lo más importante",
                fields = listOf(Field("Texto", old ?: "", "Ej.: Como triturado")),
                onDismiss = onDone,
                onSave = { (t) -> vm.saveHighlight(kid.id, ed.index, t); onDone() },
                onDelete = ed.index?.let { i -> { vm.saveHighlight(kid.id, i, ""); onDone() } },
            )
        }
        is Ed.Person -> {
            val old = ed.id?.let { id -> p.contacts.firstOrNull { it.id == id } }
            EditSheet(
                title = if (old == null) "Nuevo teléfono" else "Teléfono",
                fields = listOf(
                    Field("Quién es", old?.relation ?: "", "Ej.: Mamá"),
                    Field("Nombre", old?.name ?: "", "Ej.: Ana"),
                    Field("Teléfono", old?.phone ?: "", "Ej.: 600 123 456", keyboard = KeyboardType.Phone),
                ),
                onDismiss = onDone,
                onSave = { (rel, name, phone) ->
                    if (name.isNotBlank() || rel.isNotBlank()) vm.saveContact(kid.id, Contact(old?.id ?: newId("c"), name.trim(), rel.trim(), phone.trim()))
                    onDone()
                },
                onDelete = old?.let { o -> { vm.removeContact(kid.id, o.id); onDone() } },
            )
        }
        Ed.NewSection -> EditSheet(
            title = "Nueva sección",
            fields = listOf(Field("Nombre de la sección", "", "Ej.: Piscina")),
            onDismiss = onDone,
            saveLabel = "Crear sección",
            onSave = { (t) -> if (t.isNotBlank()) onOpenSection(vm.addSection(kid.id, t)); onDone() },
        )
        is Ed.RenameSection -> {
            val sec = p.section(ed.sectionId) ?: return onDone()
            EditSheet(
                title = "Sección",
                fields = listOf(Field("Nombre", sec.title)),
                onDismiss = onDone,
                onSave = { (t) -> vm.renameSection(kid.id, sec.id, t); onDone() },
                onDelete = { vm.deleteSection(kid.id, sec.id); onDone() },
                deleteLabel = "Borrar la sección",
            )
        }
        Ed.Profile -> KidEditor(vm, kid, onDismiss = onDone)
    }
}

@Composable
private fun PassportGrid(
    kid: Kid,
    speaking: Boolean,
    onListen: () -> Unit,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Ed) -> Unit,
) {
    val p = kid.passport
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Gutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("Pasaporte", 26, 900, modifier = Modifier.weight(1f))
            ListenButton(speaking, "Escuchar el pasaporte", Auna.White, onListen)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .card(Auna.GreyCard, 26.dp)
                .clickable(role = Role.Button, onClickLabel = "Editar perfil") { onEdit(Ed.Profile) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KidPhoto(kid, 100.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt(kid.name, 28, 900, lineHeight = 1.1)
                if (kid.age.isNotBlank()) Txt(kid.age, 16, 700, Auna.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (p.noHabla) Tag("No hablo", Auna.White, Auna.Ink)
                    Tag("${p.done}/$PASSPORT_TOTAL", Auna.White, Auna.Ink)
                }
            }
            AIcon(Icons.Pencil, Auna.Muted, 20.dp, 2.2f)
        }

        Column(
            Modifier.fillMaxWidth().card(Auna.MintStrong, 22.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AIcon(Icons.Star, Auna.Dot, 20.dp)
                Txt("Lo más importante", 16, 900, Auna.DeepGreen)
            }
            p.highlights.forEachIndexed { i, text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 40.dp)
                        .clickable(role = Role.Button, onClickLabel = "Editar") { onEdit(Ed.Highlight(i)) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AIcon(highlightIcon(text), Auna.DeepGreen, 22.dp, 2f)
                    Txt(text, 17, 800, modifier = Modifier.weight(1f))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 40.dp)
                    .clickable(role = Role.Button) { onEdit(Ed.Highlight(null)) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AIcon(Icons.Plus, Auna.Green, 22.dp, 2.4f)
                Txt(if (p.highlights.isEmpty()) "Añade lo más importante" else "Añadir", 16, 800, Auna.Green)
            }
        }

        val tiles = p.sections.map { sec ->
            Triple(sec, sectionIcons[sec.id] ?: Icons.Sparkle, !sec.custom && p.missingIn(sec.id).isNotEmpty())
        }
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (sec, icon, missing) ->
                    SectionTile(
                        title = sec.title,
                        icon = icon,
                        highlighted = sec.id == "idioma",
                        badge = if (missing) "Falta" else null,
                        modifier = Modifier.weight(1f),
                    ) { onOpen(sec.id) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        PrimaryButton("Añadir sección", onAdd, Modifier.fillMaxWidth(), height = 64.dp, icon = Icons.Plus)
    }
}

@Composable
private fun ListenButton(speaking: Boolean, label: String, bg: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    CircleIconButton(
        if (speaking) Icons.Close else Icons.VolumeFull,
        if (speaking) "Parar la lectura" else label,
        onClick,
        bg = if (speaking) Auna.Green else bg,
        iconColor = if (speaking) Auna.White else Auna.Green,
        stroke = 2.2f,
    )
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

@Composable
private fun SectionDetail(
    kid: Kid,
    section: Section,
    speaking: Boolean,
    onListen: () -> Unit,
    onBack: () -> Unit,
    onTalk: () -> Unit,
    onEdit: (Ed) -> Unit,
) {
    val p = kid.passport
    val isIdioma = section.id == "idioma"
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleIconButton(Icons.ChevronLeft, "Volver", onBack)
            Txt(section.title, 24, 900, modifier = Modifier.weight(1f))
            if (section.custom) CircleIconButton(Icons.Pencil, "Cambiar nombre o borrar", { onEdit(Ed.RenameSection(section.id)) }, iconColor = Auna.Green, stroke = 2.2f)
            ListenButton(speaking, "Escuchar", Auna.Mint, onListen)
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

            if (isIdioma) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    YesNoCard("Sí", p.yes, Auna.Mint, Auna.Green, Auna.Ink, Modifier.weight(1f)) { onEdit(Ed.YesNo) }
                    YesNoCard("No", p.no, Auna.CoralBg, Auna.Coral, Auna.Coral, Modifier.weight(1f)) { onEdit(Ed.YesNo) }
                }
                p.signals.forEach { sg ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .card(Auna.White, 18.dp)
                            .clickable(role = Role.Button, onClickLabel = "Editar") { onEdit(Ed.Sig(sg.id)) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Txt(sg.hago, 17, 800)
                        if (sg.significa.isNotBlank()) Tag(sg.significa, Auna.MintStrong, Auna.DeepGreen, fontSize = 15)
                        if (sg.hacer.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AIcon(Icons.Check, Auna.Green, 18.dp, 2.6f, Modifier.padding(top = 2.dp))
                                Txt(sg.hacer, 15, 600, Auna.Muted)
                            }
                        }
                    }
                }
                AddRow("Añadir señal") { onEdit(Ed.Sig(null)) }
            }

            section.groups.forEach { g -> GroupCard(g, section.custom && section.groups.size == 1) { idx -> onEdit(Ed.Item(section.id, g.id, idx)) } }

            if (section.id == "sobre") {
                Column(
                    Modifier.fillMaxWidth().card(Auna.White, 20.dp).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Txt("Teléfonos de la familia", 14, 800, Auna.Muted, upper = true)
                    p.contacts.forEach { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp)
                                .clickable(role = Role.Button, onClickLabel = "Editar") { onEdit(Ed.Person(c.id)) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Auna.Mint), contentAlignment = Alignment.Center) {
                                Txt((c.name.ifBlank { c.relation }).take(1).uppercase(), 15, 900, Auna.Green)
                            }
                            Column(Modifier.weight(1f)) {
                                Txt(listOf(c.relation, c.name).filter { it.isNotBlank() }.joinToString(" · "), 16, 800)
                                Txt(c.phone.ifBlank { "Falta el teléfono" }, 14, 700, if (c.phone.isBlank()) Auna.Forest else Auna.Muted)
                            }
                        }
                    }
                    AddRow("Añadir teléfono", inCard = true) { onEdit(Ed.Person(null)) }
                }
            }

            if (p.isEmpty(section.id) && !isIdioma && section.groups.size <= 1 && section.custom) {
                Column(
                    Modifier.fillMaxWidth().card(Auna.White, 22.dp).padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Girasol(64.dp)
                    Txt("Aún está vacía", 19, 900)
                }
            }

            val missing = if (section.custom) emptyList() else p.missingIn(section.id)
            if (missing.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().card(Auna.MintStrong, 20.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Auna.Dot))
                    Txt("Falta: ${missing.joinToString(", ")}", 16, 800, Auna.DeepGreen, Modifier.weight(1f))
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

@Composable
private fun YesNoCard(
    label: String,
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    titleColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .card(bg, 18.dp)
            .clickable(role = Role.Button, onClickLabel = "Editar") { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Txt(label, 20, 900, titleColor)
        Txt(text.ifBlank { "Toca para añadir" }, 15, 600, textColor)
    }
}

@Composable
private fun AddRow(label: String, inCard: Boolean = false, onClick: () -> Unit) {
    if (inCard) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).clickable(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AIcon(Icons.Plus, Auna.Green, 22.dp, 2.4f)
            Txt(label, 16, 800, Auna.Green)
        }
    } else {
        AButton(onClick, Modifier.fillMaxWidth(), bg = Auna.Bg, border = Auna.Border, radius = 18.dp, minHeight = 52.dp) {
            AIcon(Icons.Plus, Auna.Green, 20.dp, 2.4f)
            Txt(label, 16, 800, Auna.Green)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupCard(g: Group, hideTitle: Boolean, onItem: (Int?) -> Unit) {
    Column(
        Modifier.fillMaxWidth().card(Auna.White, 20.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!hideTitle) Txt(g.title, 14, 800, Auna.Muted, upper = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            g.items.forEachIndexed { i, item ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (g.warm) Auna.MintStrong else Auna.Mint)
                        .clickable(role = Role.Button, onClickLabel = "Editar") { onItem(i) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Txt(item, 16, 700, if (g.warm) Auna.DeepGreen else Auna.Green) }
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, Auna.Border, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { onItem(null) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AIcon(Icons.Plus, Auna.Green, 18.dp, 2.4f)
                Txt("Añadir", 15, 800, Auna.Green)
            }
        }
    }
}
