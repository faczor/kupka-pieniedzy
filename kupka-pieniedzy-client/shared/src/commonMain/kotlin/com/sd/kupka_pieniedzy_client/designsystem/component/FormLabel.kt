package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/** Etykieta pola formularza (UPPERCASE). */
@Composable
fun FormLabel(text: String, modifier: Modifier = Modifier) {
    AppText(
        text = text.uppercase(),
        variant = TextVariant.Label,
        color = KupkaTheme.colors.onSurfaceMedium,
        modifier = modifier,
    )
}

/** Etykieta pola z dopiskiem „· opcjonalnie”. */
@Composable
fun FormLabelOptional(label: String, optional: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AppText(
            "${label.uppercase()} ",
            variant = TextVariant.Label,
            color = KupkaTheme.colors.onSurfaceMedium,
        )
        AppText(
            "· $optional",
            variant = TextVariant.Caption,
            color = KupkaTheme.colors.onSurfaceLow,
        )
    }
}
