package com.sd.kupka_pieniedzy_client.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Logotypy dostawców logowania (Google / Apple) jako [ImageVector] z oryginalnych ścieżek SVG —
 * brand-accurate, bez dodatkowego pipeline'u assetów. Renderować przez `Image` (zachowuje własne
 * kolory), nie `Icon` (tintuje).
 */
object BrandLogos {

    val Google: ImageVector by lazy {
        ImageVector.Builder(
                name = "GoogleLogo",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 48f,
                viewportHeight = 48f,
            )
            .apply {
                addPath(
                    PathParser()
                        .parsePathString(
                            "M24 9.5c3.5 0 6.6 1.2 9 3.6l6.7-6.7C35.6 2.4 30.2 0 24 0 14.6 0 6.4 5.4 2.5 13.3l7.8 6.1C12.2 13.3 17.6 9.5 24 9.5z"
                        )
                        .toNodes(),
                    fill = SolidColor(Color(0xFFEA4335)),
                )
                addPath(
                    PathParser()
                        .parsePathString(
                            "M46.5 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.7c-.6 3-2.3 5.5-4.8 7.2l7.5 5.8C43.9 38 46.5 31.8 46.5 24.5z"
                        )
                        .toNodes(),
                    fill = SolidColor(Color(0xFF4285F4)),
                )
                addPath(
                    PathParser()
                        .parsePathString(
                            "M10.3 28.4c-.5-1.5-.8-3.1-.8-4.9s.3-3.4.8-4.9l-7.8-6.1C.9 16 0 19.9 0 23.5s.9 7.5 2.5 11l7.8-6.1z"
                        )
                        .toNodes(),
                    fill = SolidColor(Color(0xFFFBBC05)),
                )
                addPath(
                    PathParser()
                        .parsePathString(
                            "M24 47c6.2 0 11.4-2 15.2-5.5l-7.5-5.8c-2 1.4-4.7 2.3-7.7 2.3-6.4 0-11.8-3.8-13.7-9.4l-7.8 6.1C6.4 42.6 14.6 47 24 47z"
                        )
                        .toNodes(),
                    fill = SolidColor(Color(0xFF34A853)),
                )
            }
            .build()
    }

    val Apple: ImageVector by lazy {
        ImageVector.Builder(
                name = "AppleLogo",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
            .apply {
                addPath(
                    PathParser()
                        .parsePathString(
                            "M16.365 1.43c0 1.14-.42 2.21-1.13 3.02-.78.9-2.06 1.6-3.13 1.51-.13-1.1.42-2.27 1.09-3.01.74-.84 2.06-1.46 3.17-1.52zM20.5 17.06c-.55 1.27-.82 1.84-1.53 2.96-.99 1.57-2.39 3.53-4.12 3.54-1.54.02-1.94-1-4.03-.99-2.09.01-2.53 1.01-4.07.99-1.73-.02-3.06-1.78-4.05-3.35C-.02 16.32-.31 11.5 1.36 8.93 2.55 7.1 4.42 6.02 6.18 6.02c1.79 0 2.92 1 4.4 1 1.44 0 2.31-1 4.4-1 1.57 0 3.24.86 4.42 2.34-3.89 2.13-3.26 7.68 1.1 8.7z"
                        )
                        .toNodes(),
                    fill = SolidColor(Color(0xFF000000)),
                )
            }
            .build()
    }
}
