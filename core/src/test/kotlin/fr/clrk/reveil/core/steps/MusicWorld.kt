package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.PlaylistSource
import fr.clrk.reveil.core.PrefetchPlan
import fr.clrk.reveil.core.Track
import fr.clrk.reveil.core.WakeQueueBuilder
import fr.clrk.reveil.core.WakeSession
import java.io.IOException
import java.time.ZonedDateTime
import kotlin.random.Random

/** Testing API for wake-up music: a fake YouTube playlist, a fake cache, a seeded shuffle. */
class MusicWorld {
    val online = FakePlaylistSource()
    var cached = listOf<Track>()
    var played = setOf<String>()
    var playlist: PlaylistId? = PlaylistId("PLtest")
    var capacity = 0
    val random = Random(SEED)
    lateinit var now: ZonedDateTime
    lateinit var nextWake: ZonedDateTime
    lateinit var session: WakeSession
    lateinit var prefetch: PrefetchPlan

    fun ring(previousFirst: String? = null): WakeSession {
        session = WakeSession(WakeQueueBuilder(online, random).build(playlist, cached, previousFirst))
        return session
    }

    companion object {
        private const val SEED = 42
        fun tracks(ids: String) = Formats.list(ids).map { Track(it, "Titre $it") }
    }
}

class FakePlaylistSource : PlaylistSource {
    var tracks = listOf<Track>()
    var reachable = true

    override fun tracks(id: PlaylistId): List<Track> {
        if (!reachable) throw IOException("hors-ligne")
        return tracks
    }
}
