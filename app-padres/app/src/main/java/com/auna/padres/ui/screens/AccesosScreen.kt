package com.auna.padres.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.data.Access
import com.auna.padres.data.AccessRequest
import com.auna.padres.data.Kid
import com.auna.padres.data.PERM_EDIT
import com.auna.padres.data.PERM_PROPOSE
import com.auna.padres.data.PERM_VIEW
import com.auna.padres.data.newId
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.ChoiceButton
import com.auna.padres.ui.EditSheet
import com.auna.padres.ui.Field
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.Initial
import com.auna.padres.ui.Navi
import com.auna.padres.ui.NoKid
import com.auna.padres.ui.PrimaryButton
import com.auna.padres.ui.Routes
import com.auna.padres.ui.Tag
import com.auna.padres.ui.Toast
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna
import kotlinx.coroutines.delay
import java.util.Calendar

private const val DAY = 24L * 60 * 60 * 1000

private val permissions = listOf(PERM_EDIT to "Puede editar", PERM_VIEW to "Solo ver", PERM_PROPOSE to "Ver y proponer")
private val durations = listOf("Siempre", "Solo hoy", "1 semana", "1 mes", "Todo el curso")

/** Fecha de fin para cada duración (null = para siempre). */
private fun untilFor(label: String): Long? {
    val now = Calendar.getInstance()
    return when (label) {
        "Solo hoy" -> now.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis
        "1 día" -> now.timeInMillis + DAY
        "1 semana" -> now.timeInMillis + 7 * DAY
        "1 mes" -> now.apply { add(Calendar.MONTH, 1) }.timeInMillis
        "Todo el curso" -> {
            // Hasta el 30 de junio (fin de curso escolar en España).
            val y = if (now.get(Calendar.MONTH) >= Calendar.JULY) now.get(Calendar.YEAR) + 1 else now.get(Calendar.YEAR)
            Calendar.getInstance().apply { set(y, Calendar.JUNE, 30, 23, 59) }.timeInMillis
        }
        else -> null
    }
}

fun whenText(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    val now = Calendar.getInstance()
    val hm = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    return if (c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && c.get(Calendar.YEAR) == now.get(Calendar.YEAR)) "hoy, $hm"
    else "${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}, $hm"
}

fun permissionLabel(p: String) = permissions.firstOrNull { it.first == p }?.second ?: "Solo ver"

fun expiryLabel(until: Long?): String? {
    if (until == null) return null
    val days = ((until - System.currentTimeMillis()) / DAY).toInt()
    return when {
        until < System.currentTimeMillis() -> "Caducado"
        days == 0 -> "Hasta hoy"
        days == 1 -> "Caduca mañana"
        else -> "Caduca en $days días"
    }
}

private fun metaOf(a: Access): String = when {
    a.members != null -> "${a.members} personas · ${permissionLabel(a.permission)}"
    else -> listOfNotNull(permissionLabel(a.permission), expiryLabel(a.until) ?: "Siempre").joinToString(" · ")
}

private fun invitationText(kid: Kid, a: Access): String {
    val perm = when (a.permission) {
        PERM_EDIT -> "puedes verlo y cambiarlo"
        PERM_PROPOSE -> "puedes verlo y proponer cambios"
        else -> "puedes verlo"
    }
    val until = a.until?.let { u ->
        val c = Calendar.getInstance().apply { timeInMillis = u }
        " hasta el ${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}"
    } ?: ""
    return "Hola ${a.name}: te he dado acceso al pasaporte de ${kid.name} en Aúna ($perm$until). " +
        "Ahí está lo que necesitas saber para entender y cuidar a ${kid.name}."
}

fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(send, "Enviar invitación").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private data class Undo(val text: String, val action: () -> Unit)

@Composable
fun AccesosScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val kid = s.activeKid
    val context = LocalContext.current
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var customFor by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Access?>(null) }
    var inviting by remember { mutableStateOf(false) }
    var undo by remember { mutableStateOf<Undo?>(null) }

    LaunchedEffect(undo) { if (undo != null) { delay(6000); undo = null } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Txt(if (kid != null) "Accesos de ${kid.name}" else "Accesos", 26, 900)
        if (kid == null) {
            NoKid({ navi.tab(Routes.PERFILES) })
            return@Column
        }

        s.requestsFor(kid.id).forEach { req ->
            RequestCard(
                req = req,
                custom = customFor == req.id,
                onToggleCustom = { customFor = if (customFor == req.id) null else req.id },
                onAnswer = { access ->
                    vm.answerRequest(req.id, access)
                    customFor = null
                    undo = Undo(
                        if (access == null) "Solicitud rechazada" else "${req.name.substringBefore(' ')} ya tiene acceso",
                    ) { vm.restoreRequest(req, access?.id) }
                },
            )
        }

        undo?.let { u -> Toast(u.text, "Deshacer", { u.action(); undo = null }) }

        Column(Modifier.fillMaxWidth().card(Auna.White, 22.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
            kid.access.forEachIndexed { index, a ->
                val isOpen = open == a.id
                val warn = a.until != null
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 72.dp)
                            .clickable(role = Role.Button) { open = if (isOpen) null else a.id }
                            .semantics { stateDescription = if (isOpen) "Abierto" else "Cerrado" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Initial(a.name.take(1).uppercase(), 44.dp, if (warn) Auna.MintStrong else Auna.Mint, if (warn) Auna.DeepGreen else Auna.Green)
                        Column(Modifier.weight(1f)) {
                            Txt(if (a.role.isNotBlank() && a.role != a.name) "${a.name} · ${a.role.lowercase()}" else a.name, 17, 800)
                            Txt(metaOf(a), 14, 700, if (warn) Auna.Forest else Auna.Muted)
                        }
                        AIcon(if (isOpen) Icons.ChevronUp else Icons.ChevronDown, Auna.Muted, 20.dp, 2.5f)
                    }
                    if (isOpen) {
                        Row(Modifier.padding(start = 56.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AButton({ editing = a }, bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 44.dp, hPadding = 14.dp) {
                                Txt("Cambiar", 15, 800)
                            }
                            AButton(
                                {
                                    vm.removeAccess(kid.id, a.id)
                                    open = null
                                    undo = Undo("${a.name} ya no tiene acceso") { vm.restoreAccess(kid.id, a, index) }
                                },
                                bg = Auna.White, border = Auna.Forest, radius = 14.dp, minHeight = 44.dp, hPadding = 14.dp,
                            ) { Txt("Quitar acceso", 15, 800, Auna.Forest) }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Auna.Line))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .clickable(role = Role.Button) { navi.open(Routes.QR) },
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
                AIcon(Icons.ChevronRight, Auna.Muted, 20.dp, 2.5f)
            }
        }

        PrimaryButton(
            "Invitar",
            {
                // Con la cuenta de la familia: se envía el enlace a la web y el código de la tarjeta.
                val text = vm.invitationText(kid)
                if (text != null) share(context, text) else inviting = true
            },
            Modifier.fillMaxWidth(), radius = 18.dp, icon = Icons.Plus,
        )
        if (vm.invitationText(kid) != null) {
            Txt("Le llegará un enlace a la web de Aúna. Cuando cree su cuenta y te envíe su nota, la verás aquí arriba.", 14, 700, Auna.Muted)
        }

        val views = s.views.filter { it.kidId == kid.id }
        if (views.isNotEmpty()) {
            Txt("Últimas consultas", 14, 800, Auna.Muted, Modifier.padding(top = 6.dp), upper = true)
            Column(Modifier.fillMaxWidth().card(Auna.White, 22.dp).padding(horizontal = 16.dp, vertical = 6.dp)) {
                views.take(8).forEach { v ->
                    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                        AIcon(Icons.Eye, Auna.Muted, 18.dp, 2.2f)
                        Txt(v.name.ifBlank { "Alguien" }, 16, 800, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        Txt(whenText(v.at), 14, 700, Auna.Muted)
                    }
                }
            }
        }
    }

    if (kid != null && (inviting || editing != null)) {
        AccessEditor(
            access = editing,
            onDismiss = { inviting = false; editing = null },
            onSave = { a, send ->
                vm.saveAccess(kid.id, a)
                if (send) share(context, invitationText(kid, a))
                inviting = false; editing = null
            },
        )
    }
}

@Composable
private fun RequestCard(req: AccessRequest, custom: Boolean, onToggleCustom: () -> Unit, onAnswer: (Access?) -> Unit) {
    var dur by rememberSaveable(req.id) { mutableStateOf("1 mes") }
    var perm by rememberSaveable(req.id) { mutableStateOf(PERM_VIEW) }
    val first = req.name.substringBefore(' ')
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
            Initial(req.name.take(1), 52.dp, Auna.MintStrong, Auna.DeepGreen, 22)
            Column(Modifier.weight(1f)) {
                Txt(req.name, 18, 900)
                Txt(listOf(req.role, req.place).filter { it.isNotBlank() }.joinToString(" · "), 15, 600, Auna.Muted)
            }
            Tag("NUEVA", Auna.CoralBg, Auna.Coral, fontSize = 12, weight = 900, hPad = 8.dp)
        }
        if (req.note.isNotBlank()) {
            Txt("«${req.note}»", 16, 700, lineHeight = 1.35, modifier = Modifier.fillMaxWidth().card(Auna.GreyCard, 16.dp).padding(horizontal = 14.dp, vertical = 12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AButton({ onAnswer(null) }, Modifier.weight(1f), bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 52.dp, hPadding = 6.dp) {
                Txt("No", 15, 800)
            }
            AButton(
                { onAnswer(Access(req.id, first, req.role, PERM_VIEW, untilFor("Solo hoy"))) },
                Modifier.weight(1f), bg = Auna.White, border = Auna.Green, radius = 14.dp, minHeight = 52.dp, hPadding = 6.dp,
            ) { Txt("Solo hoy", 15, 800, Auna.Green) }
            AButton(
                onToggleCustom,
                Modifier.weight(1f).semantics { stateDescription = if (custom) "Abierto" else "Cerrado" },
                bg = Auna.CoralBg, border = Auna.CoralBorder, radius = 14.dp, minHeight = 52.dp, hPadding = 6.dp,
            ) { Txt("Personalizar", 15, 800, Auna.Coral) }
        }
        if (custom) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Auna.Line))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt("¿Cuánto tiempo?", 15, 800, Auna.Muted)
                listOf("1 día", "1 semana", "1 mes", "Todo el curso").chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { d ->
                            ChoiceButton(d, dur == d, { dur = d }, Modifier.weight(1f), selBg = Auna.CoralBg, selBorder = Auna.Coral, borderColor = Auna.CoralBorder)
                        }
                    }
                }
                Txt("¿Qué puede hacer?", 15, 800, Auna.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PERM_VIEW to "Solo ver", PERM_PROPOSE to "Ver y proponer").forEach { (id, label) ->
                        ChoiceButton(label, perm == id, { perm = id }, Modifier.weight(1f), selBg = Auna.CoralBg, selBorder = Auna.Coral, borderColor = Auna.CoralBorder)
                    }
                }
                PrimaryButton(
                    "Dar acceso", { onAnswer(Access(req.id, first, req.role, perm, untilFor(dur))) },
                    Modifier.fillMaxWidth(), height = 52.dp, fontSize = 16, radius = 14.dp,
                )
            }
        }
    }
}

/** Invitar a alguien nuevo o cambiar un acceso existente. */
@Composable
private fun AccessEditor(access: Access?, onDismiss: () -> Unit, onSave: (Access, Boolean) -> Unit) {
    var perm by remember { mutableStateOf(access?.permission ?: PERM_VIEW) }
    var dur by remember { mutableStateOf(if (access == null) "Siempre" else if (access.until == null) "Siempre" else "Igual") }
    var send by remember { mutableStateOf(access == null) }
    EditSheet(
        title = if (access == null) "Invitar" else "Cambiar acceso",
        fields = listOf(
            Field("Nombre", access?.name ?: "", "Ej.: Marta"),
            Field("Quién es", access?.role ?: "", "Ej.: Tía, profe de natación…"),
        ),
        onDismiss = onDismiss,
        saveLabel = if (access == null) "Invitar" else "Guardar",
        onSave = { (name, role) ->
            if (name.isNotBlank()) {
                val until = if (dur == "Igual") access?.until else untilFor(dur)
                onSave(
                    (access ?: Access(newId("a"), "", "")).copy(name = name.trim(), role = role.trim(), permission = perm, until = until),
                    send,
                )
            }
        },
        extra = {
            Txt("¿Qué puede hacer?", 15, 800, Auna.Muted)
            permissions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (id, label) -> ChoiceButton(label, perm == id, { perm = id }, Modifier.weight(1f)) }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
            Txt("¿Durante cuánto tiempo?", 15, 800, Auna.Muted)
            val options = (if (access?.until != null) listOf("Igual") else emptyList()) + durations
            options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        val label = if (d == "Igual") (expiryLabel(access?.until) ?: "Igual") else d
                        ChoiceButton(label, dur == d, { dur = d }, Modifier.weight(1f))
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .card(Auna.White, 18.dp)
                    .toggleable(value = send, role = Role.Checkbox) { send = it }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("Enviarle la invitación (WhatsApp, SMS…)", 16, 800, modifier = Modifier.weight(1f))
                Checkbox(send, null, colors = CheckboxDefaults.colors(checkedColor = Auna.Green, uncheckedColor = Auna.Muted))
            }
        },
    )
}
