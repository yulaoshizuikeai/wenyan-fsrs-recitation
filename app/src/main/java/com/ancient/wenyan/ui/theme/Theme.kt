package com.ancient.wenyan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ancient Chinese Cultural Color Palette
val XuanPaperLight = Color(0xFFF7F4EB)
val XuanPaperDeep = Color(0xFFF2ECE1)
val XuanPaperCard = Color(0xFFFFFDF8)
val XuanBorder = Color(0xFFE2DAC7)
val InkCharcoal = Color(0xFF232120)
val InkMedium = Color(0xFF57524E)
val InkFaded = Color(0xFF8C827A)
val BambooGreen = Color(0xFF2C4F3D)
val MountainTeal = Color(0xFF3E5C59)
val CinnabarRed = Color(0xFF9E2A2B)
val MutedGold = Color(0xFFB58D3D)
val CeladonBlue = Color(0xFF4A6B6C)

val AncientColorScheme = lightColorScheme(
    primary = BambooGreen,
    onPrimary = Color.White,
    primaryContainer = XuanPaperDeep,
    onPrimaryContainer = InkCharcoal,
    secondary = CinnabarRed,
    onSecondary = Color.White,
    background = XuanPaperLight,
    onBackground = InkCharcoal,
    surface = XuanPaperCard,
    onSurface = InkCharcoal,
    surfaceVariant = XuanPaperDeep,
    onSurfaceVariant = InkMedium,
    outline = XuanBorder
)

@Composable
fun WenYanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AncientColorScheme,
        content = content
    )
}
