package com.ancient.wenyan.domain.gaokao

/**
 * GaoKao Contextual Understanding Dictation (高考情境化理解性默写) Data Model.
 */
data class GaoKaoScenarioQuestion(
    val id: String,
    val articleId: String,
    val articleTitle: String,
    val author: String,
    val prompt: String,                  // 考题题干情境提示 (例如：“诸葛亮在《出师表》中劝诫后主不应妄自菲薄的句子是：”)
    val answer: String,                  // 对应原文标准答案 (例如：“不宜妄自菲薄，引喻失义”)
    val keyPoints: List<String>,         // 易错字、通假字及核心采分点 (例如：["妄自菲薄", "引喻失义", "注意‘义’不要误写作‘议’"])
    val explanation: String = "",        // 语境阐释与名句赏析
    val isGaoKaoRealExam: Boolean = true // 是否为历年高考真题/名校模拟真题
)
