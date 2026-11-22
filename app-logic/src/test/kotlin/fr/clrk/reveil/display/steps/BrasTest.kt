package fr.clrk.reveil.display.steps

import fr.clrk.reveil.display.ArmPose
import fr.clrk.reveil.display.Tonearm
import fr.clrk.reveil.display.Turntable
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import kotlin.math.abs

class BrasWorld {
    var pose = ArmPose(lift = 0f, swing = 0f)
    var elapsedMs = 0L
    var bobs: List<Float> = emptyList()
    val speed get() = Turntable.speed(elapsedMs)
}

class BrasTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/bras.feature", ::BrasWorld) {
        step("le curseur d'arrêt est à \"(\\d+)\" %") { (p) -> pose = Tonearm.pose(p.toFloat() / 100) }
        step("la sonnerie dure depuis \"(\\d+)\" ms") { (ms) -> elapsedMs = ms.toLong(); pose = Tonearm.start(elapsedMs) }
        step("le disque est à l'arrêt") { assertEquals(0f, speed) }
        step("le disque accélère") { assertTrue(speed > 0f && speed < 1f, "speed=$speed") }
        step("le disque tourne à \"(\\d+)\" tours par minute") { (rpm) -> assertEquals(rpm.toInt(), Turntable.rpm(elapsedMs).toInt()) }
        step("le bras est posé") { assertTrue(pose.lift < 0.05f, "lift=${pose.lift}") }
        step("le bras est soulevé") { assertTrue(pose.lift > 0.5f, "lift=${pose.lift}") }
        step("le bras est au-dessus du disque") { assertTrue(pose.swing < 0.1f, "swing=${pose.swing}") }
        step("le bras est entre le disque et son support") { assertTrue(pose.swing in 0.1f..0.9f, "swing=${pose.swing}") }
        step("le disque tourne à pleine vitesse") { bobs = (0 until 36).map { Tonearm.bob(it * 10f, 1f) } }
        step("le disque est arrêté") { bobs = (0 until 36).map { Tonearm.bob(it * 10f, 0f) } }
        step("le bras monte et descend d'au plus \"(\\d+)\" pixel à chaque tour") { (px) ->
            assertTrue(bobs.max() > 0f && bobs.min() < 0f && bobs.all { abs(it) <= px.toFloat() }, "bobs=$bobs")
        }
        step("le bras ne monte ni ne descend") { assertTrue(bobs.all { it == 0f }) }
        step("le bras est sur son support") { assertTrue(pose.swing > 0.9f, "swing=${pose.swing}") }
    }
}
