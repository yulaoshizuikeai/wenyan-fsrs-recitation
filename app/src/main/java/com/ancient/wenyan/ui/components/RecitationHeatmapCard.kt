package com.ancient.wenyan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
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

@Composable
fun RecitationHeatmapCard(
    heatmapStats: HeatmapStats,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    var selectedDateInfo by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val emptyCellColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val level1Color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.65f) else Color(0xFFBFDBFE)
    val level2Color = if (isDark) Color(0xFF2563EB).copy(alpha = 0.75f) else Color(0xFF60A5FA)
    val level3Color = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val level4Color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1E3A8A)

    // Display past 14 weeks (98 days)
    val totalWeeks = 14
    val startDate = remember { today.minusWeeks((totalWeeks - 1).toLong()).with(DayOfWeek.MONDAY) }
    val endDate = remember { today.plusDays((7 - today.dayOfWeek.value).toLong()) } // till end of current week

    val daysMatrix = remember(startDate, endDate) {
        val matrix = Array(7) { arrayOfNulls<LocalDate>(totalWeeks) }
        var curr = startDate
        var col = 0
        while (!curr.isAfter(endDate) && col < totalWeeks) {
            val row = curr.dayOfWeek.value - 1 // 0: Mon, 6: Sun
            matrix[row][col] = curr
            if (row == 6) col++
            curr = curr.plusDays(1)
        }
        matrix
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Total Review Count Badge
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

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BgSurfaceMuted
                ) {
                    Text(
                        text = "共研读 ${heatmapStats.totalReviews} 次",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Streak & Total Statistics Row
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

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Heatmap Grid (Scrollable horizontally)
            val scrollState = rememberScrollState()
            LaunchedEffect(scrollState) {
                // Scroll to the latest weeks once layout measurement provides a valid maxValue
                snapshotFlow { scrollState.maxValue }
                    .filter { it > 0 && it < Int.MAX_VALUE }
                    .first()
                scrollState.scrollTo(scrollState.maxValue)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Weekday labels on the left: 一, 三, 五, 日
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.5.dp),
                    modifier = Modifier.padding(end = 4.dp, top = 1.dp)
                ) {
                    listOf("一", "", "三", "", "五", "", "日").forEach { label ->
                        Box(modifier = Modifier.size(15.dp), contentAlignment = Alignment.Center) {
                            if (label.isNotEmpty()) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Days Matrix: 7 rows x totalWeeks columns
                Row(horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    for (col in 0 until totalWeeks) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.5.dp)) {
                            for (row in 0 until 7) {
                                val date = daysMatrix[row][col]
                                if (date != null && !date.isAfter(endDate)) {
                                    val dateStr = date.format(formatter)
                                    val count = heatmapStats.dailyReviewMap[dateStr] ?: 0
                                    val isToday = (date == today)
                                    val isFuture = date.isAfter(today)

                                    val cellColor = when {
                                        isFuture -> Color.Transparent
                                        count == 0 -> emptyCellColor
                                        count in 1..4 -> level1Color
                                        count in 5..9 -> level2Color
                                        count in 10..19 -> level3Color
                                        else -> level4Color
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(15.dp)
                                            .background(
                                                color = cellColor,
                                                shape = RoundedCornerShape(3.5.dp)
                                            )
                                            .then(
                                                if (isToday) Modifier.border(1.4.dp, StreakFlame, RoundedCornerShape(3.5.dp))
                                                else Modifier
                                            )
                                            .clickable(enabled = !isFuture) {
                                                hapticManager.tapLight()
                                                selectedDateInfo = Pair(dateStr, count)
                                            }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Selected Date Detail or Tip + Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedDateInfo != null) {
                    val (dStr, cnt) = selectedDateInfo!!
                    Text(
                        text = "$dStr · 背诵 $cnt 次",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = if (cnt > 0) StudyBlueAccent else TextSecondary
                    )
                } else {
                    Text(
                        text = "轻触方格查看背诵记录",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = TextTertiary
                    )
                }

                // Legend: 少 ⬜ 🟦 🟦 🟦 🟦 多
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(text = "少", fontSize = 10.sp, fontFamily = FontFamily.SansSerif, color = TextTertiary)
                    Box(modifier = Modifier.size(10.dp).background(emptyCellColor, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(10.dp).background(level1Color, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(10.dp).background(level2Color, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(10.dp).background(level3Color, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(10.dp).background(level4Color, RoundedCornerShape(2.dp)))
                    Text(text = "多", fontSize = 10.sp, fontFamily = FontFamily.SansSerif, color = TextTertiary)
                }
            }
        }
    }
}

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
