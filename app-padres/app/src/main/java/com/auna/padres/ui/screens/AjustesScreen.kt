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
    val cloudUi by vm.cloudUi.collectAsState()
    var accountSheet by rememberSaveable { mutableStateOf(false) }
    var accountMsg by remember { mutableStateOf<String?>(null) }
    var accountBusy by remember { mutableStateOf(false) }
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

        if (com.auna.padres.cloud.CloudConfig.enabled) {
            Row(
                Modifier.fillMaxWidth().card(Auna.White, 22.dp).clickable(role = Role.Button) { accountSheet = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AIcon(Icons.People, Auna.Green, 24.dp, 2.2f)
                Column(Modifier.weight(1f)) {
                    Txt("Cuenta de la familia", 17, 800)
                    Txt(
                        when {
                            !cloudUi.signedIn -> "Sin conectar: la web no ve el pasaporte"
                            cloudUi.syncing -> "Sincronizando…"
                            cloudUi.error != null -> cloudUi.error!!
                            else -> "Conectada · ${cloudUi.email ?: ""}"
                        },
                        14, 700, if (!cloudUi.signedIn || cloudUi.error != null) Auna.Forest else Auna.Green,
                    )
                }
                AIcon(Icons.ChevronRight, Auna.Muted, 20.dp, 2.5f)
            }
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
                    Txt("IA para Hablar y Comprobar", 17, 800)
                    Txt(
                        when {
                            key.isNotBlank() -> "Conectada a ${com.auna.padres.Ai.providerName(key)}"
                            vm.aiReady -> "IA gratuita de Aúna (con tu cuenta)"
                            else -> "Sin conectar: añade tu clave o entra en tu cuenta"
                        },
                        14, 700, if (!vm.aiReady) Auna.Forest else Auna.Green,
                    )
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

    if (accountSheet) {
        if (cloudUi.signedIn) {
            EditSheet(
                title = "Cuenta de la familia",
                fields = emptyList(),
                onDismiss = { accountSheet = false },
                saveLabel = if (cloudUi.syncing) "Sincronizando…" else "Sincronizar ahora",
                onSave = { vm.syncNow() },
                onDelete = { vm.cloudSignOut(); accountSheet = false },
                deleteLabel = "Cerrar sesión",
                deleteConfirmText = "El pasaporte se queda en este móvil y en la nube. Puedes volver a entrar cuando quieras.",
                extra = {
                    Txt("Conectada como ${cloudUi.email ?: ""}.", 16, 800)
                    Txt(
                        "El pasaporte se guarda también en la nube para que las personas que aceptes lo vean en la web. " +
                            "Las secciones «Solo familia» no se comparten. Las solicitudes, propuestas y avisos de la web llegan aquí solos.",
                        14, 600, Auna.Muted, lineHeight = 1.4,
                    )
                    cloudUi.error?.let { Txt(it, 15, 800, Auna.Coral) }
                },
            )
        } else {
            EditSheet(
                title = "Cuenta de la familia",
                fields = listOf(
                    Field("Correo", cloudUi.email ?: "", "familia@correo.es", keyboard = androidx.compose.ui.text.input.KeyboardType.Email),
                    Field("Contraseña", "", "Mínimo 8 caracteres", secret = true),
                ),
                onDismiss = { if (!accountBusy) { accountSheet = false; accountMsg = null } },
                saveLabel = if (accountBusy) "Entrando…" else "Entrar",
                onSave = { (email, pass) ->
                    if (!accountBusy) { accountBusy = true; accountMsg = null
                        scope.launch { val e = vm.cloudSignIn(email, pass, create = false); accountBusy = false; if (e == null) accountSheet = false else accountMsg = e }
                    }
                },
                onDelete = null,
                extra = {
                    Txt(
                        "Con la cuenta de la familia, profes y familiares pueden pedir acceso desde la web y tú decides aquí. " +
                            "Si es tu primera vez, escribe tu correo y una contraseña y pulsa «Crear cuenta».",
                        14, 600, Auna.Muted, lineHeight = 1.4,
                    )
                    accountMsg?.let { Txt(it, 15, 800, Auna.Coral) }
                },
                secondaryLabel = "Crear cuenta",
                onSecondary = { (email, pass) ->
                    if (!accountBusy) { accountBusy = true; accountMsg = null
                        scope.launch { val e = vm.cloudSignIn(email, pass, create = true); accountBusy = false; if (e == null) accountSheet = false else accountMsg = e }
                    }
                },
            )
        }
    }

    if (keySheet) {
        EditSheet(
            title = "Conectar la IA",
            fields = listOf(Field("Clave de Groq (gratis) o de Claude", key, "gsk_… o sk-ant-…", secret = true)),
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
                    "Aúna usa una IA para ordenar lo que cuentas en «Hablar» y para contestar en «Comprueba las respuestas». " +
                        "Gratis: entra en console.groq.com con tu cuenta de Google, ve a API Keys › Create API Key y pega aquí la clave (empieza por gsk_). " +
                        "También vale una clave de Claude (console.anthropic.com, de pago). La clave se guarda solo en este móvil. " +
                        "Al usarla se envía a Groq o a Anthropic el pasaporte del niño activo y lo que acabas de contar o preguntar.",
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
