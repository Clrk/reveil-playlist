package fr.clrk.reveil.core

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest

/** The YouTube Music web session the user opened in the app (cookies only, no Google project). */
class YtMusicSession(val cookies: String) {
    private val sapisid: String? = cookies.split(";")
        .map { it.trim().substringBefore("=") to it.trim().substringAfter("=", "") }
        .firstOrNull { it.first in SESSION_COOKIES && it.second.isNotEmpty() }
        ?.second

    val connected: Boolean get() = sapisid != null

    /** Same signature as the YouTube Music website: SHA-1 of "time SAPISID origin". */
    fun authorization(epochSeconds: Long): String? = sapisid?.let {
        val digest = MessageDigest.getInstance("SHA-1").digest("$epochSeconds $it $ORIGIN".toByteArray())
        "SAPISIDHASH ${epochSeconds}_${digest.joinToString("") { byte -> "%02x".format(byte) }}"
    }

    companion object {
        const val ORIGIN = "https://music.youtube.com"
        private val SESSION_COOKIES = setOf("SAPISID", "__Secure-3PAPISID")
    }
}

/** Reads YouTube Music web responses; tolerant to layout changes by looking for renderers anywhere. */
object YtMusicParser {
    private const val BROWSE_PREFIX = "VL"
    private const val UNKNOWN_TITLE = "Titre inconnu"
    private const val MAX_PAGES = 20

    fun library(json: String): List<PlaylistChoice> = renderers(json, "musicTwoRowItemRenderer").mapNotNull(::toChoice)

    fun tracks(json: String): List<Track> = renderers(json, "musicResponsiveListItemRenderer").mapNotNull(::toTrack)

    /** Reads a long playlist page after page (YouTube Music sends about 100 tracks per page). */
    fun allTracks(firstPage: String, nextPage: (String) -> String): List<Track> {
        val pages = generateSequence(firstPage) { page -> continuation(page)?.let(nextPage) }.take(MAX_PAGES)
        return pages.flatMap { tracks(it) }.toList()
    }

    private fun continuation(json: String): String? =
        renderers(json, "continuationCommand").firstNotNullOfOrNull { it.text("token") }

    private fun toChoice(item: JsonObject): PlaylistChoice? {
        val browseId = item.text("navigationEndpoint", "browseEndpoint", "browseId") ?: return null
        val id = PlaylistLink.parse(browseId.removePrefix(BROWSE_PREFIX)) ?: return null
        val cover = item.path("thumbnailRenderer", "musicThumbnailRenderer", "thumbnail", "thumbnails").objects().lastOrNull()
        return PlaylistChoice(id, runs(item["title"]), runs(item["subtitle"]), thumbnail = cover?.text("url"))
    }

    private fun toTrack(item: JsonObject): Track? {
        val videoId = item.text("playlistItemData", "videoId") ?: return null
        val firstColumn = item["flexColumns"].objects().firstOrNull()
        return Track(videoId, runs(firstColumn?.path("musicResponsiveListItemFlexColumnRenderer", "text")).ifBlank { UNKNOWN_TITLE })
    }

    private fun runs(element: JsonElement?): String =
        (element as? JsonObject)?.get("runs").objects().mapNotNull { it.text("text") }.joinToString("")

    private fun JsonElement?.objects(): List<JsonObject> = (this as? JsonArray).orEmpty().filterIsInstance<JsonObject>()

    private fun renderers(json: String, key: String): List<JsonObject> = try {
        collect(Json.parseToJsonElement(json), key)
    } catch (_: SerializationException) {
        emptyList()
    }

    private fun collect(element: JsonElement, key: String): List<JsonObject> = when (element) {
        is JsonObject -> listOfNotNull(element[key] as? JsonObject) + element.values.flatMap { collect(it, key) }
        is JsonArray -> element.flatMap { collect(it, key) }
        is JsonPrimitive -> emptyList()
    }

}

private fun JsonObject.path(vararg keys: String): JsonElement? =
    keys.fold(this as JsonElement?) { current, key -> (current as? JsonObject)?.get(key) }

private fun JsonObject.text(vararg keys: String): String? = (path(*keys) as? JsonPrimitive)?.content

/** Reads a playlist with the user's account first (private playlists), then anonymously. */
class FallbackPlaylistSource(private val primary: PlaylistSource, private val secondary: PlaylistSource) : PlaylistSource {
    override fun tracks(id: PlaylistId): List<Track> =
        runCatching { primary.tracks(id) }.getOrDefault(emptyList()).ifEmpty { secondary.tracks(id) }
}
