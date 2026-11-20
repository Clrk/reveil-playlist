package fr.clrk.reveil.core

/** How a download run ends: WorkManager retries the evening one, the hand-started one shows its failure. */
enum class PrefetchResult { DONE, RETRY_LATER, FAILED }

/** [upToDate]: every planned track arrived, so the playlist can be remembered as fully downloaded. */
data class PrefetchReport(val result: PrefetchResult, val upToDate: Boolean)

object PrefetchOutcome {
    /** [planned] is null when the playlist could not be reached; [stored] tracks arrived out of it. */
    fun of(planned: Int?, stored: Int, byHand: Boolean): PrefetchReport {
        val nothing = planned == null || (planned > 0 && stored == 0)
        val failed = if (byHand) PrefetchResult.FAILED else PrefetchResult.RETRY_LATER
        return PrefetchReport(if (nothing) failed else PrefetchResult.DONE, stored == planned)
    }
}
