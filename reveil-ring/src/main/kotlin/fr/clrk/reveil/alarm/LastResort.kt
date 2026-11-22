package fr.clrk.reveil.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/** ExoPlayer could not even play the fallback sound: let the platform ring it, or the system alarm. */
internal object LastResort {
    /** Starts the sound in a loop and returns how to stop it, or null when nothing could ring. */
    fun ring(context: Context, own: Uri?): (() -> Unit)? {
        val uri = own ?: Settings.System.DEFAULT_ALARM_ALERT_URI
        if (own != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return LoopingAlarm.play(context, uri)
        return loopingRingtone(context, uri)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun loopingRingtone(context: Context, uri: Uri): (() -> Unit)? = RingtoneManager.getRingtone(context, uri)?.run {
        isLooping = true
        play()
        ::stop
    }
}
