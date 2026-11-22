package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.FallbackPlaylistSource
import fr.clrk.reveil.core.PlaylistChoice
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.PlaylistSource
import fr.clrk.reveil.core.Track
import fr.clrk.reveil.core.YtMusicParser
import fr.clrk.reveil.core.YtMusicSession
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestFactory
import java.io.IOException

/** Testing API for the signed-in YouTube Music account. */
class AccountWorld {
    lateinit var session: YtMusicSession
    var authorization: String? = null
    var library = listOf<PlaylistChoice>()
    var tracks = listOf<Track>()
    var withAccount = PlaylistSource { throw IOException("pas de compte") }
    var withoutAccount = PlaylistSource { emptyList() }
    val asked = mutableListOf<String>()

    fun fixture(name: String) = requireNotNull(javaClass.classLoader.getResource("fixtures/$name")).readText()

    fun found(ids: String) = PlaylistSource { MusicWorld.tracks(ids) }
}

class CompteTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/compte.feature", ::AccountWorld) {
        step("les cookies de YouTube Music \"([^\"]+)\"") { (cookies) -> session = YtMusicSession(cookies) }
        step("je suis connecté") { assertEquals(true, session.connected) }
        step("je ne suis pas connecté") { assertEquals(false, session.connected) }
        step("une requête part à l'instant \"(\\d+)\"") { (epoch) -> authorization = session.authorization(epoch.toLong()) }
        step("elle ne porte pas d'autorisation") { assertEquals(null, authorization) }
        step("elle porte l'autorisation \"([^\"]+)\"") { (expected) -> assertEquals(expected, authorization) }
        libraryAndFallbackSteps()
        pagingSteps()
    }
}

private fun fr.clrk.reveil.core.bdd.StepRegistry<AccountWorld>.libraryAndFallbackSteps() {
    step("YouTube Music renvoie la bibliothèque \"([^\"]+)\"") { (file) -> library = YtMusicParser.library(fixture(file)) }
    step("mes playlists sont \"([^\"]*)\"") { (titles) -> assertEquals(Formats.list(titles), library.map { it.title }) }
    step("la playlist \"([^\"]+)\" a l'identifiant \"([^\"]+)\"") { (title, id) ->
        assertEquals(id, library.single { it.title == title }.id.value)
    }
    step("YouTube Music renvoie la playlist \"([^\"]+)\"") { (file) -> tracks = YtMusicParser.tracks(fixture(file)) }
    step("ses morceaux sont \"([^\"]+)\"") { (titles) -> assertEquals(Formats.list(titles), tracks.map { it.title }) }
    step("le morceau \"([^\"]+)\" a l'identifiant \"([^\"]+)\"") { (title, id) ->
        assertEquals(id, tracks.single { it.title == title }.id)
    }
    step("la lecture avec mon compte échoue") { withAccount = PlaylistSource { throw IOException("refus") } }
    step("la lecture avec mon compte trouve \"([^\"]+)\"") { (ids) -> withAccount = found(ids) }
    step("la lecture sans compte trouve \"([^\"]+)\"") { (ids) -> withoutAccount = found(ids) }
    step("la playlist donne \"([^\"]+)\"") { (ids) ->
        val tracks = FallbackPlaylistSource(withAccount, withoutAccount).tracks(PlaylistId("PLx"))
        assertEquals(Formats.list(ids), tracks.map { it.id })
    }
}

private fun fr.clrk.reveil.core.bdd.StepRegistry<AccountWorld>.pagingSteps() {
    step("YouTube Music renvoie la playlist \"([^\"]+)\" puis la suite \"([^\"]+)\"") { (first, next) ->
        tracks = YtMusicParser.allTracks(fixture(first)) { token -> asked += token; fixture(next) }
    }
    step("la suite a été demandée avec \"([^\"]+)\"") { (token) -> assertEquals(listOf(token), asked) }
    step("YouTube Music renvoie sans fin la playlist \"([^\"]+)\"") { (file) ->
        tracks = YtMusicParser.allTracks(fixture(file)) { token -> asked += token; fixture(file) }
    }
    step("\"(\\d+)\" pages au plus ont été lues") { (pages) -> assertEquals(pages.toInt() - 1, asked.size) }
}
