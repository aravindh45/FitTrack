package com.personal.fittrack.ui.theme

import androidx.compose.ui.graphics.Color

// Google brand-inspired seed colors, used to build a vivid Material 3 scheme on devices
// without dynamic color (Android 11 and below). On Android 12+ we prefer the system's
// wallpaper-derived dynamic color, per Material You guidance.
val GoogleBlue = Color(0xFF4285F4)
val GoogleGreen = Color(0xFF34A853)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleRed = Color(0xFFEA4335)

val LightPrimary = GoogleBlue
val LightOnPrimary = Color.White
val LightPrimaryContainer = Color(0xFFD6E3FF)
val LightOnPrimaryContainer = Color(0xFF001B3F)

val LightSecondary = GoogleGreen
val LightOnSecondary = Color.White
val LightSecondaryContainer = Color(0xFFBEF0C8)
val LightOnSecondaryContainer = Color(0xFF00210B)

val LightTertiary = GoogleYellow
val LightOnTertiary = Color(0xFF3F2E00)
val LightTertiaryContainer = Color(0xFFFFE08C)
val LightOnTertiaryContainer = Color(0xFF261A00)

val LightError = GoogleRed
val LightBackground = Color(0xFFFCFCFF)
val LightSurface = Color(0xFFFCFCFF)
val LightSurfaceVariant = Color(0xFFE1E2EC)

val DarkPrimary = Color(0xFFAAC7FF)
val DarkOnPrimary = Color(0xFF002E69)
val DarkPrimaryContainer = Color(0xFF004494)
val DarkOnPrimaryContainer = Color(0xFFD6E3FF)

val DarkSecondary = Color(0xFFA3D4AB)
val DarkOnSecondary = Color(0xFF003912)
val DarkSecondaryContainer = Color(0xFF005220)
val DarkOnSecondaryContainer = Color(0xFFBEF0C8)

val DarkTertiary = Color(0xFFEEC248)
val DarkOnTertiary = Color(0xFF3F2E00)
val DarkTertiaryContainer = Color(0xFF5B4300)
val DarkOnTertiaryContainer = Color(0xFFFFE08C)

val DarkBackground = Color(0xFF1A1C1E)
val DarkSurface = Color(0xFF1A1C1E)
val DarkSurfaceVariant = Color(0xFF44474F)

// Semantic colors used by dashboard/progress UI, not part of the Material scheme itself.
val CalorieOverBudget = GoogleRed
val CalorieOnTrack = GoogleGreen
val CalorieApproachingLimit = GoogleYellow
