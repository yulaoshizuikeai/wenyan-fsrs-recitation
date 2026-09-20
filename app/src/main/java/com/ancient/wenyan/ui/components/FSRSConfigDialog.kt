package com.ancient.wenyan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.FSRSEngine
import com.ancient.wenyan.domain.fsrs.OptimizationResult
import com.ancient.wenyan.domain.model.RecitationOrderMode
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Chinese descriptive labels for the 19 FSRS-5 weights
private val WEIGHT_DESCRIPTIONS = listOf(
    "w0: 重来初始稳固度 (S0 Again)",
    "w1: 困难初始稳固度 (S0 Hard)",
    "w2: 良好初始稳固度 (S0 Good)",
    "w3: 简单初始稳固度 (S0 Easy)",
    "w4: 初始难度基准 (D0 Base)",
    "w5: 难度递增阶梯 (D0 Slope)",
    "w6: 评分难度增量 (ΔD Modifier)",
    "w7: 难度均值回归 (D Mean Reversion)",
    "w8: 良好复习稳定度增长 (Recall S Base)",
    "w9: 稳定度幂次阻尼 (Recall S Damping)",
    "w10: 可提取性指数因子 (Recall Retrievability)",
    "w11: 遗忘后长期衰减系数 (Forget S Base)",
    "w12: 遗忘难度阻尼 (Forget Damping)",
    "w13: 遗忘稳定度因子 (Forget Stability)",
    "w14: 遗忘留存衰减指数 (Forget Retrievability)",
    "w15: 困难惩罚系数 (Hard Penalty)",
    "w16: 简单奖励系数 (Easy Bonus)",
    "w17: 短期复习倍率 (Short-term Multiplier)",
    "w18: 短期评分偏置 (Short-term Grade Offset)"
)

private val MAX_INTERVAL_OPTIONS = listOf(
    365 to "1年",
    730 to "2年",
    3650 to "10年",
    36500 to "终身"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FSRSConfigDialog(
    repository: WenYanRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    val coroutineScope = rememberCoroutineScope()
    var isOptimizing by remember { mutableStateOf(false) }

    // Read current engine states
    var retention by remember { mutableFloatStateOf(repository.fsrsEngine.requestRetention.toFloat()) }
    var factor by remember { mutableFloatStateOf(repository.fsrsEngine.recitationStabilityFactor.toFloat()) }
    var maxInterval by remember { mutableIntStateOf(repository.fsrsEngine.maximumInterval) }
    var currentWeights by remember { mutableStateOf(repository.fsrsEngine.weights.clone()) }
    var autoTuneEnabled by remember { mutableStateOf(repository.isAutoTuneEnabled()) }
    var selectedOrderMode by remember { mutableStateOf(repository.recitationOrderMode.value) }

    var optimizationResult by remember { mutableStateOf<OptimizationResult?>(null) }
    var isExpandedWeights by remember { mutableStateOf(false) }

    val reviewLogs = remember { repository.getReviewLogs() }
    val lastOptimizedTime = remember { repository.getLastOptimizedTime() }

    val lastOptFormatted = remember(lastOptimizedTime) {
        lastOptimizedTime?.let {
            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BgSurface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // ============================================================
                // Top Header
                // ============================================================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(StudyBlueLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = StudyBlueAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FSRS 记忆调度与参数调优",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "FSRS-5 Engine & Adaptive Recitation",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextTertiary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ============================================================
                // Scrollable Content
                // ============================================================
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --------------------------------------------------------
                    // 0. Recitation Sequence Preference (顺承篇章原序)
                    // --------------------------------------------------------
                    item {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StudyBlueAccent.copy(alpha = 0.35f)),
                            colors = CardDefaults.outlinedCardColors(containerColor = StudyBlueLight.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.FormatLineSpacing,
                                            contentDescription = null,
                                            tint = StudyBlueAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "背诵次序偏好",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selectedOrderMode == RecitationOrderMode.SEQUENTIAL) SuccessGreen.copy(alpha = 0.12f) else BorderSubtle
                                    ) {
                                        Text(
                                            text = if (selectedOrderMode == RecitationOrderMode.SEQUENTIAL) "推荐 · 保护语脉" else "自定义",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (selectedOrderMode == RecitationOrderMode.SEQUENTIAL) SuccessGreen else TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "对于背诵古诗文言文，避免因记忆算法打乱诗文起承转合。调度器在保留 FSRS 精确到期计算的同时，确保同一篇目内卡片严格顺承原文先后次序。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                RecitationOrderMode.entries.forEach { mode ->
                                    val isSelected = (selectedOrderMode == mode)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) BgSurface else Color.Transparent,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) StudyBlueAccent else BorderSubtle
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable {
                                                hapticManager.tapLight()
                                                selectedOrderMode = mode
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    hapticManager.tapLight()
                                                    selectedOrderMode = mode
                                                },
                                                colors = RadioButtonDefaults.colors(selectedColor = StudyBlueAccent)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = mode.displayName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = mode.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextTertiary,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --------------------------------------------------------
                    // 1. Core Scheduling Sliders
                    // --------------------------------------------------------
                    item {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            colors = CardDefaults.outlinedCardColors(containerColor = BgCanvas)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "调度核心参数",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Request Retention Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "目标记忆保留率",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StudyBlueLight
                                    ) {
                                        Text(
                                            text = "${(retention * 100).roundToInt()}%",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = StudyBlueAccent
                                        )
                                    }
                                }
                                Slider(
                                    value = retention,
                                    onValueChange = { retention = it },
                                    valueRange = 0.80f..0.97f,
                                    steps = 16,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudyBlueAccent,
                                        activeTrackColor = StudyBlueAccent,
                                        inactiveTrackColor = BorderFocus
                                    )
                                )
                                Text(
                                    text = "保留率越高，复习越频繁、记忆越牢固。古文背诵建议设为 90% ~ 95%。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    thickness = 0.6.dp,
                                    color = BorderSubtle
                                )

                                // Recitation Stability Factor Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "诗文稳定度调节系数",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StudyBlueLight
                                    ) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", factor)}x",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = StudyBlueAccent
                                        )
                                    }
                                }
                                Slider(
                                    value = factor,
                                    onValueChange = { factor = it },
                                    valueRange = 0.50f..1.00f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudyBlueAccent,
                                        activeTrackColor = StudyBlueAccent,
                                        inactiveTrackColor = BorderFocus
                                    )
                                )
                                Text(
                                    text = "文言句子比单个单词更易遗忘。系数越小，初次掌握后的复习越紧凑（推荐 0.72x）。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    thickness = 0.6.dp,
                                    color = BorderSubtle
                                )

                                // Maximum Interval Chips
                                Text(
                                    text = "最大复习间隔封顶",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MAX_INTERVAL_OPTIONS.forEach { (days, label) ->
                                        val isSelected = maxInterval == days
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                hapticManager.tapLight()
                                                maxInterval = days
                                            },
                                            label = {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = StudyNavy,
                                                selectedLabelColor = Color.White,
                                                containerColor = BgSurface,
                                                labelColor = TextSecondary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = if (isSelected) StudyNavy else BorderSubtle
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --------------------------------------------------------
                    // 2. Local Adaptive Auto-Tuning Card
                    // --------------------------------------------------------
                    item {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StudyBlueAccent.copy(alpha = 0.35f)),
                            colors = CardDefaults.outlinedCardColors(containerColor = StudyBlueLight.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = StudyBlueAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "智能参数调优",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = StudyNavy
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = SuccessGreen.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "本地计算",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SuccessGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "根据你的背诵与复习记录，自动优化最契合你的记忆参数与复习周期。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Log Stats Row & Auto-tune toggle
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = "背诵时后台自动微调",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "已积累 ${reviewLogs.size} 条复习记录${if (lastOptFormatted != null) " · 上次优化 $lastOptFormatted" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextTertiary
                                        )
                                    },
                                    trailingContent = {
                                        Switch(
                                            checked = autoTuneEnabled,
                                            onCheckedChange = {
                                                hapticManager.tapLight()
                                                autoTuneEnabled = it
                                                repository.setAutoTuneEnabled(it)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = StudyBlueAccent
                                            )
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Trigger Optimization Button
                                Button(
                                    onClick = {
                                        if (!isOptimizing) {
                                            isOptimizing = true
                                            hapticManager.successPulse()
                                            soundManager.playCorrect()
                                            coroutineScope.launch {
                                                try {
                                                    val res = repository.optimizeFSRSParameters()
                                                    optimizationResult = res
                                                    if (res.success) {
                                                        currentWeights = res.optimizedWeights.clone()
                                                    }
                                                } finally {
                                                    isOptimizing = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isOptimizing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudyBlueAccent)
                                ) {
                                    if (isOptimizing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "正在智能迭代优化中...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "一键优化记忆参数",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Optimization Result Banner
                                AnimatedVisibility(
                                    visible = optimizationResult != null,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    val result = optimizationResult ?: return@AnimatedVisibility
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (result.success) SuccessGreen.copy(alpha = 0.10f) else WarningGold.copy(alpha = 0.10f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (result.success) SuccessGreen.copy(alpha = 0.35f) else WarningGold.copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = if (result.success) SuccessGreen else WarningGold,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (result.success) "优化成功！记忆拟合度提升 ${result.improvementPercentage}%" else "提示",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (result.success) SuccessGreen else WarningGold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = result.summaryText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimary
                                            )
                                            if (result.detailedChanges.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                result.detailedChanges.take(3).forEach { change ->
                                                    Text(
                                                        text = "• $change",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --------------------------------------------------------
                    // 3. Expandable 19 Weights Inspector
                    // --------------------------------------------------------
                    item {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            colors = CardDefaults.outlinedCardColors(containerColor = BgCanvas)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            hapticManager.tapLight()
                                            isExpandedWeights = !isExpandedWeights
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Dataset,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "查看 19 项 FSRS 核心参数",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpandedWeights) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = TextTertiary
                                    )
                                }

                                if (isExpandedWeights) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "这些参数由 FSRS 算法团队基于海量记忆数据测算得出，日常使用建议保持默认或使用上方自动优化：",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextTertiary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        currentWeights.forEachIndexed { index, weightValue ->
                                            val label = WEIGHT_DESCRIPTIONS.getOrElse(index) { "w$index: 未知参数" }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(BgSurface, RoundedCornerShape(8.dp))
                                                    .border(0.6.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = String.format(Locale.US, "%.4f", weightValue),
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = StudyBlueAccent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.8.dp,
                    color = BorderSubtle
                )

                // ============================================================
                // Bottom Actions (Reset + Save)
                // ============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            hapticManager.tapLight()
                            repository.resetFSRSSettingsToDefault()
                            repository.setRecitationOrderMode(RecitationOrderMode.SEQUENTIAL)
                            selectedOrderMode = RecitationOrderMode.SEQUENTIAL
                            retention = 0.93f
                            factor = 0.72f
                            maxInterval = 36500
                            currentWeights = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS.clone()
                            optimizationResult = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "恢复默认参数",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            hapticManager.successPulse()
                            soundManager.playCorrect()
                            repository.setRecitationOrderMode(selectedOrderMode)
                            repository.updateFSRSSettings(
                                weights = currentWeights,
                                retention = retention.toDouble(),
                                factor = factor.toDouble(),
                                maxInterval = maxInterval
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyNavy)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "保存并生效",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
