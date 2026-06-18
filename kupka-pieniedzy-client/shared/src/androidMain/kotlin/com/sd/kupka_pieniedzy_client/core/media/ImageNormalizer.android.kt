package com.sd.kupka_pieniedzy_client.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Android: dekoduje bajty (BitmapFactory radzi sobie też z HEIC od API 28), zmniejsza długi bok do
 * [maxLongEdgePx] i koduje do JPEG. Dwustopniowo: najpierw `inSampleSize` (zgrubny downsample bez
 * ładowania pełnej bitmapy — chroni przed OOM), potem dokładne skalowanie.
 */
internal actual fun normalizeReceiptImageToJpeg(
    bytes: ByteArray,
    maxLongEdgePx: Int,
    quality: Int,
): ByteArray {
    // 1. Same wymiary, bez alokacji pikseli.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val srcLongEdge = max(bounds.outWidth, bounds.outHeight)
    if (srcLongEdge <= 0) return bytes // nie udało się zdekodować — wyślij oryginał

    // 2. Zgrubny downsample potęgą 2, potem precyzyjne dopasowanie.
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(srcLongEdge, maxLongEdgePx) }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return bytes
    val longEdge = max(decoded.width, decoded.height)
    val scaled =
        if (longEdge > maxLongEdgePx) {
            val ratio = maxLongEdgePx.toFloat() / longEdge
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).roundToInt().coerceAtLeast(1),
                (decoded.height * ratio).roundToInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }

    return ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }
}

/** Największa potęga 2 taka, że źródłowy długi bok / sampleSize wciąż ≥ target. */
private fun sampleSize(srcLongEdge: Int, target: Int): Int {
    var s = 1
    while (srcLongEdge / (s * 2) >= target) s *= 2
    return s
}
