package fr.clrk.reveil.core

/** A raw search hit from YouTube Music, as the extractor returns it. */
data class FoundPlaylist(val url: String, val title: String, val author: String, val trackCount: Long, val thumbnail: String? = null)

/** A playlist the user can pick for an alarm. */
data class PlaylistChoice(
    val id: PlaylistId,
    val title: String,
    val author: String = "",
    val trackCount: Long = -1,
    val thumbnail: String? = null,
)

/** Port to the YouTube Music playlist search. */
fun interface PlaylistSearch {
    fun search(query: String): List<FoundPlaylist>
}

sealed interface SearchResult {
    data object Idle : SearchResult

    data class Found(val playlists: List<PlaylistChoice>) : SearchResult

    data class Message(val text: String) : SearchResult
}

/** One field for everything: a pasted link is taken as is, any other text searches YouTube Music. */
class PlaylistFinder(private val search: PlaylistSearch) {
    fun find(text: String): SearchResult {
        val query = text.trim()
        PlaylistLink.parse(query)?.let { return SearchResult.Found(listOf(PlaylistChoice(it, "Playlist ${it.value}"))) }
        return if (query.length < MIN_QUERY) SearchResult.Idle else searchOnline(query)
    }

    private fun searchOnline(query: String): SearchResult {
        val found = runCatching { search.search(query) }.getOrElse { return SearchResult.Message(OFFLINE) }
        val choices = found.mapNotNull(::toChoice).distinctBy { it.id }
        return if (choices.isEmpty()) SearchResult.Message("Aucune playlist trouvée pour « $query »") else SearchResult.Found(choices)
    }

    private fun toChoice(found: FoundPlaylist) =
        PlaylistLink.parse(found.url)?.let { PlaylistChoice(it, found.title, found.author, found.trackCount, found.thumbnail) }

    private companion object {
        const val MIN_QUERY = 2
        const val OFFLINE = "Pas de connexion à YouTube Music"
    }
}
