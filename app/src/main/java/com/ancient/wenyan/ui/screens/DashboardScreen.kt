package com.ancient.wenyan.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.ui.components.BookSelectionDialog
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: WenYanRepository,
    onStartTodayReview: () -> Unit,
    onStartGaoKaoReview: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPractice: () -> Unit
) {
    val stats by repository.statsFlow.collectAsState()
    val selectedBookScope by repository.selectedBookScope.collectAsState()
    val selectedBookName by repository.selectedBookName.collectAsState()
    val heatmapStats by repository.heatmapStatsFlow.collectAsState()

    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    var isSoundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }

    var showBookDialog by remember { mutableStateOf(false) }

    if (showBookDialog) {
        BookSelectionDialog(
            currentScope = selectedBookScope,
            currentName = selectedBookName,
            onDismiss = { showBookDialog = false },
            onConfirmSelection = { newScope, newName ->
                repository.setSelectedBookScope(newScope, newName)
                showBookDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "文言背诵",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(StreakFlame.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "高中课标",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = StreakFlame,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "FSRS-5 间隔重复记忆 · 熟读成诵",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                },
                actions = {
                    // Mute/Unmute sound effect
                    IconButton(
                        onClick = {
                            val next = !isSoundEnabled
                            isSoundEnabled = next
                            soundManager.isSoundEnabled = next
                            if (next) soundManager.playClick()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(BgSurface, RoundedCornerShape(18.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "音效开关",
                            tint = if (isSoundEnabled) StudyBlueAccent else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCanvas)
            )
        },
        containerColor = BgCanvas
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // ================================================================
            // 1. Unified Hero Study Deck (连胜打卡 + 教材切换 + 四维记忆指标 + 主行动按钮)
            // ================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BgSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Top row: Streak (Left) & Book Pill (Right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left: Streak counter
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            color = if (heatmapStats.currentStreak > 0) StreakFlame.copy(alpha = 0.12f) else BgSurfaceMuted,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "打卡火焰",
                                        tint = if (heatmapStats.currentStreak > 0) StreakFlame else TextTertiary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${heatmapStats.currentStreak}",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.SansSerif,
                                            color = if (heatmapStats.currentStreak > 0) TextPrimary else TextTertiary,
                                            letterSpacing = (-0.5).sp,
                                            lineHeight = 30.sp
                                        )
                                        Text(
                                            text = "天连胜",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif,
                                            color = if (heatmapStats.currentStreak > 0) StreakFlame else TextTertiary,
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = if (heatmapStats.currentStreak > 0) "连胜坚持中 · 日拱一卒" else "今日未打卡 · 开启新连胜",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Right: Book Selector Pill
                            val targetArticlesCount = if (selectedBookScope.isNullOrEmpty()) {
                                100
                            } else {
                                CurriculumDataSource.ALL_ARTICLES.count { it.moduleId in selectedBookScope!! }
                            }

                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        soundManager.playClick()
                                        showBookDialog = true
                                    },
                                shape = RoundedCornerShape(20.dp),
                                color = BgSurfaceMuted,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = StudyBlueAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$selectedBookName (${targetArticlesCount}篇)",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "切换",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Middle: 4-Dimension FSRS Memory Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetric(label = "待复习", value = "${stats.dueCards}", color = if (stats.dueCards > 0) DueRed else TextPrimary)
                            StatMetric(label = "学习中", value = "${stats.learningCards}", color = StudyBlueAccent)
                            StatMetric(label = "已稳固", value = "${stats.reviewCards}", color = SuccessGreen)
                            StatMetric(label = "留存率", value = "${stats.retentionPercentage.toInt()}%", color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Study Button with spring feedback
                        val reviewInteractionSource = remember { MutableInteractionSource() }
                        val isReviewPressed by reviewInteractionSource.collectIsPressedAsState()
                        val reviewScale by animateFloatAsState(
                            targetValue = if (isReviewPressed) 0.97f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "review_btn_scale"
                        )

                        Button(
                            onClick = {
                                soundManager.playClick()
                                onStartTodayReview()
                            },
                            interactionSource = reviewInteractionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .scale(reviewScale),
                            colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (stats.dueCards > 0) "开始今日复习 (${stats.dueCards} 句到期)" else "开启今日研习新词句",
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "➔", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // ================================================================
            // 2. Classical Quote Card (每日文韵金句)
            // ================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BgSurface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "名句鉴赏 · 日有所诵",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = TextSecondary
                            )
                            Text(
                                text = "《短歌行》· 曹操",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif,
                                color = TextTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "“山不厌高，海不厌深。周公吐哺，天下归心。”",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            color = TextPrimary,
                            lineHeight = 26.sp
                        )

                        Text(
                            text = "高山不辞土石才见其巍峨，大海不纳细流难成其浩瀚。周公求贤一饭三吐哺，天下豪杰由是真心归附。",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            color = TextSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // ================================================================
            // 3. Quick Action Dual Tiles (快捷分流卡片)
            // ================================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GaoKao 72 Quick Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                soundManager.playClick()
                                onStartGaoKaoReview()
                            }
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BgSurface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(StreakFlame.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = StreakFlame,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "高考 72 篇专项",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = TextPrimary
                            )
                            Text(
                                text = "必背考点一键抽查",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Practice Quick Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                soundManager.playClick()
                                onNavigateToPractice()
                            }
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BgSurface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(StudyBlueLight, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                    tint = StudyBlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "跨篇随机练习",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = TextPrimary
                            )
                            Text(
                                text = "自由设定抽取范围",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
            color = TextTertiary,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
