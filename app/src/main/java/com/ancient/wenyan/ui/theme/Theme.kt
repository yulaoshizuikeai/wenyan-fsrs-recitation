package com.ancient.wenyan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Modern Minimalist Study Palette (Vercel Clean Gray + Study Navy + Streak Flame)
val BgCanvas = Color(0xFFF8FAFC)        // Slate 50 全局背景
val BgSurface = Color(0xFFFFFFFF)       // 纯白卡片底色
val BgSurfaceMuted = Color(0xFFF1F5F9)  // Slate 100 浅灰胶囊与卡片次级底
val BorderSubtle = Color(0xFFE2E8F0)    // Slate 200 1dp发丝线边框
val BorderFocus = Color(0xFFCBD5E1)     // Slate 300 聚焦描边

val TextPrimary = Color(0xFF0F172A)     // Slate 900 一级标题与重点数字
val TextSecondary = Color(0xFF475569)   // Slate 600 二级说明与正文
val TextTertiary = Color(0xFF94A3B8)    // Slate 400 辅助提示与日期

val StudyNavy = Color(0xFF0F172A)       // Vercel / Readwise 沉稳深海军蓝主色
val StudyBlueAccent = Color(0xFF2563EB) // 科技亮蓝 (Blue 600)
val StudyBlueLight = Color(0xFFEFF6FF)  // 柔和微蓝底 (Blue 50)

val StreakFlame = Color(0xFFF97316)     // 活力连胜橙红火焰 (Orange 500)
val SuccessGreen = Color(0xFF10B981)    // 成功/已掌握 (Emerald 500)
val DueRed = Color(0xFFEF4444)          // 到期提醒鲜红 (Red 500)
val WarningGold = Color(0xFFD97706)     // 警示金黄 (Amber 600)

// Backward Compatibility Aliases for legacy views mapped to clean modern tokens
val XuanPaperLight = BgCanvas
val XuanPaperDeep = BgSurfaceMuted
val XuanPaperCard = BgSurface
val XuanBorder = BorderSubtle
val InkCharcoal = TextPrimary
val InkMedium = TextSecondary
val InkFaded = TextTertiary
val BambooGreen = StudyNavy
val MountainTeal = StudyNavy
val CinnabarRed = StreakFlame
val MutedGold = WarningGold
val CeladonBlue = StudyBlueAccent

// Modern Typography: All SansSerif by design
val ModernTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        letterSpacing = (-1.0).sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = (-0.2).sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = TextTertiary
    )
)

val ModernColorScheme = lightColorScheme(
    primary = StudyNavy,
    onPrimary = Color.White,
    primaryContainer = StudyBlueLight,
    onPrimaryContainer = StudyBlueAccent,
    secondary = StreakFlame,
    onSecondary = Color.White,
    background = BgCanvas,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgSurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

val AncientColorScheme = ModernColorScheme

@Composable
fun WenYanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ModernColorScheme,
        typography = ModernTypography,
        content = content
    )
}
