package com.ancient.wenyan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.BuildConfig
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

    // Dialog / Sheet Visibility States
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

    // Dialogs & Sheets
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
                    Text(
                        text = "设置中心",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp)
        ) {
            // ================================================================
            // 分组一：学习与调度 (Study & Algorithm) - 4合1大卡片
            // ================================================================
            item {
                SettingsCategoryHeader(title = "学习与调度")
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // 1. 每日目标
                        val newDesc = if (studyGoals.dailyNewLimit >= 999) "不限" else if (studyGoals.dailyNewLimit <= 0) "暂停新学" else "${studyGoals.dailyNewLimit}句"
                        val revDesc = if (studyGoals.dailyReviewLimit >= 999) "不限" else "${studyGoals.dailyReviewLimit}句"
                        SettingsActionItem(
                            icon = Icons.Default.Flag,
                            title = "每日新学与复习上限",
                            subtitle = "新学 $newDesc · 复习 $revDesc · ${studyGoals.orderPreference.displayName}",
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showDailyGoalDialog = true
                            }
                        )

                        SettingsItemDivider()

                        // 2. 篇章排布与原序
                        SettingsActionItem(
                            icon = Icons.Default.Reorder,
                            title = "篇章排布与原序",
                            subtitle = recitationOrderMode.displayName,
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showOrderModeDialog = true
                            }
                        )

                        SettingsItemDivider()

                        // 3. FSRS 算法
                        SettingsActionItem(
                            icon = Icons.Default.Psychology,
                            title = "FSRS-5 算法参数调优",
                            subtitle = "保留率 ${(repository.fsrsEngine.requestRetention * 100).toInt()}% · 19项权重参数拟合",
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showFSRSDialog = true
                            }
                        )

                        SettingsItemDivider()

                        // 4. 定时背诵提醒
                        val hourStr = reminderTime.first.toString().padStart(2, '0')
                        val minStr = reminderTime.second.toString().padStart(2, '0')
                        SettingsActionItem(
                            icon = Icons.Default.NotificationsActive,
                            title = "每日定时背诵通知",
                            subtitle = if (isReminderOn) "每日 $hourStr:$minStr 定时推送" else "打卡提醒已暂停",
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showReminderDialog = true
                            }
                        )
                    }
                }
            }

            // ================================================================
            // 分组二：偏好与教材 (Preferences & Curriculum) - 2合1大卡片
            // ================================================================
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsCategoryHeader(title = "偏好与教材")
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // 1. 视听偏好
                        SettingsActionItem(
                            icon = Icons.Default.Vibration,
                            title = "动效音效与触感震动",
                            subtitle = if (soundManager.isSoundEnabled && hapticManager.isHapticEnabled) "音效开启 · 触感开启" else "部分已关闭",
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showFeedbackDialog = true
                            }
                        )

                        SettingsItemDivider()

                        // 2. 教材选篇
                        SettingsActionItem(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
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
            }

            // ================================================================
            // 分组三：维护与关于 (System & About) - 3合1大卡片
            // ================================================================
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsCategoryHeader(title = "数据与关于")
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // 1. 使用指南
                        SettingsActionItem(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = "新手使用指南",
                            subtitle = "重温 FSRS 算法与背诵技巧",
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showTutorialDialog = true
                            }
                        )

                        SettingsItemDivider()

                        // 2. 高危操作：重置数据
                        SettingsActionItem(
                            icon = Icons.Default.DeleteOutline,
                            title = "重置所有背诵数据",
                            subtitle = "清空本地卡片记忆历史，恢复为未学状态",
                            isDestructive = true,
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showResetConfirmDialog = true
                            }
                        )

                        SettingsItemDivider()

                        // 3. 关于文言背诵
                        SettingsActionItem(
                            icon = Icons.Default.Info,
                            title = "关于 文言背诵",
                            subtitle = "版本 v${BuildConfig.VERSION_NAME} · 离线文库 72篇高考必背 · FSRS-5 算法驱动",
                            onClick = {}
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
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}

/**
 * 彻底锁定垂直居中的设置条目。
 * 摒弃 M3 ListItem 在多行模式下强制 leadingContent Top 对齐的黑盒行为，
 * 无论文字多少行，左侧 38dp 图标与右侧操作标均坚固保持在几何中心水平线上，绝不被挤上去。
 */
@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val iconColor = if (isDestructive) DueRed else StudyBlueAccent
    val iconBgColor = if (isDestructive) DueRed.copy(alpha = 0.12f) else StudyBlueLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 左侧图标：尺寸锁定 38dp，绝对居中
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconBgColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 2. 中间文本区域：自动占据剩余宽度，垂直居中排版
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                fontSize = 14.sp,
                color = if (isDestructive) DueRed else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3. 右侧操作标识：高危操作展示醒目红标，常规操作展示微透 Chevron
        if (isDestructive) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = DueRed.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "重置",
                    color = DueRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
