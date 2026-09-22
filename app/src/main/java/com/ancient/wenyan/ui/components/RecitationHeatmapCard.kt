package com.ancient.wenyan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.domain.model.HeatmapStats
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.theme.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Color Palette Definitions
// ─────────────────────────────────────────────────────────────────────────────

enum class HeatmapColorTheme(val label: String) {
    ANKI("Anki"),
    MONET("莫奈"),
    OCEAN("深海")
}

private data class HeatmapPalette(
    val empty: Color,
    val level1: Color,
    val level2: Color,
    val level3: Color,
    val level4: Color,
    val todayBorder: Color
)

@Composable
private fun paletteFor(theme: HeatmapColorTheme, isDark: Boolean): HeatmapPalette = when (theme) {
    // ── Anki: Classic GitHub-green style ──────────────────────────────────────
    HeatmapColorTheme.ANKI -> if (isDark) HeatmapPalette(
        empty     = Color(0xFF1B2028),
        level1    = Color(0xFF1E3A1E),
        level2    = Color(0xFF276227),
        level3    = Color(0xFF4A9E4A),
        level4    = Color(0xFF6FBE6F),
        todayBorder = Color(0xFF89D489)
    ) else HeatmapPalette(
        empty     = Color(0xFFEBEDF0),
        level1    = Color(0xFF9BE9A8),
        level2    = Color(0xFF40C463),
        level3    = Color(0xFF30A14E),
        level4    = Color(0xFF216E39),
        todayBorder = Color(0xFF216E39)
    )

    // ── Monet: Inspired by Monet's water-lily palette ──────────────────────────
    // Blues, lilacs, sage greens, dusty roses extracted from "Water Lilies" series
    HeatmapColorTheme.MONET -> if (isDark) HeatmapPalette(
        empty     = Color(0xFF1A1C2E),
        level1    = Color(0xFF2D3B5E),
        level2    = Color(0xFF4A5E8F),
        level3    = Color(0xFF7B8FBF),
        level4    = Color(0xFFB5C3E8),
        todayBorder = Color(0xFFD4B8D0)
    ) else HeatmapPalette(
        empty     = Color(0xFFF0EDF5),
        level1    = Color(0xFFD4B8D0),   // 莫奈玫瑰紫
        level2    = Color(0xFF9BAFD4),   // 晨雾蓝
        level3    = Color(0xFF607CB8),   // 睡莲湖蓝
        level4    = Color(0xFF3A5A9E),   // 深水蓝
        todayBorder = Color(0xFF8B6FA8)  // 紫鸢尾
    )

    // ── Ocean: Deep blue-teal gradient ───────────────────────────────────────
    HeatmapColorTheme.OCEAN -> if (isDark) HeatmapPalette(
        empty     = Color(0xFF0D1B2A),
        level1    = Color(0xFF1A3A4A),
        level2    = Color(0xFF1E6B7A),
        level3    = Color(0xFF1DA1B0),
        level4    = Color(0xFF4DCBD8),
        todayBorder = Color(0xFF4DCBD8)
    ) else HeatmapPalette(
        empty     = Color(0xFFE8F4F8),
        level1    = Color(0xFFB3DDE8),
        level2    = Color(0xFF5BBCD1),
        level3    = Color(0xFF1E8FA3),
        level4    = Color(0xFF0D6073),
        todayBorder = Color(0xFF0D6073)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RecitationHeatmapCard(
    heatmapStats: HeatmapStats,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM ''yy") }
    var selectedDateInfo by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val isDark = isSystemInDarkTheme()
    var colorTheme by remember { mutableStateOf(HeatmapColorTheme.ANKI) }
    val palette = paletteFor(colorTheme, isDark)

    // Animated palette transitions
    val animEmpty   by animateColorAsState(palette.empty,   tween(320), label = "empty")
    val animL1      by animateColorAsState(palette.level1,  tween(320), label = "l1")
    val animL2      by animateColorAsState(palette.level2,  tween(320), label = "l2")
    val animL3      by animateColorAsState(palette.level3,  tween(320), label = "l3")
    val animL4      by animateColorAsState(palette.level4,  tween(320), label = "l4")
    val animBorder  by animateColorAsState(palette.todayBorder, tween(320), label = "border")

    // Anki-style: 14 weeks + month labels
    val totalWeeks = 14
    val startDate = remember { today.minusWeeks((totalWeeks - 1).toLong()).with(DayOfWeek.MONDAY) }
    val endDate = remember { today.plusDays((7 - today.dayOfWeek.value).toLong()) }

    val daysMatrix = remember(startDate, endDate) {
        val matrix = Array(7) { arrayOfNulls<LocalDate>(totalWeeks) }
        var curr = startDate
        var col = 0
        while (!curr.isAfter(endDate) && col < totalWeeks) {
            val row = curr.dayOfWeek.value - 1
            matrix[row][col] = curr
            if (row == 6) col++
            curr = curr.plusDays(1)
        }
        matrix
    }

    // ── Adaptive thresholds based on actual data distribution ─────────────────
    // Collect all non-zero counts in the visible window (past 14 weeks, up to today)
    val adaptiveThresholds = remember(heatmapStats.dailyReviewMap, startDate) {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val counts = buildList {
            var d = startDate
            val now = LocalDate.now()
            while (!d.isAfter(now)) {
                val c = heatmapStats.dailyReviewMap[d.format(fmt)] ?: 0
                if (c > 0) add(c)
                d = d.plusDays(1)
            }
        }.sorted()

        if (counts.isEmpty()) {
            // No data yet — sensible defaults so legend still renders
            intArrayOf(1, 5, 10, 20)
        } else {
            fun percentile(p: Double): Int {
                val idx = ((p / 100.0) * (counts.size - 1)).toInt().coerceIn(0, counts.size - 1)
                return counts[idx]
            }
            // p25 / p50 / p75 / max → each level always distinguishable
            intArrayOf(
                percentile(25.0).coerceAtLeast(1),
                percentile(50.0).coerceAtLeast(2),
                percentile(75.0).coerceAtLeast(3),
                counts.last()
            )
        }
    }

    // Month label positions: first col where new month appears
    val monthLabels = remember(startDate, totalWeeks) {
        buildList {
            var lastMonth = -1
            for (col in 0 until totalWeeks) {
                val date = daysMatrix[0][col] ?: continue
                if (date.monthValue != lastMonth) {
                    lastMonth = date.monthValue
                    add(col to date.format(monthFormatter))
                }
            }
        }
    }

    // Cell geometry (Anki-style: 11dp cell, 2dp gap — compact & clean)
    val cellSize = 11.dp
    val cellGap  = 2.dp
    val cellRadius = 2.dp
    // Width of weekday label column
    val labelColWidth = 20.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(StudyBlueLight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = StudyBlueAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "研墨打卡足迹",
                        fontSize = 15.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Color theme picker – compact icon row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "切换配色",
                        tint = TextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    HeatmapColorTheme.entries.forEach { theme ->
                        val isSelected = colorTheme == theme
                        val dotColor = when (theme) {
                            HeatmapColorTheme.ANKI  -> if (isDark) Color(0xFF4A9E4A) else Color(0xFF30A14E)
                            HeatmapColorTheme.MONET -> if (isDark) Color(0xFF7B8FBF) else Color(0xFF607CB8)
                            HeatmapColorTheme.OCEAN -> if (isDark) Color(0xFF1DA1B0) else Color(0xFF1E8FA3)
                        }
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 20.dp else 16.dp)
                                .background(
                                    dotColor,
                                    RoundedCornerShape(50)
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.25f),
                                        RoundedCornerShape(50)
                                    ) else Modifier
                                )
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    hapticManager.tapLight()
                                    colorTheme = theme
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Metric Row ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeatmapMetricItem(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "连续背诵",
                    value = "${heatmapStats.currentStreak} 天",
                    accentColor = StreakFlame
                )
                HeatmapMetricItem(
                    icon = Icons.Default.MilitaryTech,
                    label = "最长坚持",
                    value = "${heatmapStats.longestStreak} 天",
                    accentColor = WarningGold
                )
                HeatmapMetricItem(
                    label = "累计打卡",
                    value = "${heatmapStats.activeDays} 天",
                    accentColor = SuccessGreen
                )
                HeatmapMetricItem(
                    label = "总背诵量",
                    value = "${heatmapStats.totalReviews} 次",
                    accentColor = StudyBlueAccent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Anki-style Heatmap Grid ────────────────────────────────────────
            val scrollState = rememberScrollState()
            LaunchedEffect(scrollState) {
                snapshotFlow { scrollState.maxValue }
                    .filter { it > 0 && it < Int.MAX_VALUE }
                    .first()
                scrollState.scrollTo(scrollState.maxValue)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                // Month labels row (Anki-style: above the grid)
                Row(
                    modifier = Modifier.padding(start = labelColWidth + 4.dp)
                ) {
                    for (col in 0 until totalWeeks) {
                        val monthEntry = monthLabels.firstOrNull { it.first == col }
                        Box(
                            modifier = Modifier.width(cellSize + cellGap),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (monthEntry != null) {
                                Text(
                                    text = monthEntry.second,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))

                // Weekday labels + grid
                Row {
                    // Weekday labels: M T W T F S S (Anki style)
                    Column(
                        modifier = Modifier
                            .width(labelColWidth)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                            // Show only M, W, F to avoid crowding (Anki shows every other)
                            val showLabel = label == "M" || label == "W" || label == "F"
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .padding(end = 2.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (showLabel) {
                                    Text(
                                        text = label,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        color = TextTertiary,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 8.sp
                                    )
                                }
                            }
                        }
                    }

                    // The week columns
                    Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                        for (col in 0 until totalWeeks) {
                            Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                                for (row in 0 until 7) {
                                    val date = daysMatrix[row][col]
                                    if (date != null && !date.isAfter(endDate)) {
                                        val dateStr = date.format(formatter)
                                        val count = heatmapStats.dailyReviewMap[dateStr] ?: 0
                                        val isToday  = (date == today)
                                        val isFuture = date.isAfter(today)

                                        val rawCellColor = when {
                                            isFuture -> Color.Transparent
                                            count == 0 -> animEmpty
                                            count <= adaptiveThresholds[0] -> animL1
                                            count <= adaptiveThresholds[1] -> animL2
                                            count <= adaptiveThresholds[2] -> animL3
                                            else                           -> animL4
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(cellSize)
                                                .background(rawCellColor, RoundedCornerShape(cellRadius))
                                                .then(
                                                    if (isToday) Modifier.border(
                                                        1.dp,
                                                        animBorder,
                                                        RoundedCornerShape(cellRadius)
                                                    ) else Modifier
                                                )
                                                .clickable(
                                                    enabled = !isFuture,
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = androidx.compose.material.ripple.rememberRipple(
                                                        bounded = false,
                                                        radius = 10.dp
                                                    )
                                                ) {
                                                    hapticManager.tapLight()
                                                    selectedDateInfo = Pair(dateStr, count)
                                                }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(cellSize))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Footer: Selection Info + Legend ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedDateInfo != null) {
                    val (dStr, cnt) = selectedDateInfo!!
                    Text(
                        text = "$dStr · 背诵 $cnt 次",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = if (cnt > 0) StudyBlueAccent else TextSecondary
                    )
                } else {
                    Text(
                        text = "轻触方格查看背诵记录",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = TextTertiary
                    )
                }

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = "少", fontSize = 9.sp, fontFamily = FontFamily.SansSerif, color = TextTertiary)
                    listOf(animEmpty, animL1, animL2, animL3, animL4).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(color, RoundedCornerShape(1.5.dp))
                        )
                    }
                    Text(text = "多", fontSize = 9.sp, fontFamily = FontFamily.SansSerif, color = TextTertiary)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeatmapMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String,
    value: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = accentColor
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            color = TextTertiary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
