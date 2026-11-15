package fr.clrk.reveil.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import fr.clrk.reveil.alarm.RingState
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.PlaylistChoice
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.SearchResult
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/** Screenshots of every screen, recorded with `./gradlew :app:recordPaparazziDebug`. */
class ScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private val weekdays = DayOfWeek.entries.take(5).toSet()
    private val boulot = Alarm("1", LocalTime.of(7, 0), weekdays, "Boulot", playlistId = "PLchill", playlistTitle = "Chill matin")
    private val weekend = Alarm("2", LocalTime.of(9, 30), setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), "Week-end", enabled = false)

    private val noActions = object : EditorActions {
        override fun save(alarm: Alarm) = Unit
        override fun delete(alarm: Alarm) = Unit
        override fun test(alarm: Alarm) = Unit
        override fun prefetch(playlist: String) = Unit
        override fun close() = Unit
    }

    private val noListEvents = object : ListEvents {
        override fun add() = Unit
        override fun open(alarm: Alarm) = Unit
        override fun toggle(alarm: Alarm, enabled: Boolean) = Unit
    }

    private fun shot(content: @Composable () -> Unit) =
        paparazzi.snapshot { MaterialTheme(colorScheme = lightColorScheme()) { content() } }

    @Test
    fun liste() = shot {
        AlarmListScreen(ListUi(listOf(boulot, weekend), "Prochain réveil : lun. 07:00 (dans 8 h 12 min) Boulot", emptyList()), noListEvents)
    }

    @Test
    fun edition() = shot { AlarmEditor(boulot, "", noActions) }

    private val found = SearchResult.Found(
        listOf(
            PlaylistChoice(PlaylistId("PLa"), "Chill matin", "YouTube Music", 87),
            PlaylistChoice(PlaylistId("PLb"), "Réveil en douceur", "Lofi Girl", 120),
            PlaylistChoice(PlaylistId("PLc"), "Morning Jazz", "Jazz Vibes", 54),
            PlaylistChoice(PlaylistId("PLd"), "Café du matin — acoustique", "Coffee Music", 200),
        ),
    )

    @Test
    fun choixPlaylistVide() = shot { PlaylistPickerScreen(PickerUi(), {}, {}, {}) }

    @Test
    fun choixPlaylistResultats() = shot { PlaylistPickerScreen(PickerUi("matin", found), {}, {}, {}) }

    @Test
    fun choixPlaylistHorsLigne() =
        shot {
            val message = SearchResult.Message("YouTube Music ne répond pas. Vérifie ta connexion et réessaie.")
            PlaylistPickerScreen(PickerUi("matin", message), {}, {}, {})
        }

    @Test
    fun editionSansPlaylist() = shot { AlarmEditor(weekend, "", noActions) }

    @Test
    fun sonnerie() = shot { RingScreen(RingState.Ui("Boulot", "Norah Jones - Come Away With Me"), "07:00") {} }
}
