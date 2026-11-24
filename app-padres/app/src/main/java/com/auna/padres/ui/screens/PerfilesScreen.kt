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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.data.PASSPORT_TOTAL
import com.auna.padres.ui.AButton
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.CircleIconButton
import com.auna.padres.ui.Girasol
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.KidEditor
import com.auna.padres.ui.KidPhoto
import com.auna.padres.ui.Navi
import com.auna.padres.ui.PrimaryButton
import com.auna.padres.ui.ProgressBar
import com.auna.padres.ui.Routes
import com.auna.padres.ui.Tag
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna

@Composable
fun PerfilesScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var created by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Txt("Mis perfiles", 26, 900)

        s.kids.forEach { k ->
            val active = s.activeKid?.id == k.id
            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (active) Auna.Mint else Auna.White)
                    .border(2.dp, if (active) Auna.Green else Auna.White, shape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clickable(role = Role.Button) { vm.selectKid(k.id) }
                            .semantics { contentDescription = "Elegir el perfil de ${k.name}" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        KidPhoto(k, 72.dp, placeholderColor = Auna.Balance, showLabel = false)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Txt(k.name, 22, 900)
                                if (active) Tag("Activo", Auna.Green, Auna.White, fontSize = 12, weight = 900, hPad = 8.dp, vPad = 2.dp)
                            }
                            if (k.age.isNotBlank()) Txt(k.age, 15, 700, Auna.Muted)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProgressBar(k.passport.done / PASSPORT_TOTAL.toFloat(), Modifier.weight(1f), height = 8.dp, track = Auna.Line)
                                Txt("${k.passport.done}/$PASSPORT_TOTAL", 13, 900)
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircleIconButton(
                            Icons.Pencil, "Editar el perfil de ${k.name}", { editingId = k.id },
                            size = 44.dp, iconSize = 20.dp, iconColor = Auna.Green, stroke = 2.2f,
                        )
                        CircleIconButton(
                            Icons.Trash, "Eliminar el perfil de ${k.name}", { confirm = k.id },
                            size = 44.dp, iconSize = 20.dp, iconColor = Auna.Forest, stroke = 2.2f,
                        )
                    }
                }
                if (confirm == k.id) {
                    Column(
                        Modifier.fillMaxWidth().card(Auna.MintStrong, 18.dp).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Txt("¿Eliminar a ${k.name}?", 17, 900, Auna.DeepGreen)
                        Txt("Se borran su pasaporte, su QR y sus accesos. No se puede deshacer.", 15, 700, Auna.DeepGreen)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AButton({ confirm = null }, Modifier.weight(1f), bg = Auna.White, border = Auna.Border, radius = 14.dp, minHeight = 48.dp) {
                                Txt("Cancelar", 15, 800)
                            }
                            AButton(
                                {
                                    if (created == k.name) created = null
                                    vm.deleteKid(k.id)
                                    confirm = null
                                },
                                Modifier.weight(1f), bg = Auna.Forest, radius = 14.dp, minHeight = 48.dp,
                            ) { Txt("Eliminar", 15, 900, Auna.White) }
                        }
                    }
                }
            }
        }

        if (s.kids.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().card(Auna.White, 24.dp).padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Girasol(72.dp)
                Txt("Aún no hay perfiles", 19, 900)
            }
        }

        PrimaryButton("Añadir niño", { adding = true; confirm = null }, Modifier.fillMaxWidth(), height = 64.dp, icon = Icons.Plus)

        val just = created
        if (just != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .card(Auna.Leaf, 22.dp)
                    .clickable(role = Role.Button) { navi.tab(Routes.HABLAR) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Auna.White), contentAlignment = Alignment.Center) {
                    AIcon(Icons.Mic, Auna.Green, 26.dp, 2.2f)
                }
                Txt("Hablar de $just", 18, 900, modifier = Modifier.weight(1f))
                AIcon(Icons.ChevronRight, Auna.Green, 22.dp, 2.5f)
            }
        }

        if (s.activeKid != null) {
            Row(
                Modifier.fillMaxWidth().card(Auna.Mint, 22.dp).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                    Triple(Icons.Passport, "Su pasaporte") { navi.tab(Routes.PASAPORTE) },
                    Triple(Icons.Scan, "Su QR") { navi.open(Routes.QR) },
                    Triple(Icons.People, "Sus accesos") { navi.tab(Routes.ACCESOS) },
                ).forEach { (icon, label, go) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.Button, onClick = go)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AIcon(icon, Auna.Green, 26.dp, 2f)
                        Txt(label, 14, 800, Auna.Green)
                    }
                }
            }
        }
    }

    if (adding) {
        KidEditor(vm, null, onDismiss = { adding = false }, onCreated = { _, name -> created = name })
    }
    editingId?.let { id ->
        val kid = s.kids.firstOrNull { it.id == id }
        if (kid == null) editingId = null else KidEditor(vm, kid, onDismiss = { editingId = null })
    }
}
