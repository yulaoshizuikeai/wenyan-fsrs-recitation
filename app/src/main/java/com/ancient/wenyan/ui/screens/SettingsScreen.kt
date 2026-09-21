package com.ancient.wenyan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.model.RecitationOrderMode
import com.ancient.wenyan.notification.ReminderWorker
import com.ancient.wenyan.ui.components.*
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: WenYanRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    // Dialog Visibility States
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showFSRSDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showBookDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showOrderModeDialog by remember { mutableStateOf(false) }

    // Observed States
    val studyGoals by repository.studyGoalsConfig.collectAsState()
    val recitationOrderMode by repository.recitationOrderMode.collectAsState()
    val selectedBookName by repository.selectedBookName.collectAsState()
    val selectedBookScope by repository.selectedBookScope.collectAsState()

    var reminderTime by remember { mutableStateOf(repository.getReminderTime()) }
    var isReminderOn by remember { mutableStateOf(repository.isReminderEnabled()) }

    // Dialogs
    if (showDailyGoalDialog) {
        DailyGoalSettingsDialog(
            currentConfig = studyGoals,
            onDismiss = { showDailyGoalDialog = false },
            onConfirm = { newConfig ->
                repository.setStudyGoalsConfig(newConfig)
                showDailyGoalDialog = false
            }
        )
    }

    if (showFSRSDialog) {
        FSRSConfigDialog(
            repository = repository,
            onDismiss = { showFSRSDialog = false }
        )
    }

    if (showReminderDialog) {
        ReminderSettingsDialog(
            initialHour = reminderTime.first,
            initialMinute = reminderTime.second,
            isReminderEnabled = isReminderOn,
            onDismiss = { showReminderDialog = false },
            onConfirm = { hour, minute, enabled ->
                repository.setReminderTime(hour, minute)
                repository.setReminderEnabled(enabled)
                reminderTime = Pair(hour, minute)
                isReminderOn = enabled
                if (enabled) {
                    ReminderWorker.scheduleDailyReminder(context, hour, minute)
                } else {
                    ReminderWorker.cancelDailyReminder(context)
                }
                showReminderDialog = false
            }
        )
    }

    if (showFeedbackDialog) {
        FeedbackPreferencesDialog(
            onDismiss = { showFeedbackDialog = false }
        )
    }

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

    if (showTutorialDialog) {
        OnboardingTutorialDialog(
            onDismiss = { showTutorialDialog = false },
            onComplete = {
                repository.setOnboardingCompleted(true)
                showTutorialDialog = false
            }
        )
    }

    if (showOrderModeDialog) {
        AlertDialog(
            onDismissRequest = { showOrderModeDialog = false },
            title = {
                Text(
                    text = "选择篇章背诵顺序",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecitationOrderMode.entries.forEach { mode ->
                        val isSelected = (recitationOrderMode == mode)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) StudyBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hapticManager.tapLight()
                                    soundManager.playClick()
                                    repository.setRecitationOrderMode(mode)
                                    showOrderModeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        hapticManager.tapLight()
                                        soundManager.playClick()
                                        repository.setRecitationOrderMode(mode)
                                        showOrderModeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = StudyBlueAccent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = mode.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrderModeDialog = false }) {
                    Text("完成")
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = DueRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "确定重置所有背诵数据？",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "此操作将清空已记录的卡片复习历史、FSRS 稳定度状态与连胜打卡，恢复至未学习状态。该操作不可撤销！",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        hapticManager.warningThud()
                        soundManager.playWrong()
                        repository.clearPersistedCardStates()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DueRed)
                ) {
                    Text("确认重置", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "设置中心",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "文言背诵偏好与功能管理",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回主页",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // ================================================================
            // Submenu 1: 学习与复习目标 (Daily Study & Review Goals)
            // ================================================================
            item {
                SettingsCategoryHeader(
                    title = "学习与复习目标",
                    subtitle = "类似 Anki 的每日新卡配额与复习负荷控制"
                )
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Tile 1: Daily Target Setting
                        val newDesc = if (studyGoals.dailyNewLimit >= 999) "不限" else if (studyGoals.dailyNewLimit <= 0) "暂停新学" else "${studyGoals.dailyNewLimit} 句"
                        val revDesc = if (studyGoals.dailyReviewLimit >= 999) "不限" else "${studyGoals.dailyReviewLimit} 句"
                        SettingsActionItem(
                            icon = Icons.Default.Flag,
                            iconTint = StudyBlueAccent,
                            iconBg = StudyBlueLight,
                            title = "每日新学与复习上限",
                            subtitle = "新学上限：$newDesc · 复习上限：$revDesc · ${studyGoals.orderPreference.displayName}",
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showDailyGoalDialog = true
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Tile 2: Recitation Sequence Mode
                        SettingsActionItem(
                            icon = Icons.Default.Reorder,
                            iconTint = StreakFlame,
                            iconBg = StreakFlame.copy(alpha = 0.12f),
                            title = "篇章排布与原序",
                            subtitle = recitationOrderMode.displayName,
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showOrderModeDialog = true
                            }
                        )
                    }
                }
            }

            // ================================================================
            // Submenu 2: FSRS 记忆算法调优 (Memory Algorithm)
            // ================================================================
            item {
                SettingsCategoryHeader(
                    title = "记忆算法与调度",
                    subtitle = "最新 FSRS-5 间隔重复模型参数与自适应拟合"
                )
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsActionItem(
                        icon = Icons.Default.Psychology,
                        iconTint = StudyBlueAccent,
                        iconBg = StudyBlueLight,
                        title = "FSRS-5 算法参数调优",
                        subtitle = "记忆保留率 ${(repository.fsrsEngine.requestRetention * 100).toInt()}% · 19项权重矩阵与自动拟合",
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            showFSRSDialog = true
                        }
                    )
                }
            }

            // ================================================================
            // Submenu 3: 提醒与通知 (Reminders)
            // ================================================================
            item {
                SettingsCategoryHeader(
                    title = "背诵打卡提醒",
                    subtitle = "设定每日坚持背诵的最佳推送时钟"
                )
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val hourStr = reminderTime.first.toString().padStart(2, '0')
                    val minStr = reminderTime.second.toString().padStart(2, '0')
                    SettingsActionItem(
                        icon = Icons.Default.NotificationsActive,
                        iconTint = SuccessGreen,
                        iconBg = SuccessGreen.copy(alpha = 0.12f),
                        title = "每日定时背诵通知",
                        subtitle = if (isReminderOn) "每日 $hourStr:$minStr 定时推送督促背诵" else "定时打卡提醒已暂停",
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            showReminderDialog = true
                        }
                    )
                }
            }

            // ================================================================
            // Submenu 4: 交互与多媒体 (Sensory Feedback)
            // ================================================================
            item {
                SettingsCategoryHeader(
                    title = "视听与触感偏好",
                    subtitle = "按键音效、答题翻卡声与震动触觉调节"
                )
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsActionItem(
                        icon = Icons.Default.Vibration,
                        iconTint = Color(0xFF8E24AA),
                        iconBg = Color(0xFF8E24AA).copy(alpha = 0.12f),
                        title = "动效音效与触感震动",
                        subtitle = if (soundManager.isSoundEnabled && hapticManager.isHapticEnabled) "音效开启 · 触感开启" else "部分已关闭",
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            showFeedbackDialog = true
                        }
                    )
                }
            }

            // ================================================================
            // Submenu 5: 教材与篇目范围 (Curriculum Selection)
            // ================================================================
            item {
                SettingsCategoryHeader(
                    title = "教材与篇目范围",
                    subtitle = "切换当前背诵的高考与统编教材篇目"
                )
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsActionItem(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        iconTint = StudyBlueAccent,
                        iconBg = StudyBlueLight,
                        title = "背诵课本与选篇范围",
                        subtitle = "当前所选：$selectedBookName",
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            showBookDialog = true
                        }
                    )
                }
            }

            // ================================================================
            // Submenu 6: 维护与关于 (Maintenance & About)
            // ================================================================
            item {
                SettingsCategoryHeader(
                    title = "应用维护与关于",
                    subtitle = "新手引导、复习数据维护与版本信息"
                )
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsActionItem(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconBg = MaterialTheme.colorScheme.surfaceVariant,
                            title = "新手使用指南",
                            subtitle = "重温 FSRS 记忆算法、翻卡及默写背诵技巧",
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showTutorialDialog = true
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsActionItem(
                            icon = Icons.Default.DeleteOutline,
                            iconTint = DueRed,
                            iconBg = DueRed.copy(alpha = 0.12f),
                            title = "重置所有背诵数据",
                            subtitle = "清空本地卡片记忆历史，重置为新篇章",
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showResetConfirmDialog = true
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // App Version & About
                        ListItem(
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(StudyBlueAccent.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = StudyBlueAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = "关于 文言背诵",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "版本 v1.2.0 · 离线文库 72篇高考必背 · 统编 11 册教材 · FSRS-5 算法驱动",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
