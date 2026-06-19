package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

// Przycisk social ma stałe białe tło (wymogi brandingu Google/Apple), niezależnie od dark theme.
private val SocialButtonBg = Color.White
private val SocialButtonFg = Color(0xFF1A1A1A)
private val SocialButtonHeight = 56.dp

/**
 * Przycisk logowania dostawcą (Google / Apple): białe tło + logotyp brandu + etykieta. [loading]
 * pokazuje spinner zamiast logo; [enabled]=false wygasza i blokuje (gdy drugi dostawca trwa).
 */
@Composable
fun SocialButton(
    text: String,
    logo: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val alpha = if (enabled || loading) 1f else 0.5f
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(SocialButtonHeight)
                .clip(KupkaTheme.shapes.buttonShape)
                .background(SocialButtonBg.copy(alpha = alpha))
                .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = SocialButtonFg,
                strokeWidth = 2.dp,
            )
        } else {
            Image(imageVector = logo, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(KupkaTheme.spacing.m))
        AppText(text = text, variant = TextVariant.Button, color = SocialButtonFg)
    }
}
