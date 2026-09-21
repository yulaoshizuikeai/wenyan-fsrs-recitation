package com.ancient.wenyan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.model.ActiveSession
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.components.DuolingoStyleCelebration
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipCardScreen(
    title: String,
    cards: List<Pair<Flashcard, CardFsrsState>>,
    repository: WenYanRepository,
    onBack: () -> Unit,
    initialIndex: Int = 0,
    initialCompletedCount: Int = 0,
    sessionId: String = "session_default",
    sessionType: String = "GENERAL",
    onProgressUpdate: ((currentIndex: Int, completedCount: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }
    var isSoundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }

    if (cards.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BgSurface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(StudyBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StudyBlueAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无待复习闪卡",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextPrimary
                    )
                    Text(
                        text = "所选范围内的卡片均已温习完毕，可前往篇目文库开启新篇章",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("返回主页", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        return
    }

    val sessionQueue = remember(cards) { mutableStateListOf(*cards.toTypedArray()) }
    var currentIndex by remember(initialIndex) { mutableIntStateOf(initialIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0))) }
    var completedCount by remember(initialCompletedCount) { mutableIntStateOf(initialCompletedCount) }
    var isFinished by rememberSaveable { mutableStateOf(false) }

    fun persistCurrentSession(idx: Int, count: Int) {
        if (sessionQueue.isNotEmpty() && idx < sessionQueue.size) {
            repository.saveActiveSession(
                ActiveSession(
                    id = sessionId,
                    title = title,
                    sessionType = sessionType,
                    cardIds = sessionQueue.map { it.first.id },
                    currentIndex = idx,
                    completedCount = count,
                    totalCards = sessionQueue.size
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        persistCurrentSession(currentIndex, completedCount)
        onProgressUpdate?.invoke(currentIndex, completedCount)
    }

    val isSessionComplete = isFinished || (sessionQueue.isNotEmpty() && currentIndex >= sessionQueue.size)

    // Sound effect & haptic fanfare on completion
    LaunchedEffect(isSessionComplete) {
        if (isSessionComplete && sessionQueue.isNotEmpty()) {
            repository.clearActiveSession()
            soundManager.playCelebration()
            hapticManager.celebrationFanfare()
        }
    }

    if (isSessionComplete) {
        // Celebratory Completion Screen with Confetti & Haptics
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
        ) {
            DuolingoStyleCelebration(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, BorderSubtle, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BgSurface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Celebration Badge
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(SuccessGreen.copy(alpha = 0.12f), CircleShape)
                                .border(2.dp, SuccessGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "完成",
                                tint = SuccessGreen,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "熟读成诵 · 本轮背诵达成！",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "已完成 $completedCount 句诗文的精准 FSRS 间隔巩固",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Pill
                        Box(
                            modifier = Modifier
                                .background(StreakFlame.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🔥 FSRS 记忆稳定性持续攀升 · 日拱一卒",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = StreakFlame,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "返回主页查验足迹",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
        return
    }

    var isTransitioning by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex) {
        isTransitioning = false
    }

    val currentPair = sessionQueue.getOrNull(currentIndex) ?: sessionQueue.last()
    val (currentCard, currentCardState) = currentPair
    val intervalPreviews = remember(currentCardState) {
        repository.fsrsEngine.previewIntervals(currentCardState)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1
                            )
                            Text(
                                text = "已背诵 $completedCount · 待巩固 ${(sessionQueue.size - currentIndex).coerceAtLeast(0)} 句",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            hapticManager.tapLight()
                            persistCurrentSession(currentIndex, completedCount)
                            onBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        // Sound Effect Toggle Button
                        IconButton(onClick = {
                            val next = !isSoundEnabled
                            isSoundEnabled = next
                            soundManager.isSoundEnabled = next
                            hapticManager.tapLight()
                            if (next) soundManager.playClick()
                        }) {
                            Icon(
                                imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "音效开关",
                                tint = if (isSoundEnabled) StudyBlueAccent else MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )

                // M3 Smooth Queue Progress Indicator
                val progressFraction = if (sessionQueue.isNotEmpty()) {
                    (currentIndex.toFloat() / sessionQueue.size).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = StudyBlueAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        var isFlipped by rememberSaveable(currentIndex, currentCard.id) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Flashcard with 3D Flip & Elevation, keyed per card to prevent timing spoiler
            key(currentIndex, currentCard.id) {
                val flipRotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "card_flip_rotation"
                )
                val cardElevation by animateDpAsState(
                    targetValue = if (isFlipped) 8.dp else 2.dp,
                    label = "card_elevation"
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            rotationY = flipRotation
                            cameraDistance = 16f * density
                        }
                        .clickable {
                            isFlipped = !isFlipped
                            soundManager.playFlip()
                            hapticManager.cardFlip()
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.outlinedCardElevation(defaultElevation = cardElevation)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .graphicsLayer {
                                // Invert content horizontally when flipped so text isn't mirrored
                                if (flipRotation > 90f) {
                                    rotationY = 180f
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Mode Pill Header (Front: Prompt vs Back: Answer + Sequential Position)
                            val isBack = flipRotation > 90f
                        val modeLabel = if (isBack) {
                            "【背面 · 填空正解与对照】"
                        } else {
                            if (currentCard.totalClozes > 1) {
                                "【语境填空 第 ${currentCard.clozeIndex}/${currentCard.totalClozes} 空】"
                            } else {
                                "【语境填空默写】"
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isBack) SuccessGreen.copy(alpha = 0.10f) else StudyBlueLight,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = modeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (isBack) SuccessGreen else StudyBlueAccent
                                )
                            }

                            if (currentCard.totalUnits > 1) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "第 ${currentCard.unitIndex + 1}/${currentCard.totalUnits} 联 · 顺承原序",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }


                        Spacer(modifier = Modifier.height(14.dp))

                        // Article title
                        Text(
                            text = currentCard.frontTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = TextSecondary
                        )

                        if (!currentCard.frontHint.isNullOrBlank()) {
                            Text(
                                text = currentCard.frontHint,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextTertiary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Front Prompt Text (Full Context with Cloze Mask)
                        Text(
                            text = currentCard.frontPrompt,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isBack) {
                            HorizontalDivider(
                                color = BorderSubtle,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Back Answer: Prominently display target answer
                            Text(
                                text = "【填空正解】",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = StudyBlueAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayAnswer = currentCard.maskedSegment
                                ?: currentCard.backAnswer.removePrefix("【填空正解】").removePrefix("【对句】")
                            Text(
                                text = displayAnswer,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                color = StudyBlueAccent,
                                textAlign = TextAlign.Center,
                                lineHeight = 38.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Full verse context comparison
                            val fullContext = currentCard.fullVerseContext
                            if (!fullContext.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "【整句对照】",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextTertiary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fullContext,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            if (!currentCard.backTranslation.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "【译文释义】",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextTertiary
                                )
                                Text(
                                    text = currentCard.backTranslation,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            if (!currentCard.precedingClauseHint.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "【上承文脉】",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = StudyBlueAccent
                                )
                                Text(
                                    text = currentCard.precedingClauseHint,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "轻触卡片或点击下方按钮翻转查看答案",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            if (!isFlipped) {
                BouncyButton(
                    text = "显示答案",
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        isFlipped = true
                        soundManager.playFlip()
                        hapticManager.cardFlip()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            } else {
                // 4 FSRS Rating Buttons with rich spring physics, unique sounds & haptic signatures
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BouncyFsrsRatingButton(
                        label = "重来",
                        badge = intervalPreviews[Rating.AGAIN] ?: "10分",
                        color = DueRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTransitioning) return@BouncyFsrsRatingButton
                        isTransitioning = true
                        soundManager.playWrong()
                        hapticManager.warningThud()
                        repository.submitRating(currentCard.id, Rating.AGAIN)
                        val updatedState = repository.getCardState(currentCard.id)
                        // 顺承原序：将重来卡片就近插入当前篇目末尾，并至少间隔3张卡避免瞬时记忆作弊
                        var lastIndexInArticle = currentIndex
                        for (i in (currentIndex + 1) until sessionQueue.size) {
                            if (sessionQueue[i].first.articleId == currentCard.articleId) {
                                lastIndexInArticle = i
                            }
                        }
                        val idealPos = maxOf(currentIndex + 3, lastIndexInArticle + 1)
                        val insertPos = idealPos.coerceAtMost(sessionQueue.size)
                        sessionQueue.add(insertPos, Pair(currentCard, updatedState))
                        // 重来不算作完成，不递增 completedCount
                        isFlipped = false
                        val nextIdx = currentIndex + 1
                        currentIndex = nextIdx
                        persistCurrentSession(nextIdx, completedCount)
                        onProgressUpdate?.invoke(nextIdx, completedCount)
                    }

                    BouncyFsrsRatingButton(
                        label = "困难",
                        badge = intervalPreviews[Rating.HARD] ?: "15分",
                        color = WarningGold,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTransitioning) return@BouncyFsrsRatingButton
                        isTransitioning = true
                        soundManager.playHard()
                        hapticManager.warningThud()
                        repository.submitRating(currentCard.id, Rating.HARD)
                        val nextCount = completedCount + 1
                        completedCount = nextCount
                        isFlipped = false
                        val nextIdx = currentIndex + 1
                        currentIndex = nextIdx
                        if (nextIdx >= sessionQueue.size) {
                            repository.clearActiveSession()
                        } else {
                            persistCurrentSession(nextIdx, nextCount)
                        }
                        onProgressUpdate?.invoke(nextIdx, nextCount)
                    }

                    BouncyFsrsRatingButton(
                        label = "良好",
                        badge = intervalPreviews[Rating.GOOD] ?: "1天",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTransitioning) return@BouncyFsrsRatingButton
                        isTransitioning = true
                        soundManager.playCorrect()
                        hapticManager.successPulse()
                        repository.submitRating(currentCard.id, Rating.GOOD)
                        val nextCount = completedCount + 1
                        completedCount = nextCount
                        isFlipped = false
                        val nextIdx = currentIndex + 1
                        currentIndex = nextIdx
                        if (nextIdx >= sessionQueue.size) {
                            repository.clearActiveSession()
                        } else {
                            persistCurrentSession(nextIdx, nextCount)
                        }
                        onProgressUpdate?.invoke(nextIdx, nextCount)
                    }

                    BouncyFsrsRatingButton(
                        label = "简单",
                        badge = intervalPreviews[Rating.EASY] ?: "3天",
                        color = StudyBlueAccent,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTransitioning) return@BouncyFsrsRatingButton
                        isTransitioning = true
                        soundManager.playEasy()
                        hapticManager.successPulse()
                        repository.submitRating(currentCard.id, Rating.EASY)
                        val nextCount = completedCount + 1
                        completedCount = nextCount
                        isFlipped = false
                        val nextIdx = currentIndex + 1
                        currentIndex = nextIdx
                        if (nextIdx >= sessionQueue.size) {
                            repository.clearActiveSession()
                        } else {
                            persistCurrentSession(nextIdx, nextCount)
                        }
                        onProgressUpdate?.invoke(nextIdx, nextCount)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Modern Bouncy Button with spring physics
 */
@Composable
fun BouncyButton(
    text: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncy_scale"
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.scale(scale),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Flip,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Tactile FSRS Rating Button with spring bounce and high contrast badges
 */
@Composable
fun BouncyFsrsRatingButton(
    label: String,
    badge: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fsrs_button_bounce"
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(54.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color.White
            )
            Text(
                text = badge,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.90f)
            )
        }
    }
}
