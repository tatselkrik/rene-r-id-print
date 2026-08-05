package com.idphoto.printing.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF18201B)
val CanvasCream = Color(0xFFF6F7F2)
val Pine = Color(0xFF165C43)
val PineDark = Color(0xFF0D3E2D)
val Mint = Color(0xFFDDEFE6)
val WarmWhite = Color(0xFFFFFEF9)
val ErrorRed = Color(0xFFB3261E)
val Muted = Color(0xFF5C665F)

private val AppColors = lightColorScheme(
    primary = Pine,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = PineDark,
    background = CanvasCream,
    onBackground = Ink,
    surface = WarmWhite,
    onSurface = Ink,
    error = ErrorRed,
)

@Composable
fun IdPhotoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        content = content,
    )
}
