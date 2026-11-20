package fr.clrk.reveil.ui

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val preparing: Map<String, Float> = emptyMap(),
    val update: UpdateNote? = null,
    val youtubeBroken: Boolean = false,
)

/** A newer version found on GitHub. */
data class UpdateNote(val version: String)

/** Tomorrow morning's weather for the list, once a [city] is chosen. */
data class WeatherLine(val city: String? = null, val morning: HalfDay? = null)

interface ListEvents {
    fun add()
    fun open(alarm: Alarm)
    fun toggle(alarm: Alarm, enabled: Boolean)
    fun unskip(alarm: Alarm)
    fun delete(alarm: Alarm)
    fun weather()
    fun update() = Unit
}

/** The crate of records: each alarm is the sleeve of its playlist, its disc out while the alarm is on. */
@Composable
fun AlarmListScreen(ui: ListUi, events: ListEvents) {
    Scaffold(
        containerColor = Night.BG,
        contentColor = Night.CREAM,
        snackbarHost = { ui.announcement?.let { Announcement(it, ui.undo) } },
        floatingActionButton = { if (ui.alarms.isNotEmpty()) AddButton(events::add, Modifier.reveal(row(ui.alarms.size) + 1)) },
    ) { padding ->
        if (ui.alarms.isEmpty()) return@Scaffold EmptyCrate(ui, events, Modifier.padding(padding))
        Crate(ui, events, Modifier.padding(padding))
    }
}

@Composable
private fun Crate(ui: ListUi, events: ListEvents, modifier: Modifier) {
    val opened = remember { SystemClock.uptimeMillis() }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier,
    ) {
        item { Header(ui, events, Modifier.reveal(0, opened)) }
        itemsIndexed(ui.alarms, key = { _, alarm -> alarm.id }) { i, alarm ->
            Box(Modifier.animateItem().reveal(row(i), opened)) {
                val notes = CardNotes(ui.offline[alarm.id], ui.skipped[alarm.id], ui.preparing[alarm.id])
                SwipeToDelete({ events.delete(alarm) }) { SleeveRow(alarm, notes, events) }
            }
        }
        item { WeatherFooter(ui.weather, events::weather, Modifier.reveal(row(ui.alarms.size), opened)) }
    }
}

@Composable
private fun Header(ui: ListUi, events: ListEvents, modifier: Modifier) {
    Column(modifier.padding(top = 14.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Réveils", fontSize = 26.sp, fontWeight = FontWeight.Light)
            Text(
                "${ui.next.title} · ${ui.next.detail}", Modifier.weight(1f).padding(start = 12.dp), color = Night.CAPTION,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
            )
        }
        Notices(ui, events)
    }
}

/** What needs the person's attention, one line each: missing permissions, YouTube, then a newer version. */
@Composable
private fun Notices(ui: ListUi, events: ListEvents) {
    ui.warnings.forEach { (text, fix) -> Warning(text, fix) }
    if (ui.youtubeBroken) Text(YOUTUBE_BROKEN, Modifier.padding(top = 10.dp), color = Night.WARN, fontSize = 13.sp)
    ui.update?.let { NewVersion(it, events::update) }
}

private const val YOUTUBE_BROKEN = "YouTube a changé et votre musique risque de ne pas se lancer. " +
    "Le réveil sonnera quand même avec la sonnerie d'alarme, le temps qu'une mise à jour arrive."

/** One line, like the warnings: a newer version, and one tap to install it. */
@Composable
private fun NewVersion(note: UpdateNote, install: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Nouvelle version ${note.version} disponible", Modifier.weight(1f), color = Night.CAPTION, fontSize = 13.sp)
        TextButton(onClick = install) { Text("Installer", color = Night.EMBER) }
    }
}

@Composable
private fun SleeveRow(alarm: Alarm, notes: CardNotes, events: ListEvents) {
    val flight = LocalFlight.current
    Row(
        Modifier.fillMaxWidth().background(Night.CARD, RoundedCornerShape(14.dp))
            .clickable { openFrom(flight, alarm, events) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val alpha by animateFloatAsState(if (alarm.enabled) 1f else OFF_ALPHA, tween(FADE_MS, easing = Calm.EASE), label = "on")
        val fade = Modifier.alpha(alpha)
        Sleeve(alarm.playlistCover, alarm.enabled, 72.dp, fade, notes.preparing, spot = alarm.id)
        val words = Modifier.alpha(arrival(alarm.id))
        Column(fade.weight(1f).then(words).padding(start = 12.dp)) {
            Text(clock(alarm.time, alarm.enabled), fontSize = 40.sp, fontWeight = FontWeight.Thin, lineHeight = 44.sp)
            DayLetters(alarm.days, Modifier.padding(top = 2.dp))
            val names = listOfNotNull(alarm.label.ifBlank { null }, alarm.playlistTitle).joinToString(" · ")
            if (names.isNotEmpty()) Text(names, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            notes.offline?.let { MusicStatus(it) }
            notes.skipped?.let { Skipped(it) { events.unskip(alarm) } }
        }
        Switch(alarm.enabled, { events.toggle(alarm, it) }, words, colors = SWITCH)
    }
}

/** Opening an alarm sends its cover to the editor, when it has a playlist to show there. */
private fun openFrom(flight: Flight, alarm: Alarm, events: ListEvents) {
    if (alarm.playlistId != null) flight.takeOff(alarm.id, EDITOR, alarm.playlistCover)
    events.open(alarm)
}

/** A new alarm's words wait for its cover to land on the card, then arrive. */
@Composable
private fun arrival(key: String): Float {
    val flight = LocalFlight.current
    val born = remember { flight.landing(key) }
    val hidden = born && flight.landing(key)
    val shown by animateFloatAsState(if (hidden) 0f else 1f, tween(Calm.ARRIVE_MS, easing = Calm.EASE), label = "words")
    return shown
}

private val SWITCH
    @Composable get() = SwitchDefaults.colors(
        checkedThumbColor = Night.EMBER, checkedTrackColor = Night.EMBER.copy(alpha = 0.3f),
        checkedBorderColor = Night.EMBER.copy(alpha = 0f),
        uncheckedThumbColor = Night.DIM, uncheckedTrackColor = Night.LINE, uncheckedBorderColor = Night.DIM,
    )

/** No alarm yet: the turntable waits, with one clear way to set the first alarm. */
@Composable
private fun EmptyCrate(ui: ListUi, events: ListEvents, modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()), Arrangement.SpaceBetween) {
        WaitingTurntable(Modifier.fillMaxWidth().height(340.dp).reveal(0))
        Column(Modifier.padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Notices(ui, events)
            Text("La platine attend.", Modifier.reveal(1), fontSize = 30.sp, fontWeight = FontWeight.ExtraLight)
            Text(
                "Aucun réveil prévu. Les chansons sont gardées sur le téléphone, le réveil sonne même hors ligne.",
                Modifier.padding(top = 10.dp).reveal(2), color = Night.CAPTION, fontSize = 15.sp, textAlign = TextAlign.Center,
            )
            WeatherFooter(ui.weather, events::weather, Modifier.padding(top = 14.dp).reveal(order = 3))
            Cta("Programmer un réveil", events::add, Modifier.padding(top = 18.dp, bottom = 28.dp).reveal(order = 4))
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier.padding(4.dp).background(Night.FROST, androidx.compose.foundation.shape.CircleShape)
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
private data class CardNotes(val offline: String?, val skipped: String?, val preparing: Float?)

@Composable
private fun Skipped(text: String, undo: () -> Unit) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(text, fontSize = 12.sp, color = Night.CAPTION)
        Text("Annuler", Modifier.clickable(onClick = undo).padding(vertical = 6.dp), color = Night.EMBER, fontSize = 13.sp)
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
private const val FADE_MS = 450
