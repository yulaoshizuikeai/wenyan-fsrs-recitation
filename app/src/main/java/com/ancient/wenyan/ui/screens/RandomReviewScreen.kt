package com.ancient.wenyan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.model.BookPresets
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomReviewScreen(
    repository: WenYanRepository,
    onBack: () -> Unit,
    onStartSession: (String, List<Pair<Flashcard, CardFsrsState>>) -> Unit
) {
    val currentRepoScope by repository.selectedBookScope.collectAsState()
    val currentRepoName by repository.selectedBookName.collectAsState()

    val scopeOptions = remember {
        listOf(
            "当前选定教材 ($currentRepoName)" to currentRepoScope,
            "全部 11 册教材 (100篇)" to null,
            "必修上册 (19篇)" to BookPresets.BOOK_BX_1.moduleIds,
            "必修下册 (17篇)" to BookPresets.BOOK_BX_2.moduleIds,
            "选择性必修上册 (10篇)" to BookPresets.BOOK_XB_1.moduleIds,
            "选择性必修中册 (8篇)" to BookPresets.BOOK_XB_2.moduleIds,
            "选择性必修下册 (17篇)" to BookPresets.BOOK_XB_3.moduleIds,
            "选修(古代诗歌散文欣赏 · 29篇)" to BookPresets.BOOK_XX_APPRECIATION.moduleIds,
            "必修全套 (上/下两册 · 36篇)" to BookPresets.SCOPE_REQUIRED_ALL.moduleIds,
            "选择性必修全套 (上/中/下 · 35篇)" to BookPresets.SCOPE_SELECTIVE_ALL.moduleIds
        )
    }

    var selectedScopeIndex by remember { mutableIntStateOf(0) }
    var selectedCount by remember { mutableIntStateOf(20) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "跨篇目随机背诵",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkCharcoal
                    )
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
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, XuanBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "选择背诵课本范围",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal
                        )
                        Text(
                            text = "可选择单册课本或整套教材进行打乱抽测",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            color = InkMedium,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

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
                                    colors = RadioButtonDefaults.colors(selectedColor = BambooGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = if (isSelected) InkCharcoal else InkMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, XuanBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "单次抽取卡片数量",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(10, 20, 30, 50).forEach { count ->
                                val isSelected = (selectedCount == count)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clickable { selectedCount = count },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) BambooGreen else XuanPaperDeep,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, XuanBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$count 题",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) androidx.compose.ui.graphics.Color.White else InkMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Launch Button
            Button(
                onClick = {
                    val scopeModules = scopeOptions[selectedScopeIndex].second
                    val randomCards = repository.getRandomQueue(selectedCount, scopeModules)
                    val sessionTitle = "随机背诵 · ${scopeOptions[selectedScopeIndex].first}"
                    onStartSession(sessionTitle, randomCards)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BambooGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "开始跨篇目随机背诵",
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
