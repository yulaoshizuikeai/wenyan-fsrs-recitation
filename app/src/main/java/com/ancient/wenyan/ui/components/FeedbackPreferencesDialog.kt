package com.ancient.wenyan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@Composable
fun FeedbackPreferencesDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    var isSoundOn by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var isHapticOn by remember { mutableStateOf(hapticManager.isHapticEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BgSurface),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(StudyBlueLight, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = StudyBlueAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "动效音效与触感",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            )
                            Text(
                                text = "Audio & Haptic Feedback",
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

                // Toggle 1: Sound Effects
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgSurfaceMuted, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = null,
                            tint = if (isSoundOn) StudyBlueAccent else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "研读音效反馈",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            )
                            Text(
                                text = "翻卡、答题、编钟凯歌声效",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isSoundOn,
                        onCheckedChange = {
                            isSoundOn = it
                            soundManager.isSoundEnabled = it
                            hapticManager.tapLight()
                            if (it) soundManager.playClick()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StudyBlueAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle 2: Haptic Vibration
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgSurfaceMuted, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = if (isHapticOn) StudyBlueAccent else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "触感震动反馈",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            )
                            Text(
                                text = "高保真微触、轻击、节律震感",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isHapticOn,
                        onCheckedChange = {
                            isHapticOn = it
                            hapticManager.isHapticEnabled = it
                            if (it) hapticManager.successPulse()
                            soundManager.playClick()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StudyBlueAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "体验感测试通道 (轻触试听试震)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Feedback Test Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FeedbackTestChip(
                            label = "翻卡纸掠",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playFlip()
                                hapticManager.cardFlip()
                            }
                        )
                        FeedbackTestChip(
                            label = "良好编钟",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playCorrect()
                                hapticManager.successPulse()
                            }
                        )
                        FeedbackTestChip(
                            label = "简单三和弦",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playEasy()
                                hapticManager.successPulse()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FeedbackTestChip(
                            label = "重来木琴",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playWrong()
                                hapticManager.warningThud()
                            }
                        )
                        FeedbackTestChip(
                            label = "挖空气泡",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playClozeReveal()
                                hapticManager.clozePop()
                            }
                        )
                        FeedbackTestChip(
                            label = "通关凯歌",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playCelebration()
                                hapticManager.celebrationFanfare()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        hapticManager.tapLight()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("完成并返回", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FeedbackTestChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = BgSurfaceMuted,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}
