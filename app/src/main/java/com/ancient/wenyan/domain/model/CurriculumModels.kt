package com.ancient.wenyan.domain.model

data class Module(
    val id: String,
    val name: String,
    val shortName: String,
    val category: String, // "REQUIRED", "SELECTIVE", "ELECTIVE"
    val isRecitationOnly: Boolean,
    val sortOrder: Int,
    val totalArticles: Int
)

data class Article(
    val id: String,
    val moduleId: String,
    val title: String,
    val subtitle: String? = null,
    val author: String,
    val dynasty: String,
    val genre: String, // "诗", "词", "散文", "文赋", "乐府", "曲", "辞赋"
    val isGaoKao72: Boolean,
    val recitationScope: String = "FULL_TEXT",
    val fullContent: String,
    val paragraphs: List<String> = emptyList(),
    val translation: String? = null,
    val annotations: List<String> = emptyList(),
    val sortOrder: Int,
    val totalSegments: Int = 0,
    val totalCards: Int = 0
)

data class Segment(
    val id: String,
    val articleId: String,
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val orderIndex: Int,
    val segmentType: String, // "COUPLET", "PROSE_SENTENCE", "SINGLE_LINE"
    val fullText: String,
    val upperClause: String? = null,
    val lowerClause: String? = null,
    val translation: String? = null,
    val isKeyQuote: Boolean = false,
    val situationalPrompt: String? = null,
    val clozeKeywords: List<String> = emptyList()
)

data class Flashcard(
    val id: String,
    val segmentId: String,
    val articleId: String,
    val cardType: String, // "UPPER_PROMPT_LOWER", "LOWER_PROMPT_UPPER", "SITUATIONAL_CLOZE", "FULL_SENTENCE_RECALL", "MULTI_CLOZE_VARIANT"
    val frontTitle: String,
    val frontPrompt: String,
    val frontHint: String? = null,
    val backAnswer: String,
    val backTranslation: String? = null,
    val backNotes: String? = null,
    val isPrimary: Boolean = true,
    val clozeIndex: Int = 1,
    val totalClozes: Int = 1,
    val fullVerseContext: String? = null,
    val maskedSegment: String? = null,
    val unitIndex: Int = 0,
    val totalUnits: Int = 1,
    val precedingClauseHint: String? = null
)

enum class RecitationOrderMode(val displayName: String, val description: String) {
    SEQUENTIAL("顺承篇章原序", "遵循诗文起承转合，按篇目聚类并在篇内严格从首句到尾句正序背诵（推荐）"),
    SRS_PRIORITY("紧迫度交错优先", "传统 SRS 顺序，按重温/学习/遗忘率紧迫度穿插排序"),
    RANDOM_SHUFFLE("完全随机乱序", "全库完全随机打乱抽背，适合考前极端自测")
}

data class ArticleProgress(
    val articleId: String,
    val totalCards: Int,
    val newCards: Int,
    val learningCards: Int,
    val reviewCards: Int,
    val masteryPercentage: Float,
    val lastStudiedTime: Long? = null
)

enum class StudyOrderPreference(val displayName: String, val description: String) {
    REVIEW_FIRST("先复习后新学", "优先完成到期复习以防遗忘堆积，再学习新卡片（Anki 经典推荐）"),
    NEW_FIRST("先新学后复习", "精力充沛时先接触新篇章，随后完成旧卡复习"),
    MIXED("混合顺承排布", "依照篇章原文结构自然混合穿插新旧句子")
}

data class StudyGoalsConfig(
    val dailyNewLimit: Int = 20,
    val dailyReviewLimit: Int = 100,
    val orderPreference: StudyOrderPreference = StudyOrderPreference.REVIEW_FIRST
)

data class TodayStudyProgress(
    val todayNewLearned: Int = 0,
    val targetNew: Int = 20,
    val todayReviewed: Int = 0,
    val targetReview: Int = 100
) {
    val isNewGoalReached: Boolean get() = if (targetNew <= 0) true else todayNewLearned >= targetNew
    val isReviewGoalReached: Boolean get() = if (targetReview <= 0) true else todayReviewed >= targetReview
    val newProgressPercentage: Float
        get() = if (targetNew <= 0) 1.0f else (todayNewLearned.toFloat() / targetNew.toFloat()).coerceIn(0f, 1f)
    val reviewProgressPercentage: Float
        get() = if (targetReview <= 0) 1.0f else (todayReviewed.toFloat() / targetReview.toFloat()).coerceIn(0f, 1f)
}

