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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.notification.ReminderWorker
import com.ancient.wenyan.ui.components.BookSelectionDialog
import com.ancient.wenyan.ui.components.OnboardingTutorialDialog
import com.ancient.wenyan.ui.components.RecitationHeatmapCard
import com.ancient.wenyan.ui.components.ReminderSettingsDialog
import com.ancient.wenyan.ui.components.StreakBannerCard
import com.ancient.wenyan.ui.sound.SoundEffectManager
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
    val selectedBookScope by repository.selectedBookScope.collectAsState()
    val selectedBookName by repository.selectedBookName.collectAsState()
    val heatmapStats by repository.heatmapStatsFlow.collectAsState()

    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    var isSoundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }

    var showBookDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember {
        mutableStateOf(!repository.isOnboardingCompleted())
    }
    var showReminderDialog by remember { mutableStateOf(false) }

    val reminderTime = remember { repository.getReminderTime() }
    val isReminderOn = remember { repository.isReminderEnabled() }

    // Book Selection Modal
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

    // Beginner Tutorial Modal
    if (showTutorialDialog) {
        OnboardingTutorialDialog(
            onDismiss = {
                repository.setOnboardingCompleted(true)
                showTutorialDialog = false
            },
            onComplete = {
                repository.setOnboardingCompleted(true)
                showTutorialDialog = false
            }
        )
    }

    // Daily Reminder Settings Modal
    if (showReminderDialog) {
        ReminderSettingsDialog(
            initialHour = reminderTime.first,
            initialMinute = reminderTime.second,
            isReminderEnabled = isReminderOn,
            onDismiss = { showReminderDialog = false },
            onConfirm = { hour, minute, enabled ->
                repository.setReminderTime(hour, minute)
                repository.setReminderEnabled(enabled)
                if (enabled) {
                    ReminderWorker.scheduleDailyReminder(context, hour, minute)
                } else {
                    ReminderWorker.cancelDailyReminder(context)
                }
                showReminderDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header: Minimalist Clean Title & Subtitle + Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "文言背诵",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "高中全篇目 · FSRS-5 间隔记忆",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Reminder Quick Toggle / Settings
                IconButton(
                    onClick = {
                        soundManager.playClick()
                        showReminderDialog = true
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(BgSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "提醒设置",
                        tint = StudyBlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sound Effect Toggle
                IconButton(
                    onClick = {
                        val next = !isSoundEnabled
                        isSoundEnabled = next
                        soundManager.isSoundEnabled = next
                        if (next) soundManager.playClick()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(BgSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "音效开关",
                        tint = if (isSoundEnabled) TextPrimary else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tutorial Entry Button
                IconButton(
                    onClick = {
                        soundManager.playClick()
                        showTutorialDialog = true
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(BgSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "研习指南",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Book Selector Card (自选背诵教材)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBookDialog = true }
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BgSurface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(StudyBlueLight, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = StudyBlueAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "背诵教材范围",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val targetArticlesCount = if (selectedBookScope.isNullOrEmpty()) {
                                100
                            } else {
                                CurriculumDataSource.ALL_ARTICLES.count { it.moduleId in selectedBookScope!! }
                            }
                            Box(
                                modifier = Modifier
                                    .background(StudyBlueLight, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$targetArticlesCount 篇",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = StudyBlueAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = selectedBookName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Switch Book Button Pill
                Box(
                    modifier = Modifier
                        .background(BgSurfaceMuted, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "切换",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 百词斩极简打卡横幅卡片 (Streak Banner Card)
        StreakBannerCard(
            currentStreak = heatmapStats.currentStreak,
            isTodayReviewed = stats.todayReviewedCount > 0,
            onStartReview = {
                soundManager.playClick()
                onStartTodayReview()
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Stats Dashboard Card (Vercel 极简现代看板)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
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
                        text = "今日记忆看板",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextPrimary
                    )
                    Text(
                        text = "FSRS-5 ACTIVE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = StudyBlueAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMetric(label = "DUE", value = "${stats.dueCards}", color = if (stats.dueCards > 0) DueRed else TextPrimary)
                    StatMetric(label = "LEARNING", value = "${stats.learningCards}", color = StudyBlueAccent)
                    StatMetric(label = "REVIEW", value = "${stats.reviewCards}", color = SuccessGreen)
                    StatMetric(label = "RETENTION", value = "${stats.retentionPercentage.toInt()}%", color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Call to Action Button: Today's FSRS Review
        val reviewInteractionSource = remember { MutableInteractionSource() }
        val isReviewPressed by reviewInteractionSource.collectIsPressedAsState()
        val reviewScale by animateFloatAsState(
            targetValue = if (isReviewPressed) 0.96f else 1.0f,
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
                .height(54.dp)
                .scale(reviewScale),
            colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (stats.dueCards > 0) "开始今日复习 (${stats.dueCards} 句待复习)" else "开始今日研习新卡",
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Recitation Heatmap Card (研墨足迹 · GitHub 蓝热力图)
        RecitationHeatmapCard(heatmapStats = heatmapStats)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "功能导航",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // Navigation Features
        DashboardNavCard(
            title = "章节篇目系统研读",
            subtitle = "必修与选修共 11 册教材 · 100 篇诗文目录循序渐进",
            icon = Icons.Default.Bookmarks,
            accentColor = StudyBlueAccent,
            onClick = onNavigateToChapters
        )

        Spacer(modifier = Modifier.height(10.dp))

        DashboardNavCard(
            title = "跨篇目随机背诵",
            subtitle = "自由设定抽取范围与卡片数量 · 高频交叉强化",
            icon = Icons.Default.Shuffle,
            accentColor = TextSecondary,
            onClick = onNavigateToRandom
        )

        Spacer(modifier = Modifier.height(10.dp))

        DashboardNavCard(
            title = "高考必背 72 篇专项",
            subtitle = "一键抽查教育部高考统编课标核心默写重点句",
            icon = Icons.Default.Star,
            accentColor = StreakFlame,
            onClick = onStartGaoKaoReview
        )

        Spacer(modifier = Modifier.height(10.dp))

        DashboardNavCard(
            title = "每日提醒设置 (Daily Reminder)",
            subtitle = "设定每日定时推送打卡通知 · 保持连续研习连胜",
            icon = Icons.Default.NotificationsActive,
            accentColor = StudyBlueAccent,
            onClick = { showReminderDialog = true }
        )

        Spacer(modifier = Modifier.height(10.dp))

        DashboardNavCard(
            title = "新手研习指南",
            subtitle = "了解 FSRS-5 算法评分、自选课本与整篇渐进遮挡",
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            accentColor = TextSecondary,
            onClick = { showTutorialDialog = true }
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
            fontFamily = FontFamily.SansSerif,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            color = TextTertiary,
            modifier = Modifier.padding(top = 3.dp)
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
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
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
                    .size(42.dp)
                    .background(BgSurfaceMuted, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextTertiary
            )
        }
    }
}
