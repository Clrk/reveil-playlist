package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.MusicSource
import fr.clrk.reveil.core.OnDemand
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.Track
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.TestFactory
import java.io.IOException

/** Testing API: a fake source that finds and resolves tracks by id. */
class OnDemandWorld : MusicSource {
    val found = mutableMapOf<String, List<Track>>()
    val unreadable = mutableSetOf<String>()
    val tried = mutableListOf<String>()
    var offline = false
    var asked = false
    lateinit var result: OnDemand.Result

    override fun search(query: String): List<Track> {
        asked = true
        if (offline) throw IOException("hors ligne")
        return found[query].orEmpty()
    }

    override fun streamUrl(track: Track): String {
        tried += track.id
        if (track.id in unreadable) throw IOException("illisible")
        return "https://flux/${track.id}"
    }

    override fun tracks(id: PlaylistId) = emptyList<Track>()
}

class ALaDemandeTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/a-la-demande.feature", ::OnDemandWorld) {
        step("la recherche \"(.+)\" trouve \"(.+)\"") { (q, ids) -> found[q] = MusicWorld.tracks(ids) }
        step("la recherche \"(.+)\" ne trouve rien") { (q) -> found[q] = emptyList() }
        step("le morceau \"(\\w+)\" ne peut pas être lu") { (id) -> unreadable += id }
        step("la source ne répond pas") { offline = true }
        step("on demande \"(.*)\"") { (q) -> result = OnDemand(this).play(q) }
        step("le morceau \"(\\w+)\" est prêt à jouer") { (id) ->
            assertEquals(OnDemand.Result.Playable(Track(id, "Titre $id"), "https://flux/$id"), result)
        }
        step("aucun morceau n'est trouvé") { assertEquals(OnDemand.Result.NotFound, result) }
        step("la source est indisponible") { assertEquals(OnDemand.Result.Unavailable, result) }
        step("la source n'a pas été interrogée") { assertFalse(asked) }
        step("le morceau \"(\\w+)\" n'a pas été essayé") { (id) -> assertFalse(id in tried) }
    }
}
