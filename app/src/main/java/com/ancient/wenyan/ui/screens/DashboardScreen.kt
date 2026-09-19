package com.ancient.wenyan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.ui.theme.*

@Composable
fun DashboardScreen(
    repository: WenYanRepository,
    onNavigateToChapters: () -> Unit,
    onNavigateToRandom: () -> Unit,
    onStartTodayReview: () -> Unit,
    onStartGaoKaoReview: () -> Unit
) {
    val stats by repository.statsFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XuanPaperLight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header: Classical Title & Seal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "文言背诵",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = InkCharcoal,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "高中课内全篇目 · FSRS 间隔重复记忆",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Vermilion Seal Stamp (朱砂印章)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .rotate(-5f)
                    .background(Color(0x159E2A2B), RoundedCornerShape(6.dp))
                    .border(2.dp, CinnabarRed, RoundedCornerShape(6.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "熟读\n成诵",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = CinnabarRed,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Dashboard Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, XuanBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日记忆看板",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkCharcoal
                    )
                    Text(
                        text = "FSRS-5 算法在线",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Serif,
                        color = BambooGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMetric(label = "到期复习", value = "${stats.dueCards}", color = CinnabarRed)
                    StatMetric(label = "学习中", value = "${stats.learningCards}", color = MutedGold)
                    StatMetric(label = "已掌握", value = "${stats.reviewCards}", color = BambooGreen)
                    StatMetric(label = "记忆保持率", value = "${stats.retentionPercentage.toInt()}%", color = CeladonBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Call to Action Button: Today's FSRS Review
        Button(
            onClick = onStartTodayReview,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BambooGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (stats.dueCards > 0) "开始今日复习 (${stats.dueCards}张到期)" else "开始今日学习新卡",
                fontSize = 17.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "功能导航",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = InkMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // Navigation Features
        DashboardNavCard(
            title = "章节篇目系统学习",
            subtitle = "必修与选修共 11 册教材 · 100篇诗文目录循序渐进",
            icon = Icons.Default.Bookmarks,
            accentColor = BambooGreen,
            onClick = onNavigateToChapters
        )

        Spacer(modifier = Modifier.height(12.dp))

        DashboardNavCard(
            title = "跨篇目随机背诵",
            subtitle = "自由设定抽取范围与卡片数量 · 高频交叉强化",
            icon = Icons.Default.Shuffle,
            accentColor = MutedGold,
            onClick = onNavigateToRandom
        )

        Spacer(modifier = Modifier.height(12.dp))

        DashboardNavCard(
            title = "高考必背 72 篇专项",
            subtitle = "一键抽查教育部高考统编课标核心默写重点句",
            icon = Icons.Default.Star,
            accentColor = CinnabarRed,
            onClick = onStartGaoKaoReview
        )
    }
}

@Composable
fun StatMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            color = InkFaded,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun DashboardNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, XuanBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    color = InkCharcoal
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = InkFaded
            )
        }
    }
}
