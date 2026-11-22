package fr.clrk.reveil

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.edit
import fr.clrk.reveil.alarm.AndroidAlarmScheduler
import fr.clrk.reveil.alarm.FileTextStore
import fr.clrk.reveil.alarm.RingChannels
import fr.clrk.reveil.alarm.RingHost
import fr.clrk.reveil.alarm.RingScreen
import fr.clrk.reveil.core.AlarmRepository
import fr.clrk.reveil.core.AlarmService
import fr.clrk.reveil.core.FallbackPlaylistSource
import fr.clrk.reveil.core.PlaylistSource
import fr.clrk.reveil.core.StreamResolver
import fr.clrk.reveil.music.AudioResolver
import fr.clrk.reveil.music.HealthStore
import fr.clrk.reveil.music.NewPipePlaylistSource
import fr.clrk.reveil.music.PrefetchScheduler
import fr.clrk.reveil.music.YouTube
import fr.clrk.reveil.music.YouTubeHealth
import fr.clrk.reveil.music.YtMusicAccount
import fr.clrk.reveil.ui.MainActivity
import fr.clrk.reveil.ui.RingActivity
import fr.clrk.reveil.update.UpdateWorker
import fr.clrk.reveil.weather.WeatherRepository
import java.io.File
import java.time.ZonedDateTime

/** Wires the pure core to its Android adapters, and hosts the ringing library. */
class ReveilApp : Application(), RingHost {
    override lateinit var alarms: AlarmService
        private set

    /** The signed-in account first, then NewPipe without account. */
    override val playlists: PlaylistSource get() = FallbackPlaylistSource(YtMusicAccount(this), NewPipePlaylistSource())
    override val streams: StreamResolver = AudioResolver
    override val downloads get() = Http.audio
    override val ringScreen = RingActivity::class.java
    override val homeScreen = MainActivity::class.java
    override val notificationIcon = R.drawable.ic_alarm
    override val display by lazy { RingScreen(this) }

    override fun onCreate() {
        super.onCreate()
        YouTube.init(Http.client)
        YouTubeHealth.init(PrefsHealthStore())
        val store = FileTextStore(File(noBackupFilesDir, "alarms.json"))
        alarms = AlarmService(AlarmRepository(store), AndroidAlarmScheduler(this, homeScreen)) { ZonedDateTime.now() }
        createChannel()
        UpdateWorker.schedule(this)
    }

    /** Call after any alarm change so the evening pre-download follows the next alarm. */
    override fun alarmsChanged() = PrefetchScheduler.reschedule(this, alarms.next())

    /** The evening download also fetches tomorrow's weather. */
    override fun beforePrefetch() = WeatherRepository(this).refresh()

    private fun createChannel() {
        val channel = NotificationChannel(RingChannels.RING, "Réveil", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
            setBypassDnd(true)
        }
        val reminder = NotificationChannel(RingChannels.REMINDER, "Rappel avant le réveil", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, reminder))
    }

    /** YouTube's health survives restarts in the same preferences as before. */
    private inner class PrefsHealthStore : HealthStore {
        private val prefs = getSharedPreferences("youtube", MODE_PRIVATE)

        override var unreadable: Int
            get() = prefs.getInt(UNREADABLE, 0)
            set(value) = prefs.edit { putInt(UNREADABLE, value) }
    }

    private companion object {
        const val UNREADABLE = "unreadable"
    }
}

val android.content.Context.app: ReveilApp get() = applicationContext as ReveilApp
