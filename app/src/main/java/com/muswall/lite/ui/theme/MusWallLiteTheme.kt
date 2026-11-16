package com.muswall.lite.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MusWallLiteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF64B5F6),
            secondary = Color(0xFF81C784),
            background = Color(0xFF0F0F14),
            surface = Color(0xFF16161E),
            surfaceVariant = Color(0xFF20202C),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}
