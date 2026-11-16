package fr.clrk.reveil.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import fr.clrk.reveil.alarm.RingService
import fr.clrk.reveil.app
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.music.PrefetchScheduler
import java.time.Duration
import java.time.LocalTime
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
        editing = Alarm.at(LocalTime.of(DEFAULT_HOUR, 0)).copy(label = "")
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

    private val listEvents = object : ListEvents {
        override fun add() { editing = Alarm.at(LocalTime.of(DEFAULT_HOUR, 0)) }
        override fun open(alarm: Alarm) { editing = alarm }
        override fun toggle(alarm: Alarm, enabled: Boolean) { app.alarms.setEnabled(alarm.id, enabled); changed() }
    }

    @Composable
    private fun Screen() {
        AlarmListScreen(ListUi(alarms, nextText(), permissionWarnings()), listEvents)
        editing?.let { AlarmEditor(it, sharedLink, actions) }
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
        if (!getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)) {
            warnings += "L'économie de batterie peut empêcher de préparer les musiques hors ligne" to ::askBatteryExemption
        }
        return warnings
    }

    /** Alarm clocks are an accepted use of this request: without it, the overnight download can be frozen. */
    @SuppressLint("BatteryLife")
    private fun askBatteryExemption() =
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:$packageName".toUri()))

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
