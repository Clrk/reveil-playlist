@file:Suppress("MagicNumber") // geometry measured on the plate pictures

package fr.clrk.reveil.ui.ring

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.imageResource
import fr.clrk.reveil.R
import fr.clrk.reveil.core.ArmPose
import fr.clrk.reveil.core.CoverCrop
import fr.clrk.reveil.core.LightStage
import fr.clrk.reveil.core.DropShadow
import fr.clrk.reveil.core.RainRunners
import fr.clrk.reveil.core.Sky

/** Plate size of the Gemini pictures, and the x the screen is centred on (the turntable). */
internal const val PLATE_W = 572f
internal const val PLATE_H = 1024f
private const val FOCUS_X = 310f

/** The six pictures share one framing: night, then a lit picture per sky (sun = pink, then gold on top). */
private class Plates(
    val night: ImageBitmap,
    val pink: ImageBitmap,
    val gold: ImageBitmap,
    val weather: Map<Sky, ImageBitmap>,
    val rain: Rain,
)

/** Still drop shadows (a tile multiplied on the light) and where the light falls on the turntable. */
private class Rain(val drops: ImageBitmap, val patch: ImageBitmap)

@Composable
private fun plates() = Plates(
    ImageBitmap.imageResource(R.drawable.scene_nuit),
    ImageBitmap.imageResource(R.drawable.scene_rose),
    ImageBitmap.imageResource(R.drawable.scene_dore),
    mapOf(
        Sky.RAIN to ImageBitmap.imageResource(R.drawable.scene_averse),
        Sky.SNOW to ImageBitmap.imageResource(R.drawable.scene_neige),
        Sky.CLOUDY to ImageBitmap.imageResource(R.drawable.scene_nuages),
        Sky.FROST to ImageBitmap.imageResource(R.drawable.scene_gel),
    ),
    Rain(
        ImageBitmap.imageResource(R.drawable.rain_drops_a),
        ImageBitmap.imageResource(R.drawable.rain_patch),
    ),
)

/**
 * What the room shows: today's [sky] lit to [light]; the record label is the track [cover] turned by [spin] degrees;
 * the tonearm follows [arm]; [seconds] since the ring started slides the rain drop shadows.
 */
data class SceneState(val light: LightStage, val sky: Sky, val cover: Painter?, val spin: Float, val arm: ArmPose, val seconds: Float = 0f)

/** The room at dawn: the night picture, lit up by the morning picture of today's sky as the light rises. */
@Composable
fun MorningScene(state: SceneState, modifier: Modifier) {
    val plates = plates()
    Canvas(modifier) {
        val scale = size.height / PLATE_H
        translate(left = size.width / 2 - FOCUS_X * scale) {
            scale(scale, pivot = Offset.Zero) { room(plates, state) }
        }
    }
}

private fun DrawScope.room(plates: Plates, state: SceneState) {
    val light = state.light
    val sunny = state.sky == Sky.CLEAR
    plate(plates.night)
    label(state.cover, state.spin, NIGHT_LABEL)
    if (sunny) plate(plates.pink, light.firstRay * PINK_GLOW * (1 - light.sunPatch))
    if (light.sunPatch > 0f) lit(light, sunny) { litRoom(plates, state, sunny) }
    tonearm(state.arm, light.sunPatch)
}

private fun DrawScope.litRoom(plates: Plates, state: SceneState, sunny: Boolean) {
    plate(plates.weather[state.sky] ?: plates.pink)
    if (sunny) plate(plates.gold, state.light.gold)
    label(state.cover, state.spin, 1f)
    if (state.sky == Sky.RAIN) rainShadows(plates.rain, state.seconds)
}

/**
 * Most drop shadows stay still; a few run toward the window in jerks, leaving droplets that fade.
 * Everything is kept to the sunlit patch and multiplied on the light.
 */
private fun DrawScope.rainShadows(rain: Rain, seconds: Float) {
    val bounds = Rect(0f, 0f, PLATE_W, PLATE_H)
    drawContext.canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.Multiply })
    drawRect(ShaderBrush(ImageShader(rain.drops, TileMode.Repeated, TileMode.Repeated)), size = Size(PLATE_W, PLATE_H))
    RainRunners.at(seconds).forEach { dropShadow(it) }
    drawImage(rain.patch, dstSize = IntSize(PLATE_W.toInt(), PLATE_H.toInt()), blendMode = BlendMode.DstIn)
    drawContext.canvas.restore()
}

/** A soft round shadow, drawn longer along the run while the drop is moving. */
private fun DrawScope.dropShadow(drop: DropShadow) {
    val core = DROP_SHADE.copy(alpha = drop.opacity)
    val brush = Brush.radialGradient(0f to core, 0.55f to core.copy(alpha = drop.opacity * 0.7f), 1f to Color.Transparent,
        center = Offset.Zero, radius = drop.radius * 1.6f)
    withTransform({
        translate(drop.x, drop.y)
        rotate(RUN_ANGLE, Offset.Zero)
        scale(drop.stretch, 1f, Offset.Zero)
    }) { drawCircle(brush, drop.radius * 1.6f, Offset.Zero) }
}

private val DROP_SHADE = Color(0xFF7A6152)

/** Direction the shadows run in, toward the window (left and a little up), in degrees. */
private const val RUN_ANGLE = 199.3f

/** The lit picture fades in while the sun patch enters from the window side (only with sun). */
private fun DrawScope.lit(light: LightStage, sweep: Boolean, content: DrawScope.() -> Unit) {
    val bounds = Rect(0f, 0f, PLATE_W, PLATE_H)
    drawContext.canvas.saveLayer(bounds, Paint().apply { alpha = light.sunPatch })
    content()
    if (sweep) drawRect(sweepBrush(light.sweep), size = Size(PLATE_W, PLATE_H), blendMode = BlendMode.DstIn)
    drawContext.canvas.restore()
}

private fun sweepBrush(edge: Float): Brush {
    val from = (edge - SWEEP_SOFT).coerceIn(0f, 1f)
    val to = (edge + SWEEP_SOFT).coerceIn(from, 1f)
    return Brush.linearGradient(
        0f to Color.Black, from to Color.Black, to to Color.Transparent, 1f to Color.Transparent,
        start = Offset(SWEEP_X0, SWEEP_Y0), end = Offset(SWEEP_X1, SWEEP_Y1),
    )
}

private fun DrawScope.plate(image: ImageBitmap, alpha: Float = 1f) =
    drawImage(image, dstSize = IntSize(PLATE_W.toInt(), PLATE_H.toInt()), alpha = alpha)

/** The record label is an ellipse on the tilted platter: draw the cover in a circle, squashed and spun. */
private fun DrawScope.label(cover: Painter?, spin: Float, brightness: Float) {
    withTransform({
        translate(LABEL_X, LABEL_Y)
        rotate(LABEL_TILT, Offset.Zero)
        scale(1f, LABEL_SQUASH, Offset.Zero)
        rotate(spin, Offset.Zero)
    }) {
        grooves(brightness)
        val circle = Path().apply { addOval(Rect(Offset.Zero, LABEL_R)) }
        if (cover != null) clipPath(circle) { centreCrop(cover, brightness) }
        drawCircle(Color(0xFFCFC9C0), radius = 1.6f, center = Offset.Zero)
    }
}

/** Draws the centre square of the cover over the label circle, scaled, never stretched. */
private fun DrawScope.centreCrop(cover: Painter, brightness: Float) {
    val known = cover.intrinsicSize.isSpecified && cover.intrinsicSize.minDimension > 0f
    val image = if (known) cover.intrinsicSize else Size(1f, 1f)
    val crop = CoverCrop.of(image.width, image.height)
    val k = LABEL_R * 2 / crop.side
    translate(-LABEL_R - crop.x * k, -LABEL_R - crop.y * k) {
        with(cover) { draw(Size(image.width * k, image.height * k), colorFilter = dim(brightness)) }
    }
}

/**
 * Faint worn arcs and dust on the vinyl, turning with the label: the eye reads the whole record spinning,
 * while the photographed reflections stay still as they would under a fixed window.
 */
private fun DrawScope.grooves(brightness: Float) {
    val shine = Color(0xFFF3E6D3).copy(alpha = 0.05f + 0.13f * brightness)
    GROOVES.forEach { (radius, start, sweep) ->
        drawArc(shine, start, sweep, useCenter = false, topLeft = Offset(-radius, -radius), size = Size(radius * 2, radius * 2),
            style = Stroke(width = 0.9f))
    }
    DUST.forEach { (radius, angle) ->
        val a = Math.toRadians(angle.toDouble())
        drawCircle(shine, 0.9f, Offset((radius * kotlin.math.cos(a)).toFloat(), (radius * kotlin.math.sin(a)).toFloat()))
    }
}

/** Radius, start angle and sweep of the worn arcs; radius and angle of the dust specks. */
private val GROOVES = listOf(
    Triple(52f, 10f, 70f),
    Triple(63f, 160f, 50f),
    Triple(74f, 250f, 85f),
    Triple(88f, 40f, 35f),
    Triple(97f, 200f, 60f),
)
private val DUST = listOf(58f to 120f, 71f to 330f, 84f to 205f, 93f to 75f)

private fun dim(brightness: Float): ColorFilter? =
    if (brightness >= 1f) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(brightness, brightness, brightness, 1f) })

private const val NIGHT_LABEL = 0.22f
private const val PINK_GLOW = 0.38f
private const val SWEEP_SOFT = 0.18f
private const val SWEEP_X0 = 40f
private const val SWEEP_Y0 = 620f
private const val SWEEP_X1 = 660f
private const val SWEEP_Y1 = 770f
private const val LABEL_X = 303.6f
private const val LABEL_Y = 661f
private const val LABEL_R = 40f
private const val LABEL_SQUASH = 0.53f
private const val LABEL_TILT = 0.9f
