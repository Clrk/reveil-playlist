package fr.clrk.reveil.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/**
 * Contract of the alarm exchange format, version 1. Another app (the Bonjour, its companion) relies on this exact text:
 * if one of these tests must change, the format changed, so [AlarmFormat.VERSION] goes up.
 */
class AlarmFormatContractTest {
    private val alarm = Alarm(
        id = "b7d2",
        time = LocalTime.of(6, 45),
        days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
        label = "Travail",
        enabled = true,
        snoozeMinutes = 9,
        playlistId = "PLmatin",
        playlistTitle = "Matin doux",
        playlistCover = "https://i.ytimg.com/vi/x/hqdefault.jpg",
        snoozedUntil = Instant.ofEpochMilli(1_700_000_000_000),
        maxSnoozes = null,
        snoozeCount = 1,
        rampSeconds = 90,
        skipped = Instant.ofEpochMilli(1_700_100_000_000),
    )
    private val minimal = Alarm.at(LocalTime.of(7, 30), "c1")

    private fun contract(name: String) = requireNotNull(javaClass.classLoader.getResource("contracts/$name")).readText().trim()

    @Test
    fun `version 1 is written exactly like this`() {
        assertEquals(contract("alarmes-format-1.json"), AlarmFormat.encode(listOf(alarm, minimal)))
    }

    @Test
    fun `version 1 is read exactly like this`() {
        assertEquals(AlarmFormat.Decoded.Alarms(listOf(alarm, minimal)), AlarmFormat.decode(contract("alarmes-format-1.json")))
    }

    @Test
    fun `the oldest version 1 message, with only the required fields, still reads with the defaults`() {
        assertEquals(AlarmFormat.Decoded.Alarms(listOf(minimal)), AlarmFormat.decode(contract("alarmes-format-1-minimal.json")))
    }
}
