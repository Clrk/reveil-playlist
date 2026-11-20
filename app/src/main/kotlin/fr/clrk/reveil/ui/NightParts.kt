package fr.clrk.reveil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.clrk.reveil.core.LightStage
import fr.clrk.reveil.core.Recurrence
import fr.clrk.reveil.core.Sky
import fr.clrk.reveil.core.Tonearm
import fr.clrk.reveil.ui.ring.Label
import fr.clrk.reveil.ui.ring.MorningScene
import fr.clrk.reveil.ui.ring.SceneState
import fr.clrk.reveil.ui.ring.WeatherIcon
import java.time.DayOfWeek

/** "L M M J V S D", the days the alarm rings in cream, the others dimmed. */
@Composable
fun DayLetters(days: Set<DayOfWeek>, modifier: Modifier = Modifier) {
    val text = buildAnnotatedString {
        DayOfWeek.entries.forEach { day ->
            withStyle(SpanStyle(color = if (day in days) Night.CREAM else Night.DIM)) { append(Recurrence.letter(day)) }
        }
    }
    Text(text, modifier, fontSize = 13.sp, letterSpacing = 3.sp)
}

/** A full page on the night background, cream text by default. */
@Composable
fun NightPage(content: @Composable () -> Unit) =
    Surface(Modifier.fillMaxSize(), color = Night.BG, contentColor = Night.CREAM, content = content)

/** The one ember button of a screen. */
@Composable
fun Cta(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier.fillMaxWidth().background(if (enabled) Night.EMBER else Night.FROST, RoundedCornerShape(30.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) Night.ON_EMBER else Night.DIM, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
}

/** The night turntable, arm at rest, framed on the deck and fading into the page below. */
@Composable
fun WaitingTurntable(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val scene = maxOf(maxHeight * (PLATE_H / (DECK_BOTTOM - DECK_TOP)), maxWidth * (PLATE_H / PLATE_W))
        MorningScene(
            SceneState(DARK, Sky.CLEAR, Label(null), 0f, Tonearm.start(0)),
            Modifier.fillMaxWidth().requiredHeight(scene).offset(y = -scene * (DECK_TOP / PLATE_H) + (scene - maxHeight) / 2),
        )
        Box(Modifier.fillMaxSize().background(FADE))
    }
}

/** Rows of the plate pictures between which the turntable sits. */
private const val DECK_TOP = 440f
private const val DECK_BOTTOM = 860f
private const val PLATE_H = 1024f
private const val PLATE_W = 572f

private val DARK = LightStage(0f, 0f, 0f, 0f, 0f)
@Suppress("MagicNumber") // stops of the fade
private val FADE = Brush.verticalGradient(
    0f to Night.BG.copy(alpha = 0.5f), 0.35f to Color.Transparent, 0.75f to Color.Transparent, 1f to Night.BG,
)

/** Tomorrow morning's sky and temperature with the city; without a city, an invitation to choose one. */
@Composable
fun WeatherFooter(line: WeatherLine, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (line.city == null) return@Row Text("Ajouter la météo du matin", color = Night.EMBER, fontSize = 14.sp)
        Text("demain matin", color = Night.CAPTION, fontSize = 13.sp)
        line.morning?.let {
            WeatherIcon(it.sky, Night.CREAM, Modifier.padding(horizontal = 6.dp).size(20.dp), animate = false)
            Text("${it.temperature}°", fontSize = 20.sp, fontWeight = FontWeight.Light)
        }
        Text("  ${line.city}", color = Night.CAPTION, fontSize = 13.sp)
    }
}
