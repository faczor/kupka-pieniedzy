package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/**
 * Wspólny render zdjęcia paragonu z [ScreenState]<ByteArray>: spinner / komunikat błędu / dekodowana
 * bitmapa. Używany w pełnoekranowym podglądzie paragonu (review) i na liście wpisów — żeby nie
 * duplikować dekodowania i obsługi stanów.
 */
@Composable
fun ReceiptImageView(
    state: ScreenState<ByteArray>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {
            is ScreenState.Loading -> LoadingIndicator()
            is ScreenState.Error ->
                AppText(
                    strings.imageLoadError,
                    variant = TextVariant.Body,
                    color = colors.onSurfaceMedium,
                    textAlign = TextAlign.Center,
                )
            is ScreenState.Content -> {
                val bitmap = remember(state.value) { decodeReceiptBitmap(state.value) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = contentScale,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AppText(
                        strings.imageLoadError,
                        variant = TextVariant.Body,
                        color = colors.onSurfaceMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Bezpieczne dekodowanie bajtów zdjęcia paragonu do bitmapy (null, gdy format nieobsługiwany). */
fun decodeReceiptBitmap(bytes: ByteArray): ImageBitmap? =
    runCatching { bytes.decodeToImageBitmap() }.getOrNull()
