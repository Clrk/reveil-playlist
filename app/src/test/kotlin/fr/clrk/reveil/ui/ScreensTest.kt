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
import fr.clrk.reveil.core.WakeInfo
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

    private val next = WakeInfo.NextCard("lun. 07:00", "dans 8 h 12 min · Boulot")
    private val ready = mapOf("1" to "15 titres prêts hors ligne")

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
        AlarmListScreen(ListUi(listOf(boulot, weekend), next, offline = ready), noListEvents)
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

    private val noPickerEvents = object : PickerEvents {
        override fun query(text: String) = Unit
        override fun tab(tab: PickerTab) = Unit
        override fun pick(choice: PlaylistChoice) = Unit
        override fun signIn() = Unit
        override fun signOut() = Unit
        override fun close() = Unit
    }

    private val mine = listOf(
        PlaylistChoice(PlaylistId("LM"), "Titres J'aime", "Playlist automatique"),
        PlaylistChoice(PlaylistId("PLlundi"), "Réveil du lundi", "Thibault • 12 titres"),
        PlaylistChoice(PlaylistId("PLroad"), "Road trip", "Thibault • 48 titres"),
    )

    private fun picker(ui: PickerUi) = shot { PlaylistPickerScreen(ui, noPickerEvents) }

    @Test
    fun mesPlaylistsNonConnecte() = picker(PickerUi())

    @Test
    fun mesPlaylists() = picker(PickerUi(library = LibraryUi.Loaded(mine)))

    @Test
    fun rechercheVide() = picker(PickerUi(tab = PickerTab.SEARCH))

    @Test
    fun rechercheResultats() = picker(PickerUi("matin", found, tab = PickerTab.SEARCH))

    @Test
    fun rechercheErreur() {
        val message = SearchResult.Message("YouTube Music ne répond pas. Vérifie ta connexion et réessaie.")
        picker(PickerUi("matin", message, tab = PickerTab.SEARCH))
    }

    @Test
    fun editionSansPlaylist() = shot { AlarmEditor(weekend, "", noActions) }

    @Test
    fun sonnerie() = paparazzi.snapshot {
        RingScreen(RingState.Ui("Boulot", "Norah Jones - Come Away With Me", "10 min · encore 3 fois"), "07:00") {}
    }

    @Test
    fun sonnerieSansSnooze() = paparazzi.snapshot {
        RingScreen(RingState.Ui("Boulot", "Norah Jones - Come Away With Me", snooze = null), "07:20") {}
    }

    @Test
    fun listeAvertissement() = shot {
        val warning = "L'économie de batterie peut empêcher de préparer les musiques hors ligne" to {}
        AlarmListScreen(ListUi(listOf(boulot, weekend), next, listOf(warning), ready), noListEvents)
    }

    @Test
    fun listeAnnonce() = shot {
        val ui = ListUi(listOf(boulot, weekend), next, offline = mapOf("1" to "Pas encore téléchargée, ce sera fait la veille"))
        AlarmListScreen(ui.copy(announcement = "Réveil dans 8 h 12 min"), noListEvents)
    }

    @Test
    fun listeVide() = shot {
        val none = WakeInfo.NextCard("Aucun réveil prévu", "Appuie sur + pour en créer un")
        AlarmListScreen(ListUi(emptyList(), none), noListEvents)
    }
}
