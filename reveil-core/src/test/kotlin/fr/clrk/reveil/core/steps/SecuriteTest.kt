package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.PrefetchOutcome
import fr.clrk.reveil.core.PrefetchResult
import fr.clrk.reveil.core.RingCheck
import fr.clrk.reveil.core.WakeSource
import fr.clrk.reveil.core.bdd.FeatureRunner
import fr.clrk.reveil.core.bdd.StepRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory

private const val COUNT = "\"(\\d+)\""
private const val SECOND = 1000L

/** Testing API for the safety net: the clock ticks every second, as the ringing service does. */
class SecuriteWorld {
    val music = MusicWorld()
    var clock = 0L
    var stopped = false

    fun ring() {
        music.ring()
        music.session.asked(clock)
    }

    fun wait(seconds: Int) = repeat(seconds) {
        clock += SECOND
        when (music.session.check(clock)) {
            RingCheck.NEXT -> music.session.asked(clock)
            RingCheck.STOP -> stopped = true
            RingCheck.KEEP -> Unit
        }
    }

    fun until(seconds: String) = wait(((seconds.toLong() * SECOND - clock) / SECOND).toInt())

    fun failed() {
        music.session.onFailed()
        music.session.asked(clock)
    }

    fun current() = (music.session.current as? WakeSource.Music)?.track?.id
}

/** Testing API for the outcome of a download run. */
class TelechargementWorld {
    var byHand = false
    var planned: Int? = 0
    var stored = 0
    val outcome get() = PrefetchOutcome.of(planned, stored, byHand)
}

private fun StepRegistry<SecuriteWorld>.securiteSteps() {
    step("les morceaux en cache \"([^\"]*)\"") { (ids) -> music.cached = MusicWorld.tracks(ids) }
    step("la playlist en ligne contient \"([^\"]*)\"") { (ids) -> music.online.tracks = MusicWorld.tracks(ids) }
    step("le téléphone est hors-ligne") { music.online.reachable = false }
    step("le réveil sonne") { ring() }
    step("rien ne démarre pendant $COUNT secondes") { (seconds) -> wait(seconds.toInt()) }
    step("$COUNT secondes passent") { (seconds) -> wait(seconds.toInt()) }
    step("le morceau en cours démarre") { music.session.onStarted() }
    step("le morceau en cours échoue") { failed() }
    step("le morceau en cours échoue $COUNT fois") { (count) -> repeat(count.toInt()) { failed() } }
    step("le lien du morceau en cours est introuvable") { failed() }
    step("le réveil joue un des morceaux \"([^\"]*)\"") { (ids) -> assertTrue(current() in Formats.list(ids)) }
    step("le réveil joue l'autre morceau parmi \"([^\"]*)\"") { (ids) ->
        val first = (music.session.queue.first() as WakeSource.Music).track.id
        assertEquals(Formats.list(ids).minus(first), listOfNotNull(current()))
    }
    step("le réveil joue toujours le premier morceau") { assertEquals(music.session.queue.first(), music.session.current) }
    step("le réveil joue la sonnerie de secours") { assertEquals(WakeSource.Ringtone, music.session.current) }
    step("l'alarme du téléphone sonne en dernier recours") { assertTrue(music.session.lastResort) }
    step("l'alarme du téléphone n'est pas utilisée") { assertFalse(music.session.lastResort) }
    step("à $COUNT secondes le réveil sonne encore") { (at) -> until(at).also { assertFalse(stopped) } }
    step("à $COUNT secondes le réveil s'arrête") { (at) -> until(at).also { assertTrue(stopped) } }
}

private fun StepRegistry<TelechargementWorld>.telechargementSteps() {
    step("la préparation est lancée à la main") { byHand = true }
    step("$COUNT morceaux à télécharger") { (count) -> planned = count.toInt() }
    step("la playlist est injoignable") { planned = null }
    step("$COUNT téléchargements réussissent") { (count) -> stored = count.toInt() }
    step("la préparation est réussie") { assertEquals(PrefetchResult.DONE, outcome.result) }
    step("la préparation sera retentée plus tard") { assertEquals(PrefetchResult.RETRY_LATER, outcome.result) }
    step("la préparation est en échec") { assertEquals(PrefetchResult.FAILED, outcome.result) }
    step("la playlist est notée comme à jour") { assertTrue(outcome.upToDate) }
    step("la playlist n'est pas notée comme à jour, pour reprendre les morceaux manquants") { assertFalse(outcome.upToDate) }
}

class SecuriteTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/securite.feature", ::SecuriteWorld) { securiteSteps() }
}

class TelechargementTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/telechargement.feature", ::TelechargementWorld) { telechargementSteps() }
}
