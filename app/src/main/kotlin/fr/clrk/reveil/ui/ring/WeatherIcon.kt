@file:Suppress("MagicNumber") // coordinates on the 24-unit icon grid

package fr.clrk.reveil.ui.ring

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import fr.clrk.reveil.core.Sky

private const val VIEW = 24f
private val CLOUD_HIGH = PathParser().parsePathString("M7 14.5h9.5a3.5 3.5 0 0 0 .4-7A5 5 0 0 0 7.3 6.6 4 4 0 0 0 7 14.5z").toPath()
private val CLOUD_LOW = PathParser().parsePathString("M7 17.5h9.5a3.5 3.5 0 0 0 .4-7A5 5 0 0 0 7.3 9.6 4 4 0 0 0 7 17.5z").toPath()
private val RAYS = PathParser().parsePathString(
    "M12 3.5v2M12 18.5v2M3.5 12h2M18.5 12h2M6 6l1.4 1.4M16.6 16.6L18 18M6 18l1.4-1.4M16.6 7.4L18 6",
).toPath()
private val FLAKE = PathParser().parsePathString(
    "M12 3.5v17M4.6 7.75l14.8 8.5M4.6 16.25l14.8-8.5M10.3 5.2L12 6.6l1.7-1.4M10.3 18.8L12 17.4l1.7 1.4",
).toPath()

/** Thin-line weather icon with one slow, quiet loop (falling drops, drifting cloud, turning rays…). */
@Composable
fun WeatherIcon(sky: Sky, color: Color, modifier: Modifier, animate: Boolean = true) {
    val loop = rememberInfiniteTransition(label = "météo")
    val fall = loop.cycle(if (sky == Sky.SNOW) SNOW_MS else RAIN_MS, animate)
    val slow = loop.cycle(SLOW_MS, animate, RepeatMode.Reverse)
    Canvas(modifier) {
        scale(size.minDimension / VIEW, pivot = Offset.Zero) { icon(sky, color, fall.value, slow.value) }
    }
}

private fun DrawScope.icon(sky: Sky, color: Color, fall: Float, slow: Float) {
    val stroke = Stroke(width = 1.5f, cap = StrokeCap.Round)
    when (sky) {
        Sky.CLEAR -> sun(color, stroke, slow)
        Sky.CLOUDY -> translate(left = slow * 1.2f - 0.6f) { drawPath(CLOUD_LOW, color, style = stroke) }
        Sky.RAIN, Sky.SNOW -> precipitation(color, stroke, fall, slow, snow = sky == Sky.SNOW)
        Sky.FROST -> rotate(slow * FROST_TURN, Offset(CENTER, CENTER)) {
            drawPath(FLAKE, color.copy(alpha = 0.75f + 0.25f * slow), style = stroke)
        }
    }
}

private fun DrawScope.sun(color: Color, stroke: Stroke, breath: Float) {
    rotate(breath * SUN_TURN, Offset(CENTER, CENTER)) { drawPath(RAYS, color, style = stroke) }
    drawCircle(color, radius = 4f * (0.94f + 0.1f * breath), center = Offset(CENTER, CENTER), style = stroke)
}

/** Three drops (or flakes) under the cloud, each on its own phase so they never fall together. */
private fun DrawScope.precipitation(color: Color, stroke: Stroke, phase: Float, drift: Float, snow: Boolean) {
    translate(left = drift - 0.5f) { drawPath(CLOUD_HIGH, color, style = stroke) }
    listOf(9f, 13f, 17f).forEachIndexed { i, x ->
        val t = (phase + i / 3f) % 1f
        val y = 17f + t * 3.5f
        val faded = color.copy(alpha = color.alpha * fade(t))
        if (snow) {
            drawCircle(faded, 0.7f, Offset(x + kotlin.math.sin(t * 6f) * 0.6f, y))
        } else {
            val top = Offset(x - t * 0.7f, y)
            drawLine(faded, top, top + Offset(-0.6f, 1.6f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

private fun fade(t: Float) = if (t < 0.25f) t / 0.25f else 1f - (t - 0.25f) / 0.75f

@Composable
private fun InfiniteTransition.cycle(ms: Int, animate: Boolean, mode: RepeatMode = RepeatMode.Restart): State<Float> =
    animateFloat(
    initialValue = 0f,
    targetValue = if (animate) 1f else 0f,
    animationSpec = infiniteRepeatable(tween(ms, easing = if (mode == RepeatMode.Restart) LinearEasing else FastOutSlowInEasing), mode),
    label = "boucle",
)

private const val CENTER = 12f
private const val RAIN_MS = 1600
private const val SNOW_MS = 3200
private const val SLOW_MS = 4000
private const val SUN_TURN = 20f
private const val FROST_TURN = 30f
