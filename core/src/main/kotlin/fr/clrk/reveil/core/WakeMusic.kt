package fr.clrk.reveil.core

import kotlin.random.Random

/** What the alarm plays: a track (from the local cache or streamed) or the system ringtone. */
sealed interface WakeSource {
    data class Music(val track: Track, val cached: Boolean) : WakeSource

    data object Ringtone : WakeSource
}

/**
 * Builds the shuffled wake-up queue: cached tracks first, then streamed ones, ringtone last.
 * Cached tracks already heard ([played], kept because no new one could replace them) come after the others.
 */
class WakeQueueBuilder(private val source: PlaylistSource, private val random: Random) {
    fun build(
        playlist: PlaylistId?,
        cached: List<Track>,
        previousFirst: String?,
        played: Set<String> = emptySet(),
    ): List<WakeSource> {
        if (playlist == null) return listOf(WakeSource.Ringtone)
        val cachedIds = cached.map { it.id }.toSet()
        val (heard, fresh) = cached.partition { it.id in played }
        val local = (fresh.shuffled(random) + heard.shuffled(random)).map { WakeSource.Music(it, cached = true) }
        val remote = online(playlist).filterNot { it.id in cachedIds }.shuffled(random)
            .map { WakeSource.Music(it, cached = false) }
        return avoidRepeat(local + remote, previousFirst) + WakeSource.Ringtone
    }

    private fun online(playlist: PlaylistId): List<Track> =
        runCatching { source.tracks(playlist) }.getOrDefault(emptyList())

    private fun avoidRepeat(music: List<WakeSource.Music>, previousFirst: String?): List<WakeSource.Music> {
        val local = music.takeWhile { it.cached }.ifEmpty { music }
        if (local.size < 2 || music.first().track.id != previousFirst) return music
        return listOf(local[1]) + music.filterNot { it === local[1] }
    }
}

/**
 * Follows the playback of a wake-up queue. Music loops forever; after [maxFailures]
 * consecutive failures (error, missing link or a track silent for [START_TIMEOUT_MS]) the session
 * falls back to the ringtone, and if even the ringtone fails, to the phone's own alarm ([lastResort]).
 * Times are milliseconds since the alarm started ringing; ringing stops after [MAX_RING_MS].
 */
class WakeSession(val queue: List<WakeSource>, private val maxFailures: Int = DEFAULT_MAX_FAILURES) {
    private val music = queue.filterIsInstance<WakeSource.Music>()
    private var index = 0
    private var failures = 0
    private var fallback = music.isEmpty()

    val current: WakeSource get() = if (fallback) WakeSource.Ringtone else music[index]

    var lastResort = false
        private set
    private var waitingSince: Long? = null

    /** The current source was asked to play at [at]: a track that stays silent will be skipped. */
    fun asked(at: Long) {
        waitingSince = at.takeIf { current is WakeSource.Music }
    }

    /** Called every second while ringing: skips a silent track, stops a ring that lasted too long. */
    fun check(at: Long): RingCheck = when {
        at >= MAX_RING_MS -> RingCheck.STOP
        silentSince(at) -> RingCheck.NEXT.also { onFailed() }
        else -> RingCheck.KEEP
    }

    private fun silentSince(at: Long) = waitingSince?.let { at - it >= START_TIMEOUT_MS } == true

    fun onStarted() {
        failures = 0
        waitingSince = null
    }

    fun onEnded() = advance()

    fun onFailed() {
        waitingSince = null
        if (fallback) lastResort = true
        failures++
        if (failures >= maxFailures) fallback = true else advance()
    }

    private fun advance() {
        if (!fallback) index = (index + 1) % music.size
    }

    companion object {
        const val DEFAULT_MAX_FAILURES = 3
        const val START_TIMEOUT_MS = 10_000L
        const val MAX_RING_MS = 30 * 60 * 1000L
    }
}

enum class RingCheck { KEEP, NEXT, STOP }
