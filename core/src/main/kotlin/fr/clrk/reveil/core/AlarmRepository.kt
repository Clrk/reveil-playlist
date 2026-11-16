package fr.clrk.reveil.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/** Persists alarms as JSON in a [TextStore]; unreadable data yields an empty list. */
class AlarmRepository(private val store: TextStore) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun all(): List<Alarm> = decode(store.read()).map { it.toAlarm() }

    fun save(alarm: Alarm) = write(all().filterNot { it.id == alarm.id } + alarm)

    fun delete(id: String) = write(all().filterNot { it.id == id })

    private fun write(alarms: List<Alarm>) = store.write(json.encodeToString(alarms.map { AlarmRecord.of(it) }))

    private fun decode(text: String?): List<AlarmRecord> = try {
        text?.let { json.decodeFromString<List<AlarmRecord>>(it) }.orEmpty()
    } catch (_: SerializationException) {
        emptyList()
    }
}

@Serializable
private data class AlarmRecord(
    val id: String,
    val minuteOfDay: Int,
    val days: List<Int>,
    val label: String,
    val enabled: Boolean,
    val snoozeMinutes: Int,
    val playlistId: String?,
    val playlistTitle: String?,
    val snoozedUntil: Long?,
    val maxSnoozes: Int = Alarm.DEFAULT_MAX_SNOOZES,
    val snoozeCount: Int = 0,
    val rampSeconds: Int = Alarm.DEFAULT_RAMP_SECONDS,
) {
    fun toAlarm() = Alarm(
        id = id,
        time = LocalTime.MIN.plusMinutes(minuteOfDay.toLong()),
        days = days.map { DayOfWeek.of(it) }.toSet(),
        label = label,
        enabled = enabled,
        snoozeMinutes = snoozeMinutes,
        playlistId = playlistId,
        playlistTitle = playlistTitle,
        snoozedUntil = snoozedUntil?.let { Instant.ofEpochMilli(it) },
        maxSnoozes = maxSnoozes.takeIf { it != UNLIMITED },
        snoozeCount = snoozeCount,
        rampSeconds = rampSeconds,
    )

    companion object {
        private const val MINUTES_PER_HOUR = 60
        private const val UNLIMITED = -1

        fun of(alarm: Alarm) = AlarmRecord(
            id = alarm.id,
            minuteOfDay = alarm.time.hour * MINUTES_PER_HOUR + alarm.time.minute,
            days = alarm.days.map { it.value }.sorted(),
            label = alarm.label,
            enabled = alarm.enabled,
            snoozeMinutes = alarm.snoozeMinutes,
            playlistId = alarm.playlistId,
            playlistTitle = alarm.playlistTitle,
            snoozedUntil = alarm.snoozedUntil?.toEpochMilli(),
            maxSnoozes = alarm.maxSnoozes ?: UNLIMITED,
            snoozeCount = alarm.snoozeCount,
            rampSeconds = alarm.rampSeconds,
        )
    }
}
