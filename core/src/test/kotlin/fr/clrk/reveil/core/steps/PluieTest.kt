package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.RainDrift
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import kotlin.math.abs
import kotlin.math.hypot

class PluieWorld {
    var seconds = 0f
    val near get() = RainDrift.offset(0, seconds)
    val far get() = RainDrift.offset(1, seconds)
}

class PluieTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/pluie.feature", ::PluieWorld) {
        step("il pleut depuis \"(\\d+)\" s") { (s) -> seconds = s.toFloat() }
        step("les gouttes n'ont pas bougé") { assertEquals(0f, hypot(near.x, near.y)) }
        step("les gouttes sont allées vers la gauche") { assertTrue(near.x < 0f, "x=${near.x}") }
        step("les gouttes sont allées vers le haut, moins que vers la gauche") {
            assertTrue(near.y < 0f && abs(near.y) < abs(near.x), "offset=$near")
        }
        step("les gouttes ont glissé de moins de \"(\\d+)\" pixels") { (px) ->
            assertTrue(hypot(near.x, near.y).let { it > 0.1f && it < px.toFloat() }, "offset=$near")
        }
        step("la seconde couche de gouttes est allée plus loin que la première") {
            assertTrue(hypot(far.x, far.y) > hypot(near.x, near.y), "near=$near far=$far")
        }
    }
}
