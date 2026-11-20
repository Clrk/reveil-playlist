package fr.clrk.reveil.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import fr.clrk.reveil.alarm.RingService
import fr.clrk.reveil.alarm.RingState
import fr.clrk.reveil.core.DayWeather
import fr.clrk.reveil.core.MorningLight
import fr.clrk.reveil.core.Tonearm
import fr.clrk.reveil.core.Turntable
import fr.clrk.reveil.ui.ring.MorningScene
import fr.clrk.reveil.ui.ring.RingClock
import fr.clrk.reveil.ui.ring.SceneState
import fr.clrk.reveil.ui.ring.SnoozeButton
import fr.clrk.reveil.ui.ring.rememberElapsed
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** Full-screen ringing screen, shown above the lock screen. Always dark: it is read with sleepy eyes. */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ui by RingState.ui.collectAsStateWithLifecycle()
            LaunchedEffect(ui) { if (ui == null) finish() }
            ui?.let { RingScreen(it, send = ::send) }
        }
    }

    /** The screen closes only once the service has really stopped ringing (the state goes null). */
    private fun send(action: String) {
        RingService.send(this, action)
    }
}

internal val RingColors = darkColorScheme(
    background = Color(0xFF06090B),
    surface = Color(0xFF06090B),
    onSurface = Color(0xFFE6D6C4),
    primary = Color(0xFFE3A36B),
    onPrimary = Color(0xFF1A1210),
)

/**
 * The room wakes up with the music: the light rises over the volume ramp, the record spins with the track cover
 * as label once the tonearm has landed on it, and sliding to stop lifts the tonearm.
 * [elapsed] fixes the light for screenshots; null follows the clock.
 */
@Composable
internal fun RingScreen(
    ui: RingState.Ui,
    now: LocalTime = LocalTime.now(),
    today: LocalDate = LocalDate.now(),
    elapsed: Duration? = null,
    send: (String) -> Unit,
) {
    val time = elapsed ?: rememberElapsed(ui.startedAt)
    val light = MorningLight(ui.rampSeconds).at(time)
    var slide by remember { mutableFloatStateOf(0f) }
    val cover = ui.cover?.let { rememberAsyncImagePainter(it) }
    MaterialTheme(colorScheme = RingColors) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                val arm = if (slide > 0f) Tonearm.pose(slide) else Tonearm.start(time.toMillis())
                val sky = DayWeather.sceneOf(ui.weather)
                val scene = SceneState(light, sky, cover, Turntable.angle(time.toMillis()), arm, time.toMillis() / MS_PER_SECOND)
                MorningScene(scene, Modifier.fillMaxSize())
                val top = Modifier.align(Alignment.TopCenter).systemBarsPadding().padding(top = 48.dp).alpha(light.text)
                RingClock(now, today, ui.weather, top)
                Column(Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
                    ui.snooze?.let { SnoozeButton(it) { send(RingService.ACTION_SNOOZE) } }
                    Spacer(Modifier.height(16.dp))
                    SlideToStop(onProgress = { slide = it }) { send(RingService.ACTION_DISMISS) }
                }
            }
        }
    }
}

private const val MS_PER_SECOND = 1000f
