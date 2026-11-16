package fr.clrk.reveil.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import fr.clrk.reveil.alarm.RingService
import fr.clrk.reveil.alarm.RingState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Full-screen ringing screen, shown above the lock screen. Always dark: it is read with sleepy eyes. */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ui by RingState.ui.collectAsStateWithLifecycle()
            LaunchedEffect(ui) { if (ui == null) finish() }
            ui?.let { RingScreen(it, send = ::send) }
        }
    }

    private fun send(action: String) {
        RingService.send(this, action)
        finish()
    }
}

internal val RingColors = darkColorScheme(
    background = Color(0xFF0F0D16),
    surface = Color(0xFF0F0D16),
    onSurface = Color(0xFFE6E0EE),
    primaryContainer = Color(0xFFCBBCFF),
    onPrimaryContainer = Color(0xFF21124D),
)

@Composable
internal fun RingScreen(
    ui: RingState.Ui,
    now: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    send: (String) -> Unit,
) {
    MaterialTheme(colorScheme = RingColors) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(now, fontSize = 88.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 24.dp))
                Text(ui.label.ifBlank { "Réveil" }, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(32.dp))
                NowPlaying(ui)
                Spacer(Modifier.weight(1f))
                ui.snooze?.let { SnoozeButton(it) { send(RingService.ACTION_SNOOZE) } }
                Spacer(Modifier.height(16.dp))
                SlideToStop { send(RingService.ACTION_DISMISS) }
            }
        }
    }
}

@Composable
private fun NowPlaying(ui: RingState.Ui) {
    Box(
        Modifier.size(200.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text("♪", fontSize = 56.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ui.cover?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        ui.nowPlaying, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
        maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SnoozeButton(detail: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(88.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Snooze", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
