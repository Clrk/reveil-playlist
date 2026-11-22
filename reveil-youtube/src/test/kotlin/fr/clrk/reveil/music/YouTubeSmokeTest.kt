package fr.clrk.reveil.music

import fr.clrk.reveil.core.OnDemand
import fr.clrk.reveil.core.PlaylistLink
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Talks to the real YouTube through the real adapter, as the alarm does: search, list a playlist, find an audio stream.
 * Skipped unless run with `-Psmoke`: the daily workflow uses it to notice when YouTube breaks NewPipeExtractor.
 */
class YouTubeSmokeTest {
    @Test
    fun aPlaylistFoundBySearchCanBePlayed() {
        val found = NewPipePlaylistSearch.search(QUERY)
        assertTrue(found.isNotEmpty(), "aucune playlist trouvée")
        val playlist = requireNotNull(PlaylistLink.parse(found.first().url)) { "lien illisible : ${found.first().url}" }
        val tracks = NewPipePlaylistSource(MAX_TRACKS).tracks(playlist)
        assertTrue(tracks.isNotEmpty(), "playlist vide")
        assertTrue(AudioResolver.streamUrl(tracks.first()).startsWith("https://"), "aucun flux audio")
    }

    @Test
    fun aSongAskedByNameCanBePlayed() {
        val result = OnDemand(YouTubeSource()).play(SONG)
        assertTrue(result is OnDemand.Result.Playable, "résultat : $result")
    }

    companion object {
        private const val SONG = "la mer charles trenet"
        private const val QUERY = "lofi morning"
        private const val MAX_TRACKS = 20

        @BeforeAll
        @JvmStatic
        fun online() {
            assumeTrue(System.getProperty("smoke") == "true")
            YouTube.init(OkHttpClient())
        }
    }
}
