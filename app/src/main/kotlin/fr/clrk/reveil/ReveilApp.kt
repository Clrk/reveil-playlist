package fr.clrk.reveil

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import fr.clrk.reveil.alarm.AndroidAlarmScheduler
import fr.clrk.reveil.alarm.FileTextStore
import fr.clrk.reveil.music.NewPipeDownloader
import fr.clrk.reveil.music.YouTubeHealth
import fr.clrk.reveil.music.PrefetchScheduler
import fr.clrk.reveil.update.UpdateWorker
import fr.clrk.reveil.core.AlarmRepository
import fr.clrk.reveil.core.AlarmService
import org.schabi.newpipe.extractor.NewPipe
import java.io.File
import java.time.ZonedDateTime

/** Wires the pure core to its Android adapters. */
class ReveilApp : Application() {
    lateinit var alarms: AlarmService
        private set

    override fun onCreate() {
        super.onCreate()
        NewPipe.init(NewPipeDownloader.instance)
        YouTubeHealth.init(this)
        val store = FileTextStore(File(noBackupFilesDir, "alarms.json"))
        alarms = AlarmService(AlarmRepository(store), AndroidAlarmScheduler(this)) { ZonedDateTime.now() }
        createChannel()
        UpdateWorker.schedule(this)
    }

    /** Call after any alarm change so the evening pre-download follows the next alarm. */
    fun alarmsChanged() = PrefetchScheduler.reschedule(this, alarms.next())

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_RING, "Réveil", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
            setBypassDnd(true)
        }
        val reminder = NotificationChannel(CHANNEL_REMINDER, "Rappel avant le réveil", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, reminder))
    }

    companion object {
        const val CHANNEL_RING = "ring"
        const val CHANNEL_REMINDER = "reminder"
    }
}

val android.content.Context.app: ReveilApp get() = applicationContext as ReveilApp
