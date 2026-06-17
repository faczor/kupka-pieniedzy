package com.sd.kupka_pieniedzy_client.feature.addexpense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/**
 * Arkusz wyboru pliku paragonu (galeria telefonu). Wybór pliku jest platformowy — w MVP
 * prezentujemy reprezentatywną galerię, a „Dodaj i analizuj” uruchamia mock analizy.
 */
@Composable
fun ColumnScope.ReceiptPickerSheetContent(onCancel: () -> Unit, onConfirm: () -> Unit) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current

    Box(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            strings.pickReceiptTitle,
            variant = TextVariant.Section,
            color = colors.onSurfaceHigh,
        )
        AppText(
            text = strings.cancel,
            variant = TextVariant.Label,
            color = colors.onSurfaceMedium,
            modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onCancel),
        )
    }

    Segmented(strings.sourceGallery, strings.sourceFiles)

    AppText(
        text = strings.recent.uppercase(),
        variant = TextVariant.Label,
        color = colors.onSurfaceLow,
        modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
    )

    val thumbs = (0 until 6).toList()
    thumbs.chunked(3).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rowItems.forEach { index ->
                Thumbnail(
                    selected = index == 0,
                    hasReceipt = index % 2 == 0,
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
        }
    }

    PrimaryButton(
        text = strings.addAndAnalyze,
        onClick = onConfirm,
        leadingIcon = AppIcons.AutoAwesome,
        modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
    )
}

@Composable
private fun Segmented(left: String, right: String) {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(colors.surfaceBg)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentChip(left, selected = true, modifier = Modifier.weight(1f))
        SegmentChip(right, selected = false, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SegmentChip(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    val colors = KupkaTheme.colors
    Box(
        modifier =
            modifier
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) colors.surfaceModal else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = text,
            variant = TextVariant.Label,
            color = if (selected) colors.onSurfaceHigh else colors.onSurfaceLow,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Thumbnail(selected: Boolean, hasReceipt: Boolean, modifier: Modifier = Modifier) {
    val colors = KupkaTheme.colors
    Box(
        modifier =
            modifier
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (hasReceipt) colors.receiptPaper else colors.surfaceElevated)
                .then(
                    if (selected) Modifier.border(3.dp, colors.primary, RoundedCornerShape(10.dp))
                    else Modifier
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasReceipt) {
            MaterialSymbol(
                AppIcons.Image,
                size = 24.dp,
                tint = colors.onSurfaceLow.copy(alpha = 0.6f),
            )
        }
        if (selected) {
            Box(
                modifier =
                    Modifier.align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(20.dp)
                        .clip(KupkaTheme.shapes.pillShape)
                        .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                MaterialSymbol(AppIcons.Check, size = 14.dp, tint = colors.onPrimary)
            }
        }
    }
}
