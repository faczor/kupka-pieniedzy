package com.sd.kupka_pieniedzy_client.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kupka_pieniedzy_client.shared.generated.resources.Res
import kupka_pieniedzy_client.shared.generated.resources.commit_mono_medium
import kupka_pieniedzy_client.shared.generated.resources.commit_mono_regular
import kupka_pieniedzy_client.shared.generated.resources.manrope_bold
import kupka_pieniedzy_client.shared.generated.resources.manrope_medium
import kupka_pieniedzy_client.shared.generated.resources.manrope_regular
import kupka_pieniedzy_client.shared.generated.resources.manrope_semibold
import org.jetbrains.compose.resources.Font

/**
 * Skala typograficzna — źródło prawdy: `tokens-typography.md` (D27). Manrope = tekst, Commit Mono =
 * liczby (tabular `tnum`). Zero literałów w ekranach.
 */
@Immutable
data class KupkaTypography(
    val heroNumber: TextStyle,
    val amountInput: TextStyle,
    /** Editorial display — nagłówki onboardingu/powitań (większe niż [title]). */
    val display: TextStyle,
    val heroLabel: TextStyle,
    val title: TextStyle,
    val section: TextStyle,
    val body: TextStyle,
    val bodyMono: TextStyle,
    val numberMd: TextStyle,
    val numberSm: TextStyle,
    val numberLg: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val button: TextStyle,
)

private const val TNUM = "tnum"

@Composable
fun buildKupkaTypography(): KupkaTypography {
    val manrope =
        FontFamily(
            Font(Res.font.manrope_regular, FontWeight.Normal),
            Font(Res.font.manrope_medium, FontWeight.Medium),
            Font(Res.font.manrope_semibold, FontWeight.SemiBold),
            Font(Res.font.manrope_bold, FontWeight.Bold),
        )
    val mono =
        FontFamily(
            Font(Res.font.commit_mono_regular, FontWeight.Normal),
            Font(Res.font.commit_mono_medium, FontWeight.Medium),
        )

    return KupkaTypography(
        heroNumber =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 52.sp,
                lineHeight = 56.sp,
                fontFeatureSettings = TNUM,
            ),
        amountInput =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 48.sp,
                lineHeight = 52.sp,
                fontFeatureSettings = TNUM,
            ),
        display =
            TextStyle(
                fontFamily = manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.4).sp,
            ),
        heroLabel =
            TextStyle(
                fontFamily = manrope,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 1.sp,
            ),
        title =
            TextStyle(
                fontFamily = manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.3.sp,
            ),
        section =
            TextStyle(
                fontFamily = manrope,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.2.sp,
            ),
        body =
            TextStyle(
                fontFamily = manrope,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        bodyMono =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontFeatureSettings = TNUM,
            ),
        numberLg =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                fontFeatureSettings = TNUM,
            ),
        numberMd =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontFeatureSettings = TNUM,
            ),
        numberSm =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontFeatureSettings = TNUM,
            ),
        label =
            TextStyle(
                fontFamily = manrope,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.2.sp,
            ),
        caption =
            TextStyle(
                fontFamily = manrope,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.1.sp,
            ),
        button =
            TextStyle(
                fontFamily = manrope,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.3.sp,
            ),
    )
}

val LocalKupkaTypography =
    staticCompositionLocalOf<KupkaTypography> {
        error("LocalKupkaTypography nie dostarczone — owiń UI w KupkaTheme")
    }
