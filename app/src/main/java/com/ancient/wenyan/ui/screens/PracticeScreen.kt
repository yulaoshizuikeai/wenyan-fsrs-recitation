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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.BookPresets
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    repository: WenYanRepository,
    onStartSession: (String, List<Pair<Flashcard, CardFsrsState>>) -> Unit,
    onStartCloze: (Article) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 高考72篇专项, 1: 跨篇自选抽测, 2: 经典长文遮挡

    val currentRepoScope by repository.selectedBookScope.collectAsState()
    val currentRepoName by repository.selectedBookName.collectAsState()

    val scopeOptions = remember {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "专项练习",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Segmented Control Filter Tabs
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("高考 72 篇", fontFamily = FontFamily.Serif, fontSize = 13.sp)
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("跨篇随机", fontFamily = FontFamily.Serif, fontSize = 13.sp)
                    }
                    SegmentedButton(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("长文遮挡", fontFamily = FontFamily.Serif, fontSize = 13.sp)
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
                                            .size(42.dp)
                                            .background(StreakFlame.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = StreakFlame,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "高考必背 72 篇专项",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "教育部普通高中统编课标核心古诗文",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Serif,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "根据教育部高中语文课程标准，精准筛选 72 篇必背名篇中的高频考查出句与名句对句，采用 FSRS 算法进行专项强化与情境默写模拟。",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = TextSecondary,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        val cards = repository.getRandomQueue(
                                            limit = 20,
                                            moduleIds = null,
                                            gaoKaoOnly = true
                                        )
                                        onStartSession("高考必背 72 篇专项背诵", cards)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "开启 72 篇专项背诵 (20题)",
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Serif,
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
                                    fontFamily = FontFamily.Serif,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                scopeOptions.forEachIndexed { index, (label, _) ->
                                    val isSelected = (selectedScopeIndex == index)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedScopeIndex = index }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedScopeIndex = index },
                                            colors = RadioButtonDefaults.colors(selectedColor = StudyNavy)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Serif,
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
                                    fontFamily = FontFamily.Serif,
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
                                                .height(38.dp)
                                                .clickable { selectedCount = count },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) StudyNavy else BgSurfaceMuted,
                                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$count 题",
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        val scopeModules = scopeOptions[selectedScopeIndex].second
                                        val randomCards = repository.getRandomQueue(selectedCount, scopeModules)
                                        val sessionTitle = "随机抽测 · ${scopeOptions[selectedScopeIndex].first}"
                                        onStartSession(sessionTitle, randomCards)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "开始抽取练习",
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Famous Long Articles for Cloze Recitation
                    item {
                        Text(
                            text = "经典长篇文赋推荐",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    val famousArticleIds = listOf("art_bx1_07", "art_bx2_05", "art_bx2_06", "art_bx2_08", "art_xb3_07")
                    famousArticleIds.forEach { artId ->
                        val article = repository.getArticle(artId)
                        if (article != null) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onStartCloze(article) }
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = BgSurface),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
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

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "《${article.title}》",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Serif,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${article.dynasty} · ${article.author} · 5级渐进遮挡",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Serif,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = "去背诵 ➔",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.SemiBold,
                                            color = StudyBlueAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
