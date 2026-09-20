package com.ancient.wenyan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Vibration
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
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.notification.ReminderWorker
import com.ancient.wenyan.ui.components.FeedbackPreferencesDialog
import com.ancient.wenyan.ui.components.OnboardingTutorialDialog
import com.ancient.wenyan.ui.components.RecitationHeatmapCard
import com.ancient.wenyan.ui.components.ReminderSettingsDialog
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootprintScreen(
    repository: WenYanRepository
) {
    val heatmapStats by repository.heatmapStatsFlow.collectAsState()
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    var showTutorialDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    var reminderTime by remember { mutableStateOf(repository.getReminderTime()) }
    var isReminderOn by remember { mutableStateOf(repository.isReminderEnabled()) }

    LaunchedEffect(showReminderDialog) {
        if (showReminderDialog) {
            reminderTime = repository.getReminderTime()
            isReminderOn = repository.isReminderEnabled()
        }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "研墨足迹",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextPrimary
                    )
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
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Heatmap Component
            item {
                RecitationHeatmapCard(heatmapStats = heatmapStats)
            }

            // System Utilities & Guidance Header
            item {
                Text(
                    text = "背诵设置与偏好",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Feedback Preferences (Sound & Haptics) Tile
            item {
                OutlinedCard(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        showFeedbackDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
                ) {
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(StreakFlame.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = StreakFlame,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                text = "动效音效与触感震动",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "按键触感、翻卡与答题音效调节",
                                style = MaterialTheme.typography.bodySmall,
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
            }

            // Reminder Setting Tile
            item {
                OutlinedCard(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        showReminderDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
                ) {
                    val hourStr = reminderTime.first.toString().padStart(2, '0')
                    val minStr = reminderTime.second.toString().padStart(2, '0')
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(StudyBlueLight, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = StudyBlueAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                text = "每日背诵定时提醒",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                text = if (isReminderOn) "每日 $hourStr:$minStr 定时推送打卡通知" else "提醒已暂停",
                                style = MaterialTheme.typography.bodySmall,
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
            }

            // Tutorial Tile
            item {
                OutlinedCard(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        showTutorialDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
                ) {
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                text = "新手使用指南",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "四步了解记忆算法与翻卡、填空背诵技巧",
                                style = MaterialTheme.typography.bodySmall,
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
            }
        }
    }
}
