package fr.clrk.reveil.music

import fr.clrk.reveil.alarm.ringHost
import fr.clrk.reveil.core.Track
import okhttp3.Request
import java.io.File

/** Covers kept next to the tracks, so the ringing screen shows them without network. */
class CoverStore internal constructor(private val dir: File, private val fetch: Fetch) {
    /** The cover kept for [track], null when it has none. */
    fun of(track: Track): File? = file(track.id).takeIf { it.exists() }

    /** Fetches the covers still missing, as for tracks kept before covers were; a failed one is tried again next time. */
    fun storeMissing(tracks: List<Track>) = tracks.filter { of(it) == null }.forEach(::store)

    /** Best effort: a track without cover still rings. */
    internal fun store(track: Track) {
        val url = fetch.host.coverOf(track) ?: return
        runCatching {
            val temp = File(dir, "${track.id}.cover.part")
            temp.writeBytes(fetch.get(Request.Builder().url(url).build()))
            temp.renameTo(file(track.id))
        }
    }

    internal fun remove(id: String) = file(id).delete()

    private fun file(id: String) = File(dir, "$id.cover")
}
