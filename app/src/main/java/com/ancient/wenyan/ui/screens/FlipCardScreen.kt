package com.ancient.wenyan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipCardScreen(
    title: String,
    cards: List<Pair<Flashcard, CardFsrsState>>,
    repository: WenYanRepository,
    onBack: () -> Unit
) {
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
                    text = "所有卡片均已复习完毕或无匹配卡片",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkFaded,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = BambooGreen)
                ) {
                    Text("返回目录", fontFamily = FontFamily.Serif)
                }
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var completedCount by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    if (isFinished || currentIndex >= cards.size) {
        // Completion screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(XuanPaperLight)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, XuanBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "完成",
                        tint = BambooGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "本轮背诵完成！",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkCharcoal
                    )
                    Text(
                        text = "已完成 $completedCount 张古诗文卡片复习",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        color = InkMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = BambooGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("返回主页", fontFamily = FontFamily.Serif, fontSize = 16.sp)
                    }
                }
            }
        }
        return
    }

    val (currentCard, currentCardState) = cards[currentIndex]
    val intervalPreviews = remember(currentCardState) {
        repository.fsrsEngine.previewIntervals(currentCardState)
    }

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
                            text = "进度：${currentIndex + 1} / ${cards.size}",
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
            // Flashcard Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { isFlipped = !isFlipped }
                    .border(1.5.dp, XuanBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
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
                                text = currentCard.frontHint ?: "",
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

                        if (isFlipped) {
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
                                    text = currentCard.backTranslation ?: "",
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
                Button(
                    onClick = { isFlipped = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BambooGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "显示答案",
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // 4 FSRS Rating Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FsrsRatingButton(
                        label = "重来",
                        badge = intervalPreviews[Rating.AGAIN] ?: "10分",
                        color = CinnabarRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        repository.submitRating(currentCard.id, Rating.AGAIN)
                        completedCount++
                        isFlipped = false
                        currentIndex++
                    }

                    FsrsRatingButton(
                        label = "困难",
                        badge = intervalPreviews[Rating.HARD] ?: "15分",
                        color = MutedGold,
                        modifier = Modifier.weight(1f)
                    ) {
                        repository.submitRating(currentCard.id, Rating.HARD)
                        completedCount++
                        isFlipped = false
                        currentIndex++
                    }

                    FsrsRatingButton(
                        label = "良好",
                        badge = intervalPreviews[Rating.GOOD] ?: "1天",
                        color = BambooGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        repository.submitRating(currentCard.id, Rating.GOOD)
                        completedCount++
                        isFlipped = false
                        currentIndex++
                    }

                    FsrsRatingButton(
                        label = "简单",
                        badge = intervalPreviews[Rating.EASY] ?: "3天",
                        color = CeladonBlue,
                        modifier = Modifier.weight(1f)
                    ) {
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

@Composable
fun FsrsRatingButton(
    label: String,
    badge: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
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
