package fr.clrk.reveil.ui

import android.content.Context
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.AlarmService
import fr.clrk.reveil.core.unused
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.WakeInfo
import fr.clrk.reveil.music.TrackCache

/** What the screens say about each alarm's music: ready offline, or being prepared right now from the [fetching] playlist. */
internal class MusicNotes(private val context: Context) {
    fun cached(playlist: String?): Int = playlist?.let { TrackCache(context, PlaylistId(it)).tracks().size } ?: 0

    private val leftBehind = mutableSetOf<String>()

    /** The playlist of a deleted alarm, when no other alarm uses it: its songs go once undo is no longer offered. */
    fun leftBehind(playlist: String?) { playlist?.let(leftBehind::add) }

    /** Removes the songs of the playlists left behind, unless an alarm uses them again by now. */
    fun freeUnused(alarms: AlarmService, scope: CoroutineScope) {
        val unused = alarms.unused(leftBehind)
        leftBehind.clear()
        scope.launch(Dispatchers.IO) { unused.forEach { TrackCache.clear(context, PlaylistId(it)) } }
    }

    /** Progress of a hand-started download, otherwise how many tracks are ready offline. */
    fun status(playlist: String, download: WakeInfo.Download?): String =
        download?.let(WakeInfo::download) ?: WakeInfo.offline(cached(playlist))

    fun offline(alarms: List<Alarm>, fetching: String?, download: WakeInfo.Download?): Map<String, String> = alarms.mapNotNull { alarm ->
        val running = download?.takeIf { on(alarm, fetching) }?.let(WakeInfo::download)
        (running ?: WakeInfo.offline(alarm, cached(alarm.playlistId)))?.let { alarm.id to it }
    }.toMap()

    /** How far the music of each alarm on the [fetching] playlist is prepared, while a download runs. */
    fun preparing(alarms: List<Alarm>, fetching: String?, download: WakeInfo.Download?): Map<String, Float> {
        val done = fraction(download) ?: return emptyMap()
        return alarms.filter { on(it, fetching) }.associate { it.id to done }
    }

    private fun on(alarm: Alarm, fetching: String?) = fetching != null && alarm.playlistId == fetching

    private fun fraction(download: WakeInfo.Download?): Float? = when (download) {
        is WakeInfo.Download.Running -> download.done.toFloat() / download.total.coerceAtLeast(1)
        WakeInfo.Download.Waiting -> 0f
        else -> null
    }
}
