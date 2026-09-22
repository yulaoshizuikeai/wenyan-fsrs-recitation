package com.ancient.wenyan.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.model.ActiveSession
import com.ancient.wenyan.ui.components.DailyGoalSettingsDialog
import com.ancient.wenyan.ui.components.BookSelectionDialog
import com.ancient.wenyan.ui.components.FSRSConfigDialog
import com.ancient.wenyan.ui.components.FeedbackPreferencesDialog
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*
import com.ancient.wenyan.ui.viewmodel.DashboardViewModel
import java.time.LocalDate

data class ClassicalQuote(
    val title: String,
    val author: String,
    val quote: String,
    val translation: String
)

val CURATED_QUOTES = listOf(
    ClassicalQuote(
        title = "短歌行",
        author = "曹操",
        quote = "“山不厌高，海不厌深。周公吐哺，天下归心。”",
        translation = "高山不辞土石才见其巍峨，大海不纳细流难成其浩瀚。志存高远者海纳百川，终成大业。"
    ),
    ClassicalQuote(
        title = "劝学",
        author = "荀子",
        quote = "“不积跬步，无以至千里；不积小流，无以成江海。”",
        translation = "不积累一步半步的行程，就无法到达千里之远；不汇聚细小的流水，就成就不了辽阔江海。背诵重在日日研读。"
    ),
    ClassicalQuote(
        title = "离骚",
        author = "屈原",
        quote = "“路漫漫其修远兮，吾将上下而求索。”",
        translation = "前方的道路漫长而悠远，我将百折不挠、上下探求心中的理想与光明。"
    ),
    ClassicalQuote(
        title = "滕王阁序",
        author = "王勃",
        quote = "“老当益壮，宁移白首之心？穷且益坚，不坠青云之志。”",
        translation = "年纪虽老志气更坚，哪能改变白头之年的操守？身处困境更需坚韧，绝不丢弃直上青云的凌云壮志。"
    ),
    ClassicalQuote(
        title = "赤壁赋",
        author = "苏轼",
        quote = "“逝者如斯，而未尝往也；盈虚者如彼，而卒莫消长也。”",
        translation = "万物变迁流逝不停，但其本源未曾消失；月亮圆缺代代相续，其本体终无增减。以旷达从容之心对岁月。"
    )
)

@Composable
fun DashboardScreen(
    repository: WenYanRepository? = null,
    viewModel: DashboardViewModel = if (repository != null) {
        remember(repository) { DashboardViewModel(repository) }
    } else {
        hiltViewModel()
    },
    onStartTodayReview: () -> Unit,
    onStartGaoKaoReview: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onResumeActiveSession: (ActiveSession) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.stats
    val selectedBookScope = uiState.selectedBookScope
    val selectedBookName = uiState.selectedBookName
    val heatmapStats = uiState.heatmapStats
    val activeSession = uiState.activeSession
    val studyGoals = uiState.studyGoals
    val todayProgress = uiState.todayProgress
    val currentRepo = remember(viewModel) { viewModel.getRepository() }

    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }
    var isSoundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }

    var showBookDialog by remember { mutableStateOf(false) }
    var showFSRSConfigDialog by remember { mutableStateOf(false) }
    var showDailyGoalDialog by remember { mutableStateOf(false) }

    // Rotating daily quote based on day-of-year, tap to cycle
    val dayOfYear = remember { LocalDate.now().dayOfYear }
    var quoteIndex by remember { mutableIntStateOf(dayOfYear % CURATED_QUOTES.size) }
    val currentQuote = CURATED_QUOTES[quoteIndex]

    // Flame Breathing Animation for active streak
    val infiniteTransition = rememberInfiniteTransition(label = "flame_breathing")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (heatmapStats.currentStreak > 0) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    if (showBookDialog) {
        BookSelectionDialog(
            currentScope = selectedBookScope,
            currentName = selectedBookName,
            onDismiss = { showBookDialog = false },
            onConfirmSelection = { newScope, newName ->
                viewModel.setSelectedBookScope(newScope, newName)
                showBookDialog = false
            }
        )
    }

    if (showDailyGoalDialog) {
        DailyGoalSettingsDialog(
            currentConfig = studyGoals,
            onDismiss = { showDailyGoalDialog = false },
            onConfirm = { newConfig ->
                viewModel.setStudyGoalsConfig(newConfig)
                showDailyGoalDialog = false
            }
        )
    }

    if (showFSRSConfigDialog) {
        FSRSConfigDialog(
            repository = currentRepo,
            onDismiss = { showFSRSConfigDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ====================================================================
        // Material 3 TopAppBar
        // ====================================================================
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "文言背诵",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StudyBlueAccent.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "高中必背",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudyBlueAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "FSRS-5 间隔记忆 · 熟读成诵",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                // Quick mute/unmute
                IconButton(
                    onClick = {
                        val next = !isSoundEnabled
                        isSoundEnabled = next
                        soundManager.isSoundEnabled = next
                        hapticManager.tapLight()
                        if (next) soundManager.playClick()
                    }
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "音效开关",
                        tint = if (isSoundEnabled) StudyBlueAccent else MaterialTheme.colorScheme.outline
                    )
                }

                // Dedicated Settings Entry to SettingsScreen
                IconButton(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        onOpenSettings()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置中心与子菜单",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        // ====================================================================
        // Scrollable Body Content
        // ====================================================================
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp)
        ) {
            // ----------------------------------------------------------------
            // 0. Active Session Resume Card (Anki-like 断点续背)
            // ----------------------------------------------------------------
            activeSession?.let { session ->
                item(key = "active_session_card") {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            onResumeActiveSession(session)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "未完待续 · 点击继续背诵",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        hapticManager.tapLight()
                                        currentRepo.clearActiveSession()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "放弃本次进度",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val progress = if (session.totalCards > 0) {
                                (session.currentIndex.toFloat() / session.totalCards.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "当前进度：第 ${session.currentIndex + 1} / ${session.totalCards} 句",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "已记 ${session.completedCount} 句",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
            // ----------------------------------------------------------------
            // 1. Mission Header: Today's Recitation Objective & Quick Jump
            // ----------------------------------------------------------------
            item(key = "mission_header_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Streak Pill via M3 SuggestionChip
                    SuggestionChip(
                        onClick = {},
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "连胜火焰",
                                tint = if (heatmapStats.currentStreak > 0) StreakFlame else MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(flameScale)
                            )
                        },
                        label = {
                            Text(
                                text = if (heatmapStats.currentStreak > 0) "${heatmapStats.currentStreak} 天连胜" else "今日未打卡",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (heatmapStats.currentStreak > 0) StreakFlame else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (heatmapStats.currentStreak > 0) StreakFlame.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = null
                    )

                    // Scope Selector Capsule via M3 AssistChip
                    AssistChip(
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            showBookDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = StudyBlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = selectedBookName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "切换教材",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // ----------------------------------------------------------------
            // 1.5 Today's Study & Review Goals Progress Card (Anki 每日学习目标看板)
            // ----------------------------------------------------------------
            item(key = "daily_goals_progress_card") {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(StudyBlueLight, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = StudyBlueAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "今日学习目标",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = studyGoals.orderPreference.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    hapticManager.tapLight()
                                    soundManager.playClick()
                                    showDailyGoalDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("调整目标", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target 1: New Cards Today
                        val newTargetStr = if (studyGoals.dailyNewLimit >= 999) "不限" else if (studyGoals.dailyNewLimit <= 0) "暂停新学" else "${studyGoals.dailyNewLimit} 句"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(SuccessGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "新学目标",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "已学 ${todayProgress.todayNewLearned} / $newTargetStr",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (todayProgress.isNewGoalReached) SuccessGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { todayProgress.newProgressPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SuccessGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target 2: Review Cards Today
                        val reviewTargetStr = if (studyGoals.dailyReviewLimit >= 999) "不限" else "${studyGoals.dailyReviewLimit} 句"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(StreakFlame, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "复习目标",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "已复习 ${todayProgress.todayReviewed} / $reviewTargetStr",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (todayProgress.isReviewGoalReached) StreakFlame else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { todayProgress.reviewProgressPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = StreakFlame,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // If both goals reached, show celebratory badge
                        if (todayProgress.isNewGoalReached && todayProgress.isReviewGoalReached) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SuccessGreen.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "今日设定的背诵目标已全部圆满达成！",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------------------
            // 2. Focused Core Memory Mission Card (M3 OutlinedCard)
            // ----------------------------------------------------------------
            item(key = "fsrs_status_card") {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header with FSRS Model indicator and tuning shortcut
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "背诵进度与记忆状态",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudyBlueLight,
                                    modifier = Modifier.clickable {
                                        hapticManager.tapLight()
                                        soundManager.playClick()
                                        showFSRSConfigDialog = true
                                    }
                                ) {
                                    Text(
                                        text = "FSRS-5",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StudyBlueAccent
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        hapticManager.tapLight()
                                        soundManager.playClick()
                                        showFSRSConfigDialog = true
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "算法设置",
                                    tint = StudyBlueAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "算法调优",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StudyBlueAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4-Dimension FSRS Memory Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetric(
                                label = "待复习",
                                value = stats.dueCards,
                                color = if (stats.dueCards > 0) DueRed else TextPrimary
                            )
                            StatMetric(
                                label = "学习中",
                                value = stats.learningCards,
                                color = StudyBlueAccent
                            )
                            StatMetric(
                                label = "已稳固",
                                value = stats.reviewCards,
                                color = SuccessGreen
                            )
                            StatMetric(
                                label = "留存率",
                                value = stats.retentionPercentage.toInt(),
                                isPercentage = true,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // High-Contrast Primary Study Button with spring bounce & tactile pulse
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
                                hapticManager.tapLight()
                                soundManager.playClick()
                                onStartTodayReview()
                            },
                            interactionSource = reviewInteractionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .scale(reviewScale),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (stats.dueCards > 0) "开始今日复习 (${stats.dueCards} 句到期)" else "开始背诵新内容",
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "➔",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // ----------------------------------------------------------------
            // 3. Classical Quote Card: Minimalist M3 OutlinedCard
            // ----------------------------------------------------------------
            item(key = "daily_quote_card") {
                OutlinedCard(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        quoteIndex = (quoteIndex + 1) % CURATED_QUOTES.size
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = StudyBlueAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "每日名句",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "《${currentQuote.title}》· ${currentQuote.author}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        AnimatedContent(
                            targetState = currentQuote,
                            transitionSpec = {
                                fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessHigh))
                            },
                            label = "quote_switch"
                        ) { quote ->
                            Column {
                                Text(
                                    text = quote.quote,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextPrimary,
                                    lineHeight = 25.sp
                                )

                                Text(
                                    text = quote.translation,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextSecondary,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------------------
            // 4. Quick Practice Dual Tiles
            // ----------------------------------------------------------------
            item(key = "quick_practice_tiles") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GaoKao 72 Quick Card
                    OutlinedCard(
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            onStartGaoKaoReview()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
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
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "必背考点一键抽查",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Practice Quick Card
                    OutlinedCard(
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            onNavigateToPractice()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
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
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "自由设定抽取范围",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
fun StatMetric(label: String, value: Int, isPercentage: Boolean = false, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                fadeOut(spring(stiffness = Spring.StiffnessHigh))
            },
            label = "stat_num"
        ) { targetVal ->
            Text(
                text = if (isPercentage) "$targetVal%" else "$targetVal",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            color = TextTertiary,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
