package fr.clrk.reveil.core

import java.time.Duration
import java.time.Instant

/** When the app may install its own update unattended: installing restarts it, so never near a wake-up. */
object SilentUpdate {
    private val MARGIN: Duration = Duration.ofHours(1)

    /** [upcoming]: the next alarm and the end of any snooze; [ringing]: an alarm rings right now. */
    fun allowed(now: Instant, upcoming: List<Instant>, ringing: Boolean): Boolean =
        !ringing && upcoming.none { it.isAfter(now) && it.isBefore(now + MARGIN) }
}
