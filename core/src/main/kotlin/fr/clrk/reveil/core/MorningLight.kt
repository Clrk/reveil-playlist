package fr.clrk.reveil.core

import java.time.Duration

/**
 * How lit the ring screen's room is, each value from 0 to 1.
 * [firstRay]: faint pink sunrise; [sunPatch]: the sun patch entering through the window;
 * [gold]: warmer light on top; [sweep]: how far the patch edge has crossed the room (≥ 1 = fully in); [text]: clock opacity.
 */
data class LightStage(val firstRay: Float, val sunPatch: Float, val gold: Float, val sweep: Float, val text: Float)

/** The morning follows the volume ramp: night when the alarm starts, full morning when the music is at full volume. */
class MorningLight(private val rampSeconds: Int) {
    fun at(elapsed: Duration): LightStage {
        val t = if (rampSeconds <= 0) 1f else (elapsed.toMillis() / (rampSeconds * MILLIS)).coerceIn(0f, 1f)
        return LightStage(
            firstRay = smoothStep(RAY_START, RAY_END, t),
            sunPatch = smoothStep(PATCH_START, PATCH_END, t),
            gold = smoothStep(GOLD_START, 1f, t) * GOLD_MAX,
            sweep = SWEEP_FROM + SWEEP_SPAN * smoothStep(PATCH_START, SWEEP_END, t),
            text = TEXT_MIN + (1 - TEXT_MIN) * t,
        )
    }

    private companion object {
        const val MILLIS = 1000f
        const val RAY_START = 0.04f
        const val RAY_END = 0.32f
        const val PATCH_START = 0.3f
        const val PATCH_END = 0.68f
        const val GOLD_START = 0.66f
        const val GOLD_MAX = 0.75f
        const val SWEEP_FROM = -0.2f
        const val SWEEP_SPAN = 1.45f
        const val SWEEP_END = 0.8f
        const val TEXT_MIN = 0.3f
    }
}
