package fr.clrk.reveil.core

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Texts that tell the user when an alarm will ring and whether its music is ready offline. */
object WakeInfo {
    data class NextCard(val title: String, val detail: String)

    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    private const val SECONDS_PER_MINUTE = 60.0
    private val day = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.FRENCH)

    /** What to say right after saving or switching on an alarm; nothing when it will not ring. */
    fun announce(alarm: Alarm, now: ZonedDateTime): String? =
        NextTrigger.after(alarm, now)?.let { "Réveil dans ${countdown(now, it)}" }

    fun next(next: Pair<Alarm, ZonedDateTime>?, now: ZonedDateTime): NextCard {
        val (alarm, at) = next ?: return NextCard("Aucun réveil prévu", "Appuie sur + pour en créer un")
        val detail = listOf("dans ${countdown(now, at)}", alarm.label).filter { it.isNotBlank() }.joinToString(" · ")
        return NextCard(at.format(day), detail)
    }

    fun skipped(alarm: Alarm, zone: ZoneId): String? = alarm.skipped?.let { "Réveil de ${it.atZone(zone).format(day)} sauté" }

    fun offline(alarm: Alarm, cachedTracks: Int): String? = when {
        alarm.playlistId == null -> null
        cachedTracks == 0 -> "Pas encore téléchargée, ce sera fait la veille"
        cachedTracks == 1 -> "1 titre prêt hors ligne"
        else -> "$cachedTracks titres prêts hors ligne"
    }

    /** Rounded up to the minute, so an alarm never looks closer than it is. */
    private fun countdown(from: ZonedDateTime, to: ZonedDateTime): String {
        val minutes = Math.ceil(Duration.between(from, to).seconds / SECONDS_PER_MINUTE).toInt()
        return if (minutes >= MINUTES_PER_DAY) days(minutes) else hours(minutes)
    }

    private fun days(minutes: Int) = units(minutes / MINUTES_PER_DAY to "j", minutes % MINUTES_PER_DAY / MINUTES_PER_HOUR to "h")

    private fun hours(minutes: Int) = units(minutes / MINUTES_PER_HOUR to "h", minutes % MINUTES_PER_HOUR to "min")

    private fun units(vararg parts: Pair<Int, String>) =
        parts.filter { it.first > 0 }.joinToString(" ") { "${it.first} ${it.second}" }
}
