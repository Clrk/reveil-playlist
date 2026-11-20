package fr.clrk.reveil.ui

import fr.clrk.reveil.core.Alarm

/** What the alarm editor can ask for. */
interface EditorActions {
    fun save(alarm: Alarm)
    fun delete(alarm: Alarm)
    fun test(alarm: Alarm)
    fun prefetch(playlist: String)
    fun skip(alarm: Alarm)
    fun close()
}

/** Saving, deleting or closing lets the editor ease out first. */
internal class Leaving(private val inner: EditorActions, private val leave: Leave, private val flight: Flight, private val home: Alarm?) :
    EditorActions by inner {
    override fun save(alarm: Alarm) {
        flight.takeOff(EDITOR, alarm.id, alarm.playlistCover)
        leave.then { inner.save(alarm) }
    }
    override fun delete(alarm: Alarm) = leave.then { inner.delete(alarm) }
    override fun close() {
        home?.let { flight.takeOff(EDITOR, it.id, it.playlistCover) }
        leave.then(inner::close)
    }
}
