package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode(val label: String) {
  SYSTEM("System Default"),
  LIGHT("Light Mode"),
  DARK("Dark Mode")
}

private val LightColorScheme = lightColorScheme(
  primary = M3Primary,
  onPrimary = M3OnPrimary,
  primaryContainer = M3PrimaryContainer,
  onPrimaryContainer = M3OnPrimaryContainer,
  secondary = M3Secondary,
  onSecondary = M3OnSecondary,
  secondaryContainer = M3SecondaryContainer,
  onSecondaryContainer = M3OnSecondaryContainer,
  background = M3Background,
  onBackground = M3OnBackground,
  surface = M3Surface,
  onSurface = M3OnSurface,
  surfaceVariant = M3SurfaceVariant,
  onSurfaceVariant = M3OnSurfaceVariant,
  outline = M3Outline,
  outlineVariant = M3OutlineVariant,
  errorContainer = AlertContainer,
  onErrorContainer = OnAlertContainer
)

private val DarkColorScheme = darkColorScheme(
  primary = androidx.compose.ui.graphics.Color(0xFFD0BCFF),
  onPrimary = androidx.compose.ui.graphics.Color(0xFF381E72),
  primaryContainer = androidx.compose.ui.graphics.Color(0xFF4F378B),
  onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFEADDFF),
  secondary = androidx.compose.ui.graphics.Color(0xFFCCC2DC),
  onSecondary = androidx.compose.ui.graphics.Color(0xFF332D41),
  secondaryContainer = androidx.compose.ui.graphics.Color(0xFF4A4458),
  onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFE8DEF8),
  background = androidx.compose.ui.graphics.Color(0xFF141218),
  onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
  surface = androidx.compose.ui.graphics.Color(0xFF1D1B20),
  onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
  surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2B2636),
  onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCAC4D0),
  outline = androidx.compose.ui.graphics.Color(0xFF938F99),
  outlineVariant = androidx.compose.ui.graphics.Color(0xFF49454F)
)

@Composable
fun ContextLogTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val systemDark = isSystemInDarkTheme()
  val darkTheme = when (themeMode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
