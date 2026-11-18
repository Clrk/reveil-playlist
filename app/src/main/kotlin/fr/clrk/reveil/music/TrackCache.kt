package fr.clrk.reveil.music

import android.content.Context
import androidx.core.content.edit
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.Track
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Audio files pre-downloaded for one playlist, plus the tracks already heard at wake-up. */
class TrackCache(context: Context, playlist: PlaylistId) {
    private val dir = File(context.filesDir, "tracks/${playlist.value}").apply { mkdirs() }
    private val index = File(dir, "index.json")
    private val prefs = context.getSharedPreferences("played-${playlist.value}", Context.MODE_PRIVATE)

    fun tracks(): List<Track> = titles().filter { file(it.key).exists() }.map { Track(it.key, it.value) }

    fun file(track: Track) = file(track.id)

    fun store(track: Track, streamUrl: String) {
        val temp = File(dir, "${track.id}.part")
        download(streamUrl, temp)
        temp.renameTo(file(track.id))
        writeTitles(titles() + (track.id to track.title))
    }

    fun remove(track: Track) {
        file(track.id).delete()
        writeTitles(titles() - track.id)
    }

    fun played(): Set<String> = prefs.getStringSet(PLAYED, emptySet()).orEmpty()

    fun markPlayed(track: Track) = prefs.edit { putStringSet(PLAYED, played() + track.id) }

    fun forgetPlayed() = prefs.edit { remove(PLAYED) }

    /** Track ids of the playlist at the last download, to spot the ones added since. */
    fun known(): Set<String> = prefs.getStringSet(KNOWN, emptySet()).orEmpty()

    fun remember(tracks: List<Track>) = prefs.edit { putStringSet(KNOWN, tracks.map { it.id }.toSet()) }

    private fun file(id: String) = File(dir, "$id.audio")

    private fun titles(): Map<String, String> {
        if (!index.exists()) return emptyMap()
        val json = JSONObject(index.readText())
        return json.keys().asSequence().associateWith { json.getString(it) }
    }

    private fun writeTitles(titles: Map<String, String>) = index.writeText(JSONObject(titles).toString())

    /** YouTube throttles big responses: fetch the file in ranged chunks. */
    private fun download(url: String, target: File) = target.outputStream().use { out ->
        var start = 0L
        do {
            val bytes = chunk(url, start)
            out.write(bytes)
            start += bytes.size
        } while (bytes.size.toLong() == CHUNK)
    }

    private fun chunk(url: String, start: Long): ByteArray {
        val request = Request.Builder().url(url).header("Range", "bytes=$start-${start + CHUNK - 1}")
            .header("User-Agent", NewPipeDownloader.USER_AGENT).build()
        return NewPipeDownloader.instance.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.bytes()
        }
    }

    private companion object {
        const val PLAYED = "played"
        const val KNOWN = "known"
        const val CHUNK = 1_000_000L
    }
}
