package fr.clrk.reveil.core

/** How far the drop shadows have slid, in picture pixels. */
data class Drift(val x: Float, val y: Float)

/**
 * A drop running down the window casts its shadow ever closer to the window, so on the turntable
 * the shadows slide slowly to the left and a little up. Two layers at different speeds avoid a conveyor look.
 */
object RainDrift {
    private val SPEEDS = floatArrayOf(5f, 7.5f)
    private const val RISE = 0.35f

    fun offset(layer: Int, seconds: Float): Drift {
        val distance = SPEEDS[layer] * seconds
        return Drift(-distance, -distance * RISE)
    }
}
