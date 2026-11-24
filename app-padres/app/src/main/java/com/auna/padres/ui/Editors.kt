package com.auna.padres.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.auna.padres.ui.theme.Auna

/** Un campo del editor. */
data class Field(
    val label: String,
    val value: String,
    val placeholder: String = "",
    val multiline: Boolean = false,
    val keyboard: KeyboardType = KeyboardType.Text,
    val secret: Boolean = false,
)

/**
 * Hoja inferior para crear o editar algo: uno o varios campos, «Guardar» y, si
 * se puede, «Borrar» (con confirmación). Reutilizada en toda la app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheet(
    title: String,
    fields: List<Field>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String = "Borrar",
    saveLabel: String = "Guardar",
    extra: @Composable () -> Unit = {},
) {
    val values = remember { mutableStateListOf(*fields.map { it.value }.toTypedArray()) }
    val focus = remember { FocusRequester() }
    val confirm = remember { mutableStateListOf<Boolean>() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Auna.Bg,
        scrimColor = Auna.Scrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(title, 22, 900, modifier = Modifier.weight(1f))
                CircleIconButton(Icons.Close, "Cerrar", onDismiss, size = 44.dp, iconSize = 20.dp)
            }
            fields.forEachIndexed { i, f ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Txt(f.label, 15, 800, Auna.Muted)
                    AField(
                        values[i], { values[i] = it }, f.placeholder,
                        Modifier.fillMaxWidth().then(if (i == 0) Modifier.focusRequester(focus) else Modifier),
                        bg = Auna.White,
                        singleLine = !f.multiline,
                        minHeight = if (f.multiline) 96.dp else 52.dp,
                        contentAlignment = if (f.multiline) Alignment.TopStart else Alignment.CenterStart,
                        keyboardOptions = if (f.secret) KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)
                        else KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = f.keyboard),
                        secret = f.secret,
                    )
                }
            }
            extra()
            PrimaryButton(saveLabel, { onSave(values.toList()) }, Modifier.fillMaxWidth())
            if (onDelete != null) {
                SecondaryButton(deleteLabel, { confirm.add(true) }, Modifier.fillMaxWidth(), height = 52.dp, fontSize = 16, color = Auna.Forest, weight = 800)
            }
        }
    }
    LaunchedEffect(Unit) { if (fields.isNotEmpty() && fields[0].value.isEmpty()) runCatching { focus.requestFocus() } }
    if (confirm.isNotEmpty() && onDelete != null) {
        ConfirmDialog(
            title = "¿$deleteLabel?",
            text = "No se puede deshacer.",
            confirmLabel = deleteLabel,
            onConfirm = { confirm.clear(); onDelete() },
            onDismiss = { confirm.clear() },
        )
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Auna.Bg,
        title = { Txt(title, 20, 900) },
        text = { Txt(text, 16, 600, Auna.Muted, lineHeight = 1.35) },
        confirmButton = { TextButton(onClick = onConfirm) { Txt(confirmLabel, 16, 900, Auna.Forest) } },
        dismissButton = { TextButton(onClick = onDismiss) { Txt("Cancelar", 16, 800) } },
    )
}

/** Aviso corto de una línea con acción opcional (p. ej. «Deshacer»). */
@Composable
fun Toast(text: String, actionLabel: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().card(Auna.Mint, 16.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AIcon(Icons.Check, Auna.Green, 18.dp, 2.8f)
        Txt(text, 16, 800, Auna.Green, Modifier.weight(1f))
        if (actionLabel != null) {
            AButton(onAction, bg = Auna.CoralBg, radius = 20.dp, minHeight = 40.dp, hPadding = 14.dp) {
                Txt(actionLabel, 15, 900, Auna.Coral)
            }
        }
    }
}
