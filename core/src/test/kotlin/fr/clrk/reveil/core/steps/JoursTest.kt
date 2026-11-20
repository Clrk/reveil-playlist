package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.Recurrence
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.TestFactory
import java.time.DayOfWeek

/** Testing API: the days picked on the creation screen. */
class JoursWorld {
    var days = emptySet<DayOfWeek>()
}

class JoursTest {
    private val names = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")

    @TestFactory
    fun scenarios() = FeatureRunner.run("features/jours.feature", ::JoursWorld) {
        step("je touche le raccourci \"([^\"]+)\"") { (label) -> days = Recurrence.entries.single { it.label == label }.days }
        step("je touche le jour \"([^\"]+)\"") { (name) -> days = Recurrence.toggle(days, DayOfWeek.of(names.indexOf(name) + 1)) }
        step("les jours cochés sont \"([^\"]+)\"") { (letters) -> assertEquals(letters, Recurrence.letters(days)) }
        step("aucun jour n'est coché") { assertEquals(emptySet<DayOfWeek>(), days) }
        step("le raccourci \"([^\"]+)\" est en surbrillance") { (label) -> assertEquals(label, Recurrence.of(days)?.label) }
        step("aucun raccourci n'est en surbrillance") { assertNull(Recurrence.of(days)) }
        step("le résumé est \"([^\"]+)\"") { (text) -> assertEquals(text, Recurrence.summary(days)) }
    }
}
