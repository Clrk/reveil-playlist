package fr.clrk.reveil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
        WaitingTurntable(Modifier.fillMaxWidth().height(230.dp))
        Column(Modifier.padding(horizontal = 24.dp).weight(1f)) {
            Text("Bonsoir.", fontSize = 40.sp, fontWeight = FontWeight.ExtraLight)
            Text(
                "Pour que la musique vous réveille à coup sûr, Android doit autoriser ${count(steps.size)} choses.",
                Modifier.padding(top = 8.dp, bottom = 12.dp), color = Night.CAPTION, fontSize = 14.sp,
            )
            steps.forEach { Line(it.title, it.why, if (it.granted) null else "Autoriser", it.fix) }
            WeatherChoice(city, onWeather)
            Spacer(Modifier.weight(1f))
            Cta(start(steps), onDone, Modifier.padding(bottom = 24.dp))
        }
    }
}

private fun start(steps: List<PermissionStep>) = if (steps.all { it.granted }) "C'est parti" else "Continuer"

@Composable
private fun WeatherChoice(city: String?, onClick: () -> Unit) {
    val why = city?.let { "Ville : $it" } ?: "Facultatif. Choisissez votre ville pour voir le temps du matin au réveil."
    Line("Météo du matin", why, if (city == null) "Choisir" else "Changer", onClick, last = true)
}

private fun count(n: Int) = listOf("zéro", "une", "deux", "trois", "quatre").getOrElse(n) { "$n" }

@Composable
private fun Line(title: String, why: String, action: String?, onClick: () -> Unit, last: Boolean = false) {
    val row = Modifier.fillMaxWidth().clickable(enabled = action != null, onClick = onClick).padding(vertical = 13.dp)
    Row(row, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp)
            Text(why, color = Night.CAPTION, fontSize = 13.sp)
        }
        Text(action ?: "✓", color = Night.EMBER, fontSize = if (action == null) 16.sp else 14.sp)
    }
    if (!last) HorizontalDivider(color = Night.LINE)
}
