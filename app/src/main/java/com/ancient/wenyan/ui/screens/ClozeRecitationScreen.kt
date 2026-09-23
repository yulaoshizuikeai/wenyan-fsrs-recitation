package com.ancient.wenyan.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.domain.cloze.ClozeEngine
import com.ancient.wenyan.domain.cloze.ClozeToken
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClozeRecitationScreen(
    article: Article,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    var selectedLevel by rememberSaveable { mutableIntStateOf(1) }
    var revealOriginal by rememberSaveable { mutableStateOf(false) }
    var revealedTokenIds by rememberSaveable(
        selectedLevel, article.id,
        stateSaver = Saver<Set<Int>, ArrayList<Int>>(
            save = { ArrayList(it) },
            restore = { it.toSet() }
        )
    ) { mutableStateOf(setOf<Int>()) }

    val fullText = article.fullContent
    val clozeKeywords = remember(article) {
        article.annotations.map { it.split("：", "（", " ").first() }.filter { it.length in 2..6 }
    }

    val tokens = remember(selectedLevel, article) {
        ClozeEngine.tokenize(
            fullText,
            selectedLevel,
            clozeKeywords.ifEmpty { listOf("天下", "君子", "故", "以", "夫", "何") }
        )
    }

    // Split text into paragraphs based on newline
    val paragraphs = remember(tokens) {
        val list = mutableListOf<MutableList<ClozeToken>>()
        var currentPara = mutableListOf<ClozeToken>()
        var subTokenId = 100_000
        for (token in tokens) {
            if (token.originalText.contains("\n")) {
                val parts = token.originalText.split("\n")
                for (i in parts.indices) {
                    if (parts[i].isNotEmpty()) {
                        val assignedId = if (i == 0) token.id else (subTokenId++)
                        currentPara.add(token.copy(id = assignedId, originalText = parts[i]))
                    }
                    if (i < parts.size - 1) {
                        list.add(currentPara)
                        currentPara = mutableListOf()
                    }
                }
            } else {
                currentPara.add(token)
            }
        }
        if (currentPara.isNotEmpty()) {
            list.add(currentPara)
        }
        list
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
                            fontFamily = FontFamily.SansSerif,
                            color = TextPrimary
                        )
                        Text(
                            text = "${article.dynasty} · ${article.author} · 渐进遮挡背诵",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = TextSecondary
                        )
                    }
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
                actions = {
                    IconButton(onClick = {
                        val next = !revealOriginal
                        revealOriginal = next
                        hapticManager.tapLight()
                        soundManager.playClick()
                    }) {
                        Icon(
                            imageVector = if (revealOriginal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (revealOriginal) "恢复遮挡" else "一键全览",
                            tint = if (revealOriginal) StreakFlame else StudyBlueAccent
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Difficulty Level Selector
            Text(
                text = "遮挡难度阶梯",
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val levels = listOf(
                0 to "L0 原文",
                1 to "L1 重点",
                2 to "L2 半句",
                3 to "L3 首字",
                4 to "L4 全盲"
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                levels.forEachIndexed { index, (level, name) ->
                    val isSelected = (selectedLevel == level)
                    SegmentedButton(
                        selected = isSelected,
                        onClick = {
                            if (selectedLevel != level) {
                                selectedLevel = level
                                revealOriginal = false
                                soundManager.playClick()
                                hapticManager.tapLight()
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = levels.size)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Reading & Interactive Cloze Card
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(vertical = 18.dp)
                ) {
                    // Tip bar
                    item(key = "tip_bar") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudyBlueLight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = StudyBlueAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "轻触文中遮挡胶囊即可实时揭晓/隐藏答案，熟背无误后可逐级提升难度",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = StudyBlueAccent,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Text FlowRow Paragraphs
                    items(
                        count = paragraphs.size,
                        key = { idx -> "para_$idx" }
                    ) { paraIndex ->
                        val paraTokens = paragraphs[paraIndex]
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            paraTokens.forEach { token ->
                                if (token.isMasked) {
                                    val isRevealed = revealOriginal || (token.id in revealedTokenIds)
                                    InteractiveClozePill(
                                        token = token,
                                        isRevealed = isRevealed,
                                        onToggle = {
                                            if (token.id in revealedTokenIds) {
                                                revealedTokenIds = revealedTokenIds - token.id
                                                hapticManager.tapLight()
                                                soundManager.playClick()
                                            } else {
                                                revealedTokenIds = revealedTokenIds + token.id
                                                hapticManager.clozePop()
                                                soundManager.playClozeReveal()
                                            }
                                        }
                                    )
                                } else {
                                    Text(
                                        text = token.originalText,
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        color = TextPrimary,
                                        lineHeight = 32.sp,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                    }

                    // Annotations
                    if (article.annotations.isNotEmpty()) {
                        item(key = "annotations_header") {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "【重点字词考点注解】",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(
                            count = article.annotations.size,
                            key = { idx -> "annot_$idx" }
                        ) { idx ->
                            val note = article.annotations[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    color = StudyBlueAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = note,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = TextSecondary,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Accessible Cloze Masking Pill with spring pop animation
 */
@Composable
fun InteractiveClozePill(
    token: ClozeToken,
    isRevealed: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pill_press"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isRevealed) StudyBlueLight else BgSurfaceMuted,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pill_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isRevealed) StudyBlueAccent else BorderSubtle,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pill_border"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = if (isRevealed) "隐藏答案" else "显示填空答案"
            ) {
                onToggle()
            },
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        AnimatedContent(
            targetState = isRevealed,
            transitionSpec = {
                (fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                 scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), initialScale = 0.85f))
                    .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessHigh)))
            },
            label = "pill_content"
        ) { revealed ->
            Box(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                if (revealed) {
                    Text(
                        text = token.originalText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = StudyBlueAccent,
                        letterSpacing = 0.5.sp
                    )
                } else {
                    Text(
                        text = "⟦ ${"_".repeat(token.originalText.length.coerceIn(2, 6))} ⟧",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextTertiary,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
