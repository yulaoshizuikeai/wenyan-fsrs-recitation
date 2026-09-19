package com.ancient.wenyan.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.components.DuolingoStyleCelebration
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipCardScreen(
    title: String,
    cards: List<Pair<Flashcard, CardFsrsState>>,
    repository: WenYanRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    var isSoundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }

    if (cards.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(XuanPaperLight)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "暂无待复习闪卡",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = InkCharcoal
                )
                Text(
                    text = "所选范围内的卡片均已复习完毕或无匹配内容",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkFaded,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = BambooGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("返回主页", fontFamily = FontFamily.Serif)
                }
            }
        }
        return
    }

    val sessionQueue = remember(cards) { mutableStateListOf(*cards.toTypedArray()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var completedCount by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    val isSessionComplete = isFinished || (sessionQueue.isNotEmpty() && currentIndex >= sessionQueue.size)

    // Sound effect on completion
    LaunchedEffect(isSessionComplete) {
        if (isSessionComplete && sessionQueue.isNotEmpty()) {
            soundManager.playCelebration()
        }
    }

    if (isSessionComplete) {
        // Duolingo-style Celebration Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(XuanPaperLight)
        ) {
            // Background Confetti explosion
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
                        .border(1.5.dp, BambooGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Celebration Seal Icon
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(BambooGreen.copy(alpha = 0.12f), CircleShape)
                                .border(2.dp, BambooGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "完成",
                                tint = BambooGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "熟读成诵 · 本轮功课达成！",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "已完成 $completedCount 张古诗文卡片的高效复习",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Serif,
                            color = InkMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats pill
                        Box(
                            modifier = Modifier
                                .background(Color(0x159E2A2B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "FSRS 记忆稳定性提升 · 日积跬步",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif,
                                color = CinnabarRed,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = BambooGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "返回主页查验足迹",
                                fontFamily = FontFamily.Serif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val currentPair = sessionQueue.getOrNull(currentIndex) ?: sessionQueue.last()
    val (currentCard, currentCardState) = currentPair
    val intervalPreviews = remember(currentCardState) {
        repository.fsrsEngine.previewIntervals(currentCardState)
    }

    // 3D Flip Animation Specs
    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_flip_rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal
                        )
                        Text(
                            text = "已研读：$completedCount · 待巩固：${(sessionQueue.size - currentIndex).coerceAtLeast(0)} 句",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            color = InkFaded
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = InkCharcoal
                        )
                    }
                },
                actions = {
                    // Sound Effect Toggle Button
                    IconButton(onClick = {
                        val next = !isSoundEnabled
                        isSoundEnabled = next
                        soundManager.isSoundEnabled = next
                        if (next) soundManager.playClick()
                    }) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "音效开关",
                            tint = if (isSoundEnabled) BambooGreen else InkFaded
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = XuanPaperLight)
            )
        },
        containerColor = XuanPaperLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Flashcard with 3D Flip graphicsLayer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable {
                        isFlipped = !isFlipped
                        soundManager.playFlip()
                    }
                    .graphicsLayer {
                        rotationY = flipRotation
                        cameraDistance = 12f * density
                    }
                    .border(1.5.dp, XuanBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        // Title header
                        Text(
                            text = currentCard.frontTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkMedium
                        )

                        if (!currentCard.frontHint.isNullOrBlank()) {
                            Text(
                                text = currentCard.frontHint,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif,
                                color = InkFaded,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Front Content
                        Text(
                            text = currentCard.frontPrompt,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        if (flipRotation > 90f) {
                            HorizontalDivider(
                                color = XuanBorder,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Answer
                            Text(
                                text = currentCard.backAnswer,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = CinnabarRed,
                                textAlign = TextAlign.Center,
                                lineHeight = 36.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            if (!currentCard.backTranslation.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "【译文释义】",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    color = InkMedium
                                )
                                Text(
                                    text = currentCard.backTranslation,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = InkCharcoal,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "（轻触卡片或点击下方按钮翻转查看）",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif,
                                color = InkFaded,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            if (!isFlipped) {
                BouncyButton(
                    text = "显示答案",
                    containerColor = BambooGreen,
                    onClick = {
                        isFlipped = true
                        soundManager.playFlip()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            } else {
                // 4 FSRS Rating Buttons with Duolingo-style bouncy physics & audio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BouncyFsrsRatingButton(
                        label = "重来",
                        badge = intervalPreviews[Rating.AGAIN] ?: "10分",
                        color = CinnabarRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        soundManager.playWrong()
                        repository.submitRating(currentCard.id, Rating.AGAIN)
                        // Re-enqueue this card so student will be tested again until mastery!
                        sessionQueue.add(currentPair)
                        completedCount++
                        isFlipped = false
                        currentIndex++
                    }

                    BouncyFsrsRatingButton(
                        label = "困难",
                        badge = intervalPreviews[Rating.HARD] ?: "15分",
                        color = MutedGold,
                        modifier = Modifier.weight(1f)
                    ) {
                        soundManager.playWrong()
                        repository.submitRating(currentCard.id, Rating.HARD)
                        completedCount++
                        isFlipped = false
                        currentIndex++
                    }

                    BouncyFsrsRatingButton(
                        label = "良好",
                        badge = intervalPreviews[Rating.GOOD] ?: "1天",
                        color = BambooGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        soundManager.playCorrect()
                        repository.submitRating(currentCard.id, Rating.GOOD)
                        completedCount++
                        isFlipped = false
                        currentIndex++
                    }

                    BouncyFsrsRatingButton(
                        label = "简单",
                        badge = intervalPreviews[Rating.EASY] ?: "3天",
                        color = CeladonBlue,
                        modifier = Modifier.weight(1f)
                    ) {
                        soundManager.playCorrect()
                        repository.submitRating(currentCard.id, Rating.EASY)
                        completedCount++
                        isFlipped = false
                        currentIndex++
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Duolingo-style Bouncy Button with spring press physics
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
        targetValue = if (isPressed) 0.94f else 1.0f,
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Duolingo-style Tactile FSRS Rating Button with spring bounce on touch
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
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = badge,
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}
