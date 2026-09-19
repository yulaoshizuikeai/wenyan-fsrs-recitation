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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.domain.cloze.ClozeEngine
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClozeRecitationScreen(
    article: Article,
    onBack: () -> Unit
) {
    var selectedLevel by remember { mutableIntStateOf(1) }
    var revealOriginal by remember { mutableStateOf(false) }

    val fullText = article.fullContent
    val clozeKeywords = remember(article) {
        article.annotations.map { it.split("：", "（", " ").first() }.filter { it.length in 2..6 }
    }

    val displayedText = remember(selectedLevel, revealOriginal, article) {
        if (revealOriginal) {
            fullText
        } else {
            when (selectedLevel) {
                0 -> ClozeEngine.generateLevel0(fullText)
                1 -> ClozeEngine.generateLevel1(fullText, clozeKeywords.ifEmpty { listOf("天下", "君子", "故", "以", "夫", "何") })
                2 -> ClozeEngine.generateLevel2(fullText)
                3 -> ClozeEngine.generateLevel3(fullText)
                4 -> ClozeEngine.generateLevel4(fullText)
                else -> fullText
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "《${article.title}》",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal
                        )
                        Text(
                            text = "${article.dynasty} · ${article.author} · 渐进遮挡背诵",
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
                    IconButton(onClick = { revealOriginal = !revealOriginal }) {
                        Icon(
                            imageVector = if (revealOriginal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (revealOriginal) "隐藏原文" else "查看原文",
                            tint = BambooGreen
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Level Selector
            Text(
                text = "选择遮挡难度：",
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = InkMedium,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val levels = listOf(
                    0 to "L0 原文",
                    1 to "L1 关键词",
                    2 to "L2 半句",
                    3 to "L3 首字",
                    4 to "L4 全盲"
                )
                levels.forEach { (level, name) ->
                    val isSelected = (selectedLevel == level)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable {
                                selectedLevel = level
                                revealOriginal = false
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BambooGreen else XuanPaperDeep,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, XuanBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else InkMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Text Scrollable Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, XuanBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = displayedText,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        color = InkCharcoal,
                        lineHeight = 32.sp,
                        letterSpacing = 1.sp
                    )

                    if (article.annotations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = XuanBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "【重点字词注解】",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkMedium
                        )
                        article.annotations.forEach { note ->
                            Text(
                                text = "• $note",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif,
                                color = InkFaded,
                                modifier = Modifier.padding(top = 4.dp),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Toggle hint text
            Text(
                text = if (revealOriginal) "当前正在查看完整原文，点击右上角眼睛可重新遮挡" else "轻触右上角眼睛图标可即时对照原文核验",
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                color = InkFaded,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
