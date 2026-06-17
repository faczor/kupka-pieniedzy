package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/** Renderuje Loading / Error(retry) / Content na podstawie [ScreenState]. */
@Composable
fun <T> StateContainer(
    state: ScreenState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is ScreenState.Loading ->
            Box(modifier.fillMaxSize(), Alignment.Center) { LoadingIndicator() }
        is ScreenState.Error ->
            Box(modifier.fillMaxSize(), Alignment.Center) {
                ErrorView(LocalStrings.current.errorMessage(state.error), onRetry)
            }
        is ScreenState.Content -> content(state.value)
    }
}

@Composable
fun LoadingIndicator(size: Int = 36) {
    val rotation = rememberInfiniteTransition()
    val angle by
        rotation.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        )
    MaterialSymbol(
        AppIcons.ProgressActivity,
        size = size.dp,
        tint = KupkaTheme.colors.primaryHover,
        modifier = Modifier.rotate(angle),
    )
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        MaterialSymbol(AppIcons.Error, size = 40.dp, tint = KupkaTheme.colors.budgetRedFill)
        AppText(
            text = message,
            variant = TextVariant.Body,
            color = KupkaTheme.colors.onSurfaceMedium,
            textAlign = TextAlign.Center,
        )
        SurfaceButton(
            text = LocalStrings.current.retry,
            onClick = onRetry,
            modifier = Modifier.width(180.dp),
        )
    }
}
