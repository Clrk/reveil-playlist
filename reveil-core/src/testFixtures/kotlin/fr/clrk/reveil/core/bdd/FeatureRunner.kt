package fr.clrk.reveil.core.bdd

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.fail

/** Binds step sentences (regex) to actions on a test world. */
class StepRegistry<W> {
    private val steps = mutableListOf<Pair<Regex, W.(List<String>) -> Unit>>()

    fun step(pattern: String, action: W.(List<String>) -> Unit) {
        steps += Regex("^$pattern$") to action
    }

    fun run(world: W, step: Step) {
        val matches = matching(step.text)
        when (matches.size) {
            0 -> fail("Étape non définie (ligne ${step.line}) : ${step.text}")
            1 -> matches.single().let { (match, action) -> world.action(match.groupValues.drop(1)) }
            else -> fail("Étape ambiguë (ligne ${step.line}) : ${step.text}")
        }
    }

    private fun matching(text: String) = steps.mapNotNull { (regex, action) -> regex.matchEntire(text)?.let { it to action } }
}

/** Turns a .feature resource into JUnit dynamic tests, one per scenario. */
object FeatureRunner {
    fun <W> run(resource: String, newWorld: () -> W, define: StepRegistry<W>.() -> Unit): DynamicContainer {
        val feature = FeatureParser.parse(load(resource))
        require(feature.scenarios.isNotEmpty()) { "Aucun scénario dans $resource" }
        val registry = StepRegistry<W>().apply(define)
        val tests = feature.scenarios.map { scenario ->
            DynamicTest.dynamicTest(scenario.name) {
                val world = newWorld()
                (feature.background + scenario.steps).forEach { registry.run(world, it) }
            }
        }
        return DynamicContainer.dynamicContainer(feature.name, tests)
    }

    private fun load(resource: String): String =
        requireNotNull(javaClass.classLoader.getResource(resource)) { "Introuvable : $resource" }.readText()
}
