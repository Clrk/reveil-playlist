package fr.clrk.reveil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.HalfDay
import fr.clrk.reveil.core.WakeInfo

/** Everything the alarm list displays, computed by the activity. [offline] maps alarm ids to their offline status. */
data class ListUi(
    val alarms: List<Alarm>,
    val next: WakeInfo.NextCard,
    val warnings: List<Pair<String, () -> Unit>> = emptyList(),
    val offline: Map<String, String> = emptyMap(),
    val announcement: String? = null,
    val skipped: Map<String, String> = emptyMap(),
    val undo: (() -> Unit)? = null,
    val weather: WeatherLine = WeatherLine(),
)

/** Tomorrow morning's weather for the list, once a [city] is chosen. */
data class WeatherLine(val city: String? = null, val morning: HalfDay? = null)

interface ListEvents {
    fun add()
    fun open(alarm: Alarm)
    fun toggle(alarm: Alarm, enabled: Boolean)
    fun unskip(alarm: Alarm)
    fun delete(alarm: Alarm)
    fun weather()
}

/** The crate of records: each alarm is the sleeve of its playlist, its disc out while the alarm is on. */
@Composable
fun AlarmListScreen(ui: ListUi, events: ListEvents) {
    Scaffold(
        containerColor = Night.BG,
        contentColor = Night.CREAM,
        snackbarHost = { ui.announcement?.let { Announcement(it, ui.undo) } },
        floatingActionButton = { if (ui.alarms.isNotEmpty()) AddButton(events::add) },
    ) { padding ->
        if (ui.alarms.isEmpty()) return@Scaffold EmptyCrate(ui, events, Modifier.padding(padding))
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(padding),
        ) {
            item { Header(ui) }
            items(ui.alarms, key = { it.id }) {
                SwipeToDelete({ events.delete(it) }) { SleeveRow(it, CardNotes(ui.offline[it.id], ui.skipped[it.id]), events) }
            }
            item { WeatherFooter(ui.weather, events::weather) }
        }
    }
}

@Composable
private fun Header(ui: ListUi) {
    Column(Modifier.statusBarsPadding().padding(top = 18.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Réveils", Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.Light)
            Text("${ui.next.title} · ${ui.next.detail}", color = Night.CAPTION, fontSize = 13.sp, maxLines = 1)
        }
        ui.warnings.forEach { (text, fix) -> Warning(text, fix) }
    }
}

@Composable
private fun SleeveRow(alarm: Alarm, notes: CardNotes, events: ListEvents) {
    Row(
        Modifier.fillMaxWidth().background(Night.CARD, RoundedCornerShape(14.dp)).padding(12.dp)
            .alpha(if (alarm.enabled) 1f else OFF_ALPHA),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Sleeve(alarm.playlistCover, alarm.enabled, 86.dp, Modifier.clickable { events.toggle(alarm, !alarm.enabled) })
        Column(Modifier.weight(1f).padding(start = 14.dp).clickable { events.open(alarm) }) {
            Text(clock(alarm.time, alarm.enabled), fontSize = 40.sp, fontWeight = FontWeight.Thin, lineHeight = 44.sp)
            DayLetters(alarm.days, Modifier.padding(top = 2.dp))
            val names = listOfNotNull(alarm.label.ifBlank { null }, alarm.playlistTitle).joinToString(" · ")
            if (names.isNotEmpty()) Text(names, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            notes.offline?.let { MusicStatus(it) }
            notes.skipped?.let { Skipped(it) { events.unskip(alarm) } }
        }
    }
}

/** No alarm yet: the turntable waits, with one clear way to set the first alarm. */
@Composable
private fun EmptyCrate(ui: ListUi, events: ListEvents, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        WaitingTurntable(Modifier.fillMaxWidth().height(340.dp))
        Spacer(Modifier.weight(1f))
        Column(Modifier.padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ui.warnings.forEach { (text, fix) -> Warning(text, fix) }
            Text("La platine attend.", fontSize = 30.sp, fontWeight = FontWeight.ExtraLight)
            Text(
                "Aucun réveil prévu. Choisissez une heure et une playlist, le reste se fait la veille.",
                Modifier.padding(top = 10.dp), color = Night.CAPTION, fontSize = 15.sp, textAlign = TextAlign.Center,
            )
            WeatherFooter(ui.weather, events::weather, Modifier.padding(top = 14.dp))
            Cta("Programmer un réveil", events::add, Modifier.padding(top = 18.dp, bottom = 28.dp))
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    Box(
        Modifier.padding(4.dp).background(Night.FROST, androidx.compose.foundation.shape.CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
    ) { Text("+", color = Night.EMBER, fontSize = 32.sp, fontWeight = FontWeight.ExtraLight) }
}

@Composable
private fun Warning(text: String, fix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), color = Night.WARN, fontSize = 13.sp)
        TextButton(onClick = fix) { Text("Autoriser", color = Night.EMBER) }
    }
}

/** Secondary lines of an alarm: offline readiness and a skipped occurrence. */
private data class CardNotes(val offline: String?, val skipped: String?)

@Composable
private fun Skipped(text: String, undo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 12.sp, color = Night.CAPTION)
        TextButton(onClick = undo) { Text("Annuler", color = Night.EMBER) }
    }
}

@Composable
private fun Announcement(text: String, undo: (() -> Unit)?) {
    Snackbar(
        Modifier.padding(16.dp), containerColor = Night.CARD, contentColor = Night.CREAM,
        action = undo?.let { { TextButton(onClick = it) { Text("Annuler", color = Night.EMBER) } } },
    ) { Text(text) }
}

/** Swiping a sleeve to the left deletes the alarm; the snackbar then offers to undo. */
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() },
        backgroundContent = {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.CenterEnd) { Text("Supprimer", color = Night.WARN) }
        },
    ) { content() }
}

/** Whether the alarm's music is ready: ember once it is, discreet while it is still being prepared. */
@Composable
internal fun MusicStatus(text: String, modifier: Modifier = Modifier) {
    val ready = text.startsWith(WakeInfo.READY)
    Text(text, modifier, fontSize = 12.sp, color = if (ready) Night.EMBER else Night.CAPTION)
}

private const val OFF_ALPHA = 0.45f
