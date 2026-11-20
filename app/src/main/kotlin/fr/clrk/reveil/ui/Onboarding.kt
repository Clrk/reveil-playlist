package fr.clrk.reveil.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** First launch, in the evening: the turntable waits, and each thing Android must allow is one plain line. */
@Composable
fun OnboardingScreen(steps: List<PermissionStep>, city: String?, onWeather: () -> Unit, onDone: () -> Unit) = NightPage {
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        WaitingTurntable(Modifier.fillMaxWidth().height(230.dp).reveal(0))
        Column(Modifier.padding(horizontal = 24.dp).weight(1f).verticalScroll(rememberScrollState())) {
            Text("Bonsoir.", Modifier.reveal(1), fontSize = 40.sp, fontWeight = FontWeight.ExtraLight)
            Text(
                "Pour que la musique vous réveille à coup sûr, Android doit autoriser ${count(steps.size)} choses.",
                Modifier.padding(top = 8.dp, bottom = 12.dp).reveal(2), color = Night.CAPTION, fontSize = 14.sp,
            )
            steps.forEachIndexed { i, step -> Line(step.title, step.why, if (step.granted) null else "Autoriser", step.fix, order = 3 + i) }
            WeatherChoice(city, onWeather, order = 3 + steps.size)
        }
        Cta(start(steps), onDone, Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).reveal(order = 4 + steps.size))
    }
}

private fun start(steps: List<PermissionStep>) = if (steps.all { it.granted }) "C'est parti" else "Continuer"

@Composable
private fun WeatherChoice(city: String?, onClick: () -> Unit, order: Int) {
    val why = city?.let { "Ville : $it" } ?: "Facultatif. Choisissez votre ville pour voir le temps du matin au réveil."
    Line("Météo du matin", why, if (city == null) "Choisir" else "Changer", onClick, order, last = true)
}

private fun count(n: Int) = listOf("zéro", "une", "deux", "trois", "quatre").getOrElse(n) { "$n" }

@Composable
private fun Line(title: String, why: String, action: String?, onClick: () -> Unit, order: Int, last: Boolean = false) {
    val row = Modifier.reveal(order).fillMaxWidth().clickable(enabled = action != null, onClick = onClick).padding(vertical = 13.dp)
    Row(row, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp)
            Text(why, color = Night.CAPTION, fontSize = 13.sp)
        }
        Text(action ?: "✓", color = Night.EMBER, fontSize = if (action == null) 16.sp else 14.sp)
    }
    if (!last) HorizontalDivider(Modifier.reveal(order), color = Night.LINE)
}
