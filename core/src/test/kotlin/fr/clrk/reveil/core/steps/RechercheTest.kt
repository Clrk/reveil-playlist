package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.FoundPlaylist
import fr.clrk.reveil.core.PlaylistFinder
import fr.clrk.reveil.core.PlaylistSearch
import fr.clrk.reveil.core.SearchResult
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestFactory
import java.io.IOException

/** Testing API: a fake YouTube Music search catalog. */
class SearchWorld : PlaylistSearch {
    private val catalog = mutableMapOf<String, List<FoundPlaylist>>()
    val queries = mutableListOf<String>()
    var online = true
    var result: SearchResult = SearchResult.Idle

    override fun search(query: String): List<FoundPlaylist> {
        queries += query
        if (!online) throw IOException("hors-ligne")
        return catalog[query].orEmpty()
    }

    fun publish(query: String, titles: List<String>) {
        catalog[query] = titles.map { FoundPlaylist(urlFor(it), it, "Auteur", TRACKS) }
    }

    fun breakLink(title: String) {
        catalog.replaceAll { _, found -> found.map { if (it.title == title) it.copy(url = "https://www.youtube.com/channel/UC1") else it } }
    }

    private fun urlFor(title: String) = "https://music.youtube.com/playlist?list=PL" + title.filter { it in 'a'..'z' || it in 'A'..'Z' }

    private companion object {
        const val TRACKS = 42L
    }
}

class RechercheTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/recherche.feature", ::SearchWorld) {
        step("YouTube Music trouve \"([^\"]+)\" pour \"([^\"]+)\"") { (titles, query) -> publish(query, Formats.list(titles)) }
        step("le résultat \"([^\"]+)\" n'est pas une playlist") { (title) -> breakLink(title) }
        step("le téléphone est hors-ligne") { online = false }
        step("je cherche \"([^\"]+)\"") { (text) -> result = PlaylistFinder(this).find(text) }
        step("aucune recherche n'est lancée") { assertEquals(emptyList<String>(), queries) }
        step("on ne me propose rien") { assertEquals(SearchResult.Idle, result) }
        step("on me propose \"([^\"]+)\"") { (titles) ->
            assertEquals(Formats.list(titles), (result as SearchResult.Found).playlists.map { it.title })
        }
        step("on me propose la playlist \"([^\"]+)\"") { (id) ->
            assertEquals(listOf(id), (result as SearchResult.Found).playlists.map { it.id.value })
        }
        step("on m'indique \"([^\"]+)\"") { (message) -> assertEquals(SearchResult.Message(message), result) }
    }
}
