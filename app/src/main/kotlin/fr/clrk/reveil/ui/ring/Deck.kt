package fr.clrk.reveil.ui.ring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import coil3.compose.rememberAsyncImagePainter
import fr.clrk.reveil.alarm.RingState
import fr.clrk.reveil.display.ArmPose
import fr.clrk.reveil.display.RecordChange
import fr.clrk.reveil.display.Tonearm
import fr.clrk.reveil.display.Turntable

/** What the turntable shows at one moment: the cover on the record, the record angle and the arm. */
data class Deck(val cover: Label, val spin: Float, val arm: ArmPose)

/**
 * The turntable at [ms] since the ring started: the arm comes onto the record, rides it, and changes records
 * between tracks; [slide] (stop gesture) takes over the arm. A [fixed] time (screenshots) computes the angle
 * directly; otherwise the record turns frame by frame, so speed changes never make it jump.
 */
@Composable
fun rememberDeck(ui: RingState.Ui, ms: Long, slide: Float, fixed: Boolean): Deck {
    val change = ui.change?.takeIf { ms >= it.endedMs }
    val speed = change?.speed(ms) ?: Turntable.speed(ms)
    val spin = if (fixed) Turntable.angle(ms) else rememberSpin(speed)
    val cover = rememberAsyncImagePainter(ui.cover).takeIf { ui.cover != null }
    val previous = rememberAsyncImagePainter(ui.previousCover).takeIf { ui.previousCover != null }
    val label = Label(cover, previous, change?.newCover(ms) ?: 1f)
    return Deck(label, spin, if (slide > 0f) Tonearm.pose(slide) else riding(change, ms, spin, speed))
}

private fun riding(change: RecordChange?, ms: Long, spin: Float, speed: Float) =
    (change?.arm(ms) ?: Tonearm.start(ms)).copy(bob = Tonearm.bob(spin, speed))

@Composable
private fun rememberSpin(speed: Float): Float {
    val current by rememberUpdatedState(speed)
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                angle = (angle + Turntable.DEGREES_PER_MS * current * (now - last)) % Turntable.TURN
                last = now
            }
        }
    }
    return angle
}

