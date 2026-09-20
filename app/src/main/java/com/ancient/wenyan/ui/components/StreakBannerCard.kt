package com.ancient.wenyan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@Composable
fun StreakBannerCard(
    currentStreak: Int,
    isTodayReviewed: Boolean,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "streak_card_scale"
    )

    // Flame Breathing Animation for active streak
    val infiniteTransition = rememberInfiniteTransition(label = "flame_breathing")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (currentStreak > 0) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Flame Icon + Streak Count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (currentStreak > 0) StreakFlame.copy(alpha = 0.12f) else BgSurfaceMuted,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "打卡火焰",
                        tint = if (currentStreak > 0) StreakFlame else TextTertiary,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(flameScale)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$currentStreak",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = if (currentStreak > 0) TextPrimary else TextTertiary,
                            letterSpacing = (-1.0).sp,
                            lineHeight = 32.sp
                        )
                        Text(
                            text = "天连胜",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = if (currentStreak > 0) StreakFlame else TextTertiary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Text(
                        text = if (currentStreak > 0) "连胜坚持中 · 日拱一卒" else "今日未打卡 · 开启新连胜",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            // Right: Status Chip or Quick Action Button
            if (isTodayReviewed) {
                Box(
                    modifier = Modifier
                        .background(SuccessGreen.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "今日已完成",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = SuccessGreen
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        onStartReview()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(
                        text = "去打卡 ➔",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White
                    )
                }
            }
        }
    }
}
