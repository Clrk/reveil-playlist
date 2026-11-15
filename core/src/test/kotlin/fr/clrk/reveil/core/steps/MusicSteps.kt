package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.PlaylistLink
import fr.clrk.reveil.core.PrefetchPolicy
import fr.clrk.reveil.core.WakeSource
import fr.clrk.reveil.core.bdd.FeatureRunner
import fr.clrk.reveil.core.bdd.StepRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory

private const val VALUE = "\"([^\"]*)\""

private fun MusicWorld.currentId() = (session.current as? WakeSource.Music)?.track?.id

private fun StepRegistry<MusicWorld>.contextSteps() {
    step("les morceaux en cache $VALUE") { (ids) -> cached = MusicWorld.tracks(ids) }
    step("la playlist en ligne contient $VALUE") { (ids) -> online.tracks = MusicWorld.tracks(ids) }
    step("le téléphone est hors-ligne") { online.reachable = false }
    step("l'alarme n'a pas de playlist") { playlist = null }
    step("nous sommes le $VALUE") { (moment) -> now = Formats.moment(moment) }
    step("le prochain réveil est le $VALUE") { (moment) -> nextWake = Formats.moment(moment) }
    step("le cache garde \"(\\d+)\" morceaux") { (count) -> capacity = count.toInt() }
    step("le morceau $VALUE a déjà été joué au réveil") { (id) -> played = played + id }
}

private fun StepRegistry<MusicWorld>.playbackSteps() {
    step("le réveil sonne") { ring() }
    step("le morceau en cours échoue") { session.onFailed() }
    step("le morceau en cours échoue \"(\\d+)\" fois") { (count) -> repeat(count.toInt()) { session.onFailed() } }
    step("le morceau en cours démarre") { session.onStarted() }
    step("le morceau en cours se termine") { session.onEnded() }
    step("le réveil joue un des morceaux $VALUE") { (ids) -> assertTrue(currentId() in Formats.list(ids)) }
    step("le réveil joue la sonnerie de secours") { assertEquals(WakeSource.Ringtone, session.current) }
    step("le réveil joue l'autre morceau parmi $VALUE") { (ids) ->
        val first = (session.queue.first() as WakeSource.Music).track.id
        assertEquals(Formats.list(ids).minus(first), listOfNotNull(currentId()))
    }
    step("la file contient $VALUE puis la sonnerie de secours") { (ids) ->
        val music = session.queue.dropLast(1).map { (it as WakeSource.Music).track.id }
        assertEquals(Formats.list(ids).sorted(), music.sorted())
        assertEquals(WakeSource.Ringtone, session.queue.last())
    }
    step("sur \"(\\d+)\" réveils successifs le premier morceau change à chaque fois") { (count) ->
        val firsts = generateSequence(ring().current) { previous ->
            ring((previous as WakeSource.Music).track.id).current
        }.take(count.toInt()).toList()
        firsts.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
    }
}

private fun StepRegistry<MusicWorld>.prefetchSteps() {
    step("le pré-téléchargement est prévu le $VALUE") { (moment) ->
        assertEquals(moment, Formats.show(PrefetchPolicy.at(nextWake, now)))
    }
    step("le pré-téléchargement choisit les morceaux") {
        prefetch = PrefetchPolicy.plan(playlistTracks(), cached, played, capacity, random)
    }
    step("il télécharge \"(\\d+)\" morceaux parmi $VALUE") { (count, ids) ->
        assertEquals(count.toInt(), prefetch.download.size)
        assertTrue(Formats.list(ids).containsAll(prefetch.download.map { it.id }))
    }
    step("il supprime $VALUE") { (ids) -> assertEquals(Formats.list(ids), prefetch.evict.map { it.id }) }
    step("il ne supprime aucun morceau") { assertEquals(emptyList<Any>(), prefetch.evict) }
}

private fun MusicWorld.playlistTracks() = runCatching { online.tracks(playlist!!) }.getOrDefault(emptyList())

private fun StepRegistry<MusicWorld>.linkSteps() {
    step("le lien $VALUE désigne la playlist $VALUE") { (link, id) ->
        assertEquals(id, PlaylistLink.parse(link)?.value)
    }
    step("le lien $VALUE n'est pas une playlist") { (link) -> assertNull(PlaylistLink.parse(link)) }
    step("l'adresse de la playlist $VALUE est $VALUE") { (id, url) ->
        assertEquals(url, PlaylistLink.parse(id)?.url)
    }
}

private fun StepRegistry<MusicWorld>.musicSteps() {
    contextSteps()
    playbackSteps()
    prefetchSteps()
    linkSteps()
}

class PlaylistTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/playlist.feature", ::MusicWorld) { musicSteps() }
}

class LectureTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/lecture.feature", ::MusicWorld) { musicSteps() }
}

class PrefetchTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/prefetch.feature", ::MusicWorld) { musicSteps() }
}
