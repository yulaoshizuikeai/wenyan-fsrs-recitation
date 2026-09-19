package com.ancient.wenyan.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.Module
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterTreeScreen(
    repository: WenYanRepository,
    onBack: () -> Unit,
    onStartFlashcards: (Article) -> Unit,
    onStartCloze: (Article) -> Unit
) {
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
                        text = "章节篇目学习",
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
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar & Filter Chip
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = {
                    Text("搜索名句、篇名、作者、朝代...", fontFamily = FontFamily.Serif, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "搜索", tint = InkMedium)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "清除", tint = InkMedium)
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = XuanPaperCard,
                    unfocusedContainerColor = XuanPaperCard,
                    focusedBorderColor = BambooGreen,
                    unfocusedBorderColor = XuanBorder
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
                    text = "全高中 11 册教材 · 共 100 篇",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium
                )

                FilterChip(
                    selected = filterGaoKaoOnly,
                    onClick = { filterGaoKaoOnly = !filterGaoKaoOnly },
                    label = {
                        Text(
                            text = "仅看高考必背72篇",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CinnabarRed.copy(alpha = 0.15f),
                        selectedLabelColor = CinnabarRed
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
                                expandedModuleIds = if (isExpanded) {
                                    expandedModuleIds - module.id
                                } else {
                                    expandedModuleIds + module.id
                                }
                            },
                            onArticleClick = { article ->
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
            containerColor = XuanPaperCard,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
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
                    fontFamily = FontFamily.Serif,
                    color = InkCharcoal
                )
                Text(
                    text = "${article.dynasty} · ${article.author} · ${article.genre}",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                Button(
                    onClick = {
                        val target = article
                        selectedArticleForModal = null
                        onStartFlashcards(target)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BambooGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("单句翻转闪卡背诵 (FSRS算法)", fontFamily = FontFamily.Serif, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val target = article
                        selectedArticleForModal = null
                        onStartCloze(target)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MountainTeal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("整篇渐进遮挡背诵", fontFamily = FontFamily.Serif, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
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
            .border(1.dp, XuanBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
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
                                if (module.isRecitationOnly) CinnabarRed else BambooGreen,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = module.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkCharcoal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${articles.size} 篇",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        color = InkFaded,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "展开/折叠",
                        tint = InkMedium
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    articles.forEach { article ->
                        val progress = remember(article) { repository.getArticleProgress(article.id) }
                        HorizontalDivider(color = XuanBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
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
                    fontFamily = FontFamily.Serif,
                    color = InkCharcoal
                )
                if (article.isGaoKao72) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = CinnabarRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "高考72",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CinnabarRed,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "${article.dynasty} · ${article.author} · ${article.genre}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                color = InkMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${progress.masteryPercentage.toInt()}% 掌握",
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = if (progress.masteryPercentage >= 80f) BambooGreen else InkFaded
            )
            LinearProgressIndicator(
                progress = { progress.masteryPercentage / 100f },
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .padding(top = 4.dp),
                color = BambooGreen,
                trackColor = XuanBorder
            )
        }
    }
}
