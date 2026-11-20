package fr.clrk.reveil.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import fr.clrk.reveil.alarm.RingState
import fr.clrk.reveil.core.DayWeather
import fr.clrk.reveil.core.HalfDay
import fr.clrk.reveil.core.Sky
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

    @Test
    fun sonnerieSansMeteoNiSnooze() = ring(null, 60, snooze = null)
}
