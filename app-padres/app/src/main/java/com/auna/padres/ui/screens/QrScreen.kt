package com.auna.padres.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.QrCard
import com.auna.padres.data.Kid
import com.auna.padres.data.Templates
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.BackHeader
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
import com.auna.padres.ui.Toast
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Mi tarjeta QR: un QR real con la ficha de emergencia, para imprimir o guardar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val kid = s.activeKid
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by rememberSaveable { mutableStateOf(false) }
    var lost by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) { if (toast != null) { delay(4000); toast = null } }

    if (kid == null) {
        Column(Modifier.fillMaxSize().padding(Gutter), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BackHeader("Mi tarjeta QR", { navi.back() })
            NoKid({ navi.tab(Routes.PERFILES) })
        }
        return
    }
    val qrText = QrCard.text(kid)
    BackHandler(enabled = preview) { preview = false }

    AnimatedContent(
        targetState = preview,
        transitionSpec = {
            val spec = tween<androidx.compose.ui.unit.IntOffset>(280, easing = FastOutSlowInEasing)
            (slideInHorizontally(spec) { if (targetState) it else -it / 4 }) togetherWith (slideOutHorizontally(spec) { if (targetState) -it / 4 else it })
        },
        label = "qr",
    ) { showPreview ->
        Box(Modifier.fillMaxSize().background(Auna.Bg)) {
            if (!showPreview) {
                CardView(
                    kid = kid, qrText = qrText, toast = toast,
                    onBack = { navi.back() },
                    onToggle = { key, on -> vm.setQr(kid.id, key, on) },
                    onPrint = { scope.launch { runCatching { QrCard.printCard(context, kid) }.onFailure { toast = "No se ha podido abrir la impresión." } } },
                    onSave = {
                        scope.launch {
                            toast = runCatching { withContext(Dispatchers.IO) { QrCard.save(context, kid) } }.getOrElse { "No se ha podido guardar la imagen." }
                        }
                    },
                    onPreview = { preview = true },
                    onLost = { lost = true },
                    onPhones = { navi.tab(Routes.PASAPORTE) },
                )
            } else {
                PreviewView(kid, qrText, onBack = { preview = false }, onCall = { phone ->
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })))
                })
            }
        }
    }

    if (lost) {
        ModalBottomSheet(
            onDismissRequest = { lost = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Auna.Bg,
            scrimColor = Auna.Scrim,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = null,
        ) {
            Column(
                Modifier.padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt("He perdido la tarjeta", 22, 900, modifier = Modifier.weight(1f))
                    CircleIconButton(Icons.Close, "Cerrar", { lost = false }, size = 44.dp, iconSize = 20.dp)
                }
                Txt(
                    "Tranquila: la tarjeta solo lleva la ficha de emergencia que eliges en «Qué se muestra». " +
                        "No da acceso al pasaporte ni a tus datos, y no lleva ningún enlace.",
                    16, 600, lineHeight = 1.4,
                )
                Txt("Imprime una nueva. Si no quieres que la tarjeta perdida diga algo, quítalo de «Qué se muestra» antes de imprimir.", 16, 600, Auna.Muted, lineHeight = 1.4)
                PrimaryButton("Imprimir otra", { lost = false; runCatching { QrCard.printCard(context, kid) } }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CardView(
    kid: Kid,
    qrText: String,
    toast: String?,
    onBack: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onPrint: () -> Unit,
    onSave: () -> Unit,
    onPreview: () -> Unit,
    onLost: () -> Unit,
    onPhones: () -> Unit,
) {
    val missingPhones = (kid.qrShow["familia"] ?: true) && kid.passport.contacts.none { it.phone.isNotBlank() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackHeader("Mi tarjeta QR", onBack)

        Row(
            Modifier.fillMaxWidth().card(Auna.Green, 20.dp).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.card(Auna.White, 12.dp).padding(8.dp)) { RealQr(qrText, 112) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Girasol(36.dp)
                Txt("Hola, soy ${kid.name}", 22, 900, Auna.White, lineHeight = 1.1)
                Txt(if (kid.passport.noHabla) "No hablo. Escanéame." else "Escanéame.", 16, 700, Auna.Leaf)
            }
        }

        toast?.let { Toast(it) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Imprimir", onPrint, Modifier.weight(1f), height = 52.dp, fontSize = 16, radius = 16.dp)
            SecondaryButton("Descargar", onSave, Modifier.weight(1f), height = 52.dp, fontSize = 16, radius = 16.dp)
        }

        Txt("Qué se muestra", 14, 800, Auna.Muted, Modifier.padding(top = 4.dp), upper = true)
        Column(Modifier.fillMaxWidth().card(Auna.White, 20.dp).padding(horizontal = 16.dp, vertical = 2.dp)) {
            Templates.qrOptions.forEachIndexed { i, opt ->
                val on = kid.qrShow[opt.key] ?: opt.defaultOn
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = 50.dp).toggleable(value = on, role = Role.Checkbox) { onToggle(opt.key, it) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt(opt.label, 17, 700, modifier = Modifier.weight(1f))
                    Checkbox(on, null, colors = CheckboxDefaults.colors(checkedColor = Auna.Green, uncheckedColor = Auna.Muted))
                }
                if (i < Templates.qrOptions.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Auna.Line))
            }
        }
        if (missingPhones) {
            AButton(onPhones, Modifier.fillMaxWidth(), bg = Auna.CoralBg, radius = 18.dp, minHeight = 52.dp, arrangement = Arrangement.spacedBy(8.dp)) {
                AIcon(Icons.Alert, Auna.Coral, 20.dp, 2.2f)
                Txt("Faltan los teléfonos: añádelos en Pasaporte › Sobre mí", 15, 800, Auna.Coral, Modifier.weight(1f))
            }
        }

        AButton(onPreview, Modifier.fillMaxWidth(), bg = Auna.Mint) {
            AIcon(Icons.Eye, Auna.Green, 22.dp, 2.2f)
            Txt("Ver lo que ve quien escanea", 17, 900, Auna.Green)
        }
        AButton(onLost, Modifier.fillMaxWidth(), bg = Auna.Bg, minHeight = 48.dp) {
            Txt("He perdido la tarjeta", 16, 800, Auna.Forest, underline = true)
        }
    }
}

@Composable
private fun PreviewView(kid: Kid, qrText: String, onBack: () -> Unit, onCall: (String) -> Unit) {
    val phones = if (kid.qrShow["familia"] ?: true) kid.passport.contacts.filter { it.phone.isNotBlank() } else emptyList()
    Column(Modifier.fillMaxSize()) {
        BackHeader("Lo que ve quien escanea", onBack, Modifier.padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 12.dp), titleSize = 22)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Auna.White)
                .border(2.dp, Auna.Border, RoundedCornerShape(30.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("Al escanear con la cámara:", 15, 800, Auna.Muted, Modifier.weight(1f))
                Tag("Vista previa", Auna.GreyCard, Auna.Ink, fontSize = 13, weight = 900)
            }
            Txt(qrText, 18, 700, lineHeight = 1.45, modifier = Modifier.fillMaxWidth().card(Auna.Bg, 18.dp).padding(16.dp))
            Txt("Funciona con cualquier móvil, sin internet y sin instalar nada.", 14, 700, Auna.Muted)
            if (phones.isNotEmpty()) {
                Txt("Prueba los teléfonos", 14, 800, Auna.Muted, upper = true)
                phones.forEach { c ->
                    AButton({ onCall(c.phone) }, Modifier.fillMaxWidth(), bg = Auna.Leaf, minHeight = 56.dp) {
                        AIcon(Icons.Phone, Auna.Ink, 22.dp)
                        Txt("Llamar a ${c.relation.ifBlank { c.name }.lowercase()} · ${c.phone}", 16, 900)
                    }
                }
            }
        }
    }
}

/** Dibuja el QR real (el mismo que se imprime). */
@Composable
private fun RealQr(text: String, sizeDp: Int) {
    val image = remember(text) { QrCard.moduleBitmap(text).asImageBitmap() }
    Image(
        image,
        contentDescription = "Código QR con la ficha de emergencia",
        modifier = Modifier.size(sizeDp.dp),
        filterQuality = FilterQuality.None,
    )
}
