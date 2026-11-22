package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.LibraryRules
import fr.clrk.reveil.core.PrefetchPolicy
import fr.clrk.reveil.core.StoredTrack
import fr.clrk.reveil.core.Track
import fr.clrk.reveil.core.bdd.FeatureRunner
import fr.clrk.reveil.core.bdd.StepRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import java.time.Duration
import kotlin.random.Random

/** Testing API: the host's library rules and the tracks on the device. */
class LibraryWorld {
    var rules = LibraryRules()
    val stored = mutableListOf<StoredTrack>()
    var playlist = listOf<Track>()
    var downloaded = 0
    var now = Formats.moment("2026-11-16 06:00").toInstant()

    fun arrived(id: String, at: String, megabytes: Long = 1) {
        stored += StoredTrack(id, megabytes * MB, Formats.moment(at).toInstant())
    }

    val expired get() = rules.expired(stored, now)

    companion object {
        const val MB = 1_000_000L
    }
}

private const val DATE = "\"(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})\""

private fun StepRegistry<LibraryWorld>.ruleSteps() {
    step("les règles par défaut de la bibliothèque") { rules = LibraryRules() }
    step("une bibliothèque de \"(\\d+)\" morceaux") { (n) -> rules = LibraryRules(tracks = n.toInt()) }
    step("une bibliothèque qui garde les morceaux \"(\\d+)\" jours") { (d) ->
        rules = LibraryRules(keepFor = Duration.ofDays(d.toLong()))
    }
    step("une bibliothèque de \"(\\d+)\" Mo au plus") { (mb) -> rules = LibraryRules(maxBytes = mb.toLong() * LibraryWorld.MB) }
    step("nous sommes le $DATE") { (at) -> now = Formats.moment(at).toInstant() }
    step("la playlist en ligne contient \"(\\d+)\" morceaux") { (n) ->
        playlist = (1..n.toInt()).map { Track("t$it", "Titre $it") }
    }
    step("le pré-téléchargement choisit les morceaux") {
        downloaded = PrefetchPolicy.plan(playlist, emptyList(), emptySet(), rules.tracks, Random(1)).download.size
    }
    step("il télécharge \"(\\d+)\" morceaux") { (n) -> assertEquals(n.toInt(), downloaded) }
}

private fun StepRegistry<LibraryWorld>.trackSteps() {
    step("le morceau \"(\\w+)\" est arrivé le $DATE") { (id, at) -> arrived(id, at) }
    step("le morceau \"(\\w+)\" de \"(\\d+)\" Mo est arrivé le $DATE") { (id, mb, at) -> arrived(id, at, mb.toLong()) }
    step("la bibliothèque garde \"(\\d+)\" morceaux") { (n) -> assertEquals(n.toInt(), rules.tracks) }
    step("aucun morceau n'expire avec l'âge") { assertNull(rules.keepFor) }
    step("la taille de la bibliothèque n'est pas limitée") { assertNull(rules.maxBytes) }
    step("le morceau \"(\\w+)\" est à renouveler") { (id) -> assertTrue(id in expired, "expired=$expired") }
    step("le morceau \"(\\w+)\" est gardé") { (id) -> assertTrue(id !in expired, "expired=$expired") }
    step("la bibliothèque retire \"(.+)\"") { (ids) -> assertEquals(Formats.list(ids), rules.overSize(stored)) }
    step("la bibliothèque ne retire rien") { assertEquals(emptyList<String>(), rules.overSize(stored)) }
}

class BibliothequeTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/bibliotheque-locale.feature", ::LibraryWorld) {
        ruleSteps()
        trackSteps()
    }
}
