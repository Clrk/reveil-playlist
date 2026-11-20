package fr.clrk.reveil.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import fr.clrk.reveil.core.WakeInfo
import java.time.ZonedDateTime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.PlaylistChoice
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.PlaylistLink
import java.time.DayOfWeek
import java.time.LocalTime

interface EditorActions {
    fun save(alarm: Alarm)
    fun delete(alarm: Alarm)
    fun test(alarm: Alarm)
    fun prefetch(playlist: String)
    fun skip(alarm: Alarm)
    fun close()
}

/** Saving, deleting or closing lets the editor ease out first. */
private class Leaving(private val inner: EditorActions, private val leave: Leave, private val flight: Flight) : EditorActions by inner {
    override fun save(alarm: Alarm) {
        flight.takeOff(EDITOR, alarm.id, alarm.playlistCover)
        leave.then { inner.save(alarm) }
    }
    override fun delete(alarm: Alarm) = leave.then { inner.delete(alarm) }
    override fun close() = leave.then(inner::close)
}

object Days {
    private val letters = listOf("L", "M", "M", "J", "V", "S", "D")
    val all = DayOfWeek.entries.zip(letters)

    fun summary(days: Set<DayOfWeek>): String = when (days) {
        emptySet<DayOfWeek>() -> "Une fois"
        DayOfWeek.entries.toSet() -> "Tous les jours"
        else -> all.filter { it.first in days }.joinToString(" ") { it.second }
    }
}

private val SNOOZE_CHOICES = listOf(5, 9, 10, 15, 20).map { it to "$it" }
private val MAX_SNOOZE_CHOICES = listOf(1, 2, 3, 5).map { it to "$it" } + (null to "∞")
private val RAMP_CHOICES = listOf(0 to "Non", 30 to "30 s", 60 to "1 min", 120 to "2 min", 300 to "5 min")

@OptIn(ExperimentalMaterial3Api::class)
private class EditorState(private val initial: Alarm, sharedLink: String, val time: TimePickerState) {
    var days by mutableStateOf(initial.days)
    var label by mutableStateOf(initial.label)
    var playlist by mutableStateOf(initialChoice(initial, sharedLink))
    var picking by mutableStateOf(false)
    var snooze by mutableStateOf(initial.snoozeMinutes)
    var maxSnoozes by mutableStateOf(initial.maxSnoozes)
    var ramp by mutableStateOf(initial.rampSeconds)

    fun setTime(value: LocalTime) {
        time.hour = value.hour
        time.minute = value.minute
    }

    fun alarm() = initial.copy(
        time = LocalTime.of(time.hour, time.minute), days = days, label = label,
        playlistId = playlist?.id?.value, playlistTitle = playlist?.title,
        playlistCover = playlist?.thumbnail, snoozeMinutes = snooze, enabled = true,
        maxSnoozes = maxSnoozes, rampSeconds = ramp,
    )
}

/** Full-screen alarm editor; [existing] enables the actions that only make sense on a saved alarm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditor(
    initial: Alarm,
    sharedLink: String,
    actions: EditorActions,
    existing: Boolean = false,
    now: () -> ZonedDateTime = ZonedDateTime::now,
    music: (String) -> String? = { null },
) {
    val time = rememberTimePickerState(initial.time.hour, initial.time.minute, is24Hour = true)
    val state = remember(initial) { EditorState(initial, sharedLink, time) }
    val leave = rememberLeave()
    val flight = LocalFlight.current
    val gentle = remember(actions) { Leaving(actions, leave, flight) }
    Layer(gentle::close) {
        Appear(leave) {
            EditorScaffold(if (existing) "Modifier le réveil" else "Nouveau réveil", state, gentle) {
                EditorForm(state, gentle, now, music)
                if (existing) SavedAlarmActions(initial, gentle)
            }
        }
    }
    if (state.picking) {
        PlaylistPicker(EDITOR, onPick = { state.playlist = it }, onClose = { state.picking = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScaffold(title: String, state: EditorState, actions: EditorActions, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = actions::close) { Icon(Icons.Default.Close, "Fermer") } },
                actions = { TextButton(onClick = { actions.save(state.alarm()) }) { Text("Enregistrer") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorForm(state: EditorState, actions: EditorActions, now: () -> ZonedDateTime, music: (String) -> String?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TimeWheel(LocalTime.of(state.time.hour, state.time.minute), state::setTime, Modifier.fillMaxWidth())
        Text(WakeInfo.announce(state.alarm(), now()).orEmpty(), color = MaterialTheme.colorScheme.primary)
    }
    DayChips(state.days) { state.days = it }
    OutlinedTextField(state.label, { state.label = it }, Modifier.fillMaxWidth(), label = { Text("Nom") }, singleLine = true)
    PlaylistField(state, music, actions)
    Choices("Montée du volume", RAMP_CHOICES, state.ramp) { state.ramp = it }
    Choices("Snooze (minutes)", SNOOZE_CHOICES, state.snooze) { state.snooze = it }
    Choices("Snoozes au maximum", MAX_SNOOZE_CHOICES, state.maxSnoozes) { state.maxSnoozes = it }
    TextButton(onClick = { actions.test(state.alarm()) }) { Text("Tester le réveil") }
}

@Composable
private fun SavedAlarmActions(alarm: Alarm, actions: EditorActions) {
    HorizontalDivider()
    if (alarm.enabled && alarm.repeating && alarm.skipped == null) {
        TextButton(onClick = { actions.skip(alarm) }) { Text("Sauter le prochain réveil") }
    }
    TextButton(onClick = { actions.delete(alarm) }) { Text("Supprimer le réveil", color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun PlaylistField(state: EditorState, music: (String) -> String?, actions: EditorActions) {
    Text("Musique du réveil")
    OutlinedCard(Modifier.fillMaxWidth().clickable { state.picking = true }) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            val choice = state.playlist
            if (choice == null) {
                Text("Sonnerie du téléphone", Modifier.padding(vertical = 16.dp))
            } else {
                MusicSummary(choice, music(choice.id.value))
            }
        }
    }
    FlowRow {
        TextButton(onClick = { state.picking = true }) { Text(if (state.playlist == null) "Choisir une playlist" else "Changer") }
        state.playlist?.let { choice ->
            TextButton(onClick = { state.playlist = null }) { Text("Retirer") }
            TextButton(onClick = { actions.prefetch(choice.id.value) }) { Text("Préparer maintenant") }
        }
    }
}

@Composable
private fun MusicSummary(choice: PlaylistChoice, status: String?) {
    PlaylistRow(choice, cover = Modifier.spot(EDITOR))
    status?.let { MusicStatus(it, Modifier.padding(bottom = 12.dp)) }
}

private fun initialChoice(alarm: Alarm, sharedLink: String): PlaylistChoice? {
    PlaylistLink.parse(sharedLink)?.let { return PlaylistChoice(it, "Playlist partagée") }
    val id = alarm.playlistId?.let(::PlaylistId) ?: return null
    return PlaylistChoice(id, alarm.playlistTitle ?: "Playlist ${id.value}", thumbnail = alarm.playlistCover)
}

@Composable
private fun <T> Choices(title: String, choices: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    Text(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        choices.forEach { (value, label) -> FilterChip(selected == value, { onPick(value) }, label = { Text(label) }) }
    }
}

@Composable
private fun DayChips(days: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Days.all.forEach { (day, letter) ->
            FilterChip(day in days, { onChange(if (day in days) days - day else days + day) }, label = { Text(letter) })
        }
    }
}

/** Where the cover sits in the full editor. */
const val EDITOR = "editor"
