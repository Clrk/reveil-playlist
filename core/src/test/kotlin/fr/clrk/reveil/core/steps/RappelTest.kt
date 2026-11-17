package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.WakeInfo
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.TestFactory

class RappelTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/rappel.feature", ::AlarmWorld) {
        alarmSteps()
        step("on m'annonce \"([^\"]+)\" pour l'alarme \"([^\"]*)\"") { (text, label) ->
            assertEquals(text, WakeInfo.announce(alarm(label), now))
        }
        step("on ne m'annonce rien pour l'alarme \"([^\"]*)\"") { (label) -> assertNull(WakeInfo.announce(alarm(label), now)) }
        step("la carte du prochain réveil affiche \"([^\"]+)\" et \"([^\"]+)\"") { (title, detail) ->
            val card = WakeInfo.next(service.next(), now)
            assertEquals(title to detail, card.title to card.detail)
        }
        step("\"(\\d+)\" titres de sa playlist sont téléchargés") { (count) -> cachedTracks = count.toInt() }
        step("l'alarme \"([^\"]*)\" indique \"([^\"]+)\"") { (label, text) ->
            assertEquals(text, WakeInfo.offline(alarm(label), cachedTracks))
        }
        step("l'alarme \"([^\"]*)\" n'indique rien sur le hors ligne") { (label) ->
            assertNull(WakeInfo.offline(alarm(label), cachedTracks))
        }
    }
}
