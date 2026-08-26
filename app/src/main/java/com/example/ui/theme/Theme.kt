package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode(val label: String) {
  SYSTEM("System Default"),
  LIGHT("Light Mode"),
  DARK("Dark Mode")
}

private val DarkColorScheme = darkColorScheme(
  primary = MonoWhite,
  onPrimary = MonoBlack,
  primaryContainer = MonoSurfaceElevated,
  onPrimaryContainer = MonoWhite,
  secondary = ActiveAccent,
  onSecondary = MonoBlack,
  secondaryContainer = ActiveAccentSubtle,
  onSecondaryContainer = ActiveAccent,
  tertiary = ActiveSuccess,
  onTertiary = MonoBlack,
  background = MonoBackground,
  onBackground = MonoTextPrimary,
  surface = MonoSurface,
  onSurface = MonoTextPrimary,
  surfaceVariant = MonoSurfaceElevated,
  onSurfaceVariant = MonoTextSecondary,
  outline = MonoBorder,
  outlineVariant = MonoBorderSubtle,
  error = ActiveDestructive,
  errorContainer = AlertContainer,
  onErrorContainer = OnAlertContainer
)

private val LightColorScheme = lightColorScheme(
  primary = MonoBlack,
  onPrimary = MonoWhite,
  primaryContainer = Color(0xFFEEEEEE),
  onPrimaryContainer = MonoBlack,
  secondary = Color(0xFF0284C7),
  onSecondary = MonoWhite,
  secondaryContainer = Color(0xFFE0F2FE),
  onSecondaryContainer = Color(0xFF0369A1),
  tertiary = Color(0xFF059669),
  onTertiary = MonoWhite,
  background = Color(0xFFFAFAFA),
  onBackground = Color(0xFF111111),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF111111),
  surfaceVariant = Color(0xFFF4F4F5),
  onSurfaceVariant = Color(0xFF52525B),
  outline = Color(0xFFE4E4E7),
  outlineVariant = Color(0xFFF4F4F5),
  error = Color(0xFFDC2626),
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun ContextLogTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
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
    else -> DarkColorScheme // High contrast minimalist dark by default
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

