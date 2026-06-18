package com.sd.kupka_pieniedzy_client.core.media

/**
 * Normalizuje zdjęcie paragonu do **JPEG** i zmniejsza długi bok do [maxLongEdgePx].
 *
 * Powód: aparaty telefonów zapisują zdjęcia w **HEIC/HEIF**, którego Claude vision NIE obsługuje
 * (przyjmuje tylko JPEG/PNG/GIF/WebP) — surowe bajty z pickera kończyły błędem Anthropic
 * „Could not process image". Transkodowanie na urządzeniu gwarantuje obsługiwany format, a
 * downscale (~1568 px / q80) tnie payload base64 (Claude i tak skaluje powyżej 1568 px).
 *
 * Implementacje platformowe: Android — `Bitmap`/`BitmapFactory`; iOS — `UIImage`.
 * Gdy dekodowanie się nie powiedzie, zwraca [bytes] bez zmian (lepiej spróbować wysłać oryginał
 * niż wywrócić flow).
 */
internal expect fun normalizeReceiptImageToJpeg(
    bytes: ByteArray,
    maxLongEdgePx: Int = 1568,
    quality: Int = 80,
): ByteArray
