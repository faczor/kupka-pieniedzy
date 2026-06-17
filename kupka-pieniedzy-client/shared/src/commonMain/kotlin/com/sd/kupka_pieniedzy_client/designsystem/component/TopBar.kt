package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/**
 * Górny pasek zadaniowy: strzałka wstecz (lewa), wyśrodkowany tytuł, opcjonalna akcja (prawa).
 * Cienka linia (hairline) na dole oddziela pasek od przewijanej treści.
 */
@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: String = AppIcons.ArrowBack,
    actionIcon: String? = null,
    onActionClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val colors = KupkaTheme.colors
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (showDivider)
                        Modifier.drawBehind {
                            drawLine(
                                color = colors.divider,
                                start = androidx.compose.ui.geometry.Offset(0f, size.height),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                strokeWidth = 1f,
                            )
                        }
                    else Modifier
                )
                .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = KupkaTheme.typography.section.copy(textAlign = TextAlign.Center),
            color = colors.onSurfaceHigh,
            modifier = Modifier.fillMaxWidth(),
        )
        MaterialSymbol(
            navigationIcon,
            size = 25.dp,
            tint = colors.onSurfaceHigh,
            modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onBack),
        )
        if (actionIcon != null && onActionClick != null) {
            MaterialSymbol(
                actionIcon,
                size = 23.dp,
                tint = colors.onSurfaceMedium,
                modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = onActionClick),
            )
        }
    }
}
