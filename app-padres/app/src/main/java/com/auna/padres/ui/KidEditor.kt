package com.auna.padres.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.auna.padres.AunaViewModel
import com.auna.padres.data.Kid
import com.auna.padres.ui.theme.Auna

/**
 * Crear o editar el perfil de un niño: nombre, edad, si habla o no y foto (de la galería).
 * Con [kid] = null crea uno nuevo y llama a [onCreated] con su id.
 */
@Composable
fun KidEditor(vm: AunaViewModel, kid: Kid?, onDismiss: () -> Unit, onCreated: (String, String) -> Unit = { _, _ -> }) {
    var noHabla by remember { mutableStateOf(kid?.passport?.noHabla ?: false) }
    var pendingPhoto by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            if (kid != null) vm.setPhoto(kid.id, uri) else pendingPhoto = uri
        }
    }
    val ageNumber = kid?.age?.takeWhile { it.isDigit() }?.ifEmpty { kid.age } ?: ""

    EditSheet(
        title = if (kid == null) "Nuevo perfil" else "Editar perfil",
        fields = listOf(
            Field("Nombre", kid?.name ?: "", "Ej.: Lucía"),
            Field("Edad", ageNumber, "Ej.: 7", keyboard = KeyboardType.Number),
        ),
        onDismiss = onDismiss,
        saveLabel = if (kid == null) "Crear perfil" else "Guardar",
        onSave = { (name, age) ->
            if (kid == null) {
                val id = vm.createKid(name, age, noHabla)
                pendingPhoto?.let { vm.setPhoto(id, it) }
                onCreated(id, name.trim().ifEmpty { "Nuevo perfil" })
            } else {
                vm.editKid(kid.id, name, age, noHabla)
            }
            onDismiss()
        },
        onDelete = kid?.let { k -> { vm.deleteKid(k.id); onDismiss() } },
        deleteLabel = "Eliminar a ${kid?.name ?: ""}",
        extra = {
            Row(
                Modifier.fillMaxWidth().card(Auna.White, 18.dp).padding12(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val pending = pendingPhoto
                if (pending != null) {
                    PendingPhoto(pending)
                } else {
                    Photo(kid?.photo, kid?.name ?: "el niño", 64.dp, placeholderColor = Auna.Balance, showLabel = false, ring = null)
                }
                Txt("Foto", 17, 800, modifier = Modifier.weight(1f))
                SecondaryButton(
                    if (kid?.photo != null || pending != null) "Cambiar" else "Elegir",
                    { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    height = 44.dp, fontSize = 15, radius = 14.dp, weight = 800,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .card(Auna.White, 18.dp)
                    .toggleable(value = noHabla, role = Role.Checkbox) { noHabla = it }
                    .padding12(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("No habla (se comunica de otra forma)", 16, 800, modifier = Modifier.weight(1f))
                Checkbox(noHabla, null, colors = CheckboxDefaults.colors(checkedColor = Auna.Green, uncheckedColor = Auna.Muted))
            }
        },
    )
}

@Composable
private fun PendingPhoto(uri: Uri) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val bmp = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                ctx.contentResolver.openInputStream(uri)!!.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 })
                }!!.asImageBitmap()
            }.getOrNull()
        }
    }.value
    if (bmp != null) Image(bmp, "Foto elegida", Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    else Photo(null, "el niño", 64.dp, placeholderColor = Auna.Balance, showLabel = false, ring = null)
}

private fun Modifier.padding12() = this.padding(12.dp)
