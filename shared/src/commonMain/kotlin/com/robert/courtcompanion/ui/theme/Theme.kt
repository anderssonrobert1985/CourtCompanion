package com.robert.courtcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = lightColorScheme(
    primary = Color(0xFF1F3A5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3F7),
    onPrimaryContainer = Color(0xFF0B1F36),
    secondary = Color(0xFFE8781E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0C7),
    onSecondaryContainer = Color(0xFF3D1C00),
    surfaceVariant = Color(0xFFEDEFF3),
)

/** Header colours for groups, in order; they repeat after the last one. */
val GroupColors = listOf(
    Color(0xFF2E6FD8), // blue
    Color(0xFFD9482B), // red
    Color(0xFF2E9E5B), // green
    Color(0xFFE8A11E), // yellow
    Color(0xFF7B4FC9), // purple
    Color(0xFF1C9AA6), // teal
    Color(0xFFD64F95), // pink
    Color(0xFF5D6B7A), // grey
)

fun groupColor(index: Int) = GroupColors[index % GroupColors.size]

@Composable
fun CourtCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
