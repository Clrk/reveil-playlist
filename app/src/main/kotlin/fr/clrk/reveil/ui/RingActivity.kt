package fr.clrk.reveil.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.clrk.reveil.alarm.RingService
import fr.clrk.reveil.alarm.RingState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Full-screen ringing screen, shown above the lock screen. */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = reveilColors()) {
                val ui by RingState.ui.collectAsStateWithLifecycle()
                LaunchedEffect(ui) { if (ui == null) finish() }
                ui?.let { RingScreen(it, ::send) }
            }
        }
    }

    private fun send(action: String) {
        RingService.send(this, action)
        finish()
    }
}

@Composable
private fun RingScreen(ui: RingState.Ui, send: (String) -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 72.sp)
            Text(ui.label.ifBlank { "Réveil" }, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text("♪ ${ui.nowPlaying}", textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalButton(onClick = { send(RingService.ACTION_SNOOZE) }) { Text("Snooze", fontSize = 20.sp) }
                Button(onClick = { send(RingService.ACTION_DISMISS) }) { Text("Arrêter", fontSize = 20.sp) }
            }
        }
    }
}
