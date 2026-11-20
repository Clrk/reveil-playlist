package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.MorningLight
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import java.time.Duration
import kotlin.math.roundToInt

class LumiereWorld {
    var rampSeconds = 60
    var elapsed: Duration = Duration.ZERO
    val light get() = MorningLight(rampSeconds).at(elapsed)
}

class LumiereTest {
    private fun percent(value: Float) = (value * 100).roundToInt()

    @TestFactory
    fun scenarios() = FeatureRunner.run("features/lumiere.feature", ::LumiereWorld) {
        step("une montée du volume sur \"(\\d+)\" secondes") { (seconds) -> rampSeconds = seconds.toInt() }
        step("la sonnerie commence") { elapsed = Duration.ZERO }
        step("la sonnerie dure depuis \"(\\d+)\" secondes") { (seconds) -> elapsed = Duration.ofSeconds(seconds.toLong()) }
        step("le premier rayon est à \"(\\d+)\" %") { (p) -> assertEquals(p.toInt(), percent(light.firstRay)) }
        step("la tache de soleil est à \"(\\d+)\" %") { (p) -> assertEquals(p.toInt(), percent(light.sunPatch)) }
        step("la lueur rose est à \"(\\d+)\" %") { (p) -> assertEquals(p.toInt(), percent(light.glow)) }
        step("la lumière dorée est à \"(\\d+)\" %") { (p) -> assertEquals(p.toInt(), percent(light.gold)) }
        step("la tache a fini d'entrer dans la pièce") { assertTrue(light.sweep >= 1f) }
        step("l'heure est affichée à \"(\\d+)\" % de luminosité") { (p) -> assertEquals(p.toInt(), percent(light.text)) }
    }
}
