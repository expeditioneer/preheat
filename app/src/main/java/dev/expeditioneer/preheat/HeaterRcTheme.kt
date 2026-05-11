package dev.expeditioneer.preheat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 theme wrapper. Uses Material You's dynamic color scheme
 * (derived from the device wallpaper) — Android 12+ is guaranteed by
 * the project's minSdk = 31, so no static-colour fallback is needed.
 */
@Composable
internal fun HeaterRcTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
