package fr.clrk.reveil.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun reveilColors(): ColorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
