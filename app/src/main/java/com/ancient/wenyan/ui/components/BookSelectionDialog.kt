package com.ancient.wenyan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import com.ancient.wenyan.domain.model.BookGroup
import com.ancient.wenyan.domain.model.BookPresets
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

@Composable
fun BookSelectionDialog(
    currentScope: Set<String>?,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirmSelection: (Set<String>?, String) -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    val hapticManager = remember { HapticManager.getInstance(context) }

    // Local state for selected modules. Empty set or null means all
    var selectedModules by remember { mutableStateOf(currentScope ?: emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BgSurface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "选择背诵教材",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = {
                        hapticManager.tapLight()
                        onDismiss()
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = TextSecondary
                        )
                    }
                }

                Text(
                    text = "当前锁定：$currentName。针对当前学习阶段聚焦课本，研读与记忆统计将精准匹配所选范围",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Scrollable List of Presets & Single Books
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Presets
                    Text(
                        text = "常用范围预设",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextPrimary
                    )

                    // 1. 全部教材
                    PresetOptionItem(
                        title = "全部 11 册教材 (100篇)",
                        subtitle = "高中统编课标内全部篇目全库覆盖",
                        isSelected = selectedModules.isEmpty(),
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            selectedModules = emptySet()
                        }
                    )

                    // 2. 必修全套
                    PresetOptionItem(
                        title = "必修全套 (上/下两册 · 36篇)",
                        subtitle = "覆盖必修上册、必修下册及古诗词诵读",
                        isSelected = selectedModules == BookPresets.SCOPE_REQUIRED_ALL.moduleIds,
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            selectedModules = BookPresets.SCOPE_REQUIRED_ALL.moduleIds
                        }
                    )

                    // 3. 选必全套
                    PresetOptionItem(
                        title = "选择性必修全套 (上/中/下 · 35篇)",
                        subtitle = "覆盖选必三册课文及古诗词诵读",
                        isSelected = selectedModules == BookPresets.SCOPE_SELECTIVE_ALL.moduleIds,
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            selectedModules = BookPresets.SCOPE_SELECTIVE_ALL.moduleIds
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "单册教材精准选择 (支持多选组合)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = TextPrimary
                    )

                    // Individual Books
                    BookPresets.ALL_SINGLE_BOOKS.forEach { book ->
                        val isFullySelected = book.moduleIds.all { it in selectedModules }
                        BookCheckboxItem(
                            book = book,
                            isSelected = isFullySelected,
                            onToggle = {
                                hapticManager.tapLight()
                                soundManager.playClick()
                                selectedModules = if (isFullySelected) {
                                    selectedModules - book.moduleIds
                                } else {
                                    selectedModules + book.moduleIds
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            selectedModules = emptySet()
                            onConfirmSelection(null, "全部 11 册教材")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text("全选课本", fontFamily = FontFamily.SansSerif)
                    }

                    Button(
                        onClick = {
                            hapticManager.successPulse()
                            soundManager.playCorrect()
                            val finalScope = if (selectedModules.isEmpty()) null else selectedModules
                            val finalName = when {
                                finalScope == null -> "全部 11 册教材"
                                finalScope == BookPresets.BOOK_BX_1.moduleIds -> "必修上册"
                                finalScope == BookPresets.BOOK_BX_2.moduleIds -> "必修下册"
                                finalScope == BookPresets.BOOK_XB_1.moduleIds -> "选择性必修上册"
                                finalScope == BookPresets.BOOK_XB_2.moduleIds -> "选择性必修中册"
                                finalScope == BookPresets.BOOK_XB_3.moduleIds -> "选择性必修下册"
                                finalScope == BookPresets.BOOK_XX_APPRECIATION.moduleIds -> "选修(诗歌散文欣赏)"
                                finalScope == BookPresets.SCOPE_REQUIRED_ALL.moduleIds -> "必修全套"
                                finalScope == BookPresets.SCOPE_SELECTIVE_ALL.moduleIds -> "选必全套"
                                else -> "自选 (${BookPresets.ALL_SINGLE_BOOKS.count { it.moduleIds.all { m -> m in finalScope } }} 册)"
                            }
                            onConfirmSelection(finalScope, finalName)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyNavy)
                    ) {
                        Text(
                            text = "确定选择",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) StudyBlueLight else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.outlineVariant
            )
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = if (isSelected) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "选中",
                        tint = StudyBlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun BookCheckboxItem(
    book: BookGroup,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    OutlinedCard(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) StudyBlueLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.outlineVariant
            )
        )
    ) {
        ListItem(
            leadingContent = {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = StudyBlueAccent,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )
            },
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Badge(
                        containerColor = StreakFlame.copy(alpha = 0.12f),
                        contentColor = StreakFlame
                    ) {
                        Text(
                            text = "${book.totalArticles}篇",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            },
            supportingContent = {
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
