package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/** Nagłówek sekcji + opcjonalna akcja po prawej (np. „Wszystkie”). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = title,
            variant = TextVariant.Section,
            color = KupkaTheme.colors.onSurfaceHigh,
        )
        if (actionText != null) {
            AppText(
                text = actionText,
                variant = TextVariant.Label,
                color = KupkaTheme.colors.primaryHover,
                modifier =
                    if (onActionClick != null) Modifier.clickable(onClick = onActionClick)
                    else Modifier,
            )
        }
    }
}
