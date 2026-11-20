package fr.clrk.reveil.core

import java.time.Duration

private const val HERMITE_A = 3f

/** Smooth 0→1 step between [from] and [to]: slow start, slow end, like light and hands moving. */
internal fun smoothStep(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (HERMITE_A - 2 * t)
}

/** Share of the wake-up ramp already done, from 0 to 1: the volume and the morning light follow the same clock. */
internal fun rampProgress(seconds: Int, elapsed: Duration): Float =
    if (seconds <= 0) 1f else (elapsed.toMillis() / (seconds * RAMP_MILLIS)).coerceIn(0f, 1f)

private const val RAMP_MILLIS = 1000f
