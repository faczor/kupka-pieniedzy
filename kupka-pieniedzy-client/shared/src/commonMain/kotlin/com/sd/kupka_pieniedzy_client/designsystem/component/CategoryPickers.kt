package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.icon.CategoryIconPalette
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.CategoryColorPalette
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor

/**
 * Wiersz wyboru ikony kategorii (paleta [CategoryIconPalette], pierwsze 6). Wspólny dla sheetu
 * „Nowa kategoria" i onboardingu — nie duplikujemy markupu/spacingu.
 */
@Composable
fun CategoryIconPickerRow(
    selectedIcon: String,
    selectedColor: Color,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CategoryIconPalette.take(6).forEach { icon ->
            IconPickCell(
                icon = icon,
                selected = icon == selectedIcon,
                color = selectedColor,
                onClick = { onPick(icon) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Wiersz wyboru koloru kategorii (paleta [CategoryColorPalette]). */
@Composable
fun CategoryColorPickerRow(
    selectedHex: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryColorPalette.forEach { hex ->
            ColorSwatch(hex = hex, selected = hex == selectedHex, onClick = { onPick(hex) })
        }
    }
}

@Composable
private fun IconPickCell(
    icon: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KupkaTheme.colors
    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(KupkaTheme.shapes.iconTileShape)
                .background(if (selected) color.copy(alpha = 0.16f) else colors.surfaceCard)
                .then(
                    if (selected) Modifier.border(1.5.dp, color, KupkaTheme.shapes.iconTileShape)
                    else Modifier
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbol(icon, size = 24.dp, tint = if (selected) color else colors.onSurfaceMedium)
    }
}

@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.size(if (selected) 38.dp else 34.dp)
                .clip(KupkaTheme.shapes.pillShape)
                .then(
                    if (selected)
                        Modifier.border(2.dp, parseHexColor(hex), KupkaTheme.shapes.pillShape)
                    else Modifier
                )
                .padding(if (selected) 4.dp else 0.dp)
                .clip(KupkaTheme.shapes.pillShape)
                .background(parseHexColor(hex))
                .clickable(onClick = onClick)
    )
}
