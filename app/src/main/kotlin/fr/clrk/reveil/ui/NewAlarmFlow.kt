package fr.clrk.reveil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.PlaylistChoice
import fr.clrk.reveil.core.Recurrence
import fr.clrk.reveil.core.WakeInfo
import java.time.DayOfWeek
import java.time.ZonedDateTime

/** The creation steps: three questions, one per screen, then a summary. */
enum class Step { TIME, DAYS, PLAYLIST, SUMMARY }

/** What the new alarm screens can ask for. */
interface FlowEvents {
    fun change(alarm: Alarm)
    fun go(step: Step)
    fun more(alarm: Alarm)
    fun save(alarm: Alarm)
    fun close()
}

/** New alarm: hour, days, playlist, then a summary that sets it; the fine settings keep their defaults. */
@Composable
fun NewAlarmFlow(initial: Alarm, onSave: (Alarm) -> Unit, onMore: (Alarm) -> Unit, onClose: () -> Unit) {
    var current by remember(initial) { mutableStateOf(initial) }
    var screen by remember(initial) { mutableStateOf(Step.TIME) }
    var picking by remember(initial) { mutableStateOf(false) }
    val leave = rememberLeave()
    val flight = LocalFlight.current
    val events = object : FlowEvents {
        override fun change(alarm: Alarm) { current = alarm }
        override fun go(step: Step) { if (step == Step.PLAYLIST) picking = true else screen = step }
        override fun more(alarm: Alarm) = onMore(alarm)
        override fun save(alarm: Alarm) {
            flight.takeOff(DRAFT, alarm.id, alarm.playlistCover)
            leave.then { onSave(alarm) }
        }
        override fun close() = leave.then(onClose)
    }
    Layer(events::close) {
        Appear(leave) { FlowScreen(current, screen, events) }
    }
    if (picking) {
        val pick = { choice: PlaylistChoice ->
            current = current.copy(playlistId = choice.id.value, playlistTitle = choice.title, playlistCover = choice.thumbnail)
            screen = Step.SUMMARY
        }
        PlaylistPicker(DRAFT, onPick = pick, onClose = { picking = false })
    }
}

@Composable
fun FlowScreen(alarm: Alarm, step: Step, events: FlowEvents, now: ZonedDateTime = ZonedDateTime.now()) {
    NightPage { FlowContent(alarm, step, events, now) }
}

@Composable
private fun FlowContent(alarm: Alarm, step: Step, events: FlowEvents, now: ZonedDateTime) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
        BackButton(step, events)
        Turn(step, { from, to -> to > from }) { shown ->
            Column(Modifier.fillMaxSize()) {
                when (shown) {
                    Step.TIME -> TimeStep(alarm, events, now)
                    Step.DAYS -> DaysStep(alarm, events, now)
                    else -> SummaryStep(alarm, events)
                }
            }
        }
    }
}

/** ✕ closes on the first question, ← goes back one step after it. */
@Composable
private fun BackButton(step: Step, events: FlowEvents) {
    val first = step == Step.TIME
    val back = { if (first) events.close() else events.go(Step.entries[step.ordinal - 1]) }
    Text(if (first) "✕" else "←", Modifier.padding(vertical = 16.dp).clickable(onClick = back), fontSize = 20.sp)
}

@Composable
private fun Progress(done: Int) {
    Row(Modifier.fillMaxWidth().reveal(0), horizontalArrangement = Arrangement.Center) {
        repeat(QUESTIONS) { i ->
            val color = if (i < done) Night.EMBER else Night.LINE
            Box(Modifier.padding(3.dp).width(22.dp).height(3.dp).background(color, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun Question(text: String) =
    Text(
        text, Modifier.fillMaxWidth().padding(top = 26.dp).reveal(1),
        fontSize = 28.sp, fontWeight = FontWeight.ExtraLight, textAlign = TextAlign.Center,
    )

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.TimeStep(alarm: Alarm, events: FlowEvents, now: ZonedDateTime) {
    Progress(1)
    Question("À quelle heure ?")
    TimeWheel(alarm.time, { events.change(alarm.copy(time = it)) }, Modifier.fillMaxWidth().padding(top = 20.dp).reveal(2))
    Countdown(alarm, now, order = 3)
    Spacer(Modifier.weight(1f))
    Cta("Suivant", { events.go(Step.DAYS) }, Modifier.padding(bottom = 24.dp).reveal(order = 4))
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DaysStep(alarm: Alarm, events: FlowEvents, now: ZonedDateTime) {
    Progress(2)
    Question("Quels jours ?")
    Row(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEachIndexed { i, day ->
            val toggle = { events.change(alarm.copy(days = Recurrence.toggle(alarm.days, day))) }
            DayDot(day, day in alarm.days, order = 2 + i / DOTS_PER_BEAT, toggle)
        }
    }
    Shortcuts(alarm.days) { events.change(alarm.copy(days = it)) }
    Countdown(alarm, now, order = 6)
    Spacer(Modifier.weight(1f))
    Cta("Suivant", { events.go(Step.PLAYLIST) }, Modifier.padding(bottom = 24.dp).reveal(order = 7))
}

@Composable
private fun DayDot(day: DayOfWeek, on: Boolean, order: Int, onClick: () -> Unit) {
    Box(
        Modifier.reveal(order).size(40.dp).background(if (on) Night.EMBER else Night.FROST, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(Recurrence.letter(day), color = if (on) Night.ON_EMBER else Night.CAPTION, fontSize = 14.sp) }
}

@Composable
private fun Shortcuts(days: Set<DayOfWeek>, onPick: (Set<DayOfWeek>) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth().padding(top = 22.dp).reveal(order = 5),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Recurrence.entries.forEach { r -> Chip(r.label, Recurrence.of(days) == r) { onPick(r.days) } }
    }
}

/** A small rounded choice, ember when picked. */
@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text, Modifier.background(if (on) Night.EMBER else Night.FROST, RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = if (on) Night.ON_EMBER else Night.CAPTION, fontSize = 13.sp,
    )
}

@Composable
private fun Countdown(alarm: Alarm, now: ZonedDateTime, order: Int) {
    WakeInfo.announce(alarm, now)?.let {
        val line = Modifier.fillMaxWidth().padding(top = 26.dp).reveal(order)
        Text(it, line, color = Night.EMBER, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SummaryStep(alarm: Alarm, events: FlowEvents) {
    Text(
        clock(alarm.time), Modifier.fillMaxWidth().padding(top = 8.dp).reveal(0),
        fontSize = 84.sp, fontWeight = FontWeight.Thin, textAlign = TextAlign.Center,
    )
    Text(Recurrence.summary(alarm.days), Modifier.fillMaxWidth().reveal(1), color = Night.CAPTION, textAlign = TextAlign.Center)
    PlaylistCard(alarm, events)
    OutlinedTextField(
        alarm.label, { events.change(alarm.copy(label = it)) }, Modifier.fillMaxWidth().padding(top = 14.dp).reveal(order = 3),
        singleLine = true, placeholder = { Text("Nom (facultatif)") },
    )
    SettingsRow(alarm, events)
    Spacer(Modifier.weight(1f))
    val last = Modifier.padding(bottom = 24.dp).reveal(order = 5)
    Cta("Programmer le réveil", { events.save(alarm) }, last, enabled = alarm.playlistId != null)
}

@Composable
private fun PlaylistCard(alarm: Alarm, events: FlowEvents) {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp).reveal(2).background(Night.FROST, RoundedCornerShape(14.dp))
            .clickable { events.go(Step.PLAYLIST) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Sleeve(alarm.playlistCover, true, 60.dp, spot = DRAFT)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(alarm.playlistTitle ?: "Aucune playlist", fontSize = 16.sp)
            Text("Morceaux préparés la veille au soir", color = Night.CAPTION, fontSize = 12.sp)
        }
        Text("Changer", color = Night.EMBER, fontSize = 14.sp)
    }
}

/** The fine settings keep their defaults; "Modifier" opens the full editor. */
@Composable
private fun SettingsRow(alarm: Alarm, events: FlowEvents) {
    val row = Modifier.reveal(order = 4).fillMaxWidth().clickable { events.more(alarm) }.padding(vertical = 14.dp)
    Row(row, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Réglages", fontSize = 15.sp)
            Text(settings(alarm), color = Night.CAPTION, fontSize = 12.sp)
        }
        Text("Modifier", color = Night.EMBER, fontSize = 14.sp)
    }
}

private fun settings(alarm: Alarm): String {
    val ramp = if (alarm.rampSeconds == 0) "Volume direct" else "Volume qui monte en ${duration(alarm.rampSeconds)}"
    val times = alarm.maxSnoozes?.let { "$it fois" } ?: "sans limite"
    return "$ramp, snooze ${alarm.snoozeMinutes} min, $times"
}

private fun duration(seconds: Int) = if (seconds < MINUTE) "$seconds s" else "${seconds / MINUTE} min"

private const val MINUTE = 60
private const val QUESTIONS = 3

/** Where the cover of the alarm being created sits, on the summary. */
internal const val DRAFT = "draft"
private const val DOTS_PER_BEAT = 3
