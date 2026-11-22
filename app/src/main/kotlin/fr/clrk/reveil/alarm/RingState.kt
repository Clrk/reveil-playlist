package fr.clrk.reveil.alarm

import android.content.Context
import android.net.Uri
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.Track
import fr.clrk.reveil.core.WakeSource
import fr.clrk.reveil.display.DayWeather
import fr.clrk.reveil.display.RecordChange
import fr.clrk.reveil.weather.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Draws the ring on this app's lock-screen activity: today's weather, covers and record changes. */
class RingScreen(private val context: Context) : RingDisplay {
    private var weatherJob: Job? = null

    override fun show(alarm: Alarm?) {
        val weather = WeatherRepository(context)
        RingState.show(alarm, weather.day())
        weatherJob = MainScope().launch { RingState.weather(withContext(Dispatchers.IO) { weather.refresh(); weather.day() }) }
    }

    override fun playing(source: WakeSource, cover: Uri?) = RingState.playing(source, cover)

    override fun ended() = RingState.ended()

    override fun started() = RingState.started()

    override fun noSnoozeLeft() = RingState.noSnoozeLeft()

    override fun hide() {
        weatherJob?.cancel()
        RingState.hide()
    }
}

/** What the lock-screen activity displays while ringing. */
object RingState {
    /**
     * [snooze] describes the snooze button, null once no snooze is left.
     * [startedAt] and [rampSeconds] drive the morning light; [weather] is today's forecast when known.
     */
    data class Ui(
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
        ui.value = Ui(snooze, rampSeconds = alarm?.rampSeconds ?: Alarm.DEFAULT_RAMP_SECONDS, weather = weather)
    }

    fun weather(day: DayWeather?) {
        if (day != null) ui.value = ui.value?.copy(weather = day)
    }

    /** The cover kept on the phone when there is one, so it shows offline; else the one online. */
    fun playing(source: WakeSource, stored: Uri? = null) {
        val track = (source as? WakeSource.Music)?.track
        ui.value = ui.value?.copy(cover = stored?.toString() ?: track?.let(::coverOf))
    }

    /** The track is over (or failed): the record starts changing, keeping the old cover until it stops. */
    fun ended() {
        val current = ui.value ?: return
        val change = RecordChange.ended(current.change, sinceStart(current))
        if (change != current.change) ui.value = current.copy(change = change, previousCover = current.cover)
    }

    /** The next track really plays: the arm may land. */
    fun started() {
        val current = ui.value ?: return
        ui.value = current.copy(change = current.change?.started(sinceStart(current)))
    }

    private fun sinceStart(current: Ui) = System.currentTimeMillis() - current.startedAt

    fun hide() { ui.value = null }

    fun noSnoozeLeft() { ui.value = ui.value?.copy(snooze = null) }

    fun coverOf(track: Track) = "https://i.ytimg.com/vi/${track.id}/hqdefault.jpg"

    private fun snoozeText(alarm: Alarm): String? = when (val left = alarm.snoozesLeft) {
        0 -> null
        null -> "${alarm.snoozeMinutes} min"
        1 -> "${alarm.snoozeMinutes} min · dernier snooze"
        else -> "${alarm.snoozeMinutes} min · encore $left fois"
    }
}
