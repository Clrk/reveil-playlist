package fr.clrk.reveil.core.bdd

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError

class FeatureParserTest {
    private val source = """
        # language: fr
        Fonctionnalité: Démo
          Contexte:
            Soit un contexte
          Scénario: Premier
            Quand je fais "x"
            Alors ça marche
    """.trimIndent()

    @Test
    fun `parse le contexte et les scénarios`() {
        val feature = FeatureParser.parse(source)
        assertEquals("Démo", feature.name)
        assertEquals(listOf("un contexte"), feature.background.map { it.text })
        assertEquals(listOf("je fais \"x\"", "ça marche"), feature.scenarios.single().steps.map { it.text })
    }

    @Test
    fun `refuse une ligne inconnue`() {
        assertThrows<IllegalArgumentException> { FeatureParser.parse("Fonctionnalité: x\nScénario: y\nBlabla") }
    }

    @Test
    fun `une étape non définie fait échouer le scénario`() {
        val registry = StepRegistry<Unit>()
        assertThrows<AssertionFailedError> { registry.run(Unit, Step("Soit", "rien", 1)) }
    }

    @Test
    fun `une étape ambiguë fait échouer le scénario`() {
        val registry = StepRegistry<Unit>().apply {
            step("a.*") {}
            step(".*b") {}
        }
        assertThrows<AssertionFailedError> { registry.run(Unit, Step("Soit", "ab", 1)) }
    }
}
