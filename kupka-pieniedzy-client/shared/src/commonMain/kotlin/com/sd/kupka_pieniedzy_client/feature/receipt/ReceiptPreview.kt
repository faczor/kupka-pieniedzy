package com.sd.kupka_pieniedzy_client.feature.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.core.time.LocalToday
import com.sd.kupka_pieniedzy_client.designsystem.component.SurfaceButton
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

private const val MAX_PREVIEW_LINES = 6

/** Pełnoekranowy podgląd paragonu źródłowego (efekt tapnięcia miniatury). */
@Composable
fun ReceiptPreviewScreen(draft: AnalyzedReceipt, onClose: () -> Unit) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val today = LocalToday.current

    Column(modifier = Modifier.fillMaxSize().background(colors.surfaceBg)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
            MaterialSymbol(
                AppIcons.Close,
                size = 25.dp,
                tint = colors.onSurfaceHigh,
                modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onClose),
            )
            Text(
                text = strings.receiptSourceTitle,
                style = KupkaTheme.typography.section,
                color = colors.onSurfaceHigh,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            MaterialSymbol(
                AppIcons.Share,
                size = 23.dp,
                tint = colors.onSurfaceMedium,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ReceiptPaper(draft, today)
            Row(
                modifier = Modifier.padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MaterialSymbol(AppIcons.PinchZoom, size = 16.dp, tint = colors.onSurfaceMedium)
                Text(
                    strings.doubleTapToZoom,
                    style = KupkaTheme.typography.caption,
                    color = colors.onSurfaceMedium,
                )
            }
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            SurfaceButton(
                text = strings.backToBreakdown,
                onClick = onClose,
                leadingIcon = AppIcons.ArrowBack,
            )
        }
    }
}

@Composable
private fun ReceiptPaper(draft: AnalyzedReceipt, today: kotlinx.datetime.LocalDate) {
    val colors = KupkaTheme.colors
    val ink = KupkaTheme.typography.bodyMono.copy(color = colors.receiptInk, fontSize = 11.sp)
    val inkMuted = ink.copy(color = colors.receiptInkMuted, fontSize = 9.5.sp)

    Column(
        modifier =
            Modifier.width(264.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.receiptPaper)
                .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Text(
            draft.store.uppercase(),
            style = ink.copy(fontSize = 15.sp),
            color = colors.receiptInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(13.dp))
        DashedSeparator()
        Spacer(Modifier.height(8.dp))

        draft.items.take(MAX_PREVIEW_LINES).forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(item.name.uppercase(), style = ink, color = colors.receiptInk)
                Text(
                    MoneyFormatter.format(item.amount, withSymbol = false),
                    style = ink,
                    color = colors.receiptInk,
                )
            }
        }
        if (draft.items.size > MAX_PREVIEW_LINES) {
            Text(
                "+ ${draft.items.size - MAX_PREVIEW_LINES} …",
                style = inkMuted,
                color = colors.receiptInkMuted,
                modifier = Modifier.padding(top = 7.dp),
            )
        }

        Spacer(Modifier.height(11.dp))
        DashedSeparator()
        Spacer(Modifier.height(11.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "SUMA ${draft.total.currency}",
                style = ink.copy(fontSize = 13.sp),
                color = colors.receiptInk,
            )
            Text(
                MoneyFormatter.format(draft.total, withSymbol = false),
                style = ink.copy(fontSize = 13.sp),
                color = colors.receiptInk,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "${draft.date} · paragon.jpg",
            style = inkMuted,
            color = colors.receiptInkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DashedSeparator() {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(1.dp)
                .background(KupkaTheme.colors.receiptInkMuted.copy(alpha = 0.4f))
    )
}
