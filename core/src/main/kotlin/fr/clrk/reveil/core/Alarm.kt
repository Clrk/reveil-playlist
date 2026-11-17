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
    val playlistTitle: String? = null,
    val snoozedUntil: Instant? = null,
    val maxSnoozes: Int? = DEFAULT_MAX_SNOOZES,
    val snoozeCount: Int = 0,
    val rampSeconds: Int = DEFAULT_RAMP_SECONDS,
    val skipped: Instant? = null,
) {
    val repeating: Boolean get() = days.isNotEmpty()

    /** Snoozes still allowed during the current wake-up; null means unlimited. */
    val snoozesLeft: Int? get() = maxSnoozes?.let { (it - snoozeCount).coerceAtLeast(0) }

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
        const val DEFAULT_MAX_SNOOZES = 3
        const val DEFAULT_RAMP_SECONDS = 60

        fun at(time: LocalTime, id: String = UUID.randomUUID().toString()) = Alarm(id = id, time = time)
    }
}
