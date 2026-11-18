package fr.clrk.reveil.core

import java.time.ZonedDateTime
import kotlin.random.Random

data class PrefetchPlan(val download: List<Track>, val evict: List<Track>)

/** Decides when to pre-download wake-up tracks and which ones to fetch or drop. */
object PrefetchPolicy {
    private const val HOURS_BEFORE_WAKE = 9L

    fun at(nextWake: ZonedDateTime, now: ZonedDateTime): ZonedDateTime =
        maxOf(nextWake.minusHours(HOURS_BEFORE_WAKE), now)

    /**
     * Keeps unplayed tracks, replaces played ones, and gives priority to tracks added to the playlist
     * since the last download ([known]): those may take the place of unplayed tracks.
     */
    fun plan(
        playlist: List<Track>,
        cached: List<Track>,
        played: Set<String>,
        capacity: Int,
        random: Random,
        known: Set<String> = emptySet(),
    ): PrefetchPlan {
        if (playlist.isEmpty()) return PrefetchPlan(emptyList(), emptyList())
        val playlistIds = playlist.map { it.id }.toSet()
        val (kept, removed) = cached.partition { it.id in playlistIds }
        val cachedIds = cached.map { it.id }.toSet()
        val (added, others) = playlist.filterNot { it.id in cachedIds || it.id in played }
            .partition { known.isNotEmpty() && it.id !in known }
        val (stale, unplayed) = kept.partition { it.id in played }
        val room = (capacity - kept.size).coerceAtLeast(0)
        val bumped = (minOf(added.size, capacity) - room - stale.size).coerceIn(0, unplayed.size)
        val queue = added.shuffled(random) + others.shuffled(random)
        val download = queue.take(room + stale.size + bumped)
        val freed = (download.size - room).coerceAtLeast(0)
        return PrefetchPlan(download, removed + (stale + unplayed.shuffled(random)).take(freed))
    }
}
