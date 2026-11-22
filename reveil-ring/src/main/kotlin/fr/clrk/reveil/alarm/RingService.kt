package fr.clrk.reveil.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.RingCheck
import fr.clrk.reveil.core.VolumeRamp
import fr.clrk.reveil.core.WakeSession
import fr.clrk.reveil.core.WakeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Foreground service that rings: shuffled music with automatic fallback to the system alarm sound. */
class RingService : Service() {
    private val scope: CoroutineScope = MainScope()
    private lateinit var player: ExoPlayer
    private lateinit var wakeLock: PowerManager.WakeLock
    private var alarmId: String? = null
    private var music: RingMusic? = null
    private var session: WakeSession? = null
    private var loading: Job? = null
    private var ringingSince = 0L

    private val host get() = ringHost
    private val display get() = host.display

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
            ACTION_DISMISS -> finish { host.alarms.dismiss(it) }
            else -> id?.let(::ring)
        }
        return START_NOT_STICKY
    }

    private fun ring(id: String) {
        alarmId = id
        val alarm = host.alarms.find(id)
        val canSnooze = alarm?.snoozesLeft != 0
        val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        ServiceCompat.startForeground(this, NOTIFICATION, ringNotification(alarm?.label, canSnooze), type)
        ringingSince = SystemClock.elapsedRealtime()
        wakeLock.acquire(WakeSession.MAX_RING_MS)
        display.show(alarm)
        scope.rampVolume(player, VolumeRamp(alarm?.rampSeconds ?: Alarm.DEFAULT_RAMP_SECONDS))
        scope.launch {
            val ringMusic = RingMusic(this@RingService, alarm?.playlistId)
            music = ringMusic
            session = ringMusic.session()
            playCurrent()
        }
        watch()
    }

    private fun watch() = scope.launch {
        while (isActive) {
            delay(TICK_MS)
            tick()
        }
    }

    /** Every second: the core skips a silent track and ends a ring that lasted too long. */
    private fun tick() = when (session?.check(elapsed)) {
        RingCheck.NEXT -> playNext()
        RingCheck.STOP -> finish { host.alarms.dismiss(it) }
        RingCheck.KEEP, null -> Unit
    }

    private val elapsed get() = SystemClock.elapsedRealtime() - ringingSince

    private fun playNext() {
        display.ended()
        playCurrent()
    }

    /** Snoozing is refused once the limit is reached: the alarm keeps ringing and the button goes away. */
    private fun snooze() {
        val id = alarmId ?: return
        if (host.alarms.snooze(id)) finish { } else display.noSnoozeLeft()
    }

    private fun playCurrent() {
        val session = session ?: return
        val current = session.current
        loading?.cancel()
        session.asked(elapsed)
        display.playing(current)
        if (current == WakeSource.Ringtone) return playRingtone()
        val track = (current as WakeSource.Music)
        loading = scope.launch {
            val uri = music?.linkOf(track) ?: return@launch onFailure()
            player.setMediaItem(MediaItem.fromUri(uri))
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.prepare()
            player.play()
        }
    }

    private fun playRingtone() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
        player.setMediaItem(MediaItem.fromUri(uri))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.prepare()
        player.play()
    }

    /** A track that fails or has no link is skipped; the core decides when to fall back. */
    private fun onFailure() {
        val session = session?.takeUnless { it.lastResort } ?: return
        session.onFailed()
        if (session.lastResort) lastResortRingtone() else playNext()
    }

    /** ExoPlayer could not even play the system sound: let the platform ring. */
    private fun lastResortRingtone() {
        val uri = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return LoopingAlarm.play(this, uri)
        RingtoneManager.getRingtone(this, uri)?.apply {
            isLooping = true
            play()
        }
    }

    private inner class PlayerEvents : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) display.started()
            val current = session?.current as? WakeSource.Music ?: return
            if (!isPlaying) return
            session?.onStarted()
            music?.started(current)
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED && session?.current is WakeSource.Music) {
                display.ended()
                session?.onEnded()
                playCurrent()
            }
        }

        override fun onPlayerError(error: PlaybackException) = onFailure()
    }

    private fun finish(action: (String) -> Unit) {
        alarmId?.let(action)
        host.alarmsChanged()
        display.hide()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        player.release()
        if (wakeLock.isHeld) wakeLock.release()
        display.hide()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SNOOZE = "fr.clrk.reveil.SNOOZE"
        const val ACTION_DISMISS = "fr.clrk.reveil.DISMISS"
        private const val NOTIFICATION = 1
        private const val TICK_MS = 1_000L
        internal const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        fun start(context: Context, alarmId: String) = ContextCompat.startForegroundService(
            context,
            Intent(context, RingService::class.java).putExtra(AlarmReceiver.EXTRA_ID, alarmId),
        )

        fun send(context: Context, action: String) =
            context.startService(Intent(context, RingService::class.java).setAction(action))
    }
}
