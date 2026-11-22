package fr.clrk.reveil.core.bdd

/** Minimal Gherkin parser (French and English keywords) for our .feature files. */
data class Step(val keyword: String, val text: String, val line: Int)

data class Scenario(val name: String, val steps: List<Step>)

data class Feature(val name: String, val background: List<Step>, val scenarios: List<Scenario>)

object FeatureParser {
    private val featureKeywords = listOf("Fonctionnalité:", "Feature:")
    private val backgroundKeywords = listOf("Contexte:", "Background:")
    private val scenarioKeywords = listOf("Scénario:", "Scenario:")
    private val stepKeywords = listOf(
        "Étant donné que", "Étant donné", "Soit", "Quand", "Lorsque", "Alors", "Et", "Mais",
        "Given", "When", "Then", "And", "But",
    )

    fun parse(source: String): Feature {
        val builder = FeatureBuilder()
        val headers = listOf<Pair<List<String>, (String) -> Unit>>(
            featureKeywords to builder::feature,
            backgroundKeywords to { _ -> builder.background() },
            scenarioKeywords to builder::scenario,
        )
        source.lines().forEachIndexed { index, raw -> parseLine(builder, headers, raw.trim(), index + 1) }
        return builder.build()
    }

    private fun parseLine(builder: FeatureBuilder, headers: List<Pair<List<String>, (String) -> Unit>>, line: String, number: Int) {
        if (line.isEmpty() || line.startsWith("#")) return
        val header = headers.firstNotNullOfOrNull { (keywords, action) -> keywords.prefixOf(line)?.let { it to action } }
        if (header != null) return header.second(line.after(header.first))
        val keyword = requireNotNull(stepKeywords.firstOrNull { line.startsWith("$it ") }) { "Ligne $number non reconnue : $line" }
        builder.step(Step(keyword, line.after(keyword), number))
    }

    private fun List<String>.prefixOf(line: String) = firstOrNull { line.startsWith(it) }

    private fun String.after(prefix: String) = removePrefix(prefix).trim()
}

private class FeatureBuilder {
    private var name = ""
    private val background = mutableListOf<Step>()
    private val scenarios = mutableListOf<Pair<String, MutableList<Step>>>()
    private var current: MutableList<Step>? = null

    fun feature(value: String) { name = value }

    fun background() { current = background }

    fun scenario(value: String) {
        val steps = mutableListOf<Step>()
        scenarios += value to steps
        current = steps
    }

    fun step(step: Step) {
        val target = requireNotNull(current) { "Étape hors scénario ligne ${step.line}" }
        target += step
    }

    fun build() = Feature(name, background.toList(), scenarios.map { Scenario(it.first, it.second.toList()) })
}
