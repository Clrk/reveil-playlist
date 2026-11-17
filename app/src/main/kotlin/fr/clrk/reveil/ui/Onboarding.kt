package fr.clrk.reveil.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** First launch: explains and asks, in one place, what Android must allow for the alarm to ring. */
@Composable
fun OnboardingScreen(steps: List<PermissionStep>, onDone: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.padding(top = 24.dp))
            Text("Bienvenue", style = MaterialTheme.typography.headlineLarge)
            Text("Pour que ton réveil sonne à coup sûr, Android doit autoriser ce qui suit.")
            steps.forEach { StepCard(it) }
            Spacer(Modifier.weight(1f))
            Button(onClick = onDone, Modifier.fillMaxWidth()) {
                Text(if (steps.all { it.granted }) "C'est parti" else "Continuer quand même")
            }
        }
    }
}

@Composable
private fun StepCard(step: PermissionStep) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(step.title, style = MaterialTheme.typography.titleMedium)
                Text(step.why, style = MaterialTheme.typography.bodyMedium)
            }
            if (step.granted) {
                Text("✓ OK", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            } else {
                FilledTonalButton(onClick = step.fix) { Text("Autoriser") }
            }
        }
    }
}
