@file:Suppress("MagicNumber") // the palette itself
package fr.clrk.reveil.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.time.LocalTime

/** The night palette shared with the ring screen: blue-black, cream, and ember for what is on. */
object Night {
    val BG = Color(0xFF06090B)
    val CREAM = Color(0xFFE6D6C4)
    val EMBER = Color(0xFFE3A36B)
    val ON_EMBER = Color(0xFF1A1210)
    val CAPTION = Color(0x8CE6D6C4)
    val DIM = Color(0x47E6D6C4)
    val LINE = Color(0x1FE6D6C4)
    val FROST = Color(0x12E6D6C4)
    val WARN = Color(0xFFD98B7A)
    val CARD = Color(0xFF111518)
}

/** Always the night palette: the app is used in the evening, and leads to the ring screen. */
@Composable
fun reveilColors(): ColorScheme = darkColorScheme(
    primary = Night.EMBER, onPrimary = Night.ON_EMBER,
    primaryContainer = Color(0xFF2E2219), onPrimaryContainer = Night.CREAM,
    secondaryContainer = Color(0xFF2E2219), onSecondaryContainer = Night.CREAM,
    tertiary = Night.EMBER,
    background = Night.BG, onBackground = Night.CREAM,
    surface = Night.BG, onSurface = Night.CREAM, onSurfaceVariant = Night.CAPTION,
    surfaceVariant = Color(0xFF14181B), surfaceContainer = Color(0xFF101417),
    surfaceContainerHigh = Color(0xFF161A1D), surfaceContainerHighest = Color(0xFF1B1F22),
    surfaceContainerLow = Color(0xFF0C1013), outline = Night.DIM, outlineVariant = Night.LINE,
    error = Night.WARN, errorContainer = Color(0xFF3A1E1A), onErrorContainer = Color(0xFFF2C4B8),
)

/** "07:00" with the minutes in ember while the alarm is on, as on the ring screen. */
fun clock(time: LocalTime, on: Boolean = true): AnnotatedString = buildAnnotatedString {
    append("%02d:".format(time.hour))
    withStyle(SpanStyle(color = if (on) Night.EMBER else Color.Unspecified)) { append("%02d".format(time.minute)) }
}
