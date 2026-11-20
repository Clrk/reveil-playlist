package fr.clrk.reveil.core

/** Tonearm position: [lift] 0 = resting on a surface, 1 = raised; [swing] 0 = over the record, 1 = on its rest. */
data class ArmPose(val lift: Float, val swing: Float)

/**
 * The arm moves like a hand would. When the alarm starts it goes from its rest onto the record;
 * slide-to-stop lifts it, swings it back to the rest and lowers it.
 */
object Tonearm {
    /** Time to bring the arm onto the record when the alarm starts. */
    const val START_MS = 2000L

    private const val LIFT_END = 0.35f
    private const val SWING_START = 0.3f
    private const val SWING_END = 0.92f
    private const val LOWER_START = 0.88f

    /** The start is the stop gesture played backwards. */
    fun start(elapsedMs: Long): ArmPose = pose(1f - elapsedMs.toFloat() / START_MS)

    fun pose(slide: Float): ArmPose {
        val p = slide.coerceIn(0f, 1f)
        return ArmPose(
            lift = smoothStep(0f, LIFT_END, p) * (1 - smoothStep(LOWER_START, 1f, p)),
            swing = smoothStep(SWING_START, SWING_END, p),
        )
    }
}
