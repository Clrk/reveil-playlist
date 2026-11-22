package fr.clrk.reveil.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

/** Below Android 9, Ringtone cannot loop: the platform's MediaPlayer rings the alarm sound in a loop instead. */
internal object LoopingAlarm {
    /** Returns how to stop it, or null when the sound could not be played. */
    fun play(context: Context, uri: Uri): (() -> Unit)? = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            setDataSource(context, uri)
            isLooping = true
            prepare()
            start()
        }
    }.getOrNull()?.let { player -> player::release }
}
