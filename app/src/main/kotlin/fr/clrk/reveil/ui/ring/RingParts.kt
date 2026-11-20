package fr.clrk.reveil.ui.ring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.runtime.withFrameMillis
import fr.clrk.reveil.core.DayWeather
import fr.clrk.reveil.core.HalfDay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Time since the alarm started, updated every frame. */
@Composable
fun rememberElapsed(startedAt: Long): Duration {
    val now by produceState(System.currentTimeMillis(), startedAt) { while (true) withFrameMillis { value = System.currentTimeMillis() } }
    return Duration.ofMillis(now - startedAt)
}

/** Big time (minutes in ember), the date, then morning and afternoon weather. */
@Composable
fun RingClock(now: LocalTime, today: LocalDate, weather: DayWeather?, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val time = buildAnnotatedString {
            append("%02d".format(now.hour))
            withStyle(SpanStyle(color = ink.copy(alpha = 0.6f))) { append(":") }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("%02d".format(now.minute)) }
        }
        Text(time, fontSize = 92.sp, fontWeight = FontWeight.Thin, color = CREAM)
        Text(today.format(DATE).replaceFirstChar { it.titlecase(Locale.FRENCH) }, fontSize = 18.sp, color = ink)
        weather?.let { WeatherRow(it, Modifier.padding(top = 18.dp)) }
    }
}

@Composable
private fun WeatherRow(day: DayWeather, modifier: Modifier) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        HalfDayCell("matin", day.morning)
        Box(Modifier.width(1.dp).fillMaxHeight().background(DIVIDER))
        HalfDayCell("après-midi", day.afternoon)
    }
}

@Composable
private fun HalfDayCell(name: String, half: HalfDay) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, fontSize = 14.sp, color = CAPTION)
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(half.sky, ICON, Modifier.size(32.dp))
            Text(" ${half.temperature}°", fontSize = 30.sp, fontWeight = FontWeight.Light, color = CREAM)
        }
    }
}

/** A real button, quiet and frosted: snoozing is the gentle choice, so it is the easy one. */
@Composable
fun SnoozeButton(detail: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(36.dp)
    Column(
        Modifier.fillMaxWidth().height(72.dp)
            .background(FROST_FILL, shape).border(1.dp, FROST_EDGE, shape)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { role = Role.Button; contentDescription = "Snooze, $detail"; onClick("Snooze") { onClick(); true } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Snooze", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(detail, fontSize = 13.sp, color = DETAIL)
    }
}

private val CREAM = Color(0xFFF4ECE2)
private val DIVIDER = Color(0x38FFE6CD)
private val CAPTION = Color(0xBFCDBBA7)
private val ICON = Color(0xFFE2D2BF)
private val FROST_FILL = Color(0x14FFE8D6)
private val FROST_EDGE = Color(0x40FFDCBE)
private val DETAIL = Color(0xFFE0CDB9)
private val DATE = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
