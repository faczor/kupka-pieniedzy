package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/**
 * Segmentowane pole kodu OTP — [length] osobnych kratek, ale oparte na **jednym** ukrytym
 * [BasicTextField]. Dzięki temu wklejenie całego kodu (np. „123456") wypełnia wszystkie kratki za
 * jednym razem, a wpisywanie/backspace/klawiatura numeryczna działają naturalnie.
 *
 * [onValueChange] dostaje surową wartość pola — filtrowanie do cyfr i ograniczenie długości robi
 * warstwa wyżej (ViewModel), zgodnie z resztą formularzy.
 */
@Composable
fun OtpCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
) {
    val colors = KupkaTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        // Kursor niewidoczny — pozycję sygnalizuje podświetlona „aktywna" kratka.
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier.fillMaxWidth(),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(length) { index ->
                    val char = value.getOrNull(index)?.toString().orEmpty()
                    val filled = char.isNotEmpty()
                    val active = index == value.length && value.length < length
                    val borderColor = if (filled || active) colors.primary else colors.outline
                    val borderWidth = if (active) 1.5.dp else 1.dp
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .height(58.dp)
                                .clip(KupkaTheme.shapes.inputShape)
                                .background(colors.surfaceCard)
                                .border(borderWidth, borderColor, KupkaTheme.shapes.inputShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = char,
                            style =
                                KupkaTheme.typography.amountInput.copy(
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center,
                                ),
                            color = colors.onSurfaceHigh,
                        )
                    }
                }
            }
        },
    )
}
