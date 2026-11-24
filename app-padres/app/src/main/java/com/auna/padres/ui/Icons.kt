package com.auna.padres.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Iconos de trazo del diseño (viewBox 24×24). Se guardan como datos de path SVG
 * para que se vean idénticos a los bocetos.
 */
class AunaIcon(val paths: List<String>, val filled: Boolean = false)

private fun circle(cx: Number, cy: Number, r: Number): String {
    val x = cx.toDouble(); val y = cy.toDouble(); val rr = r.toDouble()
    return "M${x - rr} ${y}a$rr $rr 0 1 0 ${2 * rr} 0a$rr $rr 0 1 0 ${-2 * rr} 0z"
}

private fun rect(x: Number, y: Number, w: Number, h: Number, r: Number): String {
    val x0 = x.toDouble(); val y0 = y.toDouble(); val ww = w.toDouble(); val hh = h.toDouble(); val rr = r.toDouble()
    return "M${x0 + rr} ${y0}h${ww - 2 * rr}a$rr $rr 0 0 1 $rr ${rr}v${hh - 2 * rr}" +
        "a$rr $rr 0 0 1 ${-rr} ${rr}h${-(ww - 2 * rr)}a$rr $rr 0 0 1 ${-rr} ${-rr}v${-(hh - 2 * rr)}a$rr $rr 0 0 1 $rr ${-rr}z"
}

private fun icon(vararg p: String) = AunaIcon(p.toList())

object Icons {
    val Bell = icon("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10 21h4")
    val Settings = icon(
        circle(12, 12, 3),
        "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
    )
    val Camera = icon("M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z", circle(12, 13.5, 3.5))
    val ChevronRight = icon("M9 6l6 6-6 6")
    val ChevronLeft = icon("M15 6l-6 6 6 6")
    val ChevronDown = icon("M6 9l6 6 6-6")
    val ChevronUp = icon("M6 15l6-6 6 6")
    val Alert = icon(circle(12, 12, 9), "M12 7v6M12 16.5v.5")
    val Mic = icon(rect(9, 3, 6, 11, 3), "M5 11a7 7 0 0 0 14 0M12 18v3")
    val People = icon(circle(9, 8, 3.5), "M2.5 20c.8-3.5 3.4-5.5 6.5-5.5s5.7 2 6.5 5.5", circle(17, 9, 2.5), "M17 14c2.4 0 4 1.6 4.5 4.5")
    val Scan = icon("M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M7 12h10")
    val ChatCheck = icon("M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z", "M8.5 12.5l2.5 2.5 4.5-5")
    val Home = icon("M3 11.5 12 4l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")
    val Passport = icon(rect(5, 3, 14, 18, 2), circle(12, 10, 3), "M9 16.5h6")
    val Profile = icon(circle(12, 12, 9), circle(12, 10, 3), "M6.5 18.5c1.3-2 3.2-3 5.5-3s4.2 1 5.5 3")
    val Check = icon("M5 12l5 5L20 7")
    val Plus = icon("M12 5v14M5 12h14")
    val Pencil = icon("M12 20h9", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z")
    val Book = icon("M4 5a2 2 0 0 1 2-2h12v18H6a2 2 0 0 1-2-2z")
    val Lock = icon(rect(5, 11, 14, 10, 2), "M8 11V7a4 4 0 0 1 8 0v4")
    val Volume = icon("M11 5 6 9H2v6h4l5 4z", "M15.5 8.5a5 5 0 0 1 0 7")
    val VolumeFull = icon("M11 5 6 9H2v6h4l5 4z", "M15.5 8.5a5 5 0 0 1 0 7M19 5a10 10 0 0 1 0 14")
    val Star = AunaIcon(listOf("M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5L2.5 9.4l6.6-.9z"), filled = true)
    val Utensils = icon("M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2", "M7 2v20", "M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7")
    val Mute = icon("M11 5 6 9H2v6h4l5 4z", "M22 9l-6 6M16 9l6 6")
    val Smile = icon(circle(12, 12, 9), "M8 14s1.5 2 4 2 4-2 4-2M9 9h.01M15 9h.01")
    val OpenBook = icon("M4 5a2 2 0 0 1 2-2h5v17H6a2 2 0 0 0-2 2z", "M20 5a2 2 0 0 0-2-2h-5v17h5a2 2 0 0 1 2 2z")
    val Hand = icon(
        "M18 11V6a2 2 0 0 0-4 0v5", "M14 10V4a2 2 0 0 0-4 0v6", "M10 10.5V6a2 2 0 0 0-4 0v8",
        "M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.9-6-2.4l-3.6-3.6a2 2 0 0 1 2.8-2.8L7 15",
    )
    val Headphones = icon("M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3")
    val Heart = icon("M19 14c1.5-1.5 3-3.2 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.8 0-3 .5-4.5 2-1.5-1.5-2.7-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4 3 5.5l7 7z")
    val Health = icon(circle(12, 12, 9), "M12 8v8M8 12h8")
    val Sparkle = icon("M12 3l2.2 5.8L20 11l-5.8 2.2L12 19l-2.2-5.8L4 11l5.8-2.2z")
    val Close = icon("M6 6l12 12M18 6 6 18")
    val Send = icon("M5 12h14M13 6l6 6-6 6")
    val Eye = icon("M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z", circle(12, 12, 3))
    val ArrowDown = icon("M12 4v16M6 14l6 6 6-6")
    val Trash = icon("M4 7h16M10 11v6M14 11v6M6 7l1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13M9 7V4h6v3")
    val Warning = icon("M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z", "M12 9v4M12 17h.01")
    val Phone = AunaIcon(
        listOf("M6.6 10.8a15.1 15.1 0 0 0 6.6 6.6l2.2-2.2a1 1 0 0 1 1-.25 11.4 11.4 0 0 0 3.6.57 1 1 0 0 1 1 1V20a1 1 0 0 1-1 1A17 17 0 0 1 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1c0 1.25.2 2.45.57 3.57a1 1 0 0 1-.25 1z"),
        filled = true,
    )
    val Pin = icon("M12 21s-7-6.1-7-11.5a7 7 0 0 1 14 0C19 14.9 12 21 12 21z", circle(12, 9.5, 2.5))
}

/** Dibuja un [AunaIcon]. Decorativo: la etiqueta accesible va en el botón que lo contiene. */
@Composable
fun AIcon(
    icon: AunaIcon,
    color: Color,
    size: Dp = 24.dp,
    stroke: Float = 2f,
    modifier: Modifier = Modifier,
) {
    val paths = remember(icon) { icon.paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            paths.forEach { p ->
                if (icon.filled) {
                    drawPath(p, color)
                } else {
                    drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}
