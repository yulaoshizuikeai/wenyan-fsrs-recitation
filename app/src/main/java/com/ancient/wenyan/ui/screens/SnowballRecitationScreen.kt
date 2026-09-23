package com.ancient.wenyan.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.domain.cloze.SnowballChainingEngine
import com.ancient.wenyan.domain.cloze.SnowballStage
import com.ancient.wenyan.domain.cloze.SnowballUnit
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.ui.components.DuolingoStyleCelebration
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnowballRecitationScreen(
    articleId: String? = "art_bx1_14",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }

    val article: Article = remember(articleId) {
        CurriculumDataSource.ARTICLE_MAP[articleId]
            ?: CurriculumDataSource.ARTICLE_MAP["art_bx1_14"]
            ?: CurriculumDataSource.ALL_ARTICLES.first()
    }

    val stages: List<SnowballStage> = remember(article) {
        SnowballChainingEngine.buildStages(article)
    }

    var currentStageIndex by remember { mutableIntStateOf(0) }
    var maskHistory by remember { mutableStateOf(true) }
    var isCelebrationActive by remember { mutableStateOf(false) }

    val currentStage = stages.getOrNull(currentStageIndex)
    val isCompleted = currentStageIndex >= stages.size - 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "长篇滚雪球串联背诵",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
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
                            text = "滚雪球法则：先温习前文上下文，再连背最新句联，直至一气呵成！",
                            fontSize = 12.sp,
                            color = TextSecondary
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

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isNewlyAdded) StudyBlueLight.copy(alpha = 0.35f) else BgSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isNewlyAdded) 1.5.dp else 1.dp,
                            color = if (isNewlyAdded) StudyBlueAccent else BorderSubtle
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
                                    color = if (isNewlyAdded) StudyBlueAccent else BgSurfaceMuted
                                ) {
                                    Text(
                                        text = if (isNewlyAdded) "★ 本次新加" else "第 ${index + 1} 联",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isNewlyAdded) Color.White else TextSecondary,
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
                            hapticManager.successPulse()
                            if (isCompleted) {
                                isCelebrationActive = true
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
                        Text(if (isCompleted) "已通篇贯通 · 结业！" else "背熟了 · 滚雪球下一段")
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
}
