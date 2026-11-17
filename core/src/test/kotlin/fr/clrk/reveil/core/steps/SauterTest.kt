package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.TriggerKey
import fr.clrk.reveil.core.WakeInfo
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.TestFactory

class SauterTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/sauter.feature", ::AlarmWorld) {
        alarmSteps()
        step("je saute le prochain réveil de l'alarme \"([^\"]*)\"") { (label) -> service.skipNext(id(label)) }
        step("j'annule le saut de l'alarme \"([^\"]*)\"") { (label) -> service.unskip(id(label)) }
        step("le rappel de l'alarme \"([^\"]*)\" est programmé pour le \"([^\"]+)\"") { (label, moment) ->
            assertEquals(moment, Formats.show(planned(label, TriggerKey.Kind.REMINDER)?.atZone(Formats.zone)))
        }
        step("l'alarme \"([^\"]*)\" n'a pas de rappel") { (label) -> assertNull(planned(label, TriggerKey.Kind.REMINDER)) }
        step("l'alarme \"([^\"]*)\" indique \"([^\"]+)\"") { (label, text) ->
            assertEquals(text, WakeInfo.skipped(alarm(label), Formats.zone))
        }
        step("l'alarme \"([^\"]*)\" n'indique aucun saut") { (label) -> assertNull(WakeInfo.skipped(alarm(label), Formats.zone)) }
    }
}
