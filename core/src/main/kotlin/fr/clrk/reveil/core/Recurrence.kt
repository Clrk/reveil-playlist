package fr.clrk.reveil.core

import java.time.DayOfWeek
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY

/** Shortcuts for the days an alarm rings; any other set of days is picked one by one. */
enum class Recurrence(val label: String, val days: Set<DayOfWeek>) {
    ONCE("Une seule fois", emptySet()),
    WEEKDAYS("En semaine", DayOfWeek.entries.toSet() - setOf(SATURDAY, SUNDAY)),
    WEEKEND("Le week-end", setOf(SATURDAY, SUNDAY)),
    DAILY("Tous les jours", DayOfWeek.entries.toSet()),
    ;

    companion object {
        private val LETTERS = listOf("L", "M", "M", "J", "V", "S", "D")

        /** The shortcut matching exactly these days, if any. */
        fun of(days: Set<DayOfWeek>): Recurrence? = entries.firstOrNull { it.days == days }

        fun toggle(days: Set<DayOfWeek>, day: DayOfWeek) = if (day in days) days - day else days + day

        fun letter(day: DayOfWeek) = LETTERS[day.ordinal]

        fun letters(days: Set<DayOfWeek>) = days.sorted().joinToString(" ") { letter(it) }

        fun summary(days: Set<DayOfWeek>): String = when (val shortcut = of(days)) {
            null -> letters(days)
            else -> shortcut.label
        }
    }
}
