package com.ancient.wenyan.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.GaoKaoScenarioDataSource
import com.ancient.wenyan.domain.gaokao.GaoKaoScenarioQuestion
import com.ancient.wenyan.domain.speech.RecitationDiffEngine
import com.ancient.wenyan.domain.speech.RecitationEvaluationResult
import com.ancient.wenyan.domain.ai.ScenarioDiagnosisResult
import com.ancient.wenyan.domain.ai.TypeSafeDiagnosisEngine
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GaoKaoScenarioScreen(
    initialArticleTitle: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val availableArticles = remember { GaoKaoScenarioDataSource.getAvailableArticles() }
    var selectedArticle by remember(initialArticleTitle) {
        mutableStateOf(initialArticleTitle?.takeIf { it in availableArticles } ?: "全部篇目")
    }
    var showArticlePickerSheet by remember { mutableStateOf(false) }

    // Active questions based on selected article, supports shuffling
    var activeQuestions by remember(selectedArticle) {
        mutableStateOf(GaoKaoScenarioDataSource.getQuestionsByArticle(selectedArticle))
    }

    val coroutineScope = rememberCoroutineScope()
    var currentIndex by remember(activeQuestions) { mutableIntStateOf(0) }
    var isRevealed by remember(currentIndex, activeQuestions) { mutableStateOf(false) }
    var userInput by remember(currentIndex, activeQuestions) { mutableStateOf("") }
    var evalResult by remember(currentIndex, activeQuestions) { mutableStateOf<RecitationEvaluationResult?>(null) }
    var aiDiagnosis by remember(currentIndex, activeQuestions) { mutableStateOf<ScenarioDiagnosisResult?>(null) }
    var isAiLoading by remember(currentIndex, activeQuestions) { mutableStateOf(false) }

    val currentQ = activeQuestions.getOrNull(currentIndex)

    // Helper to evaluate and dismiss keyboard
    val doEvaluate = {
        keyboardController?.hide()
        focusManager.clearFocus()
        if (userInput.isNotBlank() && currentQ != null) {
            hapticManager.tapLight()
            evalResult = RecitationDiffEngine.evaluate(userInput, currentQ.answer)
            isRevealed = true
            isAiLoading = true
            coroutineScope.launch {
                aiDiagnosis = TypeSafeDiagnosisEngine.diagnose(
                    scenarioPrompt = currentQ.prompt,
                    expectedAnswer = currentQ.answer,
                    userInput = userInput,
                    keyPoints = currentQ.keyPoints
                )
                isAiLoading = false
            }
        }
    }

    // Article Selector BottomSheet
    if (showArticlePickerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showArticlePickerSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择篇目专项练习",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showArticlePickerSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableArticles.forEach { article ->
                        val isSelected = (selectedArticle == article)
                        val count = if (article == "全部篇目") GaoKaoScenarioDataSource.QUESTIONS.size
                        else GaoKaoScenarioDataSource.QUESTIONS.count { it.articleTitle == article }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hapticManager.tapLight()
                                    selectedArticle = article
                                    activeQuestions = GaoKaoScenarioDataSource.getQuestionsByArticle(article)
                                    showArticlePickerSheet = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) StudyBlueLight else Color.Transparent,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                                0.6.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (article == "全部篇目") "全部篇目 (全真随机)" else "《$article》",
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${count}题",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "高考理解性默写专项",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "真题情境 · 通假实虚词采分突破",
                            fontSize = 11.sp,
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
                    // Shuffle button
                    IconButton(
                        onClick = {
                            hapticManager.tapLight()
                            activeQuestions = activeQuestions.shuffled()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "随机打乱题目顺序",
                            tint = StudyBlueAccent
                        )
                    }

                    // Choose article button
                    IconButton(
                        onClick = {
                            hapticManager.tapLight()
                            showArticlePickerSheet = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "筛选篇目",
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无题库题目", color = TextSecondary)
            }
            return@Scaffold
        }

        val isImeVisible = WindowInsets.isImeVisible
        val scrollState = rememberScrollState()

        LaunchedEffect(isImeVisible) {
            if (isImeVisible) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = if (isImeVisible) 6.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (isImeVisible) 8.dp else 12.dp)
        ) {
            // ==============================================================
            // 1. 篇目专项选择标签栏 (Article Filter Pills, 键盘弹出时自动隐藏以释放宝贵纵向空间)
            // ==============================================================
            if (!isImeVisible) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        AssistChip(
                            onClick = { showArticlePickerSheet = true },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = StudyBlueAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (selectedArticle == "全部篇目") "专项筛选" else "专项: $selectedArticle",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudyBlueAccent
                                )
                            }
                        )
                    }

                    items(availableArticles.take(10)) { article ->
                        val isSelected = (selectedArticle == article)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                hapticManager.tapLight()
                                selectedArticle = article
                                activeQuestions = GaoKaoScenarioDataSource.getQuestionsByArticle(article)
                            },
                            label = {
                                Text(
                                    text = if (article == "全部篇目") "全部" else article,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudyBlueAccent,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // ==============================================================
            // 2. 进度与篇目指示区 (Progress Bar & Counter)
            // ==============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudyBlueLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = StudyBlueAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "《${currentQ.articleTitle}》· ${currentQ.author}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = StudyBlueAccent
                        )
                    }
                }

                Text(
                    text = "${currentIndex + 1} / ${activeQuestions.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudyBlueAccent
                )
            }

            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / activeQuestions.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = StudyBlueAccent,
                trackColor = BgSurfaceMuted
            )

            // ==============================================================
            // 3. 题目情境卡片 (Question Scenario Card)
            // ==============================================================
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = BgSurface)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarningGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "高考真题情境",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (isImeVisible) {
                            TextButton(
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "收起键盘",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "收起键盘",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Text(
                        text = currentQ.prompt,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 23.sp,
                        letterSpacing = 0.3.sp,
                        color = TextPrimary
                    )
                }
            }

            // ==============================================================
            // 4. 输入框区 (支持键盘回车 Done 智能评测与收起键盘)
            // ==============================================================
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = BgSurface)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = {
                            userInput = it
                            evalResult = null
                        },
                        label = { Text("键入默写答案 (回车或点击下方智能评测)") },
                        placeholder = { Text("例：不宜妄自菲薄，引喻失义……") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        minLines = if (isImeVisible) 1 else 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { doEvaluate() }
                        ),
                        trailingIcon = {
                            if (userInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        userInput = ""
                                        evalResult = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "清空输入",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { doEvaluate() },
                            enabled = userInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("智能评测 (回车)", fontSize = 13.sp)
                        }

                        TextButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                hapticManager.tapLight()
                                isRevealed = !isRevealed
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRevealed) "隐藏原句" else "揭晓答案", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ==============================================================
            // 5. 智能评测反馈卡片 (LCS Evaluation Result)
            // ==============================================================
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
                            containerColor = if (res.isPerfect) SuccessGreen.copy(alpha = 0.12f) else WarningGold.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                    text = "匹配 ${res.matchedCount} · 漏 ${res.missingCount} · 冗 ${res.extraCount}",
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

            // ==============================================================
            // 5.5 TypeSafe AI 智能学情诊断卡片
            // ==============================================================
            AnimatedVisibility(
                visible = isRevealed && (isAiLoading || (aiDiagnosis != null && aiDiagnosis!!.isSuccess)),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = StreakFlame,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI 智能学情诊断 · TypeSafe 驱动",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StreakFlame
                                )
                            }
                            if (isAiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = StreakFlame
                                )
                            }
                        }

                        if (isAiLoading) {
                            Text(
                                text = "正在通过 Jev 模型智能研判审题意向与错因根源...",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        } else if (aiDiagnosis != null && aiDiagnosis!!.isSuccess) {
                            val diag = aiDiagnosis!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StreakFlame.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "意象契合 ${diag.intentRate.toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StreakFlame,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = diag.categoryDesc,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = diag.advice,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // ==============================================================
            // 6. 答案与采分易错突破卡片 (彻底解决文字挤在一起的破绽)
            // ==============================================================
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
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "标准答案",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudyBlueAccent
                        )
                        Text(
                            text = currentQ.answer,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp,
                            color = TextPrimary
                        )

                        HorizontalDivider(color = BorderSubtle, thickness = 0.6.dp)

                        Text(
                            text = "核心易错点 · 采分突破",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StreakFlame
                        )

                        // 核心修复：使用 FlowRow 替换原先将长文字强行压缩在单行的 Row，绝不产生挤压与字体重叠
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentQ.keyPoints.forEach { pt ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.6.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Text(
                                        text = pt,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        if (currentQ.explanation.isNotBlank()) {
                            Text(
                                text = currentQ.explanation,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==============================================================
            // 7. 底部题目切换导航 (Previous / Next)
            // ==============================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (currentIndex > 0) {
                            hapticManager.tapLight()
                            currentIndex--
                        }
                    },
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("上一题")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (currentIndex < activeQuestions.size - 1) {
                            hapticManager.tapLight()
                            currentIndex++
                        }
                    },
                    enabled = currentIndex < activeQuestions.size - 1,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBlueAccent)
                ) {
                    Text("下一题")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                }
            }
        }
    }
}
