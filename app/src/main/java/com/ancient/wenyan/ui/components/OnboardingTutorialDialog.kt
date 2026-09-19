package com.ancient.wenyan.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ancient.wenyan.ui.theme.*

data class TutorialStep(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val bulletPoints: List<String>
)

@Composable
fun OnboardingTutorialDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        TutorialStep(
            stepNumber = 1,
            title = "FSRS 科学间隔重复记忆",
            subtitle = "告别死记硬背 · 依记忆遗忘曲线精准调度",
            icon = Icons.Default.Psychology,
            accentColor = BambooGreen,
            bulletPoints = listOf(
                "四档复习评分：Again(重来)、Hard(困难)、Good(良好)、Easy(简单)；",
                "算法实时测算记忆稳定性与难度，预测下次复习黄金时间；",
                "每次背诵几分钟，高效巩固文言长效持久记忆。"
            )
        ),
        TutorialStep(
            stepNumber = 2,
            title = "随心选择背诵哪本书",
            subtitle = "覆盖高中 11 册教材 · 聚焦当前学期目标",
            icon = Icons.Default.MenuBook,
            accentColor = CeladonBlue,
            bulletPoints = listOf(
                "首页顶部一键切换当前背诵图书（如只背《必修上册》或《选修欣赏》）；",
                "提供“全部教材”、“必修全套”、“高考72篇”等快捷预设；",
                "今日复习与掌握进度将精准匹配所选图书范围。"
            )
        ),
        TutorialStep(
            stepNumber = 3,
            title = "单句翻卡与整篇渐进遮挡",
            subtitle = "双轨互动背诵 · 逐层攻克长篇文赋",
            icon = Icons.Default.FlipCameraAndroid,
            accentColor = CinnabarRed,
            bulletPoints = listOf(
                "单句翻转闪卡：出句测对句，正面提示、背面查验注解与释义；",
                "整篇渐进遮挡：从 L0 原文、L1 关键词、L2 半句，到 L3 首字骨架与 L4 全盲默写；",
                "逐段点选对照，攻克《劝学》《赤壁赋》《离骚》等高考长篇文言。"
            )
        ),
        TutorialStep(
            stepNumber = 4,
            title = "研墨打卡 · 背诵热力图",
            subtitle = "日积跬步以至千里 · 见证每日坚持足迹",
            icon = Icons.Default.CalendarMonth,
            accentColor = MutedGold,
            bulletPoints = listOf(
                "近百日足迹方格热力图，直观记录每天背诵强度；",
                "统计连续坚持天数与最长打卡记录，养成背诵习惯；",
                "全离线本地存储，纯净无广告，随时随地专注研读。"
            )
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, XuanBorder, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Seal & Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "新手研习指南 (${currentStep + 1}/${steps.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkMedium
                    )

                    TextButton(onClick = onComplete) {
                        Text("跳过导引", fontSize = 12.sp, fontFamily = FontFamily.Serif, color = InkFaded)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content Animated Switch
                val step = steps[currentStep]
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tutorial_step"
                ) { current ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Icon Circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(current.accentColor.copy(alpha = 0.12f), CircleShape)
                                .border(1.5.dp, current.accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = current.icon,
                                contentDescription = null,
                                tint = current.accentColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = current.title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = current.subtitle,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            color = InkMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        // Bullet Points Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, XuanBorder, RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = XuanPaperLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                current.bulletPoints.forEach { point ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "•",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = current.accentColor,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            text = point,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Serif,
                                            color = InkCharcoal,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in steps.indices) {
                        Box(
                            modifier = Modifier
                                .size(if (i == currentStep) 8.dp else 6.dp)
                                .background(
                                    if (i == currentStep) BambooGreen else XuanBorder,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InkMedium)
                        ) {
                            Text("上一步", fontFamily = FontFamily.Serif)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < steps.size - 1) {
                                currentStep++
                            } else {
                                onComplete()
                            }
                        },
                        modifier = Modifier.weight(if (currentStep > 0) 1.2f else 1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BambooGreen)
                    ) {
                        Text(
                            text = if (currentStep < steps.size - 1) "下一步" else "完成导引，开始背诵",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
