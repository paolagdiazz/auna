package com.auna.padres.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.auna.padres.R
import com.auna.padres.data.Kid
import com.auna.padres.ui.theme.Auna
import com.auna.padres.ui.theme.Nunito

val Gutter = 20.dp

/** Texto con la tipografía del diseño (Nunito). `weight` en la escala CSS (400–900). */
@Composable
fun Txt(
    text: String,
    size: Int,
    weight: Int = 700,
    color: Color = Auna.Ink,
    modifier: Modifier = Modifier,
    lineHeight: Double? = null,
    align: TextAlign = TextAlign.Start,
    upper: Boolean = false,
    underline: Boolean = false,
) {
    Text(
        text = if (upper) text.uppercase() else text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight(weight),
        fontFamily = Nunito,
        lineHeight = lineHeight?.let { (size * it).sp } ?: TextUnit.Unspecified,
        textAlign = align,
        letterSpacing = if (upper) 0.04.em else TextUnit.Unspecified,
        textDecoration = if (underline) TextDecoration.Underline else null,
    )
}

/** Botón base: fondo, borde opcional (liso o discontinuo) y área táctil completa. */
@Composable
fun AButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = Auna.Green,
    border: Color? = null,
    borderWidth: Dp = 2.dp,
    dashed: Boolean = false,
    radius: Dp = 18.dp,
    minHeight: Dp = 56.dp,
    hPadding: Dp = 16.dp,
    contentDescription: String? = null,
    arrangement: Arrangement.Horizontal = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Row(
        modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(shape)
            .background(bg)
            .then(
                when {
                    border == null -> Modifier
                    dashed -> Modifier.dashedBorder(border, borderWidth, radius)
                    else -> Modifier.border(borderWidth, border, shape)
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .padding(horizontal = hPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = arrangement,
        content = content,
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    fontSize: Int = 18,
    radius: Dp = 18.dp,
    icon: AunaIcon? = null,
    bg: Color = Auna.Green,
) {
    AButton(onClick, modifier, bg = bg, radius = radius, minHeight = height) {
        if (icon != null) AIcon(icon, Auna.White, if (height >= 64.dp) 24.dp else 22.dp, stroke = 2.6f)
        Txt(text, fontSize, 900, Auna.White)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    fontSize: Int = 18,
    radius: Dp = 18.dp,
    color: Color = Auna.Green,
    weight: Int = 900,
    icon: AunaIcon? = null,
) {
    AButton(onClick, modifier, bg = Auna.White, border = color, radius = radius, minHeight = height) {
        if (icon != null) AIcon(icon, color, 22.dp, stroke = 2.2f)
        Txt(text, fontSize, weight, color)
    }
}

/** Botón de opción (chip) con estado seleccionado. */
@Composable
fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    radius: Dp = 14.dp,
    fontSize: Int = 15,
    selBg: Color = Auna.Mint,
    selBorder: Color = Auna.Green,
    bg: Color = Auna.White,
    borderColor: Color = Auna.Border,
) {
    AButton(
        onClick,
        modifier.semantics { this.selected = selected },
        bg = if (selected) selBg else bg,
        border = if (selected) selBorder else borderColor,
        radius = radius,
        minHeight = height,
        hPadding = 12.dp,
    ) { Txt(text, fontSize, 800) }
}

@Composable
fun CircleIconButton(
    icon: AunaIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = Auna.White,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    iconColor: Color = Auna.Ink,
    stroke: Float = 2.5f,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { AIcon(icon, iconColor, iconSize, stroke) }
}

/** Cabecera de pantalla secundaria: volver + título (+ subtítulo y acción opcional). */
@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleSize: Int = 24,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircleIconButton(Icons.ChevronLeft, "Volver", onBack)
        Column(Modifier.weight(1f)) {
            Txt(title, titleSize, 900)
            if (subtitle != null) Txt(subtitle, 15, 600, Auna.Muted)
        }
        trailing()
    }
}

/** Círculo con inicial (personas sin foto). */
@Composable
fun Initial(letter: String, size: Dp = 44.dp, bg: Color = Auna.Mint, fg: Color = Auna.Green, fontSize: Int = 18) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Txt(letter, fontSize, 900, fg)
    }
}

/** Carga una foto guardada en el móvil, reducida a un tamaño razonable, fuera del hilo principal. */
@Composable
fun rememberFileBitmap(path: String): androidx.compose.ui.graphics.ImageBitmap? {
    val state = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
                val bmp = android.graphics.BitmapFactory.decodeFile(path, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
                rotateFromExif(path, bmp).asImageBitmap()
            }.getOrNull()
        }
    }
    return state.value
}

private fun rotateFromExif(path: String, bmp: android.graphics.Bitmap): android.graphics.Bitmap {
    val deg = runCatching {
        when (android.media.ExifInterface(path).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)
    if (deg == 0f) return bmp
    val m = android.graphics.Matrix().apply { postRotate(deg) }
    return android.graphics.Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}

@DrawableRes
fun photoRes(key: String?): Int? = when (key) {
    "marco" -> R.drawable.foto_marco
    "lucas" -> R.drawable.foto_lucas
    "laura" -> R.drawable.foto_laura
    else -> null
}

/** Foto redonda; si no hay foto muestra el hueco discontinuo del diseño. */
@Composable
fun Photo(
    key: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    placeholderColor: Color = Auna.Grey,
    placeholderBg: Color = Auna.White,
    showLabel: Boolean = true,
    ring: Color? = Auna.White,
) {
    val res = photoRes(key)
    val fileBitmap = if (res == null && key != null && key.startsWith("/")) rememberFileBitmap(key) else null
    val photoModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(if (ring != null) Modifier.border(3.dp, ring, CircleShape) else Modifier)
    if (res != null) {
        Image(painterResource(res), "Foto de $name", photoModifier, contentScale = ContentScale.Crop)
    } else if (fileBitmap != null) {
        Image(fileBitmap, "Foto de $name", photoModifier, contentScale = ContentScale.Crop)
    } else {
        Column(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(placeholderBg)
                .drawBehind {
                    val w = 3.dp.toPx()
                    drawCircle(
                        placeholderColor,
                        radius = this.size.minDimension / 2 - w / 2,
                        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))),
                    )
                }
                .semantics { contentDescription = "Foto de $name (pendiente)" },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AIcon(Icons.Camera, placeholderColor, if (size > 80.dp) 28.dp else 24.dp)
            if (showLabel) Txt("Foto", 13, 800, placeholderColor)
        }
    }
}

@Composable
fun KidPhoto(kid: Kid, size: Dp, placeholderColor: Color = Auna.Grey, showLabel: Boolean = true) =
    Photo(kid.photo, kid.name, size, placeholderColor = placeholderColor, showLabel = showLabel)

/** Barra de progreso del pasaporte. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 10.dp, track: Color = Auna.White) {
    Box(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Auna.Green),
        )
    }
}

/** Etiqueta pequeña redondeada (badges y chips). */
@Composable
fun Tag(text: String, bg: Color, fg: Color, fontSize: Int = 14, weight: Int = 800, radius: Dp = 10.dp, hPad: Dp = 10.dp, vPad: Dp = 3.dp) {
    Box(Modifier.clip(RoundedCornerShape(radius)).background(bg).padding(horizontal = hPad, vertical = vPad)) {
        Txt(text, fontSize, weight, fg)
    }
}

/** Girasol de Aúna (la «mascota» que pregunta y responde). */
@Composable
fun Girasol(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.girasol), contentDescription = null, modifier = modifier.size(size))
}

/** Campo de texto con el estilo del diseño. */
@Composable
fun AField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 18,
    weight: Int = 700,
    radius: Dp = 14.dp,
    minHeight: Dp = 52.dp,
    bg: Color = Auna.Bg,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    contentAlignment: Alignment = Alignment.CenterStart,
    padding: Dp = 14.dp,
    secret: Boolean = false,
) {
    val style = TextStyle(fontFamily = Nunito, fontSize = fontSize.sp, fontWeight = FontWeight(weight), color = Auna.Ink, letterSpacing = letterSpacing)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(Auna.Green),
        keyboardOptions = keyboardOptions,
        visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .defaultMinSize(minHeight = minHeight)
                    .clip(RoundedCornerShape(radius))
                    .background(bg)
                    .border(2.dp, Auna.Border, RoundedCornerShape(radius))
                    .padding(padding),
                contentAlignment = contentAlignment,
            ) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = Auna.Muted, fontWeight = FontWeight(600)))
                inner()
            }
        },
    )
}

/** Estado vacío cuando aún no hay ningún perfil de niño. */
@Composable
fun NoKid(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Auna.White)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Girasol(72.dp)
        Txt("Aún no hay perfiles", 19, 900)
        Spacer(Modifier.height(4.dp))
        PrimaryButton("Añadir niño", onAdd, Modifier.width(220.dp), icon = Icons.Plus)
    }
}

fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color,
        topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}

fun Modifier.card(bg: Color, radius: Dp): Modifier = this.clip(RoundedCornerShape(radius)).background(bg)
