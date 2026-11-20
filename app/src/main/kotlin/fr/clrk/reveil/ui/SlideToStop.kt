package fr.clrk.reveil.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TRACK_HEIGHT = 64.dp
private val THUMB = 56.dp
private const val STOP_AT = 0.85f

/** Lets the tonearm settle on its rest before the screen closes. */
private const val SETTLE_MS = 450L

/**
 * Stopping takes a deliberate swipe, so a half-asleep tap can only snooze. TalkBack users get a plain action.
 * [onProgress] reports the thumb position (0–1), e.g. to lift the tonearm along with it.
 */
@Composable
fun SlideToStop(onProgress: (Float) -> Unit = {}, onStop: () -> Unit) {
    val offset = remember { Animatable(0f) }
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(TRACK_HEIGHT)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = "Arrêter le réveil"
                onClick("Arrêter") { onStop(); true }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val max = with(LocalDensity.current) { (maxWidth - THUMB - 8.dp).toPx() }
        Text("arrêter  →", Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium)
        LaunchedEffect(offset, max) { snapshotFlow { offset.value / max }.collect(onProgress) }
        Thumb(offset, max, onStop)
    }
}

@Composable
private fun Thumb(offset: Animatable<Float, *>, max: Float, onStop: () -> Unit) {
    val scope = rememberCoroutineScope()
    Box(
        Modifier.padding(4.dp).offset { IntOffset(offset.value.roundToInt(), 0) }.size(THUMB)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    scope.launch(start = CoroutineStart.UNDISPATCHED) { offset.snapTo((offset.value + delta).coerceIn(0f, max)) }
                },
                onDragStopped = {
                    val target = if (offset.value < max * STOP_AT) 0f else max
                    if (target == max) scope.launch { stopAfterSettle(onStop) }
                    offset.animateTo(target)
                },
            ),
        contentAlignment = Alignment.Center,
    ) { }
}

/**
 * Stops once the tonearm has had time to land. Kept apart from the thumb animation, which a late drag
 * event can cancel: the stop itself must never be lost.
 */
private suspend fun stopAfterSettle(onStop: () -> Unit) {
    delay(SETTLE_MS)
    onStop()
}
