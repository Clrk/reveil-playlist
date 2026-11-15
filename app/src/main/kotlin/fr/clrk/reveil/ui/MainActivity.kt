package fr.clrk.reveil.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import fr.clrk.reveil.alarm.RingService
import fr.clrk.reveil.app
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.music.PrefetchScheduler
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Alarm list: create, edit, toggle; also receives playlist links shared from YouTube Music. */
class MainActivity : ComponentActivity() {
    private var alarms by mutableStateOf(emptyList<Alarm>())
    private var editing by mutableStateOf<Alarm?>(null)
    private var sharedLink by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotifications()
        readShare(intent)
        refresh()
        setContent { MaterialTheme(colorScheme = reveilColors()) { Screen() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readShare(intent)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun readShare(intent: Intent?) {
        val text = intent?.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT) ?: return
        sharedLink = text
        editing = Alarm.at(java.time.LocalTime.of(DEFAULT_HOUR, 0)).copy(label = "")
    }

    private fun refresh() { alarms = app.alarms.list() }

    private fun changed() {
        refresh()
        app.alarmsChanged()
    }

    private val actions = object : EditorActions {
        override fun save(alarm: Alarm) { app.alarms.update(alarm); editing = null; changed() }
        override fun delete(alarm: Alarm) { app.alarms.delete(alarm.id); editing = null; changed() }
        override fun test(alarm: Alarm) { app.alarms.update(alarm); RingService.start(this@MainActivity, alarm.id) }
        override fun prefetch(playlist: String) = PrefetchScheduler.now(this@MainActivity, playlist).let { }
        override fun close() { editing = null; sharedLink = "" }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen() {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Réveils") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { editing = Alarm.at(java.time.LocalTime.of(DEFAULT_HOUR, 0)) }) {
                    Icon(Icons.Default.Add, "Nouvelle alarme")
                }
            },
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding),
            ) {
                item { Header() }
                items(alarms, key = { it.id }) { AlarmCard(it) }
            }
        }
        editing?.let { AlarmEditor(it, sharedLink, actions) }
    }

    @Composable
    private fun Header() {
        Column {
            Text(nextText(), style = MaterialTheme.typography.titleMedium)
            permissionWarnings().forEach { (text, fix) ->
                TextButton(onClick = fix) { Text("⚠ $text", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    @Composable
    private fun AlarmCard(alarm: Alarm) {
        Card(Modifier.fillMaxWidth().clickable { editing = alarm }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(alarm.time.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 40.sp)
                    Text(listOf(alarm.label, Days.summary(alarm.days)).filter { it.isNotBlank() }.joinToString(" · "))
                }
                if (alarm.playlistId != null) Text("♪", fontSize = 24.sp)
                Switch(checked = alarm.enabled, onCheckedChange = { app.alarms.setEnabled(alarm.id, it); changed() })
            }
        }
    }

    private fun nextText(): String {
        val (alarm, at) = app.alarms.next() ?: return "Aucun réveil prévu"
        val left = Duration.between(ZonedDateTime.now(), at)
        val day = at.format(DateTimeFormatter.ofPattern("EEE HH:mm", Locale.FRENCH))
        return "Prochain réveil : $day (dans ${left.toHours()} h ${left.toMinutesPart()} min) ${alarm.label}".trim()
    }

    private fun permissionWarnings(): List<Pair<String, () -> Unit>> {
        val warnings = mutableListOf<Pair<String, () -> Unit>>()
        val notifications = getSystemService(NotificationManager::class.java)
        if (!notifications.areNotificationsEnabled()) {
            warnings += "Notifications désactivées : le réveil ne pourra pas s'afficher" to
                { openSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !notifications.canUseFullScreenIntent()) {
            warnings += "Autoriser l'affichage plein écran sur l'écran verrouillé" to
                { openSettings(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT) }
        }
        return warnings
    }

    private fun openSettings(action: String) {
        val intent = Intent(action).setData("package:$packageName".toUri())
        startActivity(intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
            .launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val DEFAULT_HOUR = 7
    }
}
