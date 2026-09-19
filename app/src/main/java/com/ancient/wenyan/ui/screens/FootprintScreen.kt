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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.notification.ReminderWorker
import com.ancient.wenyan.ui.components.OnboardingTutorialDialog
import com.ancient.wenyan.ui.components.RecitationHeatmapCard
import com.ancient.wenyan.ui.components.ReminderSettingsDialog
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

    var showTutorialDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val reminderTime = remember { repository.getReminderTime() }
    val isReminderOn = remember { repository.isReminderEnabled() }

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
                if (enabled) {
                    ReminderWorker.scheduleDailyReminder(context, hour, minute)
                } else {
                    ReminderWorker.cancelDailyReminder(context)
                }
                showReminderDialog = false
            }
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
                        fontFamily = FontFamily.Serif,
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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Heatmap Component
            item {
                RecitationHeatmapCard(heatmapStats = heatmapStats)
            }

            // System Utilities & Guidance Header
            item {
                Text(
                    text = "系统设置与指引",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Reminder Setting Tile
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            soundManager.playClick()
                            showReminderDialog = true
                        }
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BgSurface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(StudyBlueLight, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = StudyBlueAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "每日背诵定时提醒",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Serif,
                                color = TextPrimary
                            )
                            val hourStr = reminderTime.first.toString().padStart(2, '0')
                            val minStr = reminderTime.second.toString().padStart(2, '0')
                            Text(
                                text = if (isReminderOn) "每日 $hourStr:$minStr 定时推送打卡通知" else "提醒已关闭",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif,
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

            // Tutorial Tile
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            soundManager.playClick()
                            showTutorialDialog = true
                        }
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BgSurface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BgSurfaceMuted, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "新手研习指南",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Serif,
                                color = TextPrimary
                            )
                            Text(
                                text = "四步了解 FSRS 算法评分与双轨背诵技巧",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif,
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
        }
    }
}
