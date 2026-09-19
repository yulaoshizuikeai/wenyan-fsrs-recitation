package com.ancient.wenyan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomReviewScreen(
    repository: WenYanRepository,
    onBack: () -> Unit,
    onStartSession: (String, List<Pair<Flashcard, CardFsrsState>>) -> Unit
) {
    var selectedScopeIndex by remember { mutableIntStateOf(0) }
    var selectedCount by remember { mutableIntStateOf(20) }

    val scopeOptions = listOf(
        "全部 11 册教材" to null,
        "必修四册 (上/下及诵读)" to setOf("MODULE_BX_1", "MODULE_BX_1_RECITE", "MODULE_BX_2", "MODULE_BX_2_RECITE"),
        "选择性必修 (上/中/下及诵读)" to setOf("MODULE_XB_1", "MODULE_XB_1_RECITE", "MODULE_XB_2", "MODULE_XB_2_RECITE", "MODULE_XB_3", "MODULE_XB_3_RECITE"),
        "选修 (古代诗歌散文欣赏)" to setOf("MODULE_XX_APPRECIATION")
    )

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
            Column {
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
                            text = "选择背诵篇目范围",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        scopeOptions.forEachIndexed { index, (label, _) ->
                            val isSelected = (selectedScopeIndex == index)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedScopeIndex = index }
                                    .padding(vertical = 8.dp),
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
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
