package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.RecordChange
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory

class ChangementWorld {
    var change = RecordChange(endedMs = 0L)
    var ms = 0L
    val lift get() = change.arm(ms).lift
    val speed get() = change.speed(ms)
}

class ChangementTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/changement.feature", ::ChangementWorld) {
        step("un morceau qui se termine à \"(\\d+)\" ms") { (e) -> change = RecordChange.ended(null, e.toLong()) }
        step("le morceau suivant démarre à \"(\\d+)\" ms") { (s) -> change = change.started(s.toLong()) }
        step("un autre morceau se termine à \"(\\d+)\" ms") { (e) -> change = RecordChange.ended(change, e.toLong()) }
        step("le changement a commencé à \"(\\d+)\" ms") { (e) -> assertEquals(e.toLong(), change.endedMs) }
        step("le bras attend le morceau suivant") { assertTrue(change.pending) }
        step("on regarde à \"(\\d+)\" ms") { (t) -> ms = t.toLong() }
        step("le bras est soulevé") { assertTrue(lift > 0.5f, "lift=$lift") }
        step("le bras est posé") { assertTrue(lift < 0.05f, "lift=$lift") }
        step("le bras est au-dessus du disque") { assertEquals(0f, change.arm(ms).swing) }
        step("le disque ralentit") { assertTrue(speed > 0f && speed < 1f, "speed=$speed") }
        step("le disque est à l'arrêt") { assertEquals(0f, speed) }
        step("le disque accélère") { assertTrue(speed > 0f && speed < 1f, "speed=$speed") }
        step("le disque tourne à \"(\\d+)\" tours par minute") { (rpm) -> assertEquals(rpm.toInt(), (speed * 100 / 3).toInt()) }
        step("la nouvelle pochette est sur le disque") { assertEquals(1f, change.newCover(ms)) }
        step("l'ancienne pochette est sur le disque") { assertEquals(0f, change.newCover(ms)) }
        step("la nouvelle pochette se pose en fondu sur l'ancienne") {
            assertTrue(change.newCover(ms) > 0f && change.newCover(ms) < 1f, "newCover=${change.newCover(ms)}")
        }
    }
}
