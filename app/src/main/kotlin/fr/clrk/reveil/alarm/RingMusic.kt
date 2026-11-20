package fr.clrk.reveil.alarm

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.DayWeather
import fr.clrk.reveil.core.FallbackPlaylistSource
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.PlaylistSource
import fr.clrk.reveil.core.RecordChange
import fr.clrk.reveil.core.WakeQueueBuilder
import fr.clrk.reveil.core.WakeSession
import fr.clrk.reveil.core.WakeSource
import fr.clrk.reveil.music.AudioResolver
import fr.clrk.reveil.music.NewPipePlaylistSource
import fr.clrk.reveil.music.TrackCache
import fr.clrk.reveil.music.YtMusicAccount
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
            val playlist = FallbackPlaylistSource(YtMusicAccount(context), NewPipePlaylistSource())
            runBlocking { withTimeout(LIST_TIMEOUT_MS) { runInterruptible { playlist.tracks(id) } } }
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
    /**
     * [snooze] describes the snooze button, null once no snooze is left.
     * [startedAt] and [rampSeconds] drive the morning light; [weather] is today's forecast when known.
     */
    data class Ui(
        val label: String,
        val nowPlaying: String,
        val snooze: String? = null,
        val cover: String? = null,
        val startedAt: Long = System.currentTimeMillis(),
        val rampSeconds: Int = Alarm.DEFAULT_RAMP_SECONDS,
        val weather: DayWeather? = null,
        val change: RecordChange? = null,
        val previousCover: String? = null,
    )

    val ui = kotlinx.coroutines.flow.MutableStateFlow<Ui?>(null)

    fun show(alarm: Alarm?, weather: DayWeather?) {
        val snooze = alarm?.let(::snoozeText) ?: Alarm.DEFAULT_SNOOZE_MINUTES.let { "$it min" }
        ui.value = Ui(alarm?.label.orEmpty(), "", snooze, rampSeconds = alarm?.rampSeconds ?: Alarm.DEFAULT_RAMP_SECONDS, weather = weather)
    }

    fun weather(day: DayWeather?) {
        if (day != null) ui.value = ui.value?.copy(weather = day)
    }

    fun playing(source: WakeSource) {
        val track = (source as? WakeSource.Music)?.track
        ui.value = ui.value?.copy(nowPlaying = track?.title ?: "Sonnerie de secours", cover = track?.let { cover(it.id) })
    }

    /** The track is over (or failed): the record starts changing, keeping the old cover until it stops. */
    fun ended() {
        val current = ui.value ?: return
        if (current.change != null && current.change.startedMs == null) return
        ui.value = current.copy(change = RecordChange(sinceStart(current)), previousCover = current.cover)
    }

    /** The next track really plays: the arm may land. */
    fun started() {
        val current = ui.value ?: return
        val change = current.change?.takeIf { it.startedMs == null } ?: return
        ui.value = current.copy(change = change.copy(startedMs = sinceStart(current)))
    }

    private fun sinceStart(current: Ui) = System.currentTimeMillis() - current.startedAt

    fun hide() { ui.value = null }

    fun noSnoozeLeft() { ui.value = ui.value?.copy(snooze = null) }

    private fun cover(videoId: String) = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

    private fun snoozeText(alarm: Alarm): String? = when (val left = alarm.snoozesLeft) {
        0 -> null
        null -> "${alarm.snoozeMinutes} min"
        1 -> "${alarm.snoozeMinutes} min · dernier snooze"
        else -> "${alarm.snoozeMinutes} min · encore $left fois"
    }
}
