package fr.clrk.reveil.core

import java.time.Duration

/** Share of the wake-up ramp already done, from 0 to 1: the volume and the morning light follow the same clock. */
fun rampProgress(seconds: Int, elapsed: Duration): Float =
    if (seconds <= 0) 1f else (elapsed.toMillis() / (seconds * RAMP_MILLIS)).coerceIn(0f, 1f)

private const val RAMP_MILLIS = 1000f
