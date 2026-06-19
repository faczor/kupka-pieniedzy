package com.sd.kupka_pieniedzy_client.feature.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.ReceiptImageView
import com.sd.kupka_pieniedzy_client.designsystem.component.SurfaceButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/** Pełnoekranowy podgląd zdjęcia paragonu źródłowego (efekt tapnięcia miniatury). */
@Composable
fun ReceiptPreviewScreen(image: ScreenState<ByteArray>?, onClose: () -> Unit) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxSize().background(colors.surfaceBg)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
            MaterialSymbol(
                AppIcons.Close,
                size = 25.dp,
                tint = colors.onSurfaceHigh,
                modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onClose),
            )
            AppText(
                text = strings.receiptSourceTitle,
                variant = TextVariant.Section,
                color = colors.onSurfaceHigh,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        ReceiptImageView(
            state = image ?: ScreenState.Loading,
            modifier =
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        )

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            SurfaceButton(
                text = strings.backToBreakdown,
                onClick = onClose,
                leadingIcon = AppIcons.ArrowBack,
            )
        }
    }
}
