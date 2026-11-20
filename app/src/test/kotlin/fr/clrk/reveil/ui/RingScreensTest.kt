package fr.clrk.reveil.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import fr.clrk.reveil.alarm.RingState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.painter.BitmapPainter
import fr.clrk.reveil.core.ArmPose
import fr.clrk.reveil.core.DayWeather
import fr.clrk.reveil.core.HalfDay
import fr.clrk.reveil.core.MorningLight
import fr.clrk.reveil.core.Sky
import fr.clrk.reveil.core.Tonearm
import fr.clrk.reveil.ui.ring.MorningScene
import fr.clrk.reveil.ui.ring.SceneState
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** The ring screen through the morning and through the weathers, recorded with `./gradlew :app:recordPaparazziDebug`. */
class RingScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private val friday = LocalDate.of(2026, 11, 20)

    private fun ring(sky: Sky?, seconds: Long, snooze: String? = "10 min · encore 3 fois") =
        ring(sky, Duration.ofSeconds(seconds), snooze)

    private fun ring(sky: Sky?, time: Duration, snooze: String? = "10 min · encore 3 fois") = paparazzi.snapshot {
        val weather = sky?.let { DayWeather(HalfDay(it, 4), HalfDay(Sky.CLEAR, 11)) }
        val ui = RingState.Ui("Boulot", "Norah Jones - Come Away With Me", snooze, weather = weather)
        RingScreen(ui, LocalTime.of(7, 0), friday, time) {}
    }

    @Test
    fun sonnerieNuit() = ring(Sky.CLEAR, 0)

    @Test
    fun sonnerieBrasEnRoute() = ring(Sky.CLEAR, Duration.ofMillis(1000))

    @Test
    fun sonneriePremierRayon() = ring(Sky.CLEAR, 18)

    @Test
    fun sonnerieSoleil() = ring(Sky.CLEAR, 60)

    @Test
    fun sonneriePluie() = ring(Sky.RAIN, 60)

    @Test
    fun sonnerieNeige() = ring(Sky.SNOW, 60)

    @Test
    fun sonnerieNuages() = ring(Sky.CLOUDY, 60)

    @Test
    fun sonnerieGel() = ring(Sky.FROST, 60)

    /** A YouTube-like 4:3 thumbnail: black bars around a 16:9 frame, the square cover in its centre. */
    private fun thumbnail(): BitmapPainter {
        val image = ImageBitmap(480, 360)
        Canvas(image).apply {
            drawRect(0f, 0f, 480f, 360f, paint(Color.Black))
            drawRect(0f, 45f, 480f, 315f, paint(Color(0xFF3B4C5A)))
            drawRect(105f, 45f, 375f, 315f, paint(Color(0xFFD9733F)))
            drawCircle(Offset(240f, 180f), 60f, paint(Color(0xFFF4D7A1)))
        }
        return BitmapPainter(image)
    }

    private fun paint(color: Color) = Paint().also { it.color = color }

    private fun scene(sky: Sky, seconds: Float, arm: ArmPose) = paparazzi.snapshot {
        val stage = MorningLight(60).at(Duration.ofMillis((seconds * 1000).toLong()))
        val state = SceneState(stage, sky, thumbnail(), 35f, arm, seconds)
        MorningScene(state, Modifier.fillMaxSize().background(Color(0xFF06090B)))
    }

    @Test
    fun platinePochetteEtPluie() = scene(Sky.RAIN, 60f, Tonearm.pose(0f))

    @Test
    fun platinePluieQuiAGlisse() = scene(Sky.RAIN, 75f, Tonearm.pose(0f))

    @Test
    fun platineBrasSouleve() = scene(Sky.CLEAR, 60f, Tonearm.pose(0.3f))

    @Test
    fun sonnerieSansMeteoNiSnooze() = ring(null, 60, snooze = null)
}
