package com.ancient.wenyan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.ui.components.BookSelectionDialog
import com.ancient.wenyan.ui.components.OnboardingTutorialDialog
import com.ancient.wenyan.ui.components.RecitationHeatmapCard
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@Composable
fun DashboardScreen(
    repository: WenYanRepository,
    onNavigateToChapters: () -> Unit,
    onNavigateToRandom: () -> Unit,
    onStartTodayReview: () -> Unit,
    onStartGaoKaoReview: () -> Unit
) {
    val stats by repository.statsFlow.collectAsState()
    val selectedBookScope by repository.selectedBookScope.collectAsState()
    val selectedBookName by repository.selectedBookName.collectAsState()
    val heatmapStats by repository.heatmapStatsFlow.collectAsState()

    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    var isSoundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }

    var showBookDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember {
        mutableStateOf(!repository.isOnboardingCompleted())
    }

    // Book Selection Modal
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

    // Beginner Tutorial Modal
    if (showTutorialDialog) {
        OnboardingTutorialDialog(
            onDismiss = {
                repository.setOnboardingCompleted(true)
                showTutorialDialog = false
            },
            onComplete = {
                repository.setOnboardingCompleted(true)
                showTutorialDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XuanPaperLight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header: Classical Title & Seal & Tutorial Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "文言背诵",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = InkCharcoal,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "高中课内全篇目 · FSRS 间隔重复记忆",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sound Effect Toggle
                IconButton(
                    onClick = {
                        val next = !isSoundEnabled
                        isSoundEnabled = next
                        soundManager.isSoundEnabled = next
                        if (next) soundManager.playClick()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(XuanPaperCard, RoundedCornerShape(8.dp))
                        .border(1.dp, XuanBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "音效开关",
                        tint = if (isSoundEnabled) BambooGreen else InkFaded,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tutorial Entry Button
                IconButton(
                    onClick = {
                        soundManager.playClick()
                        showTutorialDialog = true
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(XuanPaperCard, RoundedCornerShape(8.dp))
                        .border(1.dp, XuanBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "新手研习指南",
                        tint = InkMedium,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Vermilion Seal Stamp (朱砂印章)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(-5f)
                        .background(Color(0x159E2A2B), RoundedCornerShape(6.dp))
                        .border(2.dp, CinnabarRed, RoundedCornerShape(6.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "熟读\n成诵",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = CinnabarRed,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Book Selector Card (自选背诵教材)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBookDialog = true }
                .border(1.2.dp, BambooGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(BambooGreen.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = BambooGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "当前背诵教材",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif,
                                color = InkMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val targetArticlesCount = if (selectedBookScope.isNullOrEmpty()) {
                                100
                            } else {
                                CurriculumDataSource.ALL_ARTICLES.count { it.moduleId in selectedBookScope!! }
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0x159E2A2B), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$targetArticlesCount 篇",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = CinnabarRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = selectedBookName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Switch Book Button Pill
                Box(
                    modifier = Modifier
                        .background(BambooGreen, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "切换图书",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Dashboard Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, XuanBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日记忆看板",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkCharcoal
                    )
                    Text(
                        text = "FSRS-5 算法在线",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Serif,
                        color = BambooGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMetric(label = "到期复习", value = "${stats.dueCards}", color = CinnabarRed)
                    StatMetric(label = "学习中", value = "${stats.learningCards}", color = MutedGold)
                    StatMetric(label = "已掌握", value = "${stats.reviewCards}", color = BambooGreen)
                    StatMetric(label = "保持率", value = "${stats.retentionPercentage.toInt()}%", color = CeladonBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Primary Call to Action Button: Today's FSRS Review
        val reviewInteractionSource = remember { MutableInteractionSource() }
        val isReviewPressed by reviewInteractionSource.collectIsPressedAsState()
        val reviewScale by animateFloatAsState(
            targetValue = if (isReviewPressed) 0.94f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "review_btn_scale"
        )

        Button(
            onClick = {
                soundManager.playClick()
                onStartTodayReview()
            },
            interactionSource = reviewInteractionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(reviewScale),
            colors = ButtonDefaults.buttonColors(containerColor = BambooGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (stats.dueCards > 0) "开始今日复习 (${stats.dueCards}张到期)" else "开始今日学习新卡",
                fontSize = 17.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recitation Heatmap Card (研墨足迹 · 背诵热力图)
        RecitationHeatmapCard(heatmapStats = heatmapStats)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "功能导航",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = InkMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // Navigation Features
        DashboardNavCard(
            title = "章节篇目系统学习",
            subtitle = "必修与选修共 11 册教材 · 100篇诗文目录循序渐进",
            icon = Icons.Default.Bookmarks,
            accentColor = BambooGreen,
            onClick = onNavigateToChapters
        )

        Spacer(modifier = Modifier.height(12.dp))

        DashboardNavCard(
            title = "跨篇目随机背诵",
            subtitle = "自由设定抽取范围与卡片数量 · 高频交叉强化",
            icon = Icons.Default.Shuffle,
            accentColor = MutedGold,
            onClick = onNavigateToRandom
        )

        Spacer(modifier = Modifier.height(12.dp))

        DashboardNavCard(
            title = "高考必背 72 篇专项",
            subtitle = "一键抽查教育部高考统编课标核心默写重点句",
            icon = Icons.Default.Star,
            accentColor = CinnabarRed,
            onClick = onStartGaoKaoReview
        )

        Spacer(modifier = Modifier.height(12.dp))

        DashboardNavCard(
            title = "新手研习指南",
            subtitle = "了解 FSRS 算法评分、自选课本与整篇渐进遮挡",
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            accentColor = CeladonBlue,
            onClick = { showTutorialDialog = true }
        )
    }
}

@Composable
fun StatMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            color = InkFaded,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun DashboardNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, XuanBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    color = InkCharcoal
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = InkFaded
            )
        }
    }
}
