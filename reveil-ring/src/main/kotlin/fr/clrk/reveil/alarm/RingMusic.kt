package fr.clrk.reveil.alarm

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.PlaylistSource
import fr.clrk.reveil.core.WakeQueueBuilder
import fr.clrk.reveil.core.WakeSession
import fr.clrk.reveil.core.WakeSource
import fr.clrk.reveil.music.TrackCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

/** Bridges the wake-up queue of the core to local files and YouTube streams. */
class RingMusic(private val context: Context, playlistId: String?) {
    private val playlist = playlistId?.let(::PlaylistId)
    private val cache = playlist?.let { TrackCache(context, it) }
    private val prefs = context.getSharedPreferences("ring", Context.MODE_PRIVATE)

    suspend fun session(): WakeSession = withContext(Dispatchers.IO) {
        val source = PlaylistSource { id ->
            val playlist = context.ringHost.playlists
            runBlocking { withTimeout(LIST_TIMEOUT_MS) { runInterruptible { playlist.tracks(id) } } }
        }
        val previousFirst = prefs.getString(LAST_FIRST, null)
        val queue = WakeQueueBuilder(source, Random.Default)
            .build(playlist, cache?.tracks().orEmpty(), previousFirst, cache?.played().orEmpty())
        (queue.firstOrNull() as? WakeSource.Music)?.let { prefs.edit { putString(LAST_FIRST, it.track.id) } }
        WakeSession(queue)
    }

    suspend fun uriOf(music: WakeSource.Music): Uri = withContext(Dispatchers.IO) {
        if (music.cached) Uri.fromFile(cache!!.file(music.track)) else context.ringHost.streams.streamUrl(music.track).toUri()
    }

    /** The track's file or stream, null when its link cannot be obtained: the track is then skipped. */
    @Suppress("TooGenericExceptionCaught")
    suspend fun linkOf(music: WakeSource.Music): Uri? = try {
        uriOf(music)
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        null
    }

    fun started(music: WakeSource.Music) = cache?.markPlayed(music.track)

    private companion object {
        const val LIST_TIMEOUT_MS = 8_000L
        const val LAST_FIRST = "lastFirst"
    }
}
