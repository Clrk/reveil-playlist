package fr.clrk.reveil.display

/**
 * Tonearm position: [lift] 0 = resting on a surface, 1 = raised; [swing] 0 = over the record, 1 = on its rest;
 * [bob] the stylus height change, in picture pixels, as it rides the record.
 */
data class ArmPose(val lift: Float, val swing: Float, val bob: Float = 0f)

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
    private const val WARP_PX = 0.8f
    private const val WOBBLE = 0.3

    /** The start is the stop gesture played backwards. */
    fun start(elapsedMs: Long): ArmPose = pose(1f - elapsedMs.toFloat() / START_MS)

    /**
     * Height change of the stylus, in picture pixels, as it rides a slightly warped record at [angle] degrees:
     * up and down once a turn, with a little second wobble, scaled by the record [speed] (0 still, 1 full).
     */
    fun bob(angle: Float, speed: Float): Float {
        val a = Math.toRadians(angle.toDouble())
        return WARP_PX * speed * ((kotlin.math.sin(a) + WOBBLE * kotlin.math.sin(2 * a + 1)) / (1 + WOBBLE)).toFloat()
    }

    fun pose(slide: Float): ArmPose {
        val p = slide.coerceIn(0f, 1f)
        return ArmPose(
            lift = smoothStep(0f, LIFT_END, p) * (1 - smoothStep(LOWER_START, 1f, p)),
            swing = smoothStep(SWING_START, SWING_END, p),
        )
    }
}
