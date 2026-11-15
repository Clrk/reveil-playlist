package fr.clrk.reveil.core

import java.net.URI
import java.net.URISyntaxException

/** A YouTube (Music) playlist identifier, e.g. PL..., OLAK5uy_..., RDCLAK... */
@JvmInline
value class PlaylistId(val value: String) {
    val url: String get() = "https://www.youtube.com/playlist?list=$value"
}

data class Track(val id: String, val title: String)

/** Port to an extractor (NewPipeExtractor, yt-dlp...) listing the tracks of a playlist. */
fun interface PlaylistSource {
    fun tracks(id: PlaylistId): List<Track>
}

/** Turns whatever the user pasted (share link, browse link, bare id) into a [PlaylistId]. */
object PlaylistLink {
    private val idPattern = Regex("^(PL|OLAK5uy_|RD|UU|FL)[A-Za-z0-9_-]+$")
    private val hosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")
    private const val BROWSE_PREFIX = "/browse/VL"

    fun parse(text: String): PlaylistId? {
        val input = text.trim()
        val candidate = if (idPattern.matches(input)) input else fromUrl(input)
        return candidate?.takeIf { idPattern.matches(it) }?.let(::PlaylistId)
    }

    private fun fromUrl(input: String): String? {
        val uri = try { URI(input) } catch (_: URISyntaxException) { return null }
        if (uri.host !in hosts) return null
        return listParameter(uri) ?: uri.path.takeIf { it.startsWith(BROWSE_PREFIX) }?.removePrefix(BROWSE_PREFIX)
    }

    private fun listParameter(uri: URI): String? = uri.rawQuery.orEmpty()
        .split("&")
        .map { it.substringBefore("=") to it.substringAfter("=", "") }
        .firstOrNull { it.first == "list" }
        ?.second
}
