package fr.clrk.reveil.core

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** A drop shadow on the turntable, in picture pixels; [stretch] lengthens it along its run. */
data class DropShadow(val x: Float, val y: Float, val radius: Float, val opacity: Float, val stretch: Float = 1f)

/**
 * One drop that runs down the window, again and again: it waits, slides [length] pixels in [jerks] jerks
 * (slowing almost to a halt between them), vanishes, and its trail of droplets fades before it starts over.
 * A drop lower on the glass casts its shadow nearer the window, so on the turntable it runs left and a little up.
 */
class RainRunner(private val path: Path, rhythm: Rhythm) {
    /** Where the shadow starts on the picture, how far it runs, and in how many jerks. */
    data class Path(val x: Float, val y: Float, val length: Float, val jerks: Int)

    /** The drop repeats every [period] seconds: it waits [wait] seconds, then runs for [run] seconds. */
    data class Rhythm(val period: Float, val phase: Float, val wait: Float, val run: Float)

    val period = rhythm.period
    val wait = rhythm.wait
    val run = rhythm.run
    private val phase = rhythm.phase
    private val jerks = path.jerks
    private val length = path.length

    private fun local(seconds: Float) = ((seconds + phase) % period + period) % period

    fun isRunning(seconds: Float) = local(seconds).let { it >= wait && it < wait + run }

    /** The running drop, or null once it has gone. */
    fun head(seconds: Float): DropShadow? {
        val t = local(seconds)
        if (t > wait + run + VANISH) return null
        val u = ((t - wait) / run).coerceIn(0f, 1f)
        val opacity = minOf(1f, t / APPEAR, (wait + run + VANISH - t) / VANISH)
        val pace = if (u in 0f..<1f && t >= wait) (1 - cos(2 * PI * jerks * u).toFloat()) / 2 else 0f
        return at(distance(u), HEAD_RADIUS, HEAD_OPACITY * opacity, 1f + STRETCH * pace)
    }

    /**
     * What the drop leaves behind: a faint wet streak and droplets every few pixels,
     * each fading from the moment the drop passed it.
     */
    fun trail(seconds: Float): List<DropShadow> {
        val t = local(seconds)
        if (t < wait || t >= wait + run + FADE) return emptyList()
        val travelled = distance(((t - wait) / run).coerceIn(0f, 1f))
        val streak = behind(t, travelled, STREAK_SPACING) { _ -> STREAK_RADIUS to STREAK_OPACITY }
        return streak + behind(t, travelled, SPACING) { i -> dropletRadius(i) to TRAIL_OPACITY }
    }

    private fun behind(t: Float, travelled: Float, spacing: Float, look: (Int) -> Pair<Float, Float>) =
        generateSequence(spacing) { it + spacing }.takeWhile { it < travelled - BEHIND }.mapIndexedNotNull { i, d ->
            val fade = 1f - (t - passedAt(d)) / FADE
            val (radius, opacity) = look(i)
            if (fade <= 0f) null else at(d, radius, opacity * fade, 1f)
        }.toList()

    /** Droplets of a few sizes, in an order that does not repeat along the trail. */
    private fun dropletRadius(i: Int) = DROPLET + DROPLET_SPREAD * ((i * SIZE_STEP + jerks) % SIZES) / (SIZES - 1)

    /** Stop-and-go progress: its slope 1 - cos(2πku) falls to zero between jerks. */
    private fun distance(u: Float) = length * (u - (sin(2 * PI * jerks * u) / (2 * PI * jerks)).toFloat())

    private fun passedAt(d: Float): Float {
        var low = 0f
        var high = 1f
        repeat(SEARCH_STEPS) {
            val mid = (low + high) / 2
            if (distance(mid) < d) low = mid else high = mid
        }
        return wait + run * low
    }

    private fun at(d: Float, radius: Float, opacity: Float, stretch: Float): DropShadow {
        val wobble = WOBBLE * sin(d / WOBBLE_WAVE + jerks)
        return DropShadow(path.x + DIR_X * d - DIR_Y * wobble, path.y + DIR_Y * d + DIR_X * wobble, radius, opacity, stretch)
    }

    companion object {
        private val NORM = hypot(1f, 0.35f)
        private val DIR_X = -1f / NORM
        private val DIR_Y = -0.35f / NORM

        /** Direction of the run on the picture, in degrees clockwise from the right: shadows stretch along it. */
        val RUN_DEGREES = (Math.toDegrees(atan2(DIR_Y, DIR_X).toDouble()).toFloat() + Turntable.TURN) % Turntable.TURN
        private const val APPEAR = 0.4f
        private const val VANISH = 0.6f
        private const val FADE = 5f
        private const val SPACING = 6f
        private const val STREAK_SPACING = 1.5f
        private const val STREAK_RADIUS = 1.1f
        private const val STREAK_OPACITY = 0.18f
        private const val BEHIND = 4f
        private const val HEAD_RADIUS = 4f
        private const val HEAD_OPACITY = 0.95f
        private const val TRAIL_OPACITY = 0.75f
        private const val DROPLET = 1.3f
        private const val DROPLET_SPREAD = 0.7f
        private const val STRETCH = 0.7f
        private const val WOBBLE = 2.5f
        private const val WOBBLE_WAVE = 13f
        private const val SEARCH_STEPS = 18
        private const val SIZE_STEP = 3
        private const val SIZES = 5
    }
}

/** The few drops that run, each on its own rhythm so the window never looks like a loop. */
object RainRunners {
    val ALL = listOf(
        RainRunner(RainRunner.Path(470f, 700f, 140f, 3), RainRunner.Rhythm(23f, 0f, 2.5f, 5f)),
        RainRunner(RainRunner.Path(420f, 790f, 160f, 4), RainRunner.Rhythm(31f, 14f, 3f, 5.5f)),
        RainRunner(RainRunner.Path(500f, 640f, 110f, 2), RainRunner.Rhythm(27f, 21f, 2f, 4.5f)),
        RainRunner(RainRunner.Path(360f, 830f, 180f, 4), RainRunner.Rhythm(37f, 1f, 4f, 6f)),
        RainRunner(RainRunner.Path(330f, 700f, 120f, 3), RainRunner.Rhythm(29f, 17f, 3f, 5f)),
    )

    fun at(seconds: Float): List<DropShadow> = ALL.flatMap { it.trail(seconds) + listOfNotNull(it.head(seconds)) }
}
