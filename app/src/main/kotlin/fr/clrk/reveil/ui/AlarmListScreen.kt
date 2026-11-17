package fr.clrk.reveil.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.WakeInfo
import java.time.format.DateTimeFormatter

/** Everything the alarm list displays, computed by the activity. [offline] maps alarm ids to their offline status. */
data class ListUi(
    val alarms: List<Alarm>,
    val next: WakeInfo.NextCard,
    val warnings: List<Pair<String, () -> Unit>> = emptyList(),
    val offline: Map<String, String> = emptyMap(),
    val announcement: String? = null,
)

interface ListEvents {
    fun add()
    fun open(alarm: Alarm)
    fun toggle(alarm: Alarm, enabled: Boolean)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(ui: ListUi, events: ListEvents) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Réveils") }) },
        snackbarHost = { ui.announcement?.let { Snackbar(Modifier.padding(16.dp)) { Text(it) } } },
        floatingActionButton = {
            FloatingActionButton(onClick = events::add) { Icon(Icons.Default.Add, "Nouvelle alarme") }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding),
        ) {
            item { Header(ui) }
            items(ui.alarms, key = { it.id }) { AlarmCard(it, ui.offline[it.id], events) }
        }
    }
}

@Composable
private fun Header(ui: ListUi) {
    Column {
        ui.warnings.forEach { (text, fix) -> WarningCard(text, fix) }
        NextCard(ui.next)
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, offline: String?, events: ListEvents) {
    Card(Modifier.fillMaxWidth().clickable { events.open(alarm) }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(alarm.time.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 40.sp)
                Text(listOf(alarm.label, Days.summary(alarm.days)).filter { it.isNotBlank() }.joinToString(" · "))
                alarm.playlistTitle?.let {
                    Text("♪ $it", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                offline?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
            Switch(checked = alarm.enabled, onCheckedChange = { events.toggle(alarm, it) })
        }
    }
}

@Composable
private fun WarningCard(text: String, fix: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = fix) { Text("Autoriser") }
        }
    }
}

@Composable
private fun NextCard(card: WakeInfo.NextCard) {
    Card(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("PROCHAIN RÉVEIL", style = MaterialTheme.typography.labelMedium)
            Text(card.title, style = MaterialTheme.typography.headlineMedium)
            Text(card.detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
