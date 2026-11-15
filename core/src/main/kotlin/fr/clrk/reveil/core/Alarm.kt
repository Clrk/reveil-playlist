package fr.clrk.reveil.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

/** An alarm as the user configures it. An empty [days] set means a one-shot alarm. */
data class Alarm(
    val id: String,
    val time: LocalTime,
    val days: Set<DayOfWeek> = emptySet(),
    val label: String = "",
    val enabled: Boolean = true,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val playlistId: String? = null,
    val snoozedUntil: Instant? = null,
) {
    val repeating: Boolean get() = days.isNotEmpty()

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10

        fun at(time: LocalTime, id: String = UUID.randomUUID().toString()) = Alarm(id = id, time = time)
    }
}
