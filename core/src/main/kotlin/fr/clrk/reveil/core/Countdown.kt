package fr.clrk.reveil.core

import java.time.Duration
import java.time.ZonedDateTime

/** Human delay before a wake-up: « 8 h 12 min », « 25 min », « 1 j 10 h ». */
internal object Countdown {
    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    private const val SECONDS_PER_MINUTE = 60.0

    /** Rounded up to the minute, so an alarm never looks closer than it is. */
    fun between(from: ZonedDateTime, to: ZonedDateTime): String {
        val minutes = Math.ceil(Duration.between(from, to).seconds / SECONDS_PER_MINUTE).toInt()
        return if (minutes >= MINUTES_PER_DAY) days(minutes) else hours(minutes)
    }

    private fun days(minutes: Int) = units(minutes / MINUTES_PER_DAY to "j", minutes % MINUTES_PER_DAY / MINUTES_PER_HOUR to "h")

    private fun hours(minutes: Int) = units(minutes / MINUTES_PER_HOUR to "h", minutes % MINUTES_PER_HOUR to "min")

    private fun units(vararg parts: Pair<Int, String>) =
        parts.filter { it.first > 0 }.joinToString(" ") { "${it.first} ${it.second}" }
}
