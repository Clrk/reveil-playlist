package fr.clrk.reveil.display

private const val HERMITE_A = 3f

/** Smooth 0→1 step between [from] and [to]: slow start, slow end, like light and hands moving. */
internal fun smoothStep(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (HERMITE_A - 2 * t)
}
