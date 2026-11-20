@file:Suppress("MagicNumber") // proportions of the drawn record
package fr.clrk.reveil.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

private const val DISC_OUT = 0.5f
private const val SLIDE_MS = 450

/**
 * A record sleeve with its playlist [cover]; the disc slides [out] of it when the alarm is on.
 * A sleeve at a [spot] hides while its cover flies, and lets its disc out once it has landed.
 * While the music is being prepared, the cover fills up from the bottom to [progress], under an ember line.
 */
@Composable
fun Sleeve(cover: String?, out: Boolean, size: Dp, modifier: Modifier = Modifier, progress: Float? = null, spot: String? = null) {
    val landed = landed(spot)
    val slide by animateFloatAsState(if (out && landed) 1f else 0f, tween(SLIDE_MS, discWait(landed), Calm.EASE), label = "disc")
    Box(modifier.size(width = size * (1 + DISC_OUT), height = size).alpha(if (landed) 1f else 0f)) {
        val shift = Modifier.offset { IntOffset((size * (0.04f + DISC_OUT * slide)).roundToPx(), 0) }
        Disc(Modifier.size(size * 0.92f).align(Alignment.CenterStart).then(shift))
        Cover(cover, Modifier.size(size).spot(spot).shadow(6.dp, RoundedCornerShape(4.dp)))
        progress?.let { Filling(it, Modifier.size(size).clip(RoundedCornerShape(4.dp))) }
    }
}

/** Once a cover has landed, its disc waits a beat, so that the card's words arrive first. */
@Composable
private fun discWait(landed: Boolean): Int {
    val flew = remember { BooleanArray(1) }
    if (!landed) flew[0] = true
    return if (flew[0]) DISC_WAIT_MS else 0
}

private const val DISC_WAIT_MS = 260

/** The playlist picture, or a warm placeholder while there is none. */
@Composable
fun Cover(url: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(modifier.clip(shape).background(PLACEHOLDER), contentAlignment = Alignment.Center) {
        Text("♪", color = Night.CREAM.copy(alpha = 0.6f), fontSize = 22.sp)
        url?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

private val PLACEHOLDER = Brush.linearGradient(listOf(Color(0xFF2B2233), Color(0xFF8A4F6E), Color(0xFFE6B58A)))

/** Black vinyl: grooves, a faint sheen and the ember label. */
@Composable
private fun Disc(modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(Color(0xFF1B1B1B), r)
        for (i in 0 until GROOVES) drawCircle(Color(0xFF3C3C3C), r * (0.42f + 0.56f * i / GROOVES), style = Stroke(0.6f))
        drawCircle(Color(0x47E6D6C4), r, style = Stroke(1.5f))
        drawCircle(Night.EMBER, r * 0.3f)
        drawCircle(Color(0xFF151515), r * 0.03f)
    }
}

/** The part still to prepare is veiled; the ember line rises as songs arrive. */
@Composable
private fun Filling(progress: Float, modifier: Modifier) {
    val shown by animateFloatAsState(progress, tween(SLIDE_MS), label = "progress")
    Canvas(modifier) {
        val edge = size.height * (1 - shown)
        drawRect(Night.BG.copy(alpha = 0.62f), size = Size(size.width, edge))
        drawLine(Night.EMBER, Offset(0f, edge), Offset(size.width, edge), strokeWidth = 2.dp.toPx())
    }
}

private const val GROOVES = 14
