package fr.clrk.reveil.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Material You colours taken from the wallpaper (Android 12+), our violet otherwise. */
@Composable
fun reveilColors(): ColorScheme {
    val dark = isSystemInDarkTheme()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return if (dark) darkColorScheme() else lightColorScheme()
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
