package fr.clrk.reveil.core

import java.time.Instant
import java.time.ZonedDateTime

/** Mirrors one alarm to the phone: its ring, its pending snooze and the reminder an hour before. */
class AlarmPlanner(private val scheduler: AlarmScheduler, private val clock: () -> ZonedDateTime) {
    fun plan(alarm: Alarm) {
        val ring = NextTrigger.after(alarm, clock())
        slot(alarm.id, TriggerKey.Kind.RING, ring?.toInstant())
        val reminder = ring?.minusHours(REMINDER_HOURS)?.takeIf { it.isAfter(clock()) }
        slot(alarm.id, TriggerKey.Kind.REMINDER, reminder?.toInstant())
        val snooze = alarm.snoozedUntil?.takeIf { it.isAfter(clock().toInstant()) }
        slot(alarm.id, TriggerKey.Kind.SNOOZE, snooze)
    }

    fun cancel(id: String) = TriggerKey.Kind.entries.forEach { scheduler.cancel(TriggerKey(id, it)) }

    private fun slot(id: String, kind: TriggerKey.Kind, at: Instant?) {
        val key = TriggerKey(id, kind)
        if (at == null) scheduler.cancel(key) else scheduler.schedule(key, at)
    }

    private companion object {
        const val REMINDER_HOURS = 1L
    }
}
