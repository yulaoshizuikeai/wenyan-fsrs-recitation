package com.ancient.wenyan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.BookPresets
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    repository: WenYanRepository,
    onStartSession: (String, List<Pair<Flashcard, CardFsrsState>>) -> Unit,
    onStartCloze: (Article) -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: 高考72篇专项, 1: 跨篇自选抽测, 2: 经典长文遮挡

    val currentRepoScope by repository.selectedBookScope.collectAsState()
    val currentRepoName by repository.selectedBookName.collectAsState()

    val scopeOptions = remember(currentRepoName, currentRepoScope) {
        listOf(
            "当前选定教材 ($currentRepoName)" to currentRepoScope,
            "全部 11 册教材 (100篇)" to null,
            "必修全套 (上/下两册 · 36篇)" to BookPresets.SCOPE_REQUIRED_ALL.moduleIds,
            "选择性必修全套 (上/中/下 · 35篇)" to BookPresets.SCOPE_SELECTIVE_ALL.moduleIds,
            "选修(古代诗歌散文欣赏 · 29篇)" to BookPresets.BOOK_XX_APPRECIATION.moduleIds
        )
    }

    var selectedScopeIndex by remember { mutableIntStateOf(0) }
    var selectedCount by remember { mutableIntStateOf(20) }

    val longArticles = remember {
        CurriculumDataSource.ALL_ARTICLES.filter {
            it.title in listOf("赤壁赋", "劝学", "师说", "阿房宫赋", "逍遥游", "离骚", "归去来兮辞并序", "滕王阁序")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "专项练习",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCanvas)
            )
        },
        containerColor = BgCanvas
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Segmented Control Filter Tabs
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            hapticManager.tapLight()
                            soundManager.playClick()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("高考 72 篇", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            hapticManager.tapLight()
                            soundManager.playClick()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("跨篇随机", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                    SegmentedButton(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            hapticManager.tapLight()
                            soundManager.playClick()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("长文遮挡", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // GaoKao 72 Mandatory Practice
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = BgSurface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(22.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(StreakFlame.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = StreakFlame,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "高考必背 72 篇专项",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "教育部普通高中统编课标核心必考篇目",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "根据教育部高中语文课程标准，汇聚 72 篇必背名篇中的高频考查出句与名句对句，采用 FSRS 算法进行专项强化与情境默写模拟，巩固考场得分点。",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextSecondary,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        hapticManager.tapLight()
                                        soundManager.playClick()
                                        val cards = repository.getRandomQueue(
                                            limit = 20,
                                            moduleIds = null,
                                            gaoKaoOnly = true
                                        )
                                        onStartSession("高考必背 72 篇专项背诵", cards)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "开启 72 篇专项背诵 (20题)",
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Cross-book Random Practice
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = BgSurface),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "选择抽测范围",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                scopeOptions.forEachIndexed { index, (label, _) ->
                                    val isSelected = (selectedScopeIndex == index)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedScopeIndex = index
                                                hapticManager.tapLight()
                                                soundManager.playClick()
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedScopeIndex = index
                                                hapticManager.tapLight()
                                                soundManager.playClick()
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = StudyBlueAccent)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            color = if (isSelected) TextPrimary else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "抽取题量",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(10, 20, 30, 50).forEach { count ->
                                        val isSelected = (selectedCount == count)
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clickable {
                                                    selectedCount = count
                                                    hapticManager.tapLight()
                                                    soundManager.playClick()
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) StudyNavy else BgSurfaceMuted,
                                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$count 题",
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        hapticManager.tapLight()
                                        soundManager.playClick()
                                        val scopeModules = scopeOptions[selectedScopeIndex].second
                                        val randomCards = repository.getRandomQueue(selectedCount, scopeModules)
                                        val sessionTitle = "随机背诵 · ${scopeOptions[selectedScopeIndex].first}"
                                        onStartSession(sessionTitle, randomCards)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "开始跨篇目随机背诵",
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Classic Long Prose Cloze Recommendations
                    item {
                        Text(
                            text = "精选经典长篇 · 重点攻克",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    items(longArticles.size) { idx ->
                        val article = longArticles[idx]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hapticManager.tapLight()
                                    soundManager.playClick()
                                    onStartCloze(article)
                                }
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = BgSurface),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(StudyBlueLight, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = StudyBlueAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "《${article.title}》",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${article.dynasty} · ${article.author} · 渐进遮挡背诵",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
