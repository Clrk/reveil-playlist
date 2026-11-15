package fr.clrk.reveil.core

import java.time.ZonedDateTime
import kotlin.random.Random

data class PrefetchPlan(val download: List<Track>, val evict: List<Track>)

/** Decides when to pre-download wake-up tracks and which ones to fetch or drop. */
object PrefetchPolicy {
    private const val HOURS_BEFORE_WAKE = 9L

    fun at(nextWake: ZonedDateTime, now: ZonedDateTime): ZonedDateTime =
        maxOf(nextWake.minusHours(HOURS_BEFORE_WAKE), now)

    fun plan(playlist: List<Track>, cached: List<Track>, played: Set<String>, capacity: Int, random: Random): PrefetchPlan {
        if (playlist.isEmpty()) return PrefetchPlan(emptyList(), emptyList())
        val playlistIds = playlist.map { it.id }.toSet()
        val (kept, removed) = cached.partition { it.id in playlistIds }
        val cachedIds = cached.map { it.id }.toSet()
        val fresh = playlist.filterNot { it.id in cachedIds || it.id in played }.shuffled(random)
        val stale = kept.filter { it.id in played }
        val room = (capacity - kept.size).coerceAtLeast(0)
        val download = fresh.take(minOf(fresh.size, room + stale.size))
        return PrefetchPlan(download, removed + stale.take((download.size - room).coerceAtLeast(0)))
    }
}
