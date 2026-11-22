package fr.clrk.reveil.core

/** Port to a title search from free text (« la mer trenet »). */
fun interface TrackSearch {
    fun search(query: String): List<Track>
}

/** Everything a host needs from a music source; YouTube through NewPipe, or yt-dlp on a server. */
interface MusicSource : PlaylistSource, StreamResolver, TrackSearch

/** « Mets tel morceau »: finds a title from free text and makes it playable. */
class OnDemand(private val source: MusicSource) {
    sealed interface Result {
        data class Playable(val track: Track, val streamUrl: String) : Result
        data object NotFound : Result
        data object Unavailable : Result
    }

    /** Tries the first [TRIES] hits in order: a title that cannot be read is skipped for the next one. */
    fun play(query: String): Result {
        val text = query.trim()
        if (text.isEmpty()) return Result.NotFound
        return runCatching { source.search(text) }.fold(::firstPlayable) { Result.Unavailable }
    }

    private fun firstPlayable(hits: List<Track>): Result =
        if (hits.isEmpty()) Result.NotFound else hits.take(TRIES).firstNotNullOfOrNull(::playable) ?: Result.Unavailable

    private fun playable(track: Track): Result? =
        runCatching { Result.Playable(track, source.streamUrl(track)) }.getOrNull()

    private companion object {
        const val TRIES = 3
    }
}
