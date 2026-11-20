package fr.clrk.reveil.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.Paparazzi

/** A minute after the screen opened: every element has arrived. */
private const val SETTLED_MS = 60_000L

/** Screenshot of a screen once its elements have arrived. */
fun Paparazzi.settled(content: @Composable () -> Unit) =
    snapshot { CompositionLocalProvider(LocalRevealClock provides SETTLED_MS) { content() } }
