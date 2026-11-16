package fr.clrk.reveil.core

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

/** Alarm use cases: every change is persisted and mirrored to the phone scheduler. */
class AlarmService(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val clock: () -> ZonedDateTime,
) {
    @Suppress("LongParameterList")
    fun create(
        time: LocalTime,
        days: Set<DayOfWeek>,
        label: String,
        playlistId: String? = null,
        snoozeMinutes: Int = Alarm.DEFAULT_SNOOZE_MINUTES,
    ): Alarm {
        val alarm = Alarm.at(time).copy(days = days, label = label, playlistId = playlistId, snoozeMinutes = snoozeMinutes)
        return update(alarm)
    }

    fun update(alarm: Alarm): Alarm {
        repository.save(alarm)
        plan(alarm)
        return alarm
    }

    fun list(): List<Alarm> = repository.all().sortedBy { it.time }

    fun find(id: String): Alarm? = repository.all().firstOrNull { it.id == id }

    fun setEnabled(id: String, enabled: Boolean) {
        find(id)?.let { update(it.copy(enabled = enabled)) }
    }

    fun delete(id: String) {
        repository.delete(id)
        TriggerKey.Kind.entries.forEach { scheduler.cancel(TriggerKey(id, it)) }
    }

    fun next(): Pair<Alarm, ZonedDateTime>? = repository.all()
        .mapNotNull { alarm -> NextTrigger.after(alarm, clock())?.let { alarm to it } }
        .minByOrNull { it.second }

    /**
     * Called when a trigger fires: one-shot alarms switch off, repeating ones move on.
     * A new wake-up (ring) restores every snooze; a snooze ringing keeps the count.
     */
    fun onRing(id: String, kind: TriggerKey.Kind = TriggerKey.Kind.RING) {
        val alarm = find(id) ?: return
        val count = if (kind == TriggerKey.Kind.RING) 0 else alarm.snoozeCount
        update(alarm.copy(enabled = alarm.repeating, snoozedUntil = null, snoozeCount = count))
    }

    /** Postpones the ringing alarm; returns false once no snooze is left. */
    fun snooze(id: String): Boolean {
        val alarm = find(id)?.takeIf { it.snoozesLeft != 0 } ?: return false
        val until = clock().plusMinutes(alarm.snoozeMinutes.toLong()).toInstant()
        update(alarm.copy(snoozedUntil = until, snoozeCount = alarm.snoozeCount + 1))
        return true
    }

    fun dismiss(id: String) {
        find(id)?.let { update(it.copy(snoozedUntil = null, snoozeCount = 0)) }
    }

    /** Restores every trigger, e.g. after the phone rebooted. */
    fun rescheduleAll() = repository.all().forEach(::plan)

    private fun plan(alarm: Alarm) {
        planSlot(TriggerKey(alarm.id, TriggerKey.Kind.RING), NextTrigger.after(alarm, clock())?.toInstant())
        val snooze = alarm.snoozedUntil?.takeIf { it.isAfter(clock().toInstant()) }
        planSlot(TriggerKey(alarm.id, TriggerKey.Kind.SNOOZE), snooze)
    }

    private fun planSlot(key: TriggerKey, at: java.time.Instant?) {
        if (at == null) scheduler.cancel(key) else scheduler.schedule(key, at)
    }
}
