package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/** Karta na `Surface.Card`, zaokrąglona (Radius.card=16). */
@Composable
fun KupkaCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(KupkaTheme.shapes.cardShape)
                .background(KupkaTheme.colors.surfaceCard)
                .padding(padding),
        content = content,
    )
}

/** Karta-lista: bez wewnętrznego paddingu (wiersze z własnymi dividerami). */
@Composable
fun KupkaListCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            modifier.clip(KupkaTheme.shapes.cardShape).background(KupkaTheme.colors.surfaceCard),
        content = content,
    )
}
