package com.ancient.wenyan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.domain.export.CertificateAndCopybookGenerator
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateAndCopybookScreen(
    articleId: String? = "art_chibifu",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }

    val article: Article = remember(articleId) {
        CurriculumDataSource.ARTICLE_MAP[articleId] ?: CurriculumDataSource.ALL_ARTICLES.first { it.id == "art_chibifu" }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: 结业文牒, 1: 书法字帖
    val certificate = remember(article) {
        CertificateAndCopybookGenerator.generateCertificate(article, 100f)
    }
    val copybook = remember(article) {
        CertificateAndCopybookGenerator.generateCopybook(article)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedTab == 0) "国风结业文牒" else "硬笔练习字帖",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "《${article.title}》· 成就见证与临摹",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSurface)
            )
        },
        containerColor = BgCanvas
    ) { innerPadding ->
        Column(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BgSurface,
                contentColor = StudyBlueAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        hapticManager.tapLight()
                        selectedTab = 0
                    },
                    text = { Text("结业文牒") },
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        hapticManager.tapLight()
                        selectedTab = 1
                    },
                    text = { Text("米字格字帖") },
                    icon = { Icon(Icons.Default.HistoryEdu, contentDescription = null) }
                )
            }

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    // Traditional Xuan Paper Style Diploma Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF8EE)) // 经典宣纸色
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Border Ornament
                            Text(
                                text = "❖ 结 业 文 牒 ❖",
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A3E3D)
                            )

                            HorizontalDivider(color = Color(0xFFD3C5A5), thickness = 1.2.dp)

                            Text(
                                text = "学子研习经典，孜孜以求",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFF6B5B52)
                            )

                            Text(
                                text = "《${certificate.articleTitle}》",
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C221E)
                            )

                            Text(
                                text = "【${certificate.dynasty}】${certificate.author} 著 · 共 ${certificate.totalSentences} 联名句",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFF7A6A5E)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = certificate.commendationText,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 26.sp,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF382A24)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Cinnabar Seal (朱砂印章)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "发牒吉日：",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = Color(0xFF8C7C70)
                                    )
                                    Text(
                                        text = certificate.issueDate,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF4A3E3D)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .rotate(-5f)
                                        .border(2.5.dp, Color(0xFFC23A22), RoundedCornerShape(8.dp))
                                        .background(Color(0xFFC23A22).copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = certificate.sealText,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC23A22),
                                        lineHeight = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Calligraphy Copybook (米字格字帖排版)
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = BgSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "米字格临摹字帖 · ${copybook.articleTitle}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            copybook.lines.take(8).forEach { line ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        line.characters.forEach { copyChar ->
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .border(0.8.dp, BorderSubtle, RoundedCornerShape(2.dp))
                                                    .background(if (copyChar.isPunctuation) Color.Transparent else Color(0xFFFAFAFA)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = copyChar.char.toString(),
                                                    fontSize = 18.sp,
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.Normal,
                                                    color = if (copyChar.isPunctuation) TextTertiary else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "提示：字帖支持在平板上手写笔临摹，或打印成纸质米字格强化肌肉记忆。",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
