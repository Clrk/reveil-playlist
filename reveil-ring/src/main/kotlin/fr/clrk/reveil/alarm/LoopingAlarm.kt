package fr.clrk.reveil.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

/** Below Android 9, Ringtone cannot loop: the platform's MediaPlayer rings the alarm sound in a loop instead. */
internal object LoopingAlarm {
    fun play(context: Context, uri: Uri) {
        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }
    }
}
