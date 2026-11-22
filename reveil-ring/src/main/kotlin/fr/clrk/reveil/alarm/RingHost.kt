package fr.clrk.reveil.alarm

import android.app.Activity
import android.content.Context
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.AlarmService
import fr.clrk.reveil.core.PlaylistSource
import fr.clrk.reveil.core.StreamResolver
import fr.clrk.reveil.core.WakeSource
import okhttp3.OkHttpClient

/**
 * What the host app gives the ringing machinery (service, receivers, evening download, track cache).
 * The host's Application implements it.
 */
interface RingHost {
    val alarms: AlarmService

    /** Lists the tracks of a playlist. */
    val playlists: PlaylistSource

    /** Turns a track into a playable audio stream. */
    val streams: StreamResolver

    /** Downloads the tracks kept on the device: carries the host's certificates and the headers the source wants. */
    val downloads: OkHttpClient

    /** Shown full screen over the lock screen while ringing. */
    val ringScreen: Class<out Activity>

    /** Opened from the status-bar alarm clock and from the reminder. */
    val homeScreen: Class<out Activity>

    /** Small icon of the ringing and reminder notifications. */
    val notificationIcon: Int

    /** Follows the ring, for the host's ringing screen. */
    val display: RingDisplay

    /** Called after any alarm change made by the ringing machinery (ring, snooze, skip, reboot). */
    fun alarmsChanged()

    /** Called when the evening download starts, before the tracks are fetched. */
    fun beforePrefetch() = Unit
}

/** What a ring goes through, in order, so the host can draw it. */
interface RingDisplay {
    fun show(alarm: Alarm?)

    fun playing(source: WakeSource)

    /** The track is over (or failed). */
    fun ended()

    /** The next track really plays. */
    fun started()

    fun noSnoozeLeft()

    fun hide()
}

/** Notification channels the ringing machinery posts on; the host creates them. */
object RingChannels {
    const val RING = "ring"
    const val REMINDER = "reminder"
}

val Context.ringHost: RingHost get() = applicationContext as RingHost
