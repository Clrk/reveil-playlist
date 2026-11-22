package fr.clrk.reveil.display

/**
 * Changing records between two tracks. The track ended at [endedMs]: the arm lifts in place and the record
 * slows to a stop, then the new cover is put on. The arm lands once the next track really plays ([startedMs]),
 * but never before the record has had time to change; then the record speeds up again.
 */
data class RecordChange(val endedMs: Long, val startedMs: Long? = null) {
    private val landAt: Long? = startedMs?.let { maxOf(it, endedMs + MIN_PAUSE_MS) }

    /** The next track has not played yet: the arm waits, lifted. */
    val pending get() = startedMs == null

    /** The next track really plays at [ms]: the arm may land, once per change. */
    fun started(ms: Long) = if (pending) copy(startedMs = ms) else this

    fun arm(ms: Long): ArmPose {
        val up = smoothStep(0f, LIFT_MS, (ms - endedMs).toFloat())
        val down = landAt?.let { smoothStep(0f, LAND_MS.toFloat(), (ms - it).toFloat()) } ?: 0f
        return ArmPose(lift = up * (1 - down), swing = 0f)
    }

    /** Share of full speed: slowing to a stop, then speeding up once the arm has landed. */
    fun speed(ms: Long): Float {
        val spinFrom = landAt?.plus(LAND_MS)
        return if (spinFrom != null && ms >= spinFrom) {
            Turntable.speedUp(ms - spinFrom)
        } else {
            (1f - (ms - endedMs) / STOP_MS).coerceIn(0f, 1f)
        }
    }

    /** How far the new cover has been set down on the stopped record: 0 old cover, 1 new cover. */
    fun newCover(ms: Long): Float = smoothStep(0f, SWAP_MS, ms - endedMs - STOP_MS)

    companion object {
        private const val LIFT_MS = 400f
        private const val LAND_MS = 500L
        private const val STOP_MS = 1200f
        private const val MIN_PAUSE_MS = 1800L
        private const val SWAP_MS = 500f

        /** A track ended (or failed) at [ms]: a new change starts, unless the [current] one still waits for its track. */
        fun ended(current: RecordChange?, ms: Long) = current?.takeIf { it.pending } ?: RecordChange(ms)
    }
}
