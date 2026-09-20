package com.ancient.wenyan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Concrete Palette Values for Light Theme (Slate 50 / Slate 900 / Vercel Navy)
private val LightBgCanvas = Color(0xFFF8FAFC)        // Slate 50 全局背景
private val LightBgSurface = Color(0xFFFFFFFF)       // 纯白卡片底色
private val LightBgSurfaceMuted = Color(0xFFF1F5F9)  // Slate 100 浅灰胶囊与卡片次级底
private val LightBorderSubtle = Color(0xFFE2E8F0)    // Slate 200 1dp发丝线边框
private val LightBorderFocus = Color(0xFFCBD5E1)     // Slate 300 聚焦描边
private val LightTextPrimary = Color(0xFF0F172A)     // Slate 900 一级标题与重点数字
private val LightTextSecondary = Color(0xFF475569)   // Slate 600 二级说明与正文
private val LightTextTertiary = Color(0xFF94A3B8)    // Slate 400 辅助提示与日期
private val LightStudyNavy = Color(0xFF0F172A)       // 沉稳深海军蓝主色
private val LightStudyBlueAccent = Color(0xFF2563EB) // 科技亮蓝 (Blue 600)
private val LightStudyBlueLight = Color(0xFFEFF6FF)  // 柔和微蓝底 (Blue 50)
private val LightStreakFlame = Color(0xFFF97316)     // 活力连胜橙红火焰 (Orange 500)
private val LightSuccessGreen = Color(0xFF10B981)    // 成功/已掌握 (Emerald 500)
private val LightDueRed = Color(0xFFEF4444)          // 到期提醒鲜红 (Red 500)
private val LightWarningGold = Color(0xFFD97706)     // 警示金黄 (Amber 600)

// Concrete Palette Values for Dark Theme (Deep Slate / Dark Slate 950 / Sky Blue)
private val DarkBgCanvas = Color(0xFF0B0F17)         // 深邃黑蓝背景
private val DarkBgSurface = Color(0xFF151D2A)        // 抬升卡片表面深色底
private val DarkBgSurfaceMuted = Color(0xFF1E293B)   // Slate 800 次级胶囊与卡片底
private val DarkBorderSubtle = Color(0xFF334155)     // Slate 700 微妙暗边框
private val DarkBorderFocus = Color(0xFF475569)      // Slate 600 聚焦描边
private val DarkTextPrimary = Color(0xFFF8FAFC)      // Slate 50 高对比主文字
private val DarkTextSecondary = Color(0xFFCBD5E1)    // Slate 300 次级说明文字
private val DarkTextTertiary = Color(0xFF94A3B8)     // Slate 400 提示与辅助文字
private val DarkStudyNavy = Color(0xFFE2E8F0)        // 暗色模式下导航/重点标题反色为清晰浅灰
private val DarkStudyBlueAccent = Color(0xFF60A5FA)  // 科技亮蓝 (Blue 400)
private val DarkStudyBlueLight = Color(0xFF1E293B)   // 暗调选中底色
private val DarkStreakFlame = Color(0xFFFB923C)      // 暗色高亮连胜橙 (Orange 400)
private val DarkSuccessGreen = Color(0xFF34D399)     // 掌握绿 (Emerald 400)
private val DarkDueRed = Color(0xFFF87171)           // 到期红 (Red 400)
private val DarkWarningGold = Color(0xFFFBBF24)      // 警示黄 (Amber 400)

// Dynamic Composable Color Tokens (Adapt effortlessly between Light & Dark modes)
val BgCanvas: Color @Composable get() = MaterialTheme.colorScheme.background
val BgSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val BgSurfaceMuted: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val BorderSubtle: Color @Composable get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
val BorderFocus: Color @Composable get() = MaterialTheme.colorScheme.outline
val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextTertiary: Color @Composable get() = if (isSystemInDarkTheme()) DarkTextTertiary else LightTextTertiary

val StudyNavy: Color @Composable get() = if (isSystemInDarkTheme()) DarkStudyNavy else LightStudyNavy
val StudyBlueAccent: Color @Composable get() = if (isSystemInDarkTheme()) DarkStudyBlueAccent else LightStudyBlueAccent
val StudyBlueLight: Color @Composable get() = if (isSystemInDarkTheme()) DarkStudyBlueLight else LightStudyBlueLight

val StreakFlame: Color @Composable get() = if (isSystemInDarkTheme()) DarkStreakFlame else LightStreakFlame
val SuccessGreen: Color @Composable get() = if (isSystemInDarkTheme()) DarkSuccessGreen else LightSuccessGreen
val DueRed: Color @Composable get() = if (isSystemInDarkTheme()) DarkDueRed else LightDueRed
val WarningGold: Color @Composable get() = if (isSystemInDarkTheme()) DarkWarningGold else LightWarningGold

// Backward Compatibility Aliases for legacy views mapped to clean modern tokens
val XuanPaperLight: Color @Composable get() = BgCanvas
val XuanPaperDeep: Color @Composable get() = BgSurfaceMuted
val XuanPaperCard: Color @Composable get() = BgSurface
val XuanBorder: Color @Composable get() = BorderSubtle
val InkCharcoal: Color @Composable get() = TextPrimary
val InkMedium: Color @Composable get() = TextSecondary
val InkFaded: Color @Composable get() = TextTertiary
val BambooGreen: Color @Composable get() = StudyNavy
val MountainTeal: Color @Composable get() = StudyNavy
val CinnabarRed: Color @Composable get() = StreakFlame
val MutedGold: Color @Composable get() = WarningGold
val CeladonBlue: Color @Composable get() = StudyBlueAccent

// Modern Typography: SansSerif by design, colors hoisted to LocalContentColor
val ModernTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        letterSpacing = (-1.0).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)

val ModernColorScheme = lightColorScheme(
    primary = LightStudyNavy,
    onPrimary = Color.White,
    primaryContainer = LightStudyBlueLight,
    onPrimaryContainer = LightStudyBlueAccent,
    secondary = LightStreakFlame,
    onSecondary = Color.White,
    background = LightBgCanvas,
    onBackground = LightTextPrimary,
    surface = LightBgSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBgSurfaceMuted,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderSubtle
)

val ModernDarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF0B0F17),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = DarkStreakFlame,
    onSecondary = Color(0xFF431407),
    background = DarkBgCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkBgSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBgSurfaceMuted,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderSubtle
)

val AncientColorScheme = ModernColorScheme

@Composable
fun WenYanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ModernDarkColorScheme else ModernColorScheme
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ModernTypography,
        content = content
    )
}
