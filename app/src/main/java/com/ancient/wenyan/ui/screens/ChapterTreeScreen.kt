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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.ArticleProgress
import com.ancient.wenyan.domain.model.Module
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*
import com.ancient.wenyan.ui.viewmodel.ChapterTreeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterTreeScreen(
    viewModel: ChapterTreeViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onStartFlashcards: (Article) -> Unit,
    onStartCloze: (Article) -> Unit,
    onOpenGaoKaoScenario: (String) -> Unit = {},
    onOpenSnowball: (String) -> Unit = {},
    onOpenCertificate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    val currentRepo = remember(viewModel) { viewModel.getRepository() }
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.stats

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterGaoKaoOnly by rememberSaveable { mutableStateOf(false) }

    // Preserve expanded modules across screen rotations
    var expandedModuleIds by rememberSaveable { mutableStateOf(emptyList<String>()) }

    var selectedArticleIdForModal by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedArticleForModal = remember(selectedArticleIdForModal) {
        selectedArticleIdForModal?.let { CurriculumDataSource.ARTICLE_MAP[it] }
    }

    val modules = uiState.modules

    // Precompute filtered modules and their matching articles outside LazyColumn composition
    val filteredModulesWithArticles = remember(modules, searchQuery, filterGaoKaoOnly) {
        modules.mapNotNull { module ->
            val articles = currentRepo.getArticlesByModule(module.id).filter { article ->
                val matchesSearch = searchQuery.isBlank() ||
                        article.title.contains(searchQuery, ignoreCase = true) ||
                        article.author.contains(searchQuery, ignoreCase = true) ||
                        article.dynasty.contains(searchQuery, ignoreCase = true) ||
                        article.fullContent.contains(searchQuery, ignoreCase = true)
                val matchesGaoKao = !filterGaoKaoOnly || article.isGaoKao72
                matchesSearch && matchesGaoKao
            }
            if (articles.isNotEmpty() || (searchQuery.isBlank() && !filterGaoKaoOnly)) {
                module to articles
            } else {
                null
            }
        }
    }

    val handleArticleClick: (Article) -> Unit = remember {
        { article ->
            hapticManager.tapLight()
            soundManager.playClick()
            selectedArticleIdForModal = article.id
        }
    }

    // Precompute and cache article progress map so list scrolling is O(1) without repeated scans
    val articleProgressMap = remember(stats, filteredModulesWithArticles) {
        val map = mutableMapOf<String, ArticleProgress>()
        filteredModulesWithArticles.forEach { (_, moduleArticles) ->
            moduleArticles.forEach { article ->
                map[article.id] = currentRepo.getArticleProgress(article.id)
            }
        }
        map
    }

    val getProgress: (Article) -> ArticleProgress = remember(articleProgressMap) {
        { article -> articleProgressMap[article.id] ?: ArticleProgress(article.id, 0, 0, 0, 0, 0f) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "章节篇目文库",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
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
                    Text(
                        text = "搜索名句、篇名、作者、朝代...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
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
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "清除搜索", tint = TextSecondary)
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
                    text = if (uiState.selectedBookScope != null) {
                        "当前课本：${uiState.selectedBookName} (${modules.size} 单元)"
                    } else {
                        "全高中 11 册教材 · 100 篇文赋"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.selectedBookScope != null) StudyBlueAccent else TextSecondary
                )

                FilterChip(
                    selected = filterGaoKaoOnly,
                    onClick = {
                        filterGaoKaoOnly = !filterGaoKaoOnly
                        hapticManager.tapLight()
                        soundManager.playClick()
                    },
                    leadingIcon = if (filterGaoKaoOnly) {
                        {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                tint = StreakFlame
                            )
                        }
                    } else null,
                    label = {
                        Text(
                            text = "高考必背72篇",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (filterGaoKaoOnly) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StreakFlame.copy(alpha = 0.12f),
                        selectedLabelColor = StreakFlame
                    )
                )
            }

            if (filteredModulesWithArticles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "未找到与“$searchQuery”相关的篇目" else "暂无匹配篇目",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // Modules & Articles List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {
                    items(
                        items = filteredModulesWithArticles,
                        key = { it.first.id }
                    ) { (module, moduleArticles) ->
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
                            onArticleClick = handleArticleClick,
                            getProgress = getProgress
                        )
                    }
                }
            }
        }
    }

    // Article Mode Selection Bottom Sheet
    selectedArticleForModal?.let { article ->
        ModalBottomSheet(
            onDismissRequest = { selectedArticleIdForModal = null },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = BgSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "《${article.title}》",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "${article.dynasty} · ${article.author} · ${article.genre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // 1. FSRS Flashcards
                Button(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        val target = article
                        selectedArticleIdForModal = null
                        onStartFlashcards(target)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "单句翻转闪卡背诵 (FSRS算法)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. GaoKao Scenario Recitation
                OutlinedButton(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        val targetTitle = article.title
                        selectedArticleIdForModal = null
                        onOpenGaoKaoScenario(targetTitle)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = StreakFlame
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "高考情境默写挑战",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = StreakFlame
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Progressive Cloze Recitation
                OutlinedButton(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        val target = article
                        selectedArticleIdForModal = null
                        onStartCloze(target)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = StudyBlueAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "整篇渐进遮挡背诵",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = StudyBlueAccent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Snowball Chaining Recitation
                OutlinedButton(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        val targetId = article.id
                        selectedArticleIdForModal = null
                        onOpenSnowball(targetId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.Snowboarding,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = StudyBlueAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "长文滚雪球串联背诵",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Certificate and Copybook
                FilledTonalButton(
                    onClick = {
                        hapticManager.tapLight()
                        soundManager.playClick()
                        val targetId = article.id
                        selectedArticleIdForModal = null
                        onOpenCertificate(targetId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = StreakFlame
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "结业文牒与硬笔字帖",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }
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
    getProgress: (Article) -> ArticleProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                    .clickable(
                        onClick = onToggle,
                        onClickLabel = if (isExpanded) "收起${module.name}" else "展开${module.name}"
                    )
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${articles.size} 篇",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
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
                        val progress = getProgress(article)
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
    progress: ArticleProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                onClickLabel = "选择《${article.title}》背诵模式"
            )
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
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
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = StreakFlame,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "${article.dynasty} · ${article.author} · ${article.genre}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            val rawMastery = progress.masteryPercentage
            val safeMastery = if (rawMastery.isNaN() || rawMastery.isInfinite()) 0f else rawMastery.coerceIn(0f, 100f)
            val progressFraction = safeMastery / 100f
            Text(
                text = "${safeMastery.toInt()}% 掌握",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                color = if (safeMastery >= 80f) SuccessGreen else TextTertiary
            )
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .padding(top = 4.dp),
                color = if (safeMastery >= 80f) SuccessGreen else StudyBlueAccent,
                trackColor = BgSurfaceMuted
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModuleCardPreview() {
    WenYanTheme {
        ModuleCard(
            module = Module(
                id = "MODULE_PREVIEW",
                name = "必修上册",
                shortName = "必修上",
                category = "REQUIRED",
                isRecitationOnly = false,
                sortOrder = 1,
                totalArticles = 15
            ),
            articles = listOf(
                Article(
                    id = "art_preview",
                    moduleId = "MODULE_PREVIEW",
                    title = "劝学",
                    author = "荀子",
                    dynasty = "先秦",
                    genre = "散文",
                    isGaoKao72 = true,
                    fullContent = "君子曰：学不可以已。",
                    sortOrder = 1
                )
            ),
            isExpanded = true,
            onToggle = {},
            onArticleClick = {},
            getProgress = {
                ArticleProgress(
                    articleId = it.id,
                    totalCards = 10,
                    newCards = 2,
                    learningCards = 3,
                    reviewCards = 5,
                    masteryPercentage = 50f
                )
            }
        )
    }
}
