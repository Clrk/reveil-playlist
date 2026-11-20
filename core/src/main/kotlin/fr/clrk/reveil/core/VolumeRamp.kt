package fr.clrk.reveil.core

import java.time.Duration

/** Volume factor applied to the player while the alarm rings: from 10 % up to full over [seconds]. */
class VolumeRamp(private val seconds: Int) {
    fun at(elapsed: Duration): Float = START + (FULL - START) * rampProgress(seconds, elapsed)

    private companion object {
        const val START = 0.1f
        const val FULL = 1f
    }
}
