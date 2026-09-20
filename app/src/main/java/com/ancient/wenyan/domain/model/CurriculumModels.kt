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
    val maskedSegment: String? = null
)

data class ArticleProgress(
    val articleId: String,
    val totalCards: Int,
    val newCards: Int,
    val learningCards: Int,
    val reviewCards: Int,
    val masteryPercentage: Float,
    val lastStudiedTime: Long? = null
)
