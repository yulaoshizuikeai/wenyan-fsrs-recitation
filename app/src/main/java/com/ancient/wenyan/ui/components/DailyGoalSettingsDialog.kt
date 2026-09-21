package com.ancient.wenyan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ancient.wenyan.domain.model.StudyGoalsConfig
import com.ancient.wenyan.domain.model.StudyOrderPreference
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyGoalSettingsDialog(
    currentConfig: StudyGoalsConfig,
    onDismiss: () -> Unit,
    onConfirm: (StudyGoalsConfig) -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    var newLimit by remember { mutableIntStateOf(currentConfig.dailyNewLimit) }
    var reviewLimit by remember { mutableIntStateOf(currentConfig.dailyReviewLimit) }
    var orderPref by remember { mutableStateOf(currentConfig.orderPreference) }

    val newOptions = listOf(
        0 to "0 (纯复习)",
        5 to "5 句",
        10 to "10 句",
        20 to "20 句",
        30 to "30 句",
        999 to "不设限"
    )

    val reviewOptions = listOf(
        20 to "20 句",
        50 to "50 句",
        100 to "100 句",
        200 to "200 句",
        999 to "不设限"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BgSurface),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(StudyBlueLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = StudyBlueAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "每日背诵与复习目标",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            )
                            Text(
                                text = "设定每日新学配额与复习负荷 (Anki风格)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextTertiary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextTertiary)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: Daily New Cards Limit
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "每日新学句子上限",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (newLimit >= 999) "不设限" else if (newLimit <= 0) "今日暂停新学" else "$newLimit 句/日",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudyBlueAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "控制每日首次学习的新卡片数量。选 0 可进入“纯复习模式”消灭积压。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            newOptions.forEach { (count, label) ->
                                val selected = (newLimit == count)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        hapticManager.tapLight()
                                        soundManager.playClick()
                                        newLimit = count
                                    },
                                    label = { Text(text = label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudyBlueLight,
                                        selectedLabelColor = StudyBlueAccent
                                    )
                                )
                            }
                        }

                        // Fine Adjustment Stepper
                        if (newLimit in 1..998) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "微调：",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalIconButton(
                                    onClick = {
                                        hapticManager.tapLight()
                                        newLimit = (newLimit - 5).coerceAtLeast(1)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "-5", modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalIconButton(
                                    onClick = {
                                        hapticManager.tapLight()
                                        newLimit = (newLimit + 5).coerceAtMost(100)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+5", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Daily Review Cards Limit
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = StreakFlame,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "每日复习上限",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (reviewLimit >= 999) "不设限" else "$reviewLimit 句/日",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StreakFlame
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "限制今日最大复习卡片量，防范复习卡片滚雪球导致弃坑。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reviewOptions.forEach { (count, label) ->
                                val selected = (reviewLimit == count)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        hapticManager.tapLight()
                                        soundManager.playClick()
                                        reviewLimit = count
                                    },
                                    label = { Text(text = label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StreakFlame.copy(alpha = 0.12f),
                                        selectedLabelColor = StreakFlame
                                    )
                                )
                            }
                        }

                        // Fine Adjustment Stepper
                        if (reviewLimit in 1..998) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "微调：",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalIconButton(
                                    onClick = {
                                        hapticManager.tapLight()
                                        reviewLimit = (reviewLimit - 10).coerceAtLeast(10)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "-10", modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalIconButton(
                                    onClick = {
                                        hapticManager.tapLight()
                                        reviewLimit = (reviewLimit + 10).coerceAtMost(500)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+10", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Study Order Preference
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = StudyBlueAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "卡片出题优先顺序",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        StudyOrderPreference.entries.forEach { pref ->
                            val isSelected = (orderPref == pref)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        hapticManager.tapLight()
                                        soundManager.playClick()
                                        orderPref = pref
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) StudyBlueLight else Color.Transparent,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            hapticManager.tapLight()
                                            soundManager.playClick()
                                            orderPref = pref
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = StudyBlueAccent
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = pref.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = pref.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            onConfirm(
                                StudyGoalsConfig(
                                    dailyNewLimit = newLimit,
                                    dailyReviewLimit = reviewLimit,
                                    orderPreference = orderPref
                                )
                            )
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyBlueAccent)
                    ) {
                        Text("保存目标", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
