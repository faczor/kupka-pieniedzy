package com.sd.kupka_pieniedzy_client.core.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.max

/**
 * iOS: dekoduje bajty przez `UIImage` (radzi sobie z HEIC), zmniejsza długi bok do [maxLongEdgePx]
 * przez przerysowanie w kontekście graficznym i koduje do JPEG (`UIImageJPEGRepresentation`).
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun normalizeReceiptImageToJpeg(
    bytes: ByteArray,
    maxLongEdgePx: Int,
    quality: Int,
): ByteArray {
    if (bytes.isEmpty()) return bytes
    val data =
        bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    val image = UIImage.imageWithData(data) ?: return bytes

    val (w, h) = image.size.useContents { width to height }
    if (w <= 0.0 || h <= 0.0) return bytes
    val longEdge = max(w, h)
    val ratio = if (longEdge > maxLongEdgePx) maxLongEdgePx / longEdge else 1.0
    val newW = w * ratio
    val newH = h * ratio

    UIGraphicsBeginImageContextWithOptions(CGSizeMake(newW, newH), false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, newW, newH))
    val resized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    val jpeg = UIImageJPEGRepresentation(resized ?: image, quality / 100.0) ?: return bytes
    return jpeg.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply { usePinned { memcpy(it.addressOf(0), bytes, length) } }
}
