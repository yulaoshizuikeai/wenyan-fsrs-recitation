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
import androidx.compose.ui.text.style.TextOverflow
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
                .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
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
                                .background(StudyBlueLight, RoundedCornerShape(10.dp)),
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = {
                        hapticManager.tapLight()
                        onDismiss()
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Current Selection Indicator
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 10.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = BgSurfaceMuted
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "当前范围",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StudyBlueAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "背诵统计精准匹配",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Scrollable List of Presets & Single Books
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Presets
                    Text(
                        text = "学习阶段推荐预设",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    // 1. 全部教材
                    PresetOptionItem(
                        title = "全部 11 册教材 (100篇)",
                        subtitle = "统编版高中必修、选择性必修及选修全量收录",
                        isSelected = selectedModules.isEmpty(),
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            selectedModules = emptySet()
                        }
                    )

                    // 2. 必修全套
                    PresetOptionItem(
                        title = "必修全套 (两册 · 36篇)",
                        subtitle = "高一学年 · 必修上、下两册课文与诵读名篇",
                        isSelected = selectedModules == BookPresets.SCOPE_REQUIRED_ALL.moduleIds,
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            selectedModules = BookPresets.SCOPE_REQUIRED_ALL.moduleIds
                        }
                    )

                    // 3. 选必全套
                    PresetOptionItem(
                        title = "选择性必修全套 (三册 · 35篇)",
                        subtitle = "高二学年 · 选必上、中、下全三册课文与诵读名篇",
                        isSelected = selectedModules == BookPresets.SCOPE_SELECTIVE_ALL.moduleIds,
                        onClick = {
                            hapticManager.tapLight()
                            soundManager.playClick()
                            selectedModules = BookPresets.SCOPE_SELECTIVE_ALL.moduleIds
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "分册教材自选 (支持组合多选)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderFocus)
                    ) {
                        Text("全选教材", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
                    }

                    Button(
                        onClick = {
                            hapticManager.successPulse()
                            soundManager.playCorrect()
                            val finalScope = if (selectedModules.isEmpty()) null else selectedModules
                            val finalName = when {
                                finalScope == null -> "全部 11 册教材"
                                finalScope == BookPresets.BOOK_BX_1.moduleIds -> BookPresets.BOOK_BX_1.name
                                finalScope == BookPresets.BOOK_BX_2.moduleIds -> BookPresets.BOOK_BX_2.name
                                finalScope == BookPresets.BOOK_XB_1.moduleIds -> BookPresets.BOOK_XB_1.name
                                finalScope == BookPresets.BOOK_XB_2.moduleIds -> BookPresets.BOOK_XB_2.name
                                finalScope == BookPresets.BOOK_XB_3.moduleIds -> BookPresets.BOOK_XB_3.name
                                finalScope == BookPresets.BOOK_XX_APPRECIATION.moduleIds -> BookPresets.BOOK_XX_APPRECIATION.name
                                finalScope == BookPresets.SCOPE_REQUIRED_ALL.moduleIds -> "必修全套"
                                finalScope == BookPresets.SCOPE_SELECTIVE_ALL.moduleIds -> "选必全套"
                                else -> "自选 (${BookPresets.ALL_SINGLE_BOOKS.count { it.moduleIds.all { m -> m in finalScope } }} 册)"
                            }
                            onConfirmSelection(finalScope, finalName)
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyNavy)
                    ) {
                        Text(
                            text = "确定选择",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) StudyBlueLight else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) StudyBlueAccent else BorderSubtle
            )
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) StudyBlueLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) StudyBlueAccent else BorderSubtle
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
                        uncheckedColor = BorderFocus
                    )
                )
            },
            headlineContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (isSelected) StudyBlueAccent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) StudyBlueAccent.copy(alpha = 0.12f) else StreakFlame.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${book.totalArticles}篇",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) StudyBlueAccent else StreakFlame,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            supportingContent = {
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
