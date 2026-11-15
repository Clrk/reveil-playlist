package fr.clrk.reveil.core

import kotlin.random.Random

/** What the alarm plays: a track (from the local cache or streamed) or the system ringtone. */
sealed interface WakeSource {
    data class Music(val track: Track, val cached: Boolean) : WakeSource

    data object Ringtone : WakeSource
}

/** Builds the shuffled wake-up queue: cached tracks first, then streamed ones, ringtone last. */
class WakeQueueBuilder(private val source: PlaylistSource, private val random: Random) {
    fun build(playlist: PlaylistId?, cached: List<Track>, previousFirst: String?): List<WakeSource> {
        if (playlist == null) return listOf(WakeSource.Ringtone)
        val cachedIds = cached.map { it.id }.toSet()
        val local = cached.shuffled(random).map { WakeSource.Music(it, cached = true) }
        val remote = online(playlist).filterNot { it.id in cachedIds }.shuffled(random)
            .map { WakeSource.Music(it, cached = false) }
        return avoidRepeat(local + remote, previousFirst) + WakeSource.Ringtone
    }

    private fun online(playlist: PlaylistId): List<Track> =
        runCatching { source.tracks(playlist) }.getOrDefault(emptyList())

    private fun avoidRepeat(music: List<WakeSource.Music>, previousFirst: String?): List<WakeSource.Music> {
        val local = music.takeWhile { it.cached }.ifEmpty { music }
        if (local.size < 2 || music.first().track.id != previousFirst) return music
        return listOf(local.last()) + music.filterNot { it === local.last() }
    }
}

/**
 * Follows the playback of a wake-up queue. Music loops forever; after [maxFailures]
 * consecutive failures (error or start timeout) the session falls back to the ringtone.
 */
class WakeSession(val queue: List<WakeSource>, private val maxFailures: Int = DEFAULT_MAX_FAILURES) {
    private val music = queue.filterIsInstance<WakeSource.Music>()
    private var index = 0
    private var failures = 0
    private var fallback = music.isEmpty()

    val current: WakeSource get() = if (fallback) WakeSource.Ringtone else music[index]

    fun onStarted() { failures = 0 }

    fun onEnded() = advance()

    fun onFailed() {
        failures++
        if (failures >= maxFailures) fallback = true else advance()
    }

    private fun advance() {
        if (!fallback) index = (index + 1) % music.size
    }

    companion object {
        const val DEFAULT_MAX_FAILURES = 3
    }
}
