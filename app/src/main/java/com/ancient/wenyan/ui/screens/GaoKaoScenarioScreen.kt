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
import com.ancient.wenyan.data.GaoKaoScenarioDataSource
import com.ancient.wenyan.domain.gaokao.GaoKaoScenarioQuestion
import com.ancient.wenyan.domain.speech.RecitationDiffEngine
import com.ancient.wenyan.domain.speech.RecitationEvaluationResult
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaoKaoScenarioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    val questions = remember { GaoKaoScenarioDataSource.QUESTIONS }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }
    var evalResult by remember { mutableStateOf<RecitationEvaluationResult?>(null) }

    val currentQ = questions.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "高考理解性默写专项",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "真题情境 · 通假实虚词采分突破",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgSurface
                )
            )
        },
        containerColor = BgCanvas
    ) { innerPadding ->
        if (currentQ == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无题库题目", color = TextSecondary)
            }
            return@Scaffold
        }

        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress bar & Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("《${currentQ.articleTitle}》· ${currentQ.author}") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                Text(
                    text = "${currentIndex + 1} / ${questions.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StudyBlueAccent
                )
            }

            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = StudyBlueAccent,
                trackColor = BgSurfaceMuted
            )

            // Question Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = BgSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarningGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "真题情境",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = currentQ.prompt,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 26.sp,
                        color = TextPrimary
                    )
                }
            }

            // Dictation Input Section
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = BgSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = {
                            userInput = it
                            evalResult = null
                        },
                        label = { Text("在此键入或语音转写默写答案") },
                        placeholder = { Text("例：不宜妄自菲薄，引喻失义……") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 4
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                hapticManager.tapLight()
                                if (userInput.isNotBlank()) {
                                    evalResult = RecitationDiffEngine.evaluate(userInput, currentQ.answer)
                                }
                                isRevealed = true
                            },
                            enabled = userInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("端侧智能评测")
                        }

                        TextButton(
                            onClick = {
                                hapticManager.tapLight()
                                isRevealed = !isRevealed
                            }
                        ) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRevealed) "隐藏原句" else "揭晓答案")
                        }
                    }
                }
            }

            // Evaluation Feedback if tested
            AnimatedVisibility(
                visible = evalResult != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                evalResult?.let { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (res.isPerfect) SuccessGreen.copy(alpha = 0.15f) else WarningGold.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (res.isPerfect) "准确率 100% · 完美无暇" else "准确率 ${res.accuracy}%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.isPerfect) SuccessGreen else WarningGold
                                )
                                Text(
                                    text = "匹配 ${res.matchedCount} / 漏 ${res.missingCount} / 冗 ${res.extraCount}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = res.feedbackMessage,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Answer & GaoKao Scoring Points
            AnimatedVisibility(
                visible = isRevealed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = BgSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "标准答案",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudyBlueAccent
                        )
                        Text(
                            text = currentQ.answer,
                            fontSize = 19.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)

                        Text(
                            text = "核心易错点 · 采分突破",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StreakFlame
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentQ.keyPoints.forEach { pt ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(pt, fontSize = 12.sp) }
                                )
                            }
                        }

                        if (currentQ.explanation.isNotBlank()) {
                            Text(
                                text = currentQ.explanation,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentIndex > 0) {
                            hapticManager.tapLight()
                            currentIndex--
                            isRevealed = false
                            userInput = ""
                            evalResult = null
                        }
                    },
                    enabled = currentIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("上一题")
                }

                Button(
                    onClick = {
                        if (currentIndex < questions.size - 1) {
                            hapticManager.tapLight()
                            currentIndex++
                            isRevealed = false
                            userInput = ""
                            evalResult = null
                        }
                    },
                    enabled = currentIndex < questions.size - 1
                ) {
                    Text("下一题")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                }
            }
        }
    }
}
