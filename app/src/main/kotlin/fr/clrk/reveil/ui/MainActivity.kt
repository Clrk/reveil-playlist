package fr.clrk.reveil.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.compose.runtime.setValue
import fr.clrk.reveil.alarm.RingService
import fr.clrk.reveil.app
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.WakeInfo
import fr.clrk.reveil.music.TrackCache
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private var download by mutableStateOf<WakeInfo.Download?>(null)
    private var undo by mutableStateOf<(() -> Unit)?>(null)
    private var resumed by mutableIntStateOf(0)
    private val prefs by lazy { getSharedPreferences("ui", MODE_PRIVATE) }
    private var onboarded by mutableStateOf(true)
    private val permissions by lazy { Permissions(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotifications()
        onboarded = prefs.getBoolean(ONBOARDED, false)
        readShare(intent)
        refresh()
        lifecycleScope.launch {
            PrefetchScheduler.nowState(this@MainActivity).collect { download = it; refresh() }
        }
        setContent { MaterialTheme(colorScheme = reveilColors()) { Screen() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readShare(intent)
    }

    override fun onResume() {
        super.onResume()
        resumed++
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
        override fun delete(alarm: Alarm) { editing = null; deleteWithUndo(alarm) }
        override fun test(alarm: Alarm) { app.alarms.update(alarm); RingService.start(this@MainActivity, alarm.id) }
        override fun prefetch(playlist: String) = PrefetchScheduler.now(this@MainActivity, playlist).let { }
        override fun skip(alarm: Alarm) { app.alarms.skipNext(alarm.id); editing = null; changed() }
        override fun close() { editing = null; sharedLink = "" }
    }

    private val listEvents = object : ListEvents {
        override fun add() { editing = Alarm.at(LocalTime.of(DEFAULT_HOUR, 0)) }
        override fun open(alarm: Alarm) { editing = alarm }
        override fun unskip(alarm: Alarm) { app.alarms.unskip(alarm.id); changed() }
        override fun delete(alarm: Alarm) = deleteWithUndo(alarm)
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
        resumed.let { }
        if (!onboarded) return OnboardingScreen(permissions.steps()) { finishOnboarding() }
        AlarmListScreen(ListUi(alarms, next, permissions.warnings(), offline(), announcement, skipped, undo), listEvents)
        LaunchedEffect(announcement) {
            delay(ANNOUNCE_MS)
            announcement = null
            undo = null
        }
        editing?.let { edited ->
            AlarmEditor(edited, sharedLink, actions, existing = alarms.any { it.id == edited.id }, music = ::musicStatus)
        }
    }

    private fun finishOnboarding() {
        prefs.edit { putBoolean(ONBOARDED, true) }
        onboarded = true
    }

    private fun deleteWithUndo(alarm: Alarm) {
        app.alarms.delete(alarm.id)
        changed()
        announcement = "Réveil supprimé"
        undo = { app.alarms.update(alarm); changed(); announcement = null; undo = null }
    }

    private fun announce(alarm: Alarm) { announcement = WakeInfo.announce(alarm, ZonedDateTime.now()) }

    /** Progress of a hand-started download, otherwise how many tracks are ready offline. */
    private fun musicStatus(playlist: String): String =
        download?.let(WakeInfo::download) ?: WakeInfo.offline(TrackCache(this, PlaylistId(playlist)).tracks().size)

    private fun offline(): Map<String, String> = alarms.mapNotNull { alarm ->
        val cached = alarm.playlistId?.let { TrackCache(this, PlaylistId(it)).tracks().size } ?: 0
        WakeInfo.offline(alarm, cached)?.let { alarm.id to it }
    }.toMap()

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
            .launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val DEFAULT_HOUR = 7
        const val ANNOUNCE_MS = 4_000L
        const val ONBOARDED = "onboarded"
    }
}
