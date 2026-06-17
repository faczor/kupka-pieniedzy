package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/** Pole tekstowe na `Surface.Card` z obrysem; opcjonalny element po prawej (np. ikona). */
@Composable
fun KupkaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(KupkaTheme.shapes.inputShape)
                .background(colors.surfaceCard)
                .border(1.dp, colors.outline, KupkaTheme.shapes.inputShape)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                AppText(placeholder, variant = TextVariant.Body, color = colors.onSurfaceLow)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = KupkaTheme.typography.body.copy(color = colors.onSurfaceHigh),
                cursorBrush = SolidColor(colors.primaryHover),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke()
    }
}

/** Karta wprowadzania kwoty — duża liczba mono + symbol waluty (formularz „Nowy wydatek”). */
@Composable
fun AmountInputCard(
    label: String,
    amountText: String,
    currencySymbol: String,
    onAmountChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = KupkaTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .background(colors.surfaceCard)
                .border(1.dp, colors.outline, KupkaTheme.shapes.cardShape)
                .padding(horizontal = 18.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(text = label.uppercase(), variant = TextVariant.Label, color = colors.onSurfaceLow)
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (amountText.isEmpty()) {
                    AppText(
                        text = placeholder,
                        variant = TextVariant.AmountInput,
                        color = colors.onSurfaceLow,
                    )
                }
                BasicTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    textStyle =
                        KupkaTheme.typography.amountInput.copy(
                            color = colors.onSurfaceHigh,
                            textAlign = TextAlign.Center,
                        ),
                    cursorBrush = SolidColor(colors.primaryHover),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            Text(
                text = currencySymbol,
                style = KupkaTheme.typography.amountInput.copy(fontSize = 24.sp),
                color = colors.onSurfaceLow,
            )
        }
    }
}
