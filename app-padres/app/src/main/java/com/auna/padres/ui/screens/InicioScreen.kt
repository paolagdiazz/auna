package com.auna.padres.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.R
import com.auna.padres.data.PASSPORT_TOTAL
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.CircleIconButton
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.KidPhoto
import com.auna.padres.ui.Navi
import com.auna.padres.ui.NoKid
import com.auna.padres.ui.ProgressBar
import com.auna.padres.ui.Routes
import com.auna.padres.ui.Tag
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna

@Composable
fun InicioScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    val kid = s.activeKid
    val pending = s.pendingProposals.size

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Gutter, end = Gutter, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.logo_horizontal),
                contentDescription = "Aúna",
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier.height(44.dp).width(128.dp),
            )
            Box(Modifier.weight(1f))
            Box {
                CircleIconButton(
                    Icons.Bell,
                    if (pending > 0) "Avisos: $pending propuestas" else "Avisos",
                    { navi.open(Routes.PROPUESTAS) },
                    iconSize = 24.dp, stroke = 2f,
                )
                if (pending > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Auna.CoralBg),
                        contentAlignment = Alignment.Center,
                    ) { Txt("$pending", 12, 900, Auna.Coral) }
                }
            }
            Box(Modifier.width(8.dp))
            CircleIconButton(Icons.Settings, "Ajustes", { navi.open(Routes.AJUSTES) }, iconSize = 24.dp, stroke = 2f)
        }

        Txt("¡Hola, familia!", 30, 900, modifier = Modifier.padding(top = 2.dp))

        if (kid == null) {
            NoKid({ navi.tab(Routes.PERFILES) })
        } else {
            // Tarjeta del pasaporte
            Column(
                Modifier
                    .fillMaxWidth()
                    .card(Auna.GreyCard, 26.dp)
                    .clickable(role = Role.Button) { navi.tab(Routes.PASAPORTE) }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KidPhoto(kid, 104.dp)
                    Column(Modifier.weight(1f)) {
                        Txt(kid.name, 30, 900, lineHeight = 1.1)
                        Txt("Mi pasaporte", 17, 700, Auna.Muted)
                    }
                    AIcon(Icons.ChevronRight, Auna.Green, 26.dp, 2.5f)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProgressBar(kid.done / PASSPORT_TOTAL.toFloat(), Modifier.weight(1f))
                    Txt("${kid.done}/$PASSPORT_TOTAL", 15, 900)
                }
            }
        }

        if (pending > 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .card(Auna.CoralBg, 20.dp)
                    .clickable(role = Role.Button) { navi.open(Routes.PROPUESTAS) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Auna.White), contentAlignment = Alignment.Center) {
                    AIcon(Icons.Alert, Auna.Coral, 22.dp, 2.2f)
                }
                Txt(
                    if (pending == 1) "1 propuesta sin revisar" else "$pending propuestas sin revisar",
                    17, 900, Auna.Coral, Modifier.weight(1f),
                )
                AIcon(Icons.ChevronRight, Auna.Coral, 22.dp, 2.5f)
            }
        }

        if (kid != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .card(Auna.Leaf, 26.dp)
                    .clickable(role = Role.Button) { navi.tab(Routes.HABLAR) }
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(68.dp).clip(CircleShape).background(Auna.White), contentAlignment = Alignment.Center) {
                    AIcon(Icons.Mic, Auna.Green, 32.dp, 2.2f)
                }
                Column(Modifier.weight(1f)) {
                    Txt("Hablar de ${kid.name}", 23, 900, lineHeight = 1.15)
                    Txt("Habla y lo ordenamos", 16, 700, Auna.DeepGreen)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("Accesos", Icons.People, Modifier.weight(1f), badge = if (s.requestPending) "1 nueva" else null) {
                navi.tab(Routes.ACCESOS)
            }
            Tile("Mi tarjeta QR", Icons.Scan, Modifier.weight(1f)) { navi.open(Routes.QR) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 68.dp)
                .card(Auna.White, 20.dp)
                .clickable(role = Role.Button) { navi.open(Routes.PROBAR) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconDisc(Icons.ChatCheck)
            Txt("Comprueba las respuestas", 17, 900, modifier = Modifier.weight(1f))
            AIcon(Icons.ChevronRight, Auna.Green, 22.dp, 2.5f)
        }
    }
}

@Composable
private fun Tile(label: String, icon: com.auna.padres.ui.AunaIcon, modifier: Modifier, badge: String? = null, onClick: () -> Unit) {
    Box(
        modifier
            .defaultMinSize(minHeight = 100.dp)
            .card(Auna.White, 20.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconDisc(icon)
            Txt(label, 17, 900)
        }
        if (badge != null) {
            Box(Modifier.align(Alignment.TopEnd)) {
                Tag(badge, Auna.CoralBg, Auna.Coral, fontSize = 12, weight = 900, hPad = 8.dp, vPad = 2.dp)
            }
        }
    }
}

@Composable
internal fun IconDisc(icon: com.auna.padres.ui.AunaIcon, size: Int = 44, bg: androidx.compose.ui.graphics.Color = Auna.Mint) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(size.dp / 2)).background(bg), contentAlignment = Alignment.Center) {
        AIcon(icon, Auna.Green, 22.dp, 2.2f)
    }
}
