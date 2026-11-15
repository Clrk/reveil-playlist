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
import fr.clrk.reveil.music.AudioResolver
import fr.clrk.reveil.music.NewPipePlaylistSource
import fr.clrk.reveil.music.TrackCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

/** Bridges the wake-up queue of the core to local files and YouTube streams. */
class RingMusic(context: Context, playlistId: String?) {
    private val playlist = playlistId?.let(::PlaylistId)
    private val cache = playlist?.let { TrackCache(context, it) }
    private val prefs = context.getSharedPreferences("ring", Context.MODE_PRIVATE)

    suspend fun session(): WakeSession = withContext(Dispatchers.IO) {
        val source = PlaylistSource { id ->
            runBlocking { withTimeout(LIST_TIMEOUT_MS) { runInterruptible { NewPipePlaylistSource().tracks(id) } } }
        }
        val previousFirst = prefs.getString(LAST_FIRST, null)
        val queue = WakeQueueBuilder(source, Random.Default).build(playlist, cache?.tracks().orEmpty(), previousFirst)
        (queue.firstOrNull() as? WakeSource.Music)?.let { prefs.edit { putString(LAST_FIRST, it.track.id) } }
        WakeSession(queue)
    }

    suspend fun uriOf(music: WakeSource.Music): Uri = withContext(Dispatchers.IO) {
        if (music.cached) Uri.fromFile(cache!!.file(music.track)) else AudioResolver.streamUrl(music.track).toUri()
    }

    fun started(music: WakeSource.Music) = cache?.markPlayed(music.track)

    private companion object {
        const val LIST_TIMEOUT_MS = 8_000L
        const val LAST_FIRST = "lastFirst"
    }
}

/** What the lock-screen activity displays while ringing. */
object RingState {
    data class Ui(val label: String, val nowPlaying: String)

    val ui = kotlinx.coroutines.flow.MutableStateFlow<Ui?>(null)

    fun show(label: String) { ui.value = Ui(label, "") }

    fun playing(source: WakeSource) {
        val title = (source as? WakeSource.Music)?.track?.title ?: "Sonnerie de secours"
        ui.value = ui.value?.copy(nowPlaying = title)
    }

    fun hide() { ui.value = null }
}
