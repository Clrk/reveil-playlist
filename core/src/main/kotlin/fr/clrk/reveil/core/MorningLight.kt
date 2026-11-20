package fr.clrk.reveil.core

import java.time.Duration

/**
 * How lit the ring screen's room is, each value from 0 to 1.
 * [firstRay]: faint pink sunrise; [sunPatch]: the sun patch entering through the window;
 * [gold]: warmer light on top; [sweep]: how far the patch edge has crossed the room (≥ 1 = fully in); [text]: clock opacity;
 * [glow]: the pink sunrise left on the walls, fading only once the sun is nearly in, so the room never dims.
 */
data class LightStage(val firstRay: Float, val sunPatch: Float, val gold: Float, val sweep: Float, val text: Float, val glow: Float = 0f)

/** The morning follows the volume ramp: night when the alarm starts, full morning when the music is at full volume. */
class MorningLight(private val rampSeconds: Int) {
    fun at(elapsed: Duration): LightStage {
        val t = rampProgress(rampSeconds, elapsed)
        val ray = smoothStep(RAY_START, RAY_END, t)
        return LightStage(
            firstRay = ray,
            sunPatch = smoothStep(PATCH_START, PATCH_END, t),
            gold = smoothStep(GOLD_START, 1f, t) * GOLD_MAX,
            sweep = SWEEP_FROM + SWEEP_SPAN * smoothStep(PATCH_START, SWEEP_END, t),
            text = TEXT_MIN + (1 - TEXT_MIN) * t,
            glow = ray * (1 - smoothStep(GLOW_FADE_START, 1f, t)),
        )
    }

    private companion object {
        const val RAY_START = 0.04f
        const val RAY_END = 0.32f
        const val PATCH_START = 0.3f
        const val PATCH_END = 0.85f
        const val GOLD_START = 0.8f
        const val GOLD_MAX = 0.75f
        const val SWEEP_FROM = -0.2f
        const val SWEEP_SPAN = 1.45f
        const val SWEEP_END = 0.95f
        const val TEXT_MIN = 0.3f
        const val GLOW_FADE_START = 0.6f
    }
}
