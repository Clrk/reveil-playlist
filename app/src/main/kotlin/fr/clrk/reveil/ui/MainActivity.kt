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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import fr.clrk.reveil.alarm.RingService
import fr.clrk.reveil.app
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.WakeInfo
import fr.clrk.reveil.music.TrackCache
import kotlinx.coroutines.delay
import fr.clrk.reveil.music.PrefetchScheduler
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Alarm list: create, edit, toggle; also receives playlist links shared from YouTube Music. */
class MainActivity : ComponentActivity() {
    private var alarms by mutableStateOf(emptyList<Alarm>())
    private var editing by mutableStateOf<Alarm?>(null)
    private var sharedLink by mutableStateOf("")
    private var announcement by mutableStateOf<String?>(null)

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
        override fun save(alarm: Alarm) { app.alarms.update(alarm); editing = null; changed(); announce(alarm) }
        override fun delete(alarm: Alarm) { app.alarms.delete(alarm.id); editing = null; changed() }
        override fun test(alarm: Alarm) { app.alarms.update(alarm); RingService.start(this@MainActivity, alarm.id) }
        override fun prefetch(playlist: String) = PrefetchScheduler.now(this@MainActivity, playlist).let { }
        override fun skip(alarm: Alarm) { app.alarms.skipNext(alarm.id); editing = null; changed() }
        override fun close() { editing = null; sharedLink = "" }
    }

    private val listEvents = object : ListEvents {
        override fun add() { editing = Alarm.at(LocalTime.of(DEFAULT_HOUR, 0)) }
        override fun open(alarm: Alarm) { editing = alarm }
        override fun unskip(alarm: Alarm) { app.alarms.unskip(alarm.id); changed() }
        override fun toggle(alarm: Alarm, enabled: Boolean) {
            app.alarms.setEnabled(alarm.id, enabled)
            changed()
            announce(alarm.copy(enabled = enabled))
        }
    }

    @Composable
    private fun Screen() {
        val next = WakeInfo.next(app.alarms.next(), ZonedDateTime.now())
        val skipped = alarms.mapNotNull { a -> WakeInfo.skipped(a, ZoneId.systemDefault())?.let { a.id to it } }.toMap()
        AlarmListScreen(ListUi(alarms, next, permissionWarnings(), offline(), announcement, skipped), listEvents)
        LaunchedEffect(announcement) {
            delay(ANNOUNCE_MS)
            announcement = null
        }
        editing?.let { edited -> AlarmEditor(edited, sharedLink, actions, existing = alarms.any { it.id == edited.id }) }
    }

    private fun announce(alarm: Alarm) { announcement = WakeInfo.announce(alarm, ZonedDateTime.now()) }

    private fun offline(): Map<String, String> = alarms.mapNotNull { alarm ->
        val cached = alarm.playlistId?.let { TrackCache(this, PlaylistId(it)).tracks().size } ?: 0
        WakeInfo.offline(alarm, cached)?.let { alarm.id to it }
    }.toMap()

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
        const val ANNOUNCE_MS = 4_000L
    }
}
