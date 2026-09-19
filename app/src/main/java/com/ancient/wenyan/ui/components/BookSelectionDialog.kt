package com.ancient.wenyan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ancient.wenyan.domain.model.BookGroup
import com.ancient.wenyan.domain.model.BookPresets
import com.ancient.wenyan.ui.theme.*

@Composable
fun BookSelectionDialog(
    currentScope: Set<String>?,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirmSelection: (Set<String>?, String) -> Unit
) {
    // Local state for selected modules. Empty set or null means all
    var selectedModules by remember { mutableStateOf(currentScope ?: emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, XuanBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = XuanPaperCard),
            elevation = CardDefaults.cardElevation(6.dp)
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
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = BambooGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "选择背诵教材",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkCharcoal
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = InkMedium
                        )
                    }
                }

                Text(
                    text = "当前选择：$currentName。针对当前学习阶段锁定课本，背诵与统计将精准匹配所选图书范围",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium,
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
                        fontFamily = FontFamily.Serif,
                        color = InkMedium
                    )

                    // 1. 全部教材
                    PresetOptionItem(
                        title = "全部 11 册教材 (100篇)",
                        subtitle = "高中课内全部篇目全库覆盖",
                        isSelected = selectedModules.isEmpty(),
                        onClick = { selectedModules = emptySet() }
                    )

                    // 2. 必修全套
                    PresetOptionItem(
                        title = "必修全套 (上/下两册 · 36篇)",
                        subtitle = "覆盖必修上册、必修下册及诵读",
                        isSelected = selectedModules == BookPresets.SCOPE_REQUIRED_ALL.moduleIds,
                        onClick = { selectedModules = BookPresets.SCOPE_REQUIRED_ALL.moduleIds }
                    )

                    // 3. 选必全套
                    PresetOptionItem(
                        title = "选择性必修全套 (上/中/下 · 35篇)",
                        subtitle = "覆盖选必三册课文及古诗词诵读",
                        isSelected = selectedModules == BookPresets.SCOPE_SELECTIVE_ALL.moduleIds,
                        onClick = { selectedModules = BookPresets.SCOPE_SELECTIVE_ALL.moduleIds }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "单册教材精准选择 (支持勾选多本)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkMedium
                    )

                    // Individual Books
                    BookPresets.ALL_SINGLE_BOOKS.forEach { book ->
                        val isFullySelected = book.moduleIds.all { it in selectedModules }
                        BookCheckboxItem(
                            book = book,
                            isSelected = isFullySelected,
                            onToggle = {
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
                            selectedModules = emptySet()
                            onConfirmSelection(null, "全部 11 册教材")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkMedium)
                    ) {
                        Text("全选课本", fontFamily = FontFamily.Serif)
                    }

                    Button(
                        onClick = {
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
                        colors = ButtonDefaults.buttonColors(containerColor = BambooGreen)
                    ) {
                        Text(
                            text = "确定选择",
                            fontFamily = FontFamily.Serif,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) BambooGreen else XuanBorder,
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEFF5F0) else XuanPaperLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    color = if (isSelected) BambooGreen else InkCharcoal
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "选中",
                    tint = BambooGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BookCheckboxItem(
    book: BookGroup,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) BambooGreen else XuanBorder,
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEFF5F0) else XuanPaperLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = BambooGreen,
                    uncheckedColor = InkMedium
                ),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = book.name,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Serif,
                        color = if (isSelected) BambooGreen else InkCharcoal
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0x159E2A2B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${book.totalArticles}篇",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            color = CinnabarRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = book.description,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
