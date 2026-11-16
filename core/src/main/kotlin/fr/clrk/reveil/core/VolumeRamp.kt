package fr.clrk.reveil.core

import java.time.Duration

/** Volume factor applied to the player while the alarm rings: from 10 % up to full over [seconds]. */
class VolumeRamp(private val seconds: Int) {
    fun at(elapsed: Duration): Float {
        if (seconds <= 0) return FULL
        val progress = (elapsed.toMillis().toFloat() / (seconds * MILLIS)).coerceAtMost(FULL)
        return START + (FULL - START) * progress
    }

    private companion object {
        const val START = 0.1f
        const val FULL = 1f
        const val MILLIS = 1000f
    }
}
