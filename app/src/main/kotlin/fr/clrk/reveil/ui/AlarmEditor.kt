package fr.clrk.reveil.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
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
import fr.clrk.reveil.core.PlaylistLink
import java.time.DayOfWeek
import java.time.LocalTime

interface EditorActions {
    fun save(alarm: Alarm)
    fun delete(alarm: Alarm)
    fun test(alarm: Alarm)
    fun prefetch(playlist: String)
    fun close()
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

private const val SNOOZE_CHOICES = "5,9,10,15,20"

@OptIn(ExperimentalMaterial3Api::class)
private class EditorState(private val initial: Alarm, sharedLink: String, val time: TimePickerState) {
    var days by mutableStateOf(initial.days)
    var label by mutableStateOf(initial.label)
    var link by mutableStateOf(sharedLink.ifBlank { initial.playlistId.orEmpty() })
    var snooze by mutableStateOf(initial.snoozeMinutes)
    val playlist get() = PlaylistLink.parse(link)
    val linkError get() = link.isNotBlank() && playlist == null

    fun alarm() = initial.copy(
        time = LocalTime.of(time.hour, time.minute), days = days, label = label,
        playlistId = playlist?.value, snoozeMinutes = snooze, enabled = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditor(initial: Alarm, sharedLink: String, actions: EditorActions) {
    val time = rememberTimePickerState(initial.time.hour, initial.time.minute, is24Hour = true)
    val state = remember(initial) { EditorState(initial, sharedLink, time) }
    AlertDialog(
        onDismissRequest = actions::close,
        confirmButton = { TextButton(enabled = !state.linkError, onClick = { actions.save(state.alarm()) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = { actions.delete(initial) }) { Text("Supprimer") } },
        text = { EditorForm(state, actions) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorForm(state: EditorState, actions: EditorActions) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeInput(state.time)
        DayChips(state.days) { state.days = it }
        OutlinedTextField(state.label, { state.label = it }, label = { Text("Nom") }, singleLine = true)
        OutlinedTextField(
            state.link, { state.link = it }, label = { Text("Lien de playlist YouTube Music") },
            isError = state.linkError, supportingText = { if (state.linkError) Text("Lien de playlist non reconnu") },
        )
        SnoozeChips(state)
        Row {
            TextButton(enabled = !state.linkError, onClick = { actions.test(state.alarm()) }) { Text("Tester") }
            state.playlist?.let { TextButton(onClick = { actions.prefetch(it.value) }) { Text("Télécharger maintenant") } }
        }
    }
}

@Composable
private fun SnoozeChips(state: EditorState) {
    Text("Snooze (minutes)")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SNOOZE_CHOICES.split(",").map(String::toInt).forEach { minutes ->
            FilterChip(state.snooze == minutes, { state.snooze = minutes }, label = { Text("$minutes") })
        }
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
