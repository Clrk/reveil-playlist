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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TRACK_HEIGHT = 64.dp
private val THUMB = 56.dp
private const val STOP_AT = 0.85f

/** Stopping takes a deliberate swipe, so a half-asleep tap can only snooze. TalkBack users get a plain action. */
@Composable
fun SlideToStop(onStop: () -> Unit) {
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
        Text("Glisser pour arrêter  →", Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium)
        Thumb(offset, max, onStop)
    }
}

@Composable
private fun Thumb(offset: Animatable<Float, *>, max: Float, onStop: () -> Unit) {
    val scope = rememberCoroutineScope()
    Box(
        Modifier.padding(4.dp).offset { IntOffset(offset.value.roundToInt(), 0) }.size(THUMB)
            .background(MaterialTheme.colorScheme.onSurface, CircleShape)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, max)) } },
                onDragStopped = { if (offset.value >= max * STOP_AT) onStop() else offset.animateTo(0f) },
            ),
        contentAlignment = Alignment.Center,
    ) { Text("■", color = MaterialTheme.colorScheme.surface) }
}
