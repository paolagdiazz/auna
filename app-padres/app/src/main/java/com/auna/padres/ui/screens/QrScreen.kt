package com.auna.padres.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.auna.padres.AunaViewModel
import com.auna.padres.data.Sample
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AField
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.BackHeader
import com.auna.padres.ui.Girasol
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.Initial
import com.auna.padres.ui.Navi
import com.auna.padres.ui.PrimaryButton
import com.auna.padres.ui.Routes
import com.auna.padres.ui.SecondaryButton
import com.auna.padres.ui.Tag
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna

/** Mi tarjeta QR + vista previa de lo que ve quien la escanea (todo simulado). */
@Composable
fun QrScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val name = s.activeKid?.name ?: "…"
    var view by rememberSaveable { mutableStateOf("card") }
    var loginStep by rememberSaveable { mutableStateOf("phone") }
    var called by rememberSaveable { mutableStateOf(false) }
    var loc by rememberSaveable { mutableStateOf(false) }
    val go: (String) -> Unit = { v -> view = v; loginStep = "phone"; called = false; loc = false }

    BackHandler(enabled = view != "card") { go(if (view == "home") "card" else "home") }

    if (view == "card") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BackHeader("Mi tarjeta QR", { navi.back() })

            Row(
                Modifier.fillMaxWidth().card(Auna.Green, 20.dp).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.card(Auna.White, 12.dp).padding(8.dp)) { QrMock(96) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Girasol(36.dp)
                    Txt("Hola, soy $name", 22, 900, Auna.White, lineHeight = 1.1)
                    Txt("No hablo. Escanéame.", 16, 700, Auna.Leaf)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Imprimir", {}, Modifier.weight(1f), height = 52.dp, fontSize = 16, radius = 16.dp)
                SecondaryButton("Descargar", {}, Modifier.weight(1f), height = 52.dp, fontSize = 16, radius = 16.dp)
            }

            Txt("Qué se muestra", 14, 800, Auna.Muted, Modifier.padding(top = 4.dp), upper = true)
            Column(Modifier.fillMaxWidth().card(Auna.White, 20.dp).padding(horizontal = 16.dp, vertical = 2.dp)) {
                Sample.qrOptions.forEachIndexed { i, opt ->
                    val on = s.qrShow[opt.key] ?: opt.defaultOn
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .toggleable(value = on, role = Role.Checkbox) { vm.setQr(opt.key, it) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Txt(opt.label, 17, 700, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = on, onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = Auna.Green, uncheckedColor = Auna.Muted),
                        )
                    }
                    if (i < Sample.qrOptions.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Auna.Line))
                }
            }

            AButton({ go("home") }, Modifier.fillMaxWidth(), bg = Auna.Mint) {
                AIcon(Icons.Eye, Auna.Green, 22.dp, 2.2f)
                Txt("Ver lo que ve quien escanea", 17, 900, Auna.Green)
            }
            AButton({}, Modifier.fillMaxWidth(), bg = Auna.Bg, minHeight = 48.dp) {
                Txt("He perdido la tarjeta", 16, 800, Auna.Forest, underline = true)
            }
        }
        return
    }

    // Vista previa (lo que ve quien escanea)
    Column(Modifier.fillMaxSize()) {
        BackHeader(
            "Lo que ve quien escanea", { go("card") },
            Modifier.padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 12.dp), titleSize = 22,
        )
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Auna.White)
                .border(2.dp, Auna.Border, RoundedCornerShape(30.dp)),
        ) {
            val frameH = maxHeight
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = frameH)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (view != "home") {
                        AButton({ go("home") }, bg = Auna.Bg, radius = 20.dp, minHeight = 40.dp, hPadding = 10.dp, arrangement = Arrangement.spacedBy(4.dp)) {
                            AIcon(Icons.ChevronLeft, Auna.Ink, 18.dp, 2.5f)
                            Txt("Atrás", 15, 800)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Tag("Vista previa", Auna.GreyCard, Auna.Ink, fontSize = 13, weight = 900)
                }

                when (view) {
                    "home" -> PreviewHome(name, Modifier.weight(1f), onLogin = { go("login") }, onEmergency = { go("emergencia") }, onTutors = { go("tutores") })
                    "login" -> PreviewLogin(loginStep, onAskCode = { loginStep = "code" }, onEnter = { navi.tab(Routes.PASAPORTE) })
                    "emergencia" -> PreviewEmergency(called) { called = true }
                    else -> PreviewTutors(loc) { loc = true }
                }
            }
        }
    }
}

@Composable
private fun PreviewHome(name: String, modifier: Modifier, onLogin: () -> Unit, onEmergency: () -> Unit, onTutors: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Auna.Bg)
            .border(2.dp, Auna.Border, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(Auna.White), contentAlignment = Alignment.Center) { Girasol(56.dp) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt("Hola, soy $name", 24, 900, lineHeight = 1.1)
            Txt("No hablo, pero me comunico", 16, 700, Auna.Muted)
        }
    }
    Txt(
        "Para ver mi pasaporte completo, inicia sesión.", 18, 700, lineHeight = 1.4, align = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AButton(onLogin, hPadding = 36.dp) { Txt("Iniciar sesión", 18, 900, Auna.White) }
    }
    Spacer(modifier.heightIn(min = 24.dp))
    Txt("¿Alguna emergencia?", 20, 900, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigAction("Emergencia", Icons.Warning, Auna.Red, Modifier.weight(1f), onEmergency)
        BigAction("Tutores legales", Icons.People, Auna.Green, Modifier.weight(1f), onTutors)
    }
}

@Composable
private fun BigAction(label: String, icon: com.auna.padres.ui.AunaIcon, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    AButton(onClick, modifier, bg = bg, radius = 22.dp, minHeight = 104.dp, hPadding = 8.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AIcon(icon, Auna.White, 34.dp, 2.2f)
            Txt(label, 18, 900, Auna.White, align = TextAlign.Center)
        }
    }
}

@Composable
private fun PreviewLogin(step: String, onAskCode: () -> Unit, onEnter: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    Txt("Iniciar sesión", 24, 900)
    if (step == "phone") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt("Tu número de móvil", 15, 800, Auna.Muted)
            AField(
                phone, { phone = it }, "600 000 000", Modifier.fillMaxWidth(),
                fontSize = 20, radius = 16.dp, minHeight = 56.dp, padding = 16.dp,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
        }
        PrimaryButton("Recibir código", onAskCode, Modifier.fillMaxWidth())
        Txt("Solo para la familia y quien ella autorice.", 15, 700, Auna.Muted, Modifier.fillMaxWidth(), align = TextAlign.Center)
    } else {
        Txt(
            "Te hemos enviado un código por WhatsApp.", 16, 800, Auna.Green,
            Modifier.fillMaxWidth().card(Auna.Mint, 16.dp).padding(horizontal = 14.dp, vertical = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt("Código", 15, 800, Auna.Muted)
            AField(
                code, { code = it.filter(Char::isDigit).take(4) }, "0000", Modifier.fillMaxWidth(),
                fontSize = 24, weight = 800, radius = 16.dp, minHeight = 56.dp, padding = 16.dp,
                letterSpacing = 0.3.em,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        }
        PrimaryButton("Entrar", onEnter, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PreviewEmergency(called: Boolean, onCall: () -> Unit) {
    AButton(onCall, Modifier.fillMaxWidth(), bg = Auna.Red, radius = 22.dp, minHeight = 88.dp, arrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
        AIcon(Icons.Phone, Auna.White, 32.dp)
        Txt("Llamar al 112", 26, 900, Auna.White)
    }
    val fg = if (called) Auna.Green else Auna.Ink
    Row(
        Modifier.fillMaxWidth().card(if (called) Auna.Mint else Auna.GreyCard, 14.dp).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AIcon(Icons.Bell, fg, 18.dp, 2.4f)
        Txt(if (called) "Avisando a mamá y papá…" else "Al llamar, avisamos a sus padres", 15, 800, fg)
    }
    Column(Modifier.fillMaxWidth().card(Auna.Bg, 18.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Txt("Di esto", 13, 900, Auna.Muted, upper = true)
        Txt("Niño de 9 años. No habla. Tiene epilepsia.", 17, 800, lineHeight = 1.35)
    }
    Column(Modifier.fillMaxWidth().card(Auna.MintStrong, 18.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Txt("Su familia dice", 13, 900, Auna.DeepGreen, upper = true)
        Txt("Ponle de lado. No le sujetes. Nada en la boca.", 17, 800, Auna.DeepGreen, lineHeight = 1.35)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AButton({}, Modifier.weight(1f), bg = Auna.Leaf, hPadding = 8.dp) { Txt("Llamar a mamá", 17, 900) }
        AButton({}, Modifier.weight(1f), bg = Auna.Leaf, hPadding = 8.dp) { Txt("Llamar a papá", 17, 900) }
    }
}

@Composable
private fun PreviewTutors(loc: Boolean, onLocation: () -> Unit) {
    Txt("Mis tutores legales", 24, 900)
    Column(Modifier.fillMaxWidth().card(Auna.Bg, 20.dp).padding(horizontal = 14.dp, vertical = 4.dp)) {
        listOf(Triple("A", "Mamá · Ana", "Llamar a mamá"), Triple("J", "Papá · Javier", "Llamar a papá")).forEachIndexed { i, (ini, label, desc) ->
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 76.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Initial(ini)
                Txt(label, 18, 900, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Auna.Green).semantics { contentDescription = desc },
                    contentAlignment = Alignment.Center,
                ) { AIcon(Icons.Phone, Auna.White, 24.dp) }
            }
            if (i == 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Auna.Line))
        }
    }
    SecondaryButton(if (loc) "Ubicación enviada" else "Enviar mi ubicación", onLocation, Modifier.fillMaxWidth(), fontSize = 17, icon = Icons.Pin)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).card(Auna.Mint, 16.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Txt("Sí", 18, 900, Auna.Green)
            Txt("Sonrío y toco tu mano", 14, 700)
        }
        Column(Modifier.weight(1f).card(Auna.CoralBg, 16.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Txt("No", 18, 900, Auna.Coral)
            Txt("Giro la cabeza", 14, 700, Auna.Coral)
        }
    }
}

/** QR de ejemplo del diseño (no contiene ningún enlace real). */
@Composable
private fun QrMock(sizeDp: Int) {
    val dark = remember {
        PathParser().parsePathString(
            "M1 1h7v7H1zM21 1h7v7h-7zM1 21h7v7H1zM3 3h3v3H3zM23 3h3v3h-3zM3 23h3v3H3zM10 1h2v2h-2zM14 2h2v3h-2zM10 5h3v2h-3zM17 6h2v2h-2zM10 10h3v2h-3zM15 9h4v2h-4zM21 10h3v3h-3zM1 10h3v2H1zM5 12h3v2H5zM12 13h2v4h-2zM16 14h3v2h-3zM24 15h4v2h-4zM20 17h2v3h-2zM10 19h4v2h-4zM15 20h2v4h-2zM19 22h4v2h-4zM24 20h3v2h-3zM10 24h3v4h-3zM18 25h2v3h-2zM22 26h5v2h-5zM2 15h4v2H2z",
        ).toPath()
    }
    val light = remember { PathParser().parsePathString("M2 2h5v5H2zM22 2h5v5h-5zM2 22h5v5H2z").toPath() }
    val outer = remember { PathParser().parsePathString("M1 1h7v7H1zM21 1h7v7h-7zM1 21h7v7H1z").toPath() }
    val inner = remember { PathParser().parsePathString("M3 3h3v3H3zM23 3h3v3h-3zM3 23h3v3H3z").toPath() }
    Canvas(Modifier.size(sizeDp.dp).semantics { contentDescription = "Código QR de ejemplo" }) {
        val k = size.minDimension / 29f
        scale(k, k, pivot = Offset.Zero) {
            drawPath(dark, Auna.Ink)
            // Ojos del QR: marco oscuro, hueco blanco, centro oscuro (mismo orden que el SVG del diseño).
            drawPath(outer, Auna.Ink)
            drawPath(light, Auna.White)
            drawPath(inner, Auna.Ink)
        }
    }
}
