package fr.clrk.reveil.core.steps

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Conversions between the natural-language values of the scenarios and Java time. */
object Formats {
    val zone: ZoneId = ZoneId.of("Europe/Paris")
    private val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val days = mapOf(
        "lun" to DayOfWeek.MONDAY, "mar" to DayOfWeek.TUESDAY, "mer" to DayOfWeek.WEDNESDAY,
        "jeu" to DayOfWeek.THURSDAY, "ven" to DayOfWeek.FRIDAY, "sam" to DayOfWeek.SATURDAY,
        "dim" to DayOfWeek.SUNDAY,
    )

    fun moment(text: String): ZonedDateTime = LocalDateTime.parse(text, dateTime).atZone(zone)

    fun show(moment: ZonedDateTime?): String = moment?.withZoneSameInstant(zone)?.format(dateTime) ?: "jamais"

    fun time(text: String): LocalTime = LocalTime.parse(text)

    fun days(text: String): Set<DayOfWeek> = text.split(",").map { days.getValue(it.trim()) }.toSet()

    fun list(text: String): List<String> = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
