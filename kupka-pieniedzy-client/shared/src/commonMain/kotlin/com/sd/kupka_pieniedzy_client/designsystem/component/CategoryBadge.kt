package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.domain.model.CategoryRef

/** Spójny badge kategorii (ikona + kolor + nazwa) jako pill. */
@Composable
fun CategoryBadge(
    ref: CategoryRef,
    modifier: Modifier = Modifier,
    expandable: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val color = parseHexColor(ref.colorHex)
    Row(
        modifier =
            modifier
                .height(27.dp)
                .clip(KupkaTheme.shapes.pillShape)
                .background(color.copy(alpha = 0.13f))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 9.dp, end = if (expandable) 7.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MaterialSymbol(ref.icon, size = 14.dp, tint = color)
        AppText(text = ref.name, variant = TextVariant.Caption, color = color)
        if (expandable) {
            MaterialSymbol(AppIcons.ExpandMore, size = 15.dp, tint = color.copy(alpha = 0.65f))
        }
    }
}

/** Pusty/„do uzupełnienia” badge z przerywaną ramką (pozycja bez kategorii). */
@Composable
fun UnassignedBadge(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = KupkaTheme.colors.budgetYellowFill
    Row(
        modifier =
            modifier
                .height(27.dp)
                .clip(KupkaTheme.shapes.pillShape)
                .dashedBorder(color.copy(alpha = 0.55f), cornerRadius = 999.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MaterialSymbol(AppIcons.Add, size = 15.dp, tint = color)
        AppText(text = text, variant = TextVariant.Caption, color = color)
    }
}

/** Większy, wybieralny chip kategorii (formularz ręczny). */
@Composable
fun CategoryChip(
    ref: CategoryRef,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = parseHexColor(ref.colorHex)
    val bg = if (selected) color.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f)
    val textColor = if (selected) color else KupkaTheme.colors.onSurfaceMedium
    Row(
        modifier =
            modifier
                .height(38.dp)
                .clip(KupkaTheme.shapes.pillShape)
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        MaterialSymbol(ref.icon, size = 17.dp, tint = color)
        AppText(text = ref.name, variant = TextVariant.Label, color = textColor)
    }
}
