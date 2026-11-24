package com.auna.padres.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.auna.padres.R

/** Paleta de la app de padres, tal cual el diseño (mock interactivo). */
object Auna {
    val Bg = Color(0xFFF3F8F5)          // fondo general
    val Ink = Color(0xFF173326)         // texto principal
    val Green = Color(0xFF0F5132)       // Verde Aúna · principal
    val Balance = Color(0xFF2E7D5B)     // Verde equilibrio
    val Forest = Color(0xFF1F5E43)      // acciones destructivas suaves
    val DeepGreen = Color(0xFF0B3D26)
    val Leaf = Color(0xFF6CC69A)        // botón Hablar
    val Mint = Color(0xFFD9F2E3)        // Menta calma
    val MintStrong = Color(0xFFC8EBD6)
    val Dot = Color(0xFF3BAA6F)
    val Muted = Color(0xFF4D6358)       // texto secundario
    val Line = Color(0xFFE3EFE8)        // separadores
    val Border = Color(0xFFD3E5DA)      // bordes de botones
    val GreyCard = Color(0xFFECEEED)
    val Grey = Color(0xFF6B7570)
    val CoralBg = Color(0xFFFDE7E4)
    val Coral = Color(0xFF8C1D18)
    val CoralBorder = Color(0xFFF2B8B0)
    val Red = Color(0xFFB3261E)         // emergencia
    val White = Color(0xFFFFFFFF)
    val Scrim = Color(0x73173326)
}

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

@Composable
fun AunaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Auna.Green,
            onPrimary = Auna.White,
            background = Auna.Bg,
            onBackground = Auna.Ink,
            surface = Auna.White,
            onSurface = Auna.Ink,
        ),
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = Nunito, color = Auna.Ink, fontSize = 16.sp),
            LocalContentColor provides Auna.Ink,
            content = content,
        )
    }
}
