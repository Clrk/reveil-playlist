package fr.clrk.reveil.core

import java.time.ZonedDateTime

/** Computes when an alarm must ring next, in the zone of the given moment (DST-safe), passing over a skipped occurrence. */
object NextTrigger {
    private const val DAYS_TO_SCAN = 14L

    fun after(alarm: Alarm, now: ZonedDateTime): ZonedDateTime? {
        if (!alarm.enabled) return null
        return (0..DAYS_TO_SCAN)
            .asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter { !alarm.repeating || it.dayOfWeek in alarm.days }
            .map { ZonedDateTime.of(it, alarm.time, now.zone) }
            .first { it.isAfter(now) && it.toInstant() != alarm.skipped }
    }
}
