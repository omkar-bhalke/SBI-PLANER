package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = PrimaryBlueLight,
  onPrimary = Color.White,
  primaryContainer = NavySurface,
  onPrimaryContainer = Color.White,
  secondary = SecondaryBlue,
  onSecondary = Color.White,
  tertiary = AccentGold,
  background = SlateDarkBackground,
  onBackground = TextPrimaryDark,
  surface = CardBackgroundDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = NavySurface,
  onSurfaceVariant = TextSecondaryDark,
  outline = OutlineDark,
  error = ExpenseRed
)

private val LightColorScheme = lightColorScheme(
  primary = PrimaryBlue,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE2E8F0),
  onPrimaryContainer = PrimaryBlue,
  secondary = SecondaryBlue,
  onSecondary = Color.White,
  tertiary = AccentGold,
  background = SlateBackground,
  onBackground = TextPrimaryLight,
  surface = CardBackgroundLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = TextSecondaryLight,
  outline = OutlineLight,
  error = ExpenseRed
)

@Composable
fun FinPulseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
