package fr.clrk.reveil.ui

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

/** Hours and minutes on two wheels, thin digits, the minutes in ember as on the ring screen. */
@Composable
fun TimeWheel(time: LocalTime, onChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Wheel(HOURS, time.hour, Night.CREAM) { onChange(time.withHour(it)) }
        Text(":", Modifier.width(30.dp), color = Night.DIM, fontSize = 70.sp, fontWeight = FontWeight.Thin)
        Wheel(MINUTES, time.minute, Night.EMBER) { onChange(time.withMinute(it)) }
    }
}

@Composable
private fun Wheel(count: Int, value: Int, color: Color, onChange: (Int) -> Unit) {
    val start = remember { LAPS / 2 * count + value }
    val state = rememberLazyListState(start)
    val centre by remember { derivedStateOf { state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > HALF_PX) 1 else 0 } }
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) onChange(state.firstVisibleItemIndex % count)
    }
    LazyColumn(
        Modifier.height(ROW * VISIBLE).width(120.dp), state = state, flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = ROW * (VISIBLE / 2)), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(count * LAPS) { i ->
            val on = i == centre
            Box(Modifier.height(ROW), contentAlignment = Alignment.Center) {
                Text(
                    "%02d".format(i % count), color = if (on) color else Night.DIM,
                    fontSize = if (on) 76.sp else 38.sp, fontWeight = FontWeight.Thin, lineHeight = 80.sp,
                )
            }
        }
    }
}

private const val HOURS = 24
private const val MINUTES = 60
private const val LAPS = 200
private const val VISIBLE = 5
private const val HALF_PX = 60
private val ROW = 74.dp
