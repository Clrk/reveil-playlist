package fr.clrk.reveil.core

/**
 * Changing records between two tracks. The track ended at [endedMs]: the arm lifts in place and the record
 * slows to a stop, then the new cover is put on. The arm lands once the next track really plays ([startedMs]),
 * but never before the record has had time to change; then the record speeds up again.
 */
data class RecordChange(val endedMs: Long, val startedMs: Long? = null) {
    private val landAt: Long? = startedMs?.let { maxOf(it, endedMs + MIN_PAUSE_MS) }

    fun arm(ms: Long): ArmPose {
        val up = smoothStep(0f, LIFT_MS, (ms - endedMs).toFloat())
        val down = landAt?.let { smoothStep(0f, LAND_MS.toFloat(), (ms - it).toFloat()) } ?: 0f
        return ArmPose(lift = up * (1 - down), swing = 0f)
    }

    /** Share of full speed: slowing to a stop, then speeding up once the arm has landed. */
    fun speed(ms: Long): Float {
        val spinFrom = landAt?.plus(LAND_MS)
        return if (spinFrom != null && ms >= spinFrom) {
            ((ms - spinFrom) / SPEED_UP_MS).coerceIn(0f, 1f)
        } else {
            (1f - (ms - endedMs) / STOP_MS).coerceIn(0f, 1f)
        }
    }

    /** How far the new cover has been set down on the stopped record: 0 old cover, 1 new cover. */
    fun newCover(ms: Long): Float = smoothStep(0f, SWAP_MS, ms - endedMs - STOP_MS)

    private companion object {
        const val LIFT_MS = 400f
        const val LAND_MS = 500L
        const val STOP_MS = 1200f
        const val SPEED_UP_MS = 1200f
        const val MIN_PAUSE_MS = 1800L
        const val SWAP_MS = 500f
    }
}
