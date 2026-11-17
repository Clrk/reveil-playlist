package fr.clrk.reveil.core

import java.time.Instant

/** Identifies one system alarm slot: the main ring, the pending snooze or the reminder shown before ringing. */
data class TriggerKey(val alarmId: String, val kind: Kind) {
    enum class Kind { RING, SNOOZE, REMINDER }
}

/** Port to the phone's exact alarm service (AlarmManager on Android). */
interface AlarmScheduler {
    fun schedule(key: TriggerKey, at: Instant)

    fun cancel(key: TriggerKey)
}

/** Port to a small persistent text slot (a file on Android). */
interface TextStore {
    fun read(): String?

    fun write(text: String)
}
