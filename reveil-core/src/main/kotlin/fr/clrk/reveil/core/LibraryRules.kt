package fr.clrk.reveil.core

import java.time.Duration
import java.time.Instant

/** A track kept on the device: its size and when it arrived. */
data class StoredTrack(val id: String, val bytes: Long, val storedAt: Instant)

/**
 * How the host's local library of tracks behaves. The default is the phone's: [tracks] = 15, renewed each evening,
 * no age limit ([keepFor]) and no size limit ([maxBytes]).
 */
data class LibraryRules(
    val tracks: Int = PHONE_TRACKS,
    val keepFor: Duration? = null,
    val maxBytes: Long? = null,
) {
    /** Tracks kept longer than [keepFor]: renewed like a track already heard, only once a new one has arrived. */
    fun expired(stored: List<StoredTrack>, now: Instant): Set<String> {
        val limit = keepFor ?: return emptySet()
        return stored.filter { Duration.between(it.storedAt, now) > limit }.map { it.id }.toSet()
    }

    /** Oldest tracks to remove so the library fits in [maxBytes]. */
    fun overSize(stored: List<StoredTrack>): List<String> {
        val limit = maxBytes ?: return emptyList()
        val oldestFirst = stored.sortedBy { it.storedAt }
        val leftAfter = oldestFirst.runningFold(stored.sumOf { it.bytes }) { total, track -> total - track.bytes }
        return oldestFirst.filterIndexed { i, _ -> leftAfter[i] > limit }.map { it.id }
    }

    companion object {
        const val PHONE_TRACKS = 15
    }
}
