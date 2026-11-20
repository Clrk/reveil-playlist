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
    private val planner = AlarmPlanner(scheduler, clock)

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

    /** Deletes the alarm; returns its playlist when no other alarm uses it, so its songs can be removed from the phone. */
    fun delete(id: String): String? {
        val playlists = repository.all().filter { it.id == id }.mapNotNull { it.playlistId }
        repository.delete(id)
        planner.cancel(id)
        return unused(playlists).firstOrNull()
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
        val newWake = kind == TriggerKey.Kind.RING
        val count = if (newWake) 0 else alarm.snoozeCount
        val skipped = alarm.skipped.takeUnless { newWake }
        update(alarm.copy(enabled = alarm.repeating, snoozedUntil = null, snoozeCount = count, skipped = skipped))
    }

    /** Postpones the ringing alarm; returns false once no snooze is left. */
    fun snooze(id: String): Boolean {
        val alarm = find(id)?.takeIf { it.snoozesLeft != 0 } ?: return false
        val until = clock().plusMinutes(alarm.snoozeMinutes.toLong()).toInstant()
        update(alarm.copy(snoozedUntil = until, snoozeCount = alarm.snoozeCount + 1))
        return true
    }

    /** Skips only the coming occurrence; a one-shot alarm has no other one, so it is switched off. */
    fun skipNext(id: String) {
        val alarm = find(id) ?: return
        val next = NextTrigger.after(alarm, clock()) ?: return
        update(if (alarm.repeating) alarm.copy(skipped = next.toInstant()) else alarm.copy(enabled = false))
    }

    fun unskip(id: String) {
        find(id)?.let { update(it.copy(skipped = null)) }
    }

    fun dismiss(id: String) {
        find(id)?.let { update(it.copy(snoozedUntil = null, snoozeCount = 0)) }
    }

    /** Restores every trigger, e.g. after the phone rebooted. */
    fun rescheduleAll() = repository.all().forEach(::plan)

    private fun plan(alarm: Alarm) = planner.plan(alarm)
}

/** Among [playlists], those no alarm uses: checked again just before their songs are removed. */
fun AlarmService.unused(playlists: Collection<String>): List<String> {
    val used = list().mapNotNull { it.playlistId }
    return playlists.filterNot { it in used }
}
