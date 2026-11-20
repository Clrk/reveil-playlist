package fr.clrk.reveil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * The one thread between screens: the playlist cover. It leaves its spot on one screen
 * and lands on its spot on the next, where the disc then slides out.
 */
@Stable
class Flight {
    private val spots = mutableMapOf<String, Rect>()
    internal var trip by mutableStateOf<Trip?>(null)
        private set

    /** The cover at spot [from] flies to spot [to] of the screen that opens next. */
    fun takeOff(from: String, to: String, cover: String?) {
        trip = spots[from]?.let { Trip(from, to, cover, it, spots[to]) }
    }

    /** True while a cover is in the air: the screen it lands on waits before showing the rest. */
    val flying get() = trip != null

    /** True while a cover is on its way to spot [key]. */
    fun landing(key: String) = trip?.to == key

    /** True while the cover of spot [key] is in the air, leaving or landing. */
    fun away(key: String) = trip.let { it != null && (it.from == key || it.to == key) }

    internal fun seen(key: String, bounds: Rect) {
        spots[key] = bounds
        trip?.takeIf { it.to == key }?.let { trip = it.copy(at = bounds) }
    }

    internal fun gone(key: String) { spots.remove(key) }

    internal fun land() { trip = null }
}

internal data class Trip(val from: String, val to: String, val cover: String?, val start: Rect, val at: Rect? = null) {
    /** Where the cover is, [done] of the way. */
    fun box(done: Float) = at?.let { lerp(start, it, done) } ?: start
}

val LocalFlight = staticCompositionLocalOf { Flight() }

/** True unless the cover of spot [key] is in the air. */
@Composable
fun landed(key: String?) = key == null || !LocalFlight.current.away(key)

/** Where a cover sits on its screen, so that it can leave from or land on it; none without a [key]. */
fun Modifier.spot(key: String?): Modifier = if (key == null) this else composed {
    val flight = LocalFlight.current
    DisposableEffect(key) { onDispose { flight.gone(key) } }
    onGloballyPositioned { flight.seen(key, it.boundsInRoot()) }.alpha(if (flight.away(key)) 0f else 1f)
}

/** The cover in the air, drawn over every screen. */
@Composable
fun FlightOverlay() {
    val flight = LocalFlight.current
    val trip = flight.trip ?: return
    val travel = remember(trip.from, trip.to) { Animatable(0f) }
    LaunchedEffect(trip.from, trip.to, trip.at != null) {
        if (trip.at == null) delay(LOST_MS) else travel.animateTo(1f, tween(FLY_MS, easing = Calm.EASE))
        flight.land()
    }
    val box = trip.box(travel.value)
    val height = sin(PI.toFloat() * travel.value)
    with(LocalDensity.current) {
        val arc = (ARC.toPx() * height).toInt()
        val place = Modifier.offset { IntOffset(box.left.toInt(), box.top.toInt() - arc) }.size(box.width.toDp(), box.height.toDp())
        Cover(trip.cover, place.shadow(REST + LIFT * height, RoundedCornerShape(4.dp)))
    }
}

/** A screen opened over another, in the same window, so that the cover can fly between them. */
@Composable
fun Layer(onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize()) { content() }
}

private const val FLY_MS = 480
private const val LOST_MS = 600L
private val REST = 6.dp
private val LIFT = 18.dp

/** The cover is lifted from the shelf: it rises a little mid-way, its shadow growing, then settles. */
private val ARC = 28.dp
