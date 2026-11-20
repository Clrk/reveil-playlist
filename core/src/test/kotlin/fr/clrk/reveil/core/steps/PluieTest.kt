package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.DropShadow
import fr.clrk.reveil.core.RainRunner
import fr.clrk.reveil.core.RainRunners
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

class PluieWorld {
    val drop: RainRunner = RainRunners.ALL.first()
    var before: DropShadow? = null
    var after: DropShadow? = null
    var trail: List<DropShadow> = emptyList()
    var speeds: List<Float> = emptyList()
    var moving: List<Int> = emptyList()

    fun assertRisesLessThanItGoesLeft() {
        val dx = after!!.x - before!!.x
        val dy = after!!.y - before!!.y
        assertTrue(dy < 0f && abs(dy) < abs(dx), "dx=$dx dy=$dy")
    }

    fun assertStretchedAlongItsRun() {
        val course = Math.toDegrees(atan2((after!!.y - before!!.y).toDouble(), (after!!.x - before!!.x).toDouble()))
        assertEquals((course + 360) % 360, RainRunner.RUN_DEGREES.toDouble(), 3.0)
    }

    fun follow() {
        speeds = (0 until 60).map { i ->
            val t = drop.wait + drop.run * i / 60
            val a = drop.head(t)!!
            val b = drop.head(t + 0.05f)!!
            hypot(b.x - a.x, b.y - a.y)
        }
    }

    fun watchTenMinutes() {
        moving = (0 until 2400).map { i -> RainRunners.ALL.count { it.isRunning(i * 0.25f) } }
    }

    fun look(from: Float, to: Float) {
        before = drop.head(from)
        after = drop.head(to)
        trail = drop.trail(to)
    }
}

class PluieTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/pluie.feature", ::PluieWorld) {
        step("on regarde une goutte à \"([\\d.]+)\" s puis à \"([\\d.]+)\" s") { (a, b) -> look(a.toFloat(), b.toFloat()) }
        step("elle n'a pas bougé") { assertEquals(before, after) }
        step("elle est allée vers la gauche") { assertTrue(after!!.x < before!!.x, "$before -> $after") }
        step("elle est allée vers le haut, moins que vers la gauche") { assertRisesLessThanItGoesLeft() }
        step("son ombre est orientée dans le sens de sa course") { assertStretchedAlongItsRun() }
        step("on suit une goutte pendant qu'elle glisse") { follow() }
        step("elle ralentit presque jusqu'à s'arrêter puis repart") {
            val slowest = speeds.indexOf(speeds.min())
            assertTrue(speeds.min() < speeds.max() * 0.1f, "speeds=$speeds")
            assertTrue(speeds.drop(slowest).max() > speeds.max() * 0.5f, "never starts again")
        }
        step("des gouttelettes sont restées entre son départ et elle") {
            assertTrue(trail.isNotEmpty() && trail.all { it.x in after!!.x..before!!.x && it.opacity > 0f }, "trail=$trail")
        }
        step("on regarde la traînée juste avant que la goutte recommence") { trail = drop.trail(drop.period - 0.01f) }
        step("la traînée a disparu") { assertTrue(trail.isEmpty(), "trail=$trail") }
        step("on regarde la vitre pendant dix minutes") { watchTenMinutes() }
        step("jamais plus de \"(\\d+)\" gouttes ne glissent à la fois") { (n) -> assertTrue(moving.max() <= n.toInt(), "moving=$moving") }
        step("en moyenne moins d'une goutte glisse à la fois") { assertTrue(moving.average() < 1.0, "avg=${moving.average()}") }
        step("au moins une goutte a glissé") { assertTrue(moving.any { it > 0 }) }
    }
}
