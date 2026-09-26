package com.ancient.wenyan.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.data.ChainedRecitationSummary
import com.ancient.wenyan.domain.cloze.SnowballChainingEngine
import com.ancient.wenyan.domain.cloze.SnowballStage
import com.ancient.wenyan.domain.cloze.SnowballUnit
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.ui.components.DuolingoStyleCelebration
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnowballRecitationScreen(
    articleId: String? = "art_bx1_14",
    repository: WenYanRepository? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val activeRepo = remember(context, repository) {
        repository ?: WenYanRepository.getInstance(context)
    }

    val article: Article = remember(articleId) {
        CurriculumDataSource.ARTICLE_MAP[articleId]
            ?: CurriculumDataSource.ARTICLE_MAP["art_bx1_14"]
            ?: CurriculumDataSource.ALL_ARTICLES.first()
    }

    val stages: List<SnowballStage> = remember(article) {
        SnowballChainingEngine.buildStages(article)
    }

    var currentStageIndex by rememberSaveable(articleId) { mutableIntStateOf(0) }
    var maskHistory by rememberSaveable(articleId) { mutableStateOf(true) }
    var isCelebrationActive by remember { mutableStateOf(false) }

    // Track unit indices where user stumbled / had transition difficulty
    var bottleneckIndices by rememberSaveable(
        articleId,
        stateSaver = Saver<Set<Int>, ArrayList<Int>>(
            save = { ArrayList(it) },
            restore = { it.toSet() }
        )
    ) { mutableStateOf(setOf<Int>()) }

    var isSettled by rememberSaveable(articleId) { mutableStateOf(false) }
    var summaryResult by remember { mutableStateOf<ChainedRecitationSummary?>(null) }
    var showSettlementDialog by remember { mutableStateOf(false) }

    val currentStage = stages.getOrNull(currentStageIndex)
    val isCompleted = currentStageIndex >= stages.size - 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "长篇滚雪球串联背诵",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StudyBlueLight
                            ) {
                                Text(
                                    text = "FSRS 联动",
                                    fontSize = 10.sp,
                                    color = StudyBlueAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "《${article.title}》· 逐段递进通篇贯通",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    FilterChip(
                        selected = maskHistory,
                        onClick = {
                            maskHistory = !maskHistory
                            hapticManager.tapLight()
                        },
                        label = { Text(if (maskHistory) "遮挡前文" else "显现全篇", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                if (maskHistory) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSurface)
            )
        },
        containerColor = BgCanvas
    ) { innerPadding ->
        if (currentStage == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("暂无长文篇章数据", color = TextSecondary)
            }
            return@Scaffold
        }

        val scrollState = rememberScrollState()

        LaunchedEffect(currentStageIndex) {
            scrollState.animateScrollTo(0)
        }

        Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text("串联第 ${currentStageIndex + 1} 联 / 共 ${stages.size} 联") },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    Text(
                        text = "${((currentStageIndex + 1).toFloat() / stages.size.toFloat() * 100f).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudyBlueAccent
                    )
                }

                LinearProgressIndicator(
                    progress = { currentStage.progressRatio },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = StudyBlueAccent,
                    trackColor = BgSurfaceMuted
                )

                // Explanatory Tag
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = BgSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snowboarding,
                            contentDescription = null,
                            tint = StudyBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "滚雪球法则：先温习前文，再连贯朗诵最新句联！背完可直接一键批量结算 FSRS 记忆库。",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Current newly added unit spotlight card with transition feedback
                val currentNewUnit = currentStage.newlyAddedUnit
                val isCurrentBottleneck = currentStageIndex in bottleneckIndices

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isCurrentBottleneck) StreakFlame.copy(alpha = 0.08f) else StudyBlueLight.copy(alpha = 0.25f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = if (isCurrentBottleneck) StreakFlame else StudyBlueAccent
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "★ 本阶段攻坚目标（第 ${currentStageIndex + 1} 联）",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentBottleneck) StreakFlame else StudyBlueAccent
                            )

                            // Bottleneck toggle button
                            FilterChip(
                                selected = isCurrentBottleneck,
                                onClick = {
                                    hapticManager.tapLight()
                                    if (isCurrentBottleneck) {
                                        bottleneckIndices = bottleneckIndices - currentStageIndex
                                    } else {
                                        bottleneckIndices = bottleneckIndices + currentStageIndex
                                        if (currentStageIndex > 0) {
                                            activeRepo.recordTransitionBottleneck(article.id, currentStageIndex - 1, currentStageIndex)
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        if (isCurrentBottleneck) "已记为转折卡壳" else "此处转折卡壳？",
                                        fontSize = 11.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isCurrentBottleneck) Icons.Default.WarningAmber else Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }

                        Text(
                            text = currentNewUnit.text,
                            fontSize = 19.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp,
                            color = TextPrimary
                        )
                    }
                }

                // Chained Units List
                Text(
                    text = "串联累加文本（共 ${currentStage.chainUnits.size} 联）：",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                currentStage.chainUnits.forEachIndexed { index: Int, unit: SnowballUnit ->
                    val isNewlyAdded = (index == currentStage.chainUnits.size - 1)
                    val isMasked = maskHistory && !isNewlyAdded
                    val isUnitBottleneck = index in bottleneckIndices

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = when {
                                isNewlyAdded -> StudyBlueLight.copy(alpha = 0.25f)
                                isUnitBottleneck -> StreakFlame.copy(alpha = 0.05f)
                                else -> BgSurface
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isNewlyAdded) 1.5.dp else 1.dp,
                            color = when {
                                isNewlyAdded -> StudyBlueAccent
                                isUnitBottleneck -> StreakFlame.copy(alpha = 0.6f)
                                else -> BorderSubtle
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when {
                                        isNewlyAdded -> StudyBlueAccent
                                        isUnitBottleneck -> StreakFlame
                                        else -> BgSurfaceMuted
                                    }
                                ) {
                                    Text(
                                        text = when {
                                            isNewlyAdded -> "★ 本次新加"
                                            isUnitBottleneck -> "第 ${index + 1} 联 · 易卡壳"
                                            else -> "第 ${index + 1} 联"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isNewlyAdded || isUnitBottleneck) Color.White else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (isMasked) {
                                    Text(
                                        text = "（盲背模式中）",
                                        fontSize = 11.sp,
                                        color = StreakFlame
                                    )
                                }
                            }

                            if (isMasked) {
                                val maskedText = unit.text.map { c -> if (c.isLetterOrDigit()) '■' else c }.joinToString("")
                                Text(
                                    text = maskedText,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 28.sp,
                                    color = TextTertiary
                                )
                            } else {
                                Text(
                                    text = unit.text,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = if (isNewlyAdded) FontWeight.Bold else FontWeight.Normal,
                                    lineHeight = 28.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStageIndex > 0) {
                        OutlinedButton(
                            onClick = {
                                hapticManager.tapLight()
                                currentStageIndex--
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("返回上一步")
                        }
                    }

                    Button(
                        onClick = {
                            soundManager.playCelebration()
                            hapticManager.successPulse()
                            if (isCompleted) {
                                // Execute batch settlement into FSRS
                                val ratingsMap = mutableMapOf<String, Rating>()
                                for (stg in stages) {
                                    val rating = if (stg.stageIndex in bottleneckIndices) {
                                        Rating.HARD
                                    } else {
                                        Rating.GOOD
                                    }
                                    for (cid in stg.newlyAddedUnit.cardIds) {
                                        ratingsMap[cid] = rating
                                    }
                                }
                                val summary = activeRepo.submitChainedRecitationBatch(ratingsMap)
                                summaryResult = summary
                                isSettled = true
                                isCelebrationActive = true
                                showSettlementDialog = true
                            } else {
                                currentStageIndex++
                            }
                        },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(
                            if (isCompleted) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCompleted) "通篇贯通 · 智能同步 FSRS" else "背熟了 · 滚雪球下一段")
                    }
                }
            }

            if (isCelebrationActive) {
                DuolingoStyleCelebration(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // FSRS Chained Recitation Settlement Modal Dialog
    if (showSettlementDialog && summaryResult != null) {
        val summary = summaryResult!!
        AlertDialog(
            onDismissRequest = { showSettlementDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = WarningGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("《${article.title}》通篇串联结业！", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "恭喜你完成了全篇滚雪球串联背诵！系统已自动对全篇卡片完成 FSRS 间隔重复计算与信用分配。",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BgSurfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("串联总联数", fontSize = 13.sp, color = TextSecondary)
                                Text("${stages.size} 联（共 ${summary.totalUnits} 张卡片）", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("顺畅连诵率", fontSize = 13.sp, color = TextSecondary)
                                val pct = if (stages.isNotEmpty()) {
                                    (((stages.size - bottleneckIndices.size).toFloat() / stages.size.toFloat()) * 100).toInt()
                                } else 100
                                Text("$pct%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("顺畅 vs 突破", fontSize = 13.sp, color = TextSecondary)
                                Text("${stages.size - bottleneckIndices.size} 联顺畅 · ${bottleneckIndices.size} 联卡壳", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("预计下次串联复习", fontSize = 13.sp, color = TextSecondary)
                                Text("约 ${"%.1f".format(summary.averageNextIntervalDays)} 天后", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StudyBlueAccent)
                            }
                        }
                    }

                    Text(
                        text = "✓ 记忆库稳定性已提升\n✓ 今日学习目标与打卡热力图已自动累加\n✓ 锁定了文言转折起承转合弱项",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettlementDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("完成并返回")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSettlementDialog = false
                        currentStageIndex = 0
                        bottleneckIndices = emptySet()
                        isSettled = false
                        isCelebrationActive = false
                    }
                ) {
                    Text("再滚雪球巩固一遍")
                }
            }
        )
    }
}

