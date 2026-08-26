package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Minimalist High-Contrast Monochrome Palette
val MonoBlack = Color(0xFF000000)
val MonoBackground = Color(0xFF0A0A0A)
val MonoSurface = Color(0xFF141414)
val MonoSurfaceElevated = Color(0xFF1C1C1C)
val MonoBorder = Color(0xFF262626)
val MonoBorderSubtle = Color(0xFF1F1F1F)

val MonoWhite = Color(0xFFFFFFFF)
val MonoTextPrimary = Color(0xFFF5F5F5)
val MonoTextSecondary = Color(0xFFA3A3A3)
val MonoTextMuted = Color(0xFF737373)

// Single Active Accent Color (Vivid Electric Cyan for Recording / Active states)
val ActiveAccent = Color(0xFF00E5FF)
val ActiveAccentDark = Color(0xFF00B4D8)
val ActiveAccentSubtle = Color(0x1F00E5FF)
val ActiveAccentBorder = Color(0x4D00E5FF)

val ActiveDestructive = Color(0xFFEF4444)
val ActiveDestructiveSubtle = Color(0x1FEF4444)
val ActiveSuccess = Color(0xFF10B981)

// Legacy aliases for backward compatibility
val CyberBackground = MonoBackground
val CyberSurface = MonoSurface
val CyberSurfaceVariant = MonoSurfaceElevated
val CyberSurfaceHover = MonoSurfaceElevated

val ElectricCyan = ActiveAccent
val ElectricCyanDark = ActiveAccentDark
val NeonViolet = ActiveAccent
val NeonPurple = ActiveAccent
val AcidGreen = ActiveSuccess
val NeonAmber = Color(0xFFF59E0B)
val CrimsonRed = ActiveDestructive

val PureWhite = MonoWhite
val TextPrimary = MonoTextPrimary
val TextSecondary = MonoTextSecondary
val TextMuted = MonoTextMuted

val GlassBorder = MonoBorder
val GlassBorderHighlight = ActiveAccentBorder

// M3 Tokens
val M3Primary = MonoWhite
val M3OnPrimary = MonoBlack
val M3PrimaryContainer = MonoSurfaceElevated
val M3OnPrimaryContainer = MonoWhite

val M3Secondary = ActiveAccent
val M3OnSecondary = MonoBlack
val M3SecondaryContainer = ActiveAccentSubtle
val M3OnSecondaryContainer = ActiveAccent

val M3Background = MonoBackground
val M3OnBackground = MonoTextPrimary

val M3Surface = MonoSurface
val M3OnSurface = MonoTextPrimary
val M3SurfaceVariant = MonoSurfaceElevated
val M3OnSurfaceVariant = MonoTextSecondary

val M3Outline = MonoBorder
val M3OutlineVariant = MonoBorderSubtle

val AlertContainer = Color(0xFF2A0E12)
val OnAlertContainer = Color(0xFFFCA5A5)

val PurplePrimary = ActiveAccent
val PurpleOnPrimary = MonoBlack
val PurplePrimaryContainer = ActiveAccentSubtle
val PurpleOnPrimaryContainer = ActiveAccent
val PurpleSecondary = ActiveAccent
val PurpleSecondaryContainer = ActiveAccentSubtle
val PurpleOnSecondaryContainer = ActiveAccent
val NeutralBackground = M3Background
val NeutralOnBackground = M3OnBackground
val NeutralSurface = M3Surface
val NeutralSurfaceVariant = M3SurfaceVariant
val NeutralOnSurface = M3OnSurface
val NeutralOnSurfaceVariant = M3OnSurfaceVariant
val NeutralOutline = M3Outline
val NeutralOutlineVariant = M3OutlineVariant

