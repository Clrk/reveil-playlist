package fr.clrk.reveil.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * How the elements of a screen arrive: one after the other, each fading in while it rises a little.
 * Unhurried but not slow: about a third of a second each, a short beat apart, with a soft landing.
 */
object Calm {
    const val ARRIVE_MS = 420
    const val BEAT_MS = 70
    const val LEAVE_MS = 200
    const val OPEN_MS = 320
    val RISE = 10.dp

    /** Fast start, long soft landing: nothing bounces or snaps. */
    val EASE = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** What leaves goes quicker than what arrives, and speeds up as it goes: it needs less attention. */
    val AWAY = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** How far element [order] has arrived [ms] after the screen opened, 0 to 1. */
    fun arrived(order: Int, ms: Long): Float = EASE.transform(((ms - order * BEAT_MS).toFloat() / ARRIVE_MS).coerceIn(0f, 1f))
}

/** A block of a screen that arrives in [order]; see [reveal]. */
@Composable
fun Arrives(order: Int, waits: Boolean = true, content: @Composable () -> Unit) =
    Column(Modifier.reveal(order, waits = waits), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }

/** Order of the n-th row of a list: the first rows cascade, the rest arrive together. */
fun row(index: Int, first: Int = 1) = first + minOf(index, MAX_ROWS)

private const val MAX_ROWS = 5

/** Frozen time since a screen opened, to draw animation frames in tests; null lets the real clock run. */
val LocalRevealClock = compositionLocalOf<Long?> { null }

/**
 * The element arrives in [order] (0 first) when its screen opens. Still in previews and screenshots.
 * In a list, pass the [opened] time of the screen: rows scrolled into view after the cascade simply stay.
 */
fun Modifier.reveal(order: Int, opened: Long? = null, waits: Boolean = true): Modifier = composed {
    val frozen = LocalRevealClock.current
    val rise = with(LocalDensity.current) { Calm.RISE.toPx() }
    val still = settled(order, opened)
    val progress = remember { Animatable(if (still) 1f else 0f) }
    if (frozen == null) Arrive(order, progress, waits)
    graphicsLayer {
        val p = if (frozen == null) progress.value else Calm.arrived(order, frozen)
        alpha = p
        translationY = (1 - p) * rise
    }
}

/** Already in place: a still screenshot, or a row scrolled into view once the cascade is over. */
@Composable
private fun settled(order: Int, opened: Long?): Boolean {
    val late = opened != null && SystemClock.uptimeMillis() - opened > order * Calm.BEAT_MS + Calm.ARRIVE_MS
    return LocalInspectionMode.current || LocalReturn.current || late
}

/** True on a screen one comes back to: it is already known, so it is simply there, without a cascade. */
val LocalReturn = compositionLocalOf { false }

@Composable
private fun Arrive(order: Int, progress: Animatable<Float, *>, waits: Boolean) {
    val flight = LocalFlight.current
    LaunchedEffect(Unit) { arrive(order, progress, if (waits) flight else null) }
}

/** The element waits for a cover in the air to land, then for its turn, then arrives. */
private suspend fun arrive(order: Int, progress: Animatable<Float, *>, flight: Flight?) {
    if (progress.value >= 1f) return
    flight?.let { f -> snapshotFlow { f.flying }.first { !it } }
    delay(order.toLong() * Calm.BEAT_MS)
    progress.animateTo(1f, tween(Calm.ARRIVE_MS, easing = Calm.EASE))
}

/** Lets a screen opened over another ease out before it is gone. */
@Stable
class Leave {
    internal val shown = MutableTransitionState(false).apply { targetState = true }
    internal var instant = false
    internal var after: () -> Unit = {}

    /** Fades the screen out, then runs [action]. */
    fun then(action: () -> Unit) {
        if (instant) return action()
        after = action
        shown.targetState = false
    }
}

@Composable
fun rememberLeave() = remember { Leave() }

/** A screen that opens over another eases in, rising slightly, and eases out through [leave]. Still in screenshots. */
@Composable
fun Appear(leave: Leave = rememberLeave(), content: @Composable () -> Unit) {
    leave.instant = LocalInspectionMode.current
    if (leave.instant) return content()
    LaunchedEffect(leave.shown.isIdle) { if (leave.shown.isIdle && !leave.shown.currentState) leave.after() }
    AnimatedVisibility(
        leave.shown,
        enter = fadeIn(tween(Calm.OPEN_MS, easing = Calm.EASE)) + slideInVertically(tween(Calm.OPEN_MS, easing = Calm.EASE)) { it / RISE },
        exit = fadeOut(tween(Calm.LEAVE_MS, easing = Calm.AWAY)) + slideOutVertically(tween(Calm.LEAVE_MS)) { it / (2 * RISE) },
    ) { content() }
}

private const val RISE = 14
