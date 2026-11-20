package fr.clrk.reveil.core

/** Which square of the cover picture fills the record label: centred, never stretched. */
object CoverCrop {
    data class Square(val x: Float, val y: Float, val side: Float)

    private const val THUMB_RATIO = 4f / 3
    private const val VIDEO_RATIO = 9f / 16
    private const val EPSILON = 0.01f

    fun of(width: Float, height: Float): Square {
        val thumbnail = kotlin.math.abs(width / height - THUMB_RATIO) < EPSILON
        val side = if (thumbnail) width * VIDEO_RATIO else minOf(width, height)
        return Square((width - side) / 2, (height - side) / 2, side)
    }
}
