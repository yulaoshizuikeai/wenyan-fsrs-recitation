package com.ancient.wenyan.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.ui.components.BookSelectionDialog
import com.ancient.wenyan.ui.components.FeedbackPreferencesDialog
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*
import java.time.LocalDate

data class ClassicalQuote(
    val title: String,
    val author: String,
    val quote: String,
    val translation: String
)

val CURATED_QUOTES = listOf(
    ClassicalQuote(
        title = "短歌行",
        author = "曹操",
        quote = "“山不厌高，海不厌深。周公吐哺，天下归心。”",
        translation = "高山不辞土石才见其巍峨，大海不纳细流难成其浩瀚。志存高远者海纳百川，终成大业。"
    ),
    ClassicalQuote(
        title = "劝学",
        author = "荀子",
        quote = "“不积跬步，无以至千里；不积小流，无以成江海。”",
        translation = "不积累一步半步的行程，就无法到达千里之远；不汇聚细小的流水，就成就不了辽阔江海。背诵重在日日研读。"
    ),
    ClassicalQuote(
        title = "离骚",
        author = "屈原",
        quote = "“路漫漫其修远兮，吾将上下而求索。”",
        translation = "前方的道路漫长而悠远，我将百折不挠、上下探求心中的理想与光明。"
    ),
    ClassicalQuote(
        title = "滕王阁序",
        author = "王勃",
        quote = "“老当益壮，宁移白首之心？穷且益坚，不坠青云之志。”",
        translation = "年纪虽老志气更坚，哪能改变白头之年的操守？身处困境更需坚韧，绝不丢弃直上青云的凌云壮志。"
    ),
    ClassicalQuote(
        title = "赤壁赋",
        author = "苏轼",
        quote = "“逝者如斯，而未尝往也；盈虚者如彼，而卒莫消长也。”",
        translation = "万物变迁流逝不停，但其本源未曾消失；月亮圆缺代代相续，其本体终无增减。以旷达从容之心对岁月。"
    )
)

@Composable
fun DashboardScreen(
    repository: WenYanRepository,
    onStartTodayReview: () -> Unit,
    onStartGaoKaoReview: () -> Unit,
    onNavigateToPractice: () -> Unit
) {
    val stats by repository.statsFlow.collectAsState()
    val selectedBookScope by repository.selectedBookScope.collectAsState()
    val selectedBookName by repository.selectedBookName.collectAsState()
    val heatmapStats by repository.heatmapStatsFlow.collectAsState()

    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }
    var isSoundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }

    var showBookDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Rotating daily quote based on day-of-year, tap to cycle
    val dayOfYear = remember { LocalDate.now().dayOfYear }
    var quoteIndex by remember { mutableIntStateOf(dayOfYear % CURATED_QUOTES.size) }
    val currentQuote = CURATED_QUOTES[quoteIndex]

    // Flame Breathing Animation for active streak
    val infiniteTransition = rememberInfiniteTransition(label = "flame_breathing")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (heatmapStats.currentStreak > 0) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    if (showBookDialog) {
        BookSelectionDialog(
            currentScope = selectedBookScope,
            currentName = selectedBookName,
            onDismiss = { showBookDialog = false },
            onConfirmSelection = { newScope, newName ->
                repository.setSelectedBookScope(newScope, newName)
                showBookDialog = false
            }
        )
    }

    if (showFeedbackDialog) {
        FeedbackPreferencesDialog(
            onDismiss = { showFeedbackDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
    ) {
        // ====================================================================
        // Top App Header: Generous status bar insets + comfortable breathing room
        // ====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "文言背诵",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(StudyBlueAccent.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "高中必背",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = StudyBlueAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "FSRS-5 间隔记忆 · 熟读成诵",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Settings & sensory sandbox
                IconButton(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        showFeedbackDialog = true
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(BgSurface, CircleShape)
                        .border(1.dp, BorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "音效与震动偏好",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick mute/unmute
                IconButton(
                    onClick = {
                        val next = !isSoundEnabled
                        isSoundEnabled = next
                        soundManager.isSoundEnabled = next
                        hapticManager.tapLight()
                        if (next) soundManager.playClick()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(BgSurface, CircleShape)
                        .border(1.dp, BorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "音效开关",
                        tint = if (isSoundEnabled) StudyBlueAccent else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ====================================================================
        // Scrollable Body Content
        // ====================================================================
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp)
        ) {
            // ----------------------------------------------------------------
            // 1. Sleek Status Bar: Streak Badge + Scope Selector Capsule
            // ----------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Streak Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = if (heatmapStats.currentStreak > 0) StreakFlame.copy(alpha = 0.10f) else BgSurfaceMuted,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "连胜火焰",
                            tint = if (heatmapStats.currentStreak > 0) StreakFlame else TextTertiary,
                            modifier = Modifier
                                .size(18.dp)
                                .scale(flameScale)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (heatmapStats.currentStreak > 0) "${heatmapStats.currentStreak} 天连胜" else "今日未研读",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = if (heatmapStats.currentStreak > 0) StreakFlame else TextSecondary
                        )
                    }

                    // Scope Selector Capsule (Click directly to switch textbook)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                showBookDialog = true
                            }
                            .background(BgSurface, RoundedCornerShape(20.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = StudyBlueAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedBookName,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "切换教材",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ----------------------------------------------------------------
            // 2. Focused Core Memory Mission Card (No nested cards, clean & decluttered)
            // ----------------------------------------------------------------
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BgSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // 4-Dimension FSRS Memory Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetric(
                                label = "待复习",
                                value = stats.dueCards,
                                color = if (stats.dueCards > 0) DueRed else TextPrimary
                            )
                            StatMetric(
                                label = "学习中",
                                value = stats.learningCards,
                                color = StudyBlueAccent
                            )
                            StatMetric(
                                label = "已稳固",
                                value = stats.reviewCards,
                                color = SuccessGreen
                            )
                            StatMetric(
                                label = "留存率",
                                value = stats.retentionPercentage.toInt(),
                                isPercentage = true,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // High-Contrast Primary Study Button with spring bounce & tactile pulse
                        val reviewInteractionSource = remember { MutableInteractionSource() }
                        val isReviewPressed by reviewInteractionSource.collectIsPressedAsState()
                        val reviewScale by animateFloatAsState(
                            targetValue = if (isReviewPressed) 0.96f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "review_btn_scale"
                        )

                        Button(
                            onClick = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                onStartTodayReview()
                            },
                            interactionSource = reviewInteractionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .scale(reviewScale),
                            colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (stats.dueCards > 0) "开始今日复习 (${stats.dueCards} 句到期)" else "开启今日研习新词句",
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "➔", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            }

            // ----------------------------------------------------------------
            // 3. Classical Quote Card: Minimalist Editorial Style
            // ----------------------------------------------------------------
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            quoteIndex = (quoteIndex + 1) % CURATED_QUOTES.size
                        }
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BgSurface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = StudyBlueAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "每日名句",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "《${currentQuote.title}》· ${currentQuote.author}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        AnimatedContent(
                            targetState = currentQuote,
                            transitionSpec = {
                                fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessHigh))
                            },
                            label = "quote_switch"
                        ) { quote ->
                            Column {
                                Text(
                                    text = quote.quote,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextPrimary,
                                    lineHeight = 25.sp
                                )

                                Text(
                                    text = quote.translation,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextSecondary,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------------------
            // 4. Quick Practice Dual Tiles
            // ----------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GaoKao 72 Quick Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                onStartGaoKaoReview()
                            }
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BgSurface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(StreakFlame.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = StreakFlame,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "高考 72 篇专项",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            )
                            Text(
                                text = "必背考点一键抽查",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Practice Quick Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                onNavigateToPractice()
                            }
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BgSurface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(StudyBlueLight, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                    tint = StudyBlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "跨篇随机练习",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            )
                            Text(
                                text = "自由设定抽取范围",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetric(label: String, value: Int, isPercentage: Boolean = false, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                fadeOut(spring(stiffness = Spring.StiffnessHigh))
            },
            label = "stat_num"
        ) { targetVal ->
            Text(
                text = if (isPercentage) "$targetVal%" else "$targetVal",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            color = TextTertiary,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
