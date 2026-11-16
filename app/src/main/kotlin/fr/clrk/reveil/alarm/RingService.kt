package fr.clrk.reveil.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import fr.clrk.reveil.R
import fr.clrk.reveil.ReveilApp
import fr.clrk.reveil.app
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.VolumeRamp
import fr.clrk.reveil.core.WakeSession
import fr.clrk.reveil.core.WakeSource
import fr.clrk.reveil.ui.RingActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Foreground service that rings: shuffled music with automatic fallback to the system alarm sound. */
class RingService : Service() {
    private val scope: CoroutineScope = MainScope()
    private lateinit var player: ExoPlayer
    private lateinit var wakeLock: PowerManager.WakeLock
    private var alarmId: String? = null
    private var music: RingMusic? = null
    private var session: WakeSession? = null
    private var watchdog: Job? = null
    private var loading: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "reveil:ring")
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_ALARM).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
            addListener(PlayerEvents())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(AlarmReceiver.EXTRA_ID) ?: alarmId
        when (intent?.action) {
            ACTION_SNOOZE -> snooze()
            ACTION_DISMISS -> finish { app.alarms.dismiss(it) }
            else -> id?.let(::ring)
        }
        return START_NOT_STICKY
    }

    private fun ring(id: String) {
        alarmId = id
        val alarm = app.alarms.find(id)
        RingState.show(alarm)
        val canSnooze = alarm?.snoozesLeft != 0
        val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        ServiceCompat.startForeground(this, NOTIFICATION, notification(alarm?.label, canSnooze), type)
        wakeLock.acquire(MAX_RING_MS)
        scope.rampVolume(player, VolumeRamp(alarm?.rampSeconds ?: Alarm.DEFAULT_RAMP_SECONDS))
        scope.launch {
            val ringMusic = RingMusic(this@RingService, alarm?.playlistId)
            music = ringMusic
            session = ringMusic.session()
            playCurrent()
        }
        scope.launch {
            delay(MAX_RING_MS)
            finish { app.alarms.dismiss(it) }
        }
    }

    /** Snoozing is refused once the limit is reached: the alarm keeps ringing. */
    private fun snooze() {
        val id = alarmId ?: return
        if (app.alarms.snooze(id)) finish { }
    }

    private fun playCurrent() {
        val current = session?.current ?: return
        watchdog?.cancel()
        loading?.cancel()
        RingState.playing(current)
        if (current == WakeSource.Ringtone) return playRingtone()
        val track = (current as WakeSource.Music)
        watchdog = scope.launch {
            delay(START_TIMEOUT_MS)
            onFailure()
        }
        loading = scope.launch {
            val uri = resolve(track) ?: return@launch onFailure()
            player.setMediaItem(MediaItem.fromUri(uri))
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.prepare()
            player.play()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun resolve(track: WakeSource.Music) = try {
        music?.uriOf(track)
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        null
    }

    private fun playRingtone() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
        player.setMediaItem(MediaItem.fromUri(uri))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.prepare()
        player.play()
    }

    private fun onFailure() {
        if (session?.current == WakeSource.Ringtone) return lastResortRingtone()
        session?.onFailed()
        playCurrent()
    }

    /** ExoPlayer could not even play the system sound: let the platform ring. */
    private fun lastResortRingtone() {
        RingtoneManager.getRingtone(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)?.apply {
            isLooping = true
            play()
        }
    }

    private inner class PlayerEvents : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val current = session?.current as? WakeSource.Music ?: return
            if (!isPlaying) return
            watchdog?.cancel()
            session?.onStarted()
            music?.started(current)
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED && session?.current is WakeSource.Music) {
                session?.onEnded()
                playCurrent()
            }
        }

        override fun onPlayerError(error: PlaybackException) = onFailure()
    }

    private fun finish(action: (String) -> Unit) {
        alarmId?.let(action)
        app.alarmsChanged()
        RingState.hide()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        player.release()
        if (wakeLock.isHeld) wakeLock.release()
        RingState.hide()
        super.onDestroy()
    }

    private fun notification(label: String?, canSnooze: Boolean): Notification {
        val fullScreen = PendingIntent.getActivity(this, 0, Intent(this, RingActivity::class.java), FLAGS)
        return NotificationCompat.Builder(this, ReveilApp.CHANNEL_RING)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(label?.ifBlank { null } ?: "Réveil")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .apply { if (canSnooze) addAction(0, "Snooze", action(ACTION_SNOOZE)) }
            .addAction(0, "Arrêter", action(ACTION_DISMISS))
            .build()
    }

    private fun action(name: String) =
        PendingIntent.getService(this, name.hashCode(), Intent(this, RingService::class.java).setAction(name), FLAGS)

    companion object {
        const val ACTION_SNOOZE = "fr.clrk.reveil.SNOOZE"
        const val ACTION_DISMISS = "fr.clrk.reveil.DISMISS"
        private const val NOTIFICATION = 1
        private const val START_TIMEOUT_MS = 10_000L
        private const val MAX_RING_MS = 30 * 60 * 1000L
        private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        fun start(context: Context, alarmId: String) = ContextCompat.startForegroundService(
            context,
            Intent(context, RingService::class.java).putExtra(AlarmReceiver.EXTRA_ID, alarmId),
        )

        fun send(context: Context, action: String) =
            context.startService(Intent(context, RingService::class.java).setAction(action))
    }
}
