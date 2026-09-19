package com.ancient.wenyan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.domain.model.HeatmapStats
import com.ancient.wenyan.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RecitationHeatmapCard(
    heatmapStats: HeatmapStats,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    var selectedDateInfo by remember { mutableStateOf<Pair<String, Int>?>(null) }

    // Display past 14 weeks (98 days)
    val totalWeeks = 14
    // Find the end date (Sunday of current week)
    val daysUntilSunday = DayOfWeek.SUNDAY.value - today.dayOfWeek.value
    val endDate = remember { today.plusDays(daysUntilSunday.toLong()) }
    val startDate = remember { endDate.minusWeeks(totalWeeks.toLong()).plusDays(1) }

    // Build the grid: 7 rows (Mon to Sun) x totalWeeks columns
    val daysMatrix = remember(heatmapStats.dailyReviewMap, today) {
        val matrix = Array(7) { arrayOfNulls<LocalDate>(totalWeeks) }
        var curr = startDate
        var weekIndex = 0
        while (!curr.isAfter(endDate) && weekIndex < totalWeeks) {
            val dayIndex = curr.dayOfWeek.value - 1 // 0 (Mon) to 6 (Sun)
            matrix[dayIndex][weekIndex] = curr
            if (curr.dayOfWeek == DayOfWeek.SUNDAY) {
                weekIndex++
            }
            curr = curr.plusDays(1)
        }
        matrix
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, XuanBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Title & Seal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = BambooGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "研墨足迹 · 背诵热力图",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkCharcoal
                    )
                }

                Text(
                    text = "近百日寒暑不辍",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium
                )
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
                    accentColor = CinnabarRed
                )
                HeatmapMetricItem(
                    icon = Icons.Default.MilitaryTech,
                    label = "最长坚持",
                    value = "${heatmapStats.longestStreak} 天",
                    accentColor = MutedGold
                )
                HeatmapMetricItem(
                    label = "累计打卡",
                    value = "${heatmapStats.activeDays} 天",
                    accentColor = BambooGreen
                )
                HeatmapMetricItem(
                    label = "总背诵量",
                    value = "${heatmapStats.totalReviews} 次",
                    accentColor = CeladonBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Heatmap Grid with horizontal scroll
            val scrollState = rememberScrollState(Int.MAX_VALUE) // Scroll to the right (latest days)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day of week labels (一, 三, 五, 日)
                Column(
                    modifier = Modifier.padding(end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val dayNames = listOf("一", "", "三", "", "五", "", "日")
                    dayNames.forEach { name ->
                        Box(
                            modifier = Modifier.size(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Serif,
                                color = InkFaded
                            )
                        }
                    }
                }

                // 7 rows x N columns
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (col in 0 until totalWeeks) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (row in 0 until 7) {
                                val date = daysMatrix[row][col]
                                if (date != null && !date.isAfter(endDate)) {
                                    val dateStr = date.format(formatter)
                                    val count = heatmapStats.dailyReviewMap[dateStr] ?: 0
                                    val isToday = (date == today)
                                    val isFuture = date.isAfter(today)

                                    val cellColor = when {
                                        isFuture -> Color.Transparent
                                        count == 0 -> Color(0xFFF1F5F9) // Slate 100
                                        count in 1..4 -> Color(0xFFBFDBFE) // Blue 200
                                        count in 5..9 -> Color(0xFF60A5FA) // Blue 400
                                        count in 10..19 -> Color(0xFF2563EB) // Blue 600
                                        else -> Color(0xFF1E3A8A) // Blue 900
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                color = cellColor,
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                            .then(
                                                if (isToday) Modifier.border(1.2.dp, StreakFlame, RoundedCornerShape(3.dp))
                                                else Modifier
                                            )
                                            .clickable(enabled = !isFuture) {
                                                selectedDateInfo = Pair(dateStr, count)
                                            }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Selected Date Detail or Tip + Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive details display
                if (selectedDateInfo != null) {
                    val (dStr, cnt) = selectedDateInfo!!
                    Text(
                        text = "$dStr · 研习 $cnt 次",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = if (cnt > 0) StudyBlueAccent else TextSecondary
                    )
                } else {
                    Text(
                        text = "轻触格点查验历史",
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
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFBFDBFE), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF60A5FA), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF2563EB), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF1E3A8A), RoundedCornerShape(2.dp)))
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
                fontFamily = FontFamily.Serif,
                color = accentColor
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            color = InkFaded,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
