package fr.clrk.reveil.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.Place
import fr.clrk.reveil.core.Recurrence
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Screenshots of the new alarm steps and of the weather city screen. */
class NewAlarmScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private val sunday = ZonedDateTime.of(2026, 11, 15, 22, 48, 0, 0, ZoneId.of("Europe/Paris"))
    private val draft = Alarm.at(LocalTime.of(7, 0), "3").copy(days = Recurrence.WEEKDAYS.days)
    private val chosen = draft.copy(playlistId = "PLchill", playlistTitle = "Chill matin")

    private val noEvents = object : FlowEvents {
        override fun change(alarm: Alarm) = Unit
        override fun go(step: Step) = Unit
        override fun more(alarm: Alarm) = Unit
        override fun save(alarm: Alarm) = Unit
        override fun close() = Unit
    }

    private fun shot(content: @Composable () -> Unit) = paparazzi.snapshot { MaterialTheme(colorScheme = reveilColors()) { content() } }

    @Test
    fun creationHeure() = shot { FlowScreen(draft, Step.TIME, noEvents, sunday) }

    @Test
    fun creationJours() = shot { FlowScreen(draft, Step.DAYS, noEvents, sunday) }

    @Test
    fun creationRecapitulatif() = shot { FlowScreen(chosen, Step.SUMMARY, noEvents, sunday) }

    @Test
    fun villeMeteo() = shot {
        val found = listOf(
            Place("Claix", 45.12, 5.67, "Auvergne-Rhône-Alpes, France"),
            Place("Claix", 45.6, 0.03, "Nouvelle-Aquitaine, France"),
        )
        CityScreen(CityUi("Claix", found, null), {}, {}, {})
    }
}
