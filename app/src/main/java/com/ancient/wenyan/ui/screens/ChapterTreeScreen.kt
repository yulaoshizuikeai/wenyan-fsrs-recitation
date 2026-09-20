package com.ancient.wenyan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.Module
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterTreeScreen(
    repository: WenYanRepository,
    onBack: () -> Unit,
    onStartFlashcards: (Article) -> Unit,
    onStartCloze: (Article) -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    var searchQuery by remember { mutableStateOf("") }
    var filterGaoKaoOnly by remember { mutableStateOf(false) }
    var expandedModuleIds by remember { mutableStateOf(setOf("MODULE_BX_1")) }
    var selectedArticleForModal by remember { mutableStateOf<Article?>(null) }

    val modules = remember { repository.getModules() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "章节篇目文库",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticManager.tapLight()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCanvas)
            )
        },
        containerColor = BgCanvas
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = {
                    Text("搜索名句、篇名、作者、朝代...", fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "搜索", tint = TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            hapticManager.tapLight()
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "清除", tint = TextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BgSurface,
                    unfocusedContainerColor = BgSurface,
                    focusedBorderColor = StudyBlueAccent,
                    unfocusedBorderColor = BorderSubtle
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "全高中 11 册教材 · 100 篇文赋",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = TextSecondary
                )

                FilterChip(
                    selected = filterGaoKaoOnly,
                    onClick = {
                        filterGaoKaoOnly = !filterGaoKaoOnly
                        hapticManager.tapLight()
                        soundManager.playClick()
                    },
                    label = {
                        Text(
                            text = "高考必背72篇",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = if (filterGaoKaoOnly) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StreakFlame.copy(alpha = 0.12f),
                        selectedLabelColor = StreakFlame
                    )
                )
            }

            // Modules & Articles List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                items(modules) { module ->
                    val moduleArticles = remember(module, searchQuery, filterGaoKaoOnly) {
                        repository.getArticlesByModule(module.id).filter { article ->
                            val matchesSearch = searchQuery.isBlank() ||
                                    article.title.contains(searchQuery, ignoreCase = true) ||
                                    article.author.contains(searchQuery, ignoreCase = true) ||
                                    article.dynasty.contains(searchQuery, ignoreCase = true) ||
                                    article.fullContent.contains(searchQuery, ignoreCase = true)
                            val matchesGaoKao = !filterGaoKaoOnly || article.isGaoKao72
                            matchesSearch && matchesGaoKao
                        }
                    }

                    if (moduleArticles.isNotEmpty() || searchQuery.isBlank()) {
                        val isExpanded = if (searchQuery.isNotBlank()) true else expandedModuleIds.contains(module.id)
                        ModuleCard(
                            module = module,
                            articles = moduleArticles,
                            isExpanded = isExpanded,
                            onToggle = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                expandedModuleIds = if (isExpanded) {
                                    expandedModuleIds - module.id
                                } else {
                                    expandedModuleIds + module.id
                                }
                            },
                            onArticleClick = { article ->
                                hapticManager.tapLight()
                                soundManager.playClick()
                                selectedArticleForModal = article
                            },
                            repository = repository
                        )
                    }
                }
            }
        }
    }

    // Article Mode Selection Bottom Sheet
    selectedArticleForModal?.let { article ->
        ModalBottomSheet(
            onDismissRequest = { selectedArticleForModal = null },
            containerColor = BgSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "《${article.title}》",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = TextPrimary
                )
                Text(
                    text = "${article.dynasty} · ${article.author} · ${article.genre}",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                Button(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        val target = article
                        selectedArticleForModal = null
                        onStartFlashcards(target)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyNavy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("单句翻转闪卡背诵 (FSRS算法)", fontFamily = FontFamily.SansSerif, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        val target = article
                        selectedArticleForModal = null
                        onStartCloze(target)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(imageVector = Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(20.dp), tint = StudyBlueAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("整篇渐进遮挡背诵", fontFamily = FontFamily.SansSerif, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = StudyBlueAccent)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ModuleCard(
    module: Module,
    articles: List<Article>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onArticleClick: (Article) -> Unit,
    repository: WenYanRepository
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (module.isRecitationOnly) StreakFlame else StudyBlueAccent,
                                RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = module.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${articles.size} 篇",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = TextTertiary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "展开/折叠",
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    articles.forEach { article ->
                        val progress = remember(article) { repository.getArticleProgress(article.id) }
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        ArticleItemRow(
                            article = article,
                            progress = progress,
                            onClick = { onArticleClick(article) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleItemRow(
    article: Article,
    progress: com.ancient.wenyan.domain.model.ArticleProgress,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = article.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    color = TextPrimary
                )
                if (article.isGaoKao72) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = StreakFlame.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "高考72",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StreakFlame,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "${article.dynasty} · ${article.author} · ${article.genre}",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${progress.masteryPercentage.toInt()}% 掌握",
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = if (progress.masteryPercentage >= 80f) SuccessGreen else TextTertiary
            )
            LinearProgressIndicator(
                progress = { progress.masteryPercentage / 100f },
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .padding(top = 4.dp),
                color = if (progress.masteryPercentage >= 80f) SuccessGreen else StudyBlueAccent,
                trackColor = BgSurfaceMuted
            )
        }
    }
}
