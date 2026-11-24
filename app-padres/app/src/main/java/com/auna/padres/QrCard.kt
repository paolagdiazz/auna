package com.auna.padres

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.res.ResourcesCompat
import androidx.print.PrintHelper
import com.auna.padres.data.Kid
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File

/**
 * Tarjeta QR de emergencia. El QR lleva el texto de la ficha (sin enlaces):
 * cualquier cámara lo lee sin internet ni app.
 */
object QrCard {

    /** Texto que va dentro del QR, solo con lo marcado en «Qué se muestra». */
    fun text(kid: Kid): String {
        val p = kid.passport
        val on = { key: String -> kid.qrShow[key] ?: com.auna.padres.data.Templates.qrDefaults()[key] ?: false }
        val lines = mutableListOf("AÚNA · FICHA DE EMERGENCIA")
        if (on("nombre")) {
            lines += buildString {
                append("Hola, soy ${kid.name}")
                if (kid.age.isNotBlank()) append(" (${kid.age})")
                append(".")
                if (p.noHabla) append(" No hablo, pero me comunico.")
            }
        }
        if (on("sino") && (p.yes.isNotBlank() || p.no.isNotBlank())) {
            lines += listOfNotNull(p.yes.takeIf { it.isNotBlank() }?.let { "SÍ: $it." }, p.no.takeIf { it.isNotBlank() }?.let { "NO: $it." }).joinToString(" ")
        }
        if (on("salud")) {
            p.group("salud", "salud")?.items?.takeIf { it.isNotEmpty() }?.let { lines += "Salud: ${it.joinToString(", ")}." }
        }
        if (on("crisis")) {
            p.group("salud", "crisis")?.items?.takeIf { it.isNotEmpty() }?.let { lines += "Si tengo una crisis: ${it.joinToString(". ")}." }
            p.group("comida", "cuidado")?.items?.takeIf { it.isNotEmpty() }?.let { lines += "Al comer: ${it.joinToString(", ")}." }
        }
        if (on("familia")) {
            val phones = p.contacts.filter { it.phone.isNotBlank() }
            if (phones.isNotEmpty()) lines += "Llama a mi familia: " + phones.joinToString(" · ") { "${listOf(it.relation, it.name).filter { s -> s.isNotBlank() }.joinToString(" ")} ${it.phone}" }
        }
        lines += "Emergencias: 112"
        return lines.joinToString("\n")
    }

    /** Matriz con un punto por módulo del QR (tamaño mínimo), para dibujarla escalada. */
    fun matrix(text: String): BitMatrix =
        QRCodeWriter().encode(
            text, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1),
        )

    /** El QR como imagen pequeña (1 px por módulo); se amplía sin suavizar al pintarla. */
    fun moduleBitmap(text: String): Bitmap {
        val m = matrix(text)
        val pixels = IntArray(m.width * m.height) { i -> if (m[i % m.width, i / m.width]) Color.parseColor("#173326") else Color.WHITE }
        return Bitmap.createBitmap(pixels, m.width, m.height, Bitmap.Config.ARGB_8888)
    }

    /** Tarjeta tamaño carné (85,6 × 54 mm) a 300 ppp. */
    fun cardBitmap(context: Context, kid: Kid): Bitmap {
        val w = 1011; val h = 638
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val green = Color.parseColor("#0F5132")
        val leaf = Color.parseColor("#6CC69A")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = green
        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 40f, 40f, paint)
        // QR sobre blanco
        val qrSize = 500
        val left = 60f; val top = (h - qrSize) / 2f
        paint.color = Color.WHITE
        c.drawRoundRect(RectF(left - 18, top - 18, left + qrSize + 18, top + qrSize + 18), 28f, 28f, paint)
        val m = matrix(text(kid))
        // Sin suavizado, para que los módulos no dejen rayas entre sí.
        val dark = Paint().apply { color = Color.parseColor("#173326"); isAntiAlias = false }
        val cell = qrSize.toFloat() / m.width
        for (y in 0 until m.height) for (x in 0 until m.width) if (m[x, y]) c.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell + 0.5f, top + (y + 1) * cell + 0.5f, dark)
        // Textos
        val black = ResourcesCompat.getFont(context, R.font.nunito_black)
        val bold = ResourcesCompat.getFont(context, R.font.nunito_bold)
        val tx = left + qrSize + 70
        context.getDrawable(R.drawable.girasol)?.let { d -> d.setBounds(tx.toInt(), 120, tx.toInt() + 110, 230); d.draw(c) }
        paint.color = Color.WHITE; paint.typeface = black; paint.textSize = 58f
        c.drawText("Hola, soy", tx, 310f, paint)
        c.drawText(kid.name, tx, 378f, paint)
        paint.color = leaf; paint.typeface = bold; paint.textSize = 38f
        c.drawText(if (kid.passport.noHabla) "No hablo." else "Escanéame", tx, 450f, paint)
        if (kid.passport.noHabla) c.drawText("Escanéame.", tx, 498f, paint)
        return bmp
    }

    /** Página A4 con la tarjeta a tamaño real y marcas de corte. */
    fun printCard(context: Context, kid: Kid) {
        val page = Bitmap.createBitmap(2480, 3508, Bitmap.Config.ARGB_8888)
        val c = Canvas(page)
        c.drawColor(Color.WHITE)
        val card = cardBitmap(context, kid)
        val x = 236f; val y = 236f
        c.drawBitmap(card, x, y, null)
        val guide = Paint().apply { color = Color.LTGRAY; strokeWidth = 3f; style = Paint.Style.STROKE; pathEffect = android.graphics.DashPathEffect(floatArrayOf(18f, 12f), 0f) }
        c.drawRect(x - 4, y - 4, x + card.width + 4, y + card.height + 4, guide)
        PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }.printBitmap("Tarjeta Aúna de ${kid.name}", page)
    }

    /** Guarda la tarjeta como imagen en la galería (Imágenes › Aúna). Devuelve dónde. */
    fun save(context: Context, kid: Kid): String {
        val bmp = cardBitmap(context, kid)
        val name = "Auna_tarjeta_${kid.name.replace(Regex("[^A-Za-zÀ-ÿ0-9]"), "_")}_${System.currentTimeMillis()}.png"
        return if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Aúna")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("No se pudo guardar")
            context.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            "Guardada en la galería (Imágenes › Aúna)"
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            "Guardada en ${dir.absolutePath}"
        }
    }
}
