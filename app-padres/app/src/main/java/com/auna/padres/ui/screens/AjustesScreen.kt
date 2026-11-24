package com.auna.padres.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.auna.padres.ui.EditSheet
import com.auna.padres.ui.Field
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.ui.AIcon
import com.auna.padres.ui.AunaIcon
import com.auna.padres.ui.BackHeader
import com.auna.padres.ui.Gutter
import com.auna.padres.ui.Icons
import com.auna.padres.ui.Navi
import com.auna.padres.ui.Routes
import com.auna.padres.ui.SecondaryButton
import com.auna.padres.ui.Txt
import com.auna.padres.ui.card
import com.auna.padres.ui.theme.Auna

@Composable
fun AjustesScreen(vm: AunaViewModel, navi: Navi) {
    val s by vm.state.collectAsState()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val key by vm.claudeKey.collectAsState()
    var keySheet by rememberSaveable { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var keyMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val names = s.kids.map { it.name }
    val namesLabel = when (names.size) {
        0 -> "Aún no hay perfiles"
        1 -> names[0]
        else -> names.dropLast(1).joinToString(", ") + " y " + names.last()
    }

    Column(
        Modifier.fillMaxSize().padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader("Ajustes", { navi.back() })

        Row(
            Modifier
                .fillMaxWidth()
                .card(Auna.Mint, 22.dp)
                .clickable(role = Role.Button) { navi.tab(Routes.PERFILES) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Auna.White), contentAlignment = Alignment.Center) {
                AIcon(Icons.Profile, Auna.Green, 24.dp, 2.2f)
            }
            Column(Modifier.weight(1f)) {
                Txt("Gestionar perfiles", 18, 900)
                Txt(namesLabel, 15, 700, Auna.Muted)
            }
            AIcon(Icons.ChevronRight, Auna.Green, 22.dp, 2.5f)
        }

        Column(Modifier.fillMaxWidth().card(Auna.White, 22.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
            LinkRow(Icons.Scan, "Mi tarjeta QR") { navi.open(Routes.QR) }
            Divider()
            LinkRow(Icons.Lock, "Privacidad") { navi.tab(Routes.ACCESOS) }
            Divider()
            ToggleRow(Icons.Volume, "Leer en voz alta las respuestas", s.readAloud) { vm.setReadAloud(it) }
            Divider()
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).clickable(role = Role.Button) { keySheet = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AIcon(Icons.Sparkle, Auna.Green, 24.dp, 2.2f)
                Column(Modifier.weight(1f)) {
                    Txt("Claude (IA)", 17, 800)
                    Txt(if (key.isBlank()) "Sin conectar: añade tu clave" else "Conectado", 14, 700, if (key.isBlank()) Auna.Forest else Auna.Green)
                }
                AIcon(Icons.ChevronRight, Auna.Muted, 20.dp, 2.5f)
            }
        }

        Spacer(Modifier.weight(1f))
        SecondaryButton(
            "Borrar todos los datos", { confirmReset = true }, Modifier.fillMaxWidth(),
            height = 52.dp, fontSize = 16, radius = 16.dp, color = Auna.Forest, weight = 800,
        )
    }

    if (keySheet) {
        EditSheet(
            title = "Conectar Claude",
            fields = listOf(Field("Clave de la API de Anthropic", key, "sk-ant-…", secret = true)),
            onDismiss = { if (!checking) { keySheet = false; keyMsg = null } },
            saveLabel = if (checking) "Comprobando…" else "Guardar y comprobar",
            onSave = { (k) ->
                if (!checking) {
                    checking = true; keyMsg = null
                    scope.launch {
                        val err = vm.saveClaudeKey(k)
                        checking = false
                        if (err == null) { keySheet = false } else keyMsg = err
                    }
                }
            },
            onDelete = if (key.isNotBlank()) ({ scope.launch { vm.saveClaudeKey("") }; keySheet = false }) else null,
            deleteLabel = "Quitar la clave",
            extra = {
                Txt(
                    "Aúna usa Claude para ordenar lo que cuentas en «Hablar» y para contestar en «Comprueba las respuestas». " +
                        "Consigue la clave en console.anthropic.com (API Keys). Se guarda solo en este móvil. " +
                        "Cuando la usas, se envía a Anthropic el pasaporte del niño activo y lo que acabas de contar o preguntar.",
                    14, 600, Auna.Muted, lineHeight = 1.4,
                )
                keyMsg?.let { Txt(it, 15, 800, Auna.Coral) }
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Auna.Bg,
            title = { Txt("¿Borrar todos los datos?", 20, 900) },
            text = {
                Txt(
                    "Se borra todo lo que has cambiado en este móvil y la app vuelve al principio. No se puede deshacer.",
                    16, 600, Auna.Muted, lineHeight = 1.35,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.resetAll(); confirmReset = false }) { Txt("Borrar", 16, 900, Auna.Forest) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Txt("Cancelar", 16, 800) }
            },
        )
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Auna.Line))

@Composable
private fun LinkRow(icon: AunaIcon, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AIcon(icon, Auna.Green, 24.dp, 2.2f)
        Txt(label, 17, 800, modifier = Modifier.weight(1f))
        AIcon(Icons.ChevronRight, Auna.Muted, 20.dp, 2.5f)
    }
}

@Composable
private fun ToggleRow(icon: AunaIcon, label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).toggleable(value = on, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AIcon(icon, Auna.Green, 24.dp, 2.2f)
        Txt(label, 17, 800, modifier = Modifier.weight(1f))
        Checkbox(on, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Auna.Green, uncheckedColor = Auna.Muted))
    }
}
