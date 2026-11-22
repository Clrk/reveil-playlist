package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.SourceEvent
import fr.clrk.reveil.core.SourceHealth
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory

/** Testing API: what YouTube answered, in order. */
class YouTubeWorld {
    var health = SourceHealth()

    fun answer(event: SourceEvent, times: String = "1") = repeat(times.toInt()) { health = health.after(event) }
}

class YouTubeTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/youtube.feature", ::YouTubeWorld) {
        step("YouTube répond normalement") { answer(SourceEvent.WORKED) }
        step("YouTube répond de façon illisible \"(\\d+)\" fois") { (times) -> answer(SourceEvent.UNREADABLE, times) }
        step("le réseau est coupé \"(\\d+)\" fois") { (times) -> answer(SourceEvent.OFFLINE, times) }
        step("aucune alerte YouTube n'est affichée") { assertFalse(health.broken) }
        step("l'alerte YouTube est affichée") { assertTrue(health.broken) }
    }
}
