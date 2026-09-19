package com.ancient.wenyan.e2e

import kotlin.math.*

/**
 * Clean Architecture Opaque Test Harness for Classical Chinese FSRS Recitation Android App.
 *
 * Provides contract models, FSRS-5 mathematical oracle, curriculum specification oracle,
 * cloze masking generator, and study session queue scheduler for requirement-driven E2E tests.
 */

// ============================================================================
// 1. Core Domain Contract Enums & Entities
// ============================================================================

enum class E2ERating(val value: Int, val label: String) {
    AGAIN(1, "重来"),
    HARD(2, "困难"),
    GOOD(3, "良好"),
    EASY(4, "简单");

    companion object {
        fun fromValue(value: Int): E2ERating =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid rating value: $value. Must be 1..4.")
    }
}

enum class E2ECardState(val value: Int, val label: String) {
    NEW(0, "未学"),
    LEARNING(1, "初学"),
    REVIEW(2, "复习"),
    RELEARNING(3, "重学");

    companion object {
        fun fromValue(value: Int): E2ECardState =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid state value: $value. Must be 0..3.")
    }
}

data class E2EModule(
    val id: String,
    val name: String,
    val shortName: String,
    val category: String, // "REQUIRED", "SELECTIVE", "ELECTIVE"
    val isRecitationOnly: Boolean,
    val sortOrder: Int,
    val totalArticles: Int
)

data class E2EArticle(
    val id: String,
    val moduleId: String,
    val title: String,
    val subtitle: String? = null,
    val author: String,
    val dynasty: String,
    val genre: String, // "诗", "词", "散文", "文赋", "乐府", "曲", "辞赋"
    val isGaoKao72: Boolean,
    val recitationScope: String = "FULL_TEXT", // "FULL_TEXT", "EXCERPT", "KEY_PARAS"
    val fullContent: String,
    val paragraphs: List<String> = emptyList(),
    val translation: String? = null,
    val annotations: List<String> = emptyList(),
    val sortOrder: Int,
    val totalSegments: Int = 0,
    val totalCards: Int = 0
)

data class E2ESegment(
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
    val clozeKeywords: List<String> = emptyList(),
    val progressiveLevel1: String = "",
    val progressiveLevel2: String = "",
    val progressiveLevel3: String = ""
)

data class E2EFlashcard(
    val id: String,
    val segmentId: String,
    val articleId: String,
    val cardType: String, // "UPPER_PROMPT_LOWER", "LOWER_PROMPT_UPPER", "SITUATIONAL_CLOZE", "FULL_SENTENCE_RECALL"
    val frontTitle: String,
    val frontPrompt: String,
    val frontHint: String? = null,
    val backAnswer: String,
    val backTranslation: String? = null,
    val backNotes: String? = null,
    val isPrimary: Boolean = true
)

data class E2ECardFsrsState(
    val cardId: String,
    val state: E2ECardState = E2ECardState.NEW,
    val step: Int? = null,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewTime: Long? = null,
    val dueTime: Long = 0L
)

data class E2EReviewLog(
    val id: Long = 0,
    val cardId: String,
    val rating: E2ERating,
    val previousState: E2ECardState,
    val currentState: E2ECardState,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int,
    val scheduledDays: Int,
    val reviewTime: Long,
    val durationMs: Long = 0L
)

data class E2EArticleProgress(
    val articleId: String,
    val totalCards: Int,
    val newCards: Int,
    val learningCards: Int,
    val reviewCards: Int,
    val masteryPercentage: Float,
    val lastStudiedTime: Long? = null
)

// ============================================================================
// 2. Authoritative FSRS-5 Mathematical & Scheduling Oracle
// ============================================================================

class E2EFsrsOracle(
    val weights: DoubleArray = DEFAULT_FSRS_5_WEIGHTS,
    val requestRetention: Double = 0.90,
    val maximumInterval: Int = 36500,
    val learningSteps: List<Long> = listOf(60_000L, 600_000L), // 1m, 10m
    val relearningSteps: List<Long> = listOf(600_000L)          // 10m
) {
    companion object {
        val DEFAULT_FSRS_5_WEIGHTS = doubleArrayOf(
            0.40255,  // w0:  S0(Again)
            1.18385,  // w1:  S0(Hard)
            3.17300,  // w2:  S0(Good)
            15.69105, // w3:  S0(Easy)
            7.19490,  // w4:  D0(Again) base difficulty
            0.53450,  // w5:  D0 step exponent
            1.46040,  // w6:  Difficulty delta factor
            0.00460,  // w7:  Mean reversion rate
            1.54575,  // w8:  Recall S base factor
            0.11920,  // w9:  Recall S stability power damping
            1.01925,  // w10: Recall S retrievability exp factor
            1.93950,  // w11: Forget S long-term base factor
            0.11000,  // w12: Forget S difficulty power damping
            0.29605,  // w13: Forget S stability power factor
            2.26980,  // w14: Forget S retrievability exp factor
            0.23150,  // w15: Hard penalty factor
            2.98980,  // w16: Easy bonus factor
            0.51655,  // w17: Short-term S multiplier factor
            0.66210   // w18: Short-term S grade offset
        )
    }

    private val decay: Double = -0.5
    private val factor: Double = 0.9.pow(1.0 / decay) - 1.0 // 19.0 / 81.0 ~ 0.2345679

    fun retrievability(elapsedDays: Int, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        val t = max(0, elapsedDays).toDouble()
        return (1.0 + factor * (t / stability)).pow(decay)
    }

    fun nextInterval(stability: Double, retention: Double = requestRetention): Int {
        if (stability <= 0.0) return 1
        val interval = (stability / factor) * (retention.pow(1.0 / decay) - 1.0)
        return min(max(round(interval).toInt(), 1), maximumInterval)
    }

    fun initialStability(rating: E2ERating): Double {
        return max(weights[rating.value - 1], 0.001)
    }

    fun initialDifficulty(rating: E2ERating): Double {
        val d = weights[4] - exp(weights[5] * (rating.value - 1.0)) + 1.0
        return d.coerceIn(1.0, 10.0)
    }

    fun nextDifficulty(currentD: Double, rating: E2ERating): Double {
        val deltaD = -weights[6] * (rating.value - 3.0)
        val dPrime = currentD + deltaD * (10.0 - currentD) / 9.0
        val dInitEasy = weights[4] - exp(weights[5] * 3.0) + 1.0
        val dDoublePrime = weights[7] * dInitEasy + (1.0 - weights[7]) * dPrime
        return dDoublePrime.coerceIn(1.0, 10.0)
    }

    fun nextRecallStability(d: Double, s: Double, r: Double, rating: E2ERating): Double {
        val hardPenalty = if (rating == E2ERating.HARD) weights[15] else 1.0
        val easyBonus = if (rating == E2ERating.EASY) weights[16] else 1.0
        val sInc = 1.0 + exp(weights[8]) * (11.0 - d) * s.pow(-weights[9]) *
                (exp((1.0 - r) * weights[10]) - 1.0) * hardPenalty * easyBonus
        return max(s * sInc, 0.001)
    }

    fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        val sLong = weights[11] * d.pow(-weights[12]) * ((s + 1.0).pow(weights[13]) - 1.0) * exp((1.0 - r) * weights[14])
        val sShort = s / exp(weights[17] * weights[18])
        return max(min(sLong, sShort), 0.001)
    }

    fun shortTermStability(s: Double, rating: E2ERating): Double {
        var sInc = exp(weights[17] * (rating.value - 3.0 + weights[18]))
        if (rating.value >= 2) {
            sInc = max(sInc, 1.0)
        }
        return max(s * sInc, 0.001)
    }

    fun formatDurationBadge(millis: Long): String {
        val minutes = millis / 60_000L
        return if (minutes < 60) "${max(1, minutes)}分" else "${minutes / 60}小时"
    }

    fun formatIntervalBadge(days: Int): String {
        return when {
            days < 1 -> "1天"
            days < 30 -> "${days}天"
            days < 365 -> "${round(days / 30.0).toInt()}个月"
            else -> "${round((days / 365.0) * 10.0) / 10.0}年"
        }
    }

    fun previewIntervals(card: E2ECardFsrsState, nowMillis: Long = System.currentTimeMillis()): Map<E2ERating, String> {
        return E2ERating.entries.associateWith { rating ->
            val (updatedCard, _) = evaluateReview(card, rating, nowMillis)
            when (updatedCard.state) {
                E2ECardState.LEARNING, E2ECardState.RELEARNING -> {
                    val stepDelay = when (rating) {
                        E2ERating.AGAIN -> learningSteps.firstOrNull() ?: 60_000L
                        E2ERating.HARD -> ((learningSteps.firstOrNull() ?: 60_000L) * 1.5).toLong()
                        else -> learningSteps.getOrNull(1) ?: 600_000L
                    }
                    formatDurationBadge(stepDelay)
                }
                E2ECardState.REVIEW -> {
                    formatIntervalBadge(updatedCard.scheduledDays)
                }
                E2ECardState.NEW -> "1天"
            }
        }
    }

    fun evaluateReview(
        card: E2ECardFsrsState,
        rating: E2ERating,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<E2ECardFsrsState, E2EReviewLog> {
        val elapsedDays = if (card.lastReviewTime == null) 0 else max(0, ((nowMillis - card.lastReviewTime) / 86_400_000L).toInt())
        val isSameDay = card.lastReviewTime != null && elapsedDays < 1

        val newStability: Double
        val newDifficulty: Double

        when (card.state) {
            E2ECardState.NEW -> {
                newStability = initialStability(rating)
                newDifficulty = initialDifficulty(rating)
            }
            E2ECardState.LEARNING, E2ECardState.RELEARNING -> {
                if (isSameDay) {
                    newStability = shortTermStability(card.stability, rating)
                    newDifficulty = nextDifficulty(card.difficulty, rating)
                } else {
                    val r = retrievability(elapsedDays, card.stability)
                    newStability = if (rating == E2ERating.AGAIN) {
                        nextForgetStability(card.difficulty, card.stability, r)
                    } else {
                        nextRecallStability(card.difficulty, card.stability, r, rating)
                    }
                    newDifficulty = nextDifficulty(card.difficulty, rating)
                }
            }
            E2ECardState.REVIEW -> {
                if (isSameDay) {
                    newStability = shortTermStability(card.stability, rating)
                } else {
                    val r = retrievability(elapsedDays, card.stability)
                    newStability = if (rating == E2ERating.AGAIN) {
                        nextForgetStability(card.difficulty, card.stability, r)
                    } else {
                        nextRecallStability(card.difficulty, card.stability, r, rating)
                    }
                }
                newDifficulty = nextDifficulty(card.difficulty, rating)
            }
        }

        var nextState = card.state
        var nextStep = card.step
        var scheduledDays = 0
        var dueMillis = nowMillis
        var lapses = card.lapses

        when (card.state) {
            E2ECardState.NEW -> {
                when (rating) {
                    E2ERating.AGAIN -> {
                        nextState = E2ECardState.LEARNING
                        nextStep = 0
                        dueMillis = nowMillis + (learningSteps.getOrNull(0) ?: 60_000L)
                    }
                    E2ERating.HARD -> {
                        nextState = E2ECardState.LEARNING
                        nextStep = 0
                        dueMillis = nowMillis + ((learningSteps.getOrNull(0) ?: 60_000L) * 1.5).toLong()
                    }
                    E2ERating.GOOD -> {
                        if (learningSteps.size <= 1) {
                            nextState = E2ECardState.REVIEW
                            nextStep = null
                            scheduledDays = nextInterval(newStability)
                            dueMillis = nowMillis + scheduledDays * 86_400_000L
                        } else {
                            nextState = E2ECardState.LEARNING
                            nextStep = 1
                            dueMillis = nowMillis + (learningSteps.getOrNull(1) ?: 600_000L)
                        }
                    }
                    E2ERating.EASY -> {
                        nextState = E2ECardState.REVIEW
                        nextStep = null
                        scheduledDays = nextInterval(newStability)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                }
            }
            E2ECardState.LEARNING -> {
                val currentStep = card.step ?: 0
                when (rating) {
                    E2ERating.AGAIN -> {
                        nextStep = 0
                        dueMillis = nowMillis + (learningSteps.getOrNull(0) ?: 60_000L)
                    }
                    E2ERating.HARD -> {
                        dueMillis = nowMillis + (learningSteps.getOrNull(currentStep) ?: 600_000L)
                    }
                    E2ERating.GOOD -> {
                        if (currentStep + 1 >= learningSteps.size) {
                            nextState = E2ECardState.REVIEW
                            nextStep = null
                            scheduledDays = nextInterval(newStability)
                            dueMillis = nowMillis + scheduledDays * 86_400_000L
                        } else {
                            nextStep = currentStep + 1
                            dueMillis = nowMillis + (learningSteps.getOrNull(nextStep) ?: 600_000L)
                        }
                    }
                    E2ERating.EASY -> {
                        nextState = E2ECardState.REVIEW
                        nextStep = null
                        scheduledDays = nextInterval(newStability)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                }
            }
            E2ECardState.REVIEW -> {
                when (rating) {
                    E2ERating.AGAIN -> {
                        nextState = E2ECardState.RELEARNING
                        nextStep = 0
                        lapses += 1
                        dueMillis = nowMillis + (relearningSteps.getOrNull(0) ?: 600_000L)
                    }
                    E2ERating.HARD -> {
                        scheduledDays = min(nextInterval(newStability), nextInterval(newStability))
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                    E2ERating.GOOD -> {
                        scheduledDays = nextInterval(newStability)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                    E2ERating.EASY -> {
                        val goodIvl = nextInterval(newStability)
                        scheduledDays = min(max(nextInterval(newStability), goodIvl + 1), maximumInterval)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                }
            }
            E2ECardState.RELEARNING -> {
                val currentStep = card.step ?: 0
                when (rating) {
                    E2ERating.AGAIN -> {
                        nextStep = 0
                        dueMillis = nowMillis + (relearningSteps.getOrNull(0) ?: 600_000L)
                    }
                    E2ERating.HARD -> {
                        dueMillis = nowMillis + (relearningSteps.getOrNull(currentStep) ?: 600_000L)
                    }
                    E2ERating.GOOD, E2ERating.EASY -> {
                        nextState = E2ECardState.REVIEW
                        nextStep = null
                        scheduledDays = nextInterval(newStability)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                }
            }
        }

        val updatedCard = card.copy(
            state = nextState,
            step = nextStep,
            stability = newStability,
            difficulty = newDifficulty,
            elapsedDays = elapsedDays,
            scheduledDays = scheduledDays,
            reps = card.reps + 1,
            lapses = lapses,
            lastReviewTime = nowMillis,
            dueTime = dueMillis
        )

        val log = E2EReviewLog(
            cardId = card.cardId,
            rating = rating,
            previousState = card.state,
            currentState = nextState,
            stability = newStability,
            difficulty = newDifficulty,
            elapsedDays = elapsedDays,
            scheduledDays = scheduledDays,
            reviewTime = nowMillis
        )

        return Pair(updatedCard, log)
    }
}

// ============================================================================
// 3. Curriculum Specification Oracle (11 Modules, 100 Articles, Gao Kao 72)
// ============================================================================

object E2ECurriculumOracle {

    val MODULES: List<E2EModule> = listOf(
        E2EModule("MODULE_BX_1", "必修上册", "必修上", "REQUIRED", false, 1, 15),
        E2EModule("MODULE_BX_1_RECITE", "必修上册(古诗词诵读)", "必修上诵读", "REQUIRED", true, 2, 4),
        E2EModule("MODULE_BX_2", "必修下册", "必修下", "REQUIRED", false, 3, 13),
        E2EModule("MODULE_BX_2_RECITE", "必修下册(古诗词诵读)", "必修下诵读", "REQUIRED", true, 4, 4),
        E2EModule("MODULE_XB_1", "选择性必修上册", "选必上", "SELECTIVE", false, 5, 6),
        E2EModule("MODULE_XB_1_RECITE", "选择性必修上册(古诗词诵读)", "选必上诵读", "SELECTIVE", true, 6, 4),
        E2EModule("MODULE_XB_2", "选择性必修中册", "选必中", "SELECTIVE", false, 7, 4),
        E2EModule("MODULE_XB_2_RECITE", "选择性必修中册(古诗词诵读)", "选必中诵读", "SELECTIVE", true, 8, 4),
        E2EModule("MODULE_XB_3", "选择性必修下册", "选必下", "SELECTIVE", false, 9, 13),
        E2EModule("MODULE_XB_3_RECITE", "选择性必修下册(古诗词诵读)", "选必下诵读", "SELECTIVE", true, 10, 4),
        E2EModule("MODULE_XX_APPRECIATION", "选修(中国古代诗歌散文欣赏)", "选修欣赏", "ELECTIVE", false, 11, 29)
    )

    val MODULE_MAP: Map<String, E2EModule> = MODULES.associateBy { it.id }

    val SAMPLE_ARTICLES: List<E2EArticle> = listOf(
        E2EArticle("art_bx1_01", "MODULE_BX_1", "沁园春·长沙", null, "毛泽东", "近现代", "词", false, "FULL_TEXT", "独立寒秋，湘江北去，橘子洲头。", listOf("独立寒秋，湘江北去，橘子洲头。"), sortOrder = 1, totalSegments = 4, totalCards = 4),
        E2EArticle("art_bx1_02", "MODULE_BX_1", "芣苢", null, "佚名", "先秦", "诗", true, "FULL_TEXT", "采采芣苢，薄言采之。采采芣苢，薄言有之。", listOf("采采芣苢，薄言采之。"), sortOrder = 2, totalSegments = 3, totalCards = 3),
        E2EArticle("art_bx1_03", "MODULE_BX_1", "插秧歌", null, "杨万里", "宋代", "诗", true, "FULL_TEXT", "田夫抛秧田妇接，小儿背秧大儿插。", listOf("田夫抛秧田妇接，小儿背秧大儿插。"), sortOrder = 3, totalSegments = 2, totalCards = 2),
        E2EArticle("art_bx1_04", "MODULE_BX_1", "短歌行", null, "曹操", "三国·魏", "诗", true, "FULL_TEXT", "对酒当歌，人生几何！譬如朝露，去日苦多。", listOf("对酒当歌，人生几何！譬如朝露，去日苦多。"), sortOrder = 4, totalSegments = 4, totalCards = 4),
        E2EArticle("art_bx1_05", "MODULE_BX_1", "归园田居·其一", "其一", "陶渊明", "东晋", "诗", true, "FULL_TEXT", "少无适俗韵，性本爱丘山。", listOf("少无适俗韵，性本爱丘山。"), sortOrder = 5, totalSegments = 4, totalCards = 4),
        E2EArticle("art_bx1_06", "MODULE_BX_1", "梦游天姥吟留别", null, "李白", "唐代", "乐府", true, "FULL_TEXT", "海客谈瀛洲，烟涛微茫信难求；越人语天姥，云霞明灭或可睹。", listOf("海客谈瀛洲，烟涛微茫信难求。"), sortOrder = 6, totalSegments = 8, totalCards = 8),
        E2EArticle("art_bx1_07", "MODULE_BX_1", "登高", null, "杜甫", "唐代", "诗", true, "FULL_TEXT", "风急天高猿啸哀，渚清沙白鸟飞回。无边落木萧萧下，不尽长江滚滚来。", listOf("风急天高猿啸哀，渚清沙白鸟飞回。"), sortOrder = 7, totalSegments = 4, totalCards = 4),
        E2EArticle("art_bx1_08", "MODULE_BX_1", "琵琶行并序", "并序", "白居易", "唐代", "乐府", true, "FULL_TEXT", "浔阳江头夜送客，枫叶荻花秋瑟瑟。", listOf("浔阳江头夜送客，枫叶荻花秋瑟瑟。"), sortOrder = 8, totalSegments = 12, totalCards = 12),
        E2EArticle("art_bx1_09", "MODULE_BX_1", "念奴娇·赤壁怀古", null, "苏轼", "宋代", "词", true, "FULL_TEXT", "大江东去，浪淘尽，千古风流人物。", listOf("大江东去，浪淘尽，千古风流人物。"), sortOrder = 9, totalSegments = 6, totalCards = 6),
        E2EArticle("art_bx1_10", "MODULE_BX_1", "永遇乐·京口北固亭怀古", null, "辛弃疾", "宋代", "词", true, "FULL_TEXT", "千古江山，英雄无觅孙仲谋处。", listOf("千古江山，英雄无觅孙仲谋处。"), sortOrder = 10, totalSegments = 6, totalCards = 6),
        E2EArticle("art_bx1_11", "MODULE_BX_1", "声声慢·寻寻觅觅", null, "李清照", "宋代", "词", true, "FULL_TEXT", "寻寻觅觅，冷冷清清，凄凄惨惨戚戚。", listOf("寻寻觅觅，冷冷清清，凄凄惨惨戚戚。"), sortOrder = 11, totalSegments = 5, totalCards = 5),
        E2EArticle("art_bx1_12", "MODULE_BX_1", "劝学", null, "荀子", "先秦", "散文", true, "FULL_TEXT", "君子曰：学不可以已。青，取之于蓝，而青于蓝；冰，水为之，而寒于水。", listOf("君子曰：学不可以已。"), sortOrder = 12, totalSegments = 10, totalCards = 10),
        E2EArticle("art_bx1_13", "MODULE_BX_1", "师说", null, "韩愈", "唐代", "散文", true, "FULL_TEXT", "古之学者必有师。师者，所以传道受业解惑也。", listOf("古之学者必有师。"), sortOrder = 13, totalSegments = 10, totalCards = 10),
        E2EArticle("art_bx1_14", "MODULE_BX_1", "赤壁赋", null, "苏轼", "宋代", "文赋", true, "FULL_TEXT", "壬戌之秋，七月既望，苏子与客泛舟游于赤壁之下。清风徐来，水波不兴。", listOf("壬戌之秋，七月既望，苏子与客泛舟游于赤壁之下。"), sortOrder = 14, totalSegments = 15, totalCards = 15),
        E2EArticle("art_bx1_15", "MODULE_BX_1", "登泰山记", null, "姚鼐", "清代", "散文", true, "FULL_TEXT", "泰山之阳，汶水西流；其阴，济水东流。", listOf("泰山之阳，汶水西流；其阴，济水东流。"), sortOrder = 15, totalSegments = 8, totalCards = 8),
        // Long prose representations:
        E2EArticle("art_xb3_03", "MODULE_XB_3", "孔雀东南飞并序", "并序", "佚名", "汉代", "乐府", true, "FULL_TEXT", "孔雀东南飞，五里一徘徊。十三能织素，十四学裁衣，十五弹箜篌，十六诵诗书。", listOf("孔雀东南飞，五里一徘徊。"), sortOrder = 3, totalSegments = 100, totalCards = 100),
        E2EArticle("art_xb3_02", "MODULE_XB_3", "离骚(节选)", "节选", "屈原", "战国·楚", "辞赋", true, "FULL_TEXT", "帝高阳之苗裔兮，朕皇考曰伯庸。摄提贞于孟陬兮，惟庚寅吾以降。", listOf("帝高阳之苗裔兮，朕皇考曰伯庸。"), sortOrder = 2, totalSegments = 40, totalCards = 40)
    )

    fun getArticlesCountForModule(moduleId: String): Int {
        return MODULE_MAP[moduleId]?.totalArticles ?: 0
    }

    fun getTotalCatalogArticleCount(): Int = MODULES.sumOf { it.totalArticles }

    fun getTotalGaoKao72Count(): Int = 72
}

// ============================================================================
// 4. Progressive Cloze Masking Oracle
// ============================================================================

object E2EClozeOracle {

    fun generateLevel0(text: String): String = text

    fun generateLevel1(text: String, keywords: List<String>): String {
        var masked = text
        for (kw in keywords) {
            if (kw.isNotBlank()) {
                masked = masked.replace(kw, "⟦ ${"_".repeat(kw.length)} ⟧")
            }
        }
        return masked
    }

    fun generateLevel2(text: String): String {
        // Half-line masking: keep first half of clauses, mask second half
        val clauses = text.split("，", "。", "；", "！", "？").filter { it.isNotBlank() }
        var result = text
        for (clause in clauses) {
            if (clause.length >= 4) {
                val half = clause.length / 2
                val toMask = clause.substring(half)
                result = result.replace(toMask, "⟦ ${"_".repeat(toMask.length)} ⟧")
            }
        }
        return result
    }

    fun generateLevel3(text: String): String {
        // Skeleton prompt: keep only the first character of each clause and punctuation
        val sb = StringBuilder()
        var atStartOfClause = true
        for (ch in text) {
            when {
                ch in listOf('，', '。', '；', '！', '？', '：', '“', '”', '、') -> {
                    sb.append(ch)
                    atStartOfClause = true
                }
                atStartOfClause -> {
                    sb.append(ch)
                    atStartOfClause = false
                }
                else -> {
                    sb.append('_')
                }
            }
        }
        return sb.toString()
    }

    fun generateLevel4(text: String): String {
        // Full blind masking: replace all characters except punctuation with underscores
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in listOf('，', '。', '；', '！', '？', '：', '“', '”', '、', '\n')) {
                sb.append(ch)
            } else {
                sb.append('_')
            }
        }
        return sb.toString()
    }
}

// ============================================================================
// 5. Study Session Simulator (Queue Scheduling & Interactions)
// ============================================================================

class E2EStudySessionSimulator(
    val fsrsOracle: E2EFsrsOracle = E2EFsrsOracle(),
    val dailyNewCardLimit: Int = 20
) {
    val cards: MutableMap<String, E2ECardFsrsState> = mutableMapOf()
    val reviewLogs: MutableList<E2EReviewLog> = mutableListOf()

    fun loadCards(initialCards: List<E2ECardFsrsState>) {
        cards.clear()
        initialCards.forEach { cards[it.cardId] = it }
    }

    fun getDueQueue(nowMillis: Long, endOfDayMillis: Long = nowMillis + 86_400_000L): List<E2ECardFsrsState> {
        val relearningQueue = cards.values
            .filter { it.state == E2ECardState.RELEARNING && it.dueTime <= nowMillis }
            .sortedBy { it.dueTime }

        val learningQueue = cards.values
            .filter { it.state == E2ECardState.LEARNING && it.dueTime <= nowMillis }
            .sortedBy { it.dueTime }

        val reviewQueue = cards.values
            .filter { it.state == E2ECardState.REVIEW && it.dueTime <= endOfDayMillis }
            .sortedWith(compareByDescending<E2ECardFsrsState> { it.lapses }.thenBy { it.dueTime })

        val newCardsReviewedToday = reviewLogs.count {
            it.previousState == E2ECardState.NEW && it.reviewTime >= (nowMillis - 86_400_000L)
        }
        val remainingNewQuota = max(0, dailyNewCardLimit - newCardsReviewedToday)

        val newQueue = cards.values
            .filter { it.state == E2ECardState.NEW }
            .take(remainingNewQuota)

        return relearningQueue + learningQueue + reviewQueue + newQueue
    }

    fun submitRating(cardId: String, rating: E2ERating, nowMillis: Long = System.currentTimeMillis()): E2ECardFsrsState {
        val currentCard = cards[cardId] ?: throw NoSuchElementException("Card not found: $cardId")
        val (updatedCard, log) = fsrsOracle.evaluateReview(currentCard, rating, nowMillis)
        cards[cardId] = updatedCard
        reviewLogs.add(log)
        return updatedCard
    }

    fun calculateArticleProgress(articleId: String, totalCardsCount: Int): E2EArticleProgress {
        val articleCards = cards.values.filter { it.cardId.startsWith("card_$articleId") || it.cardId.contains(articleId) }
        val newCount = articleCards.count { it.state == E2ECardState.NEW }
        val learningCount = articleCards.count { it.state in listOf(E2ECardState.LEARNING, E2ECardState.RELEARNING) }
        val reviewCount = articleCards.count { it.state == E2ECardState.REVIEW }
        val total = if (articleCards.isNotEmpty()) articleCards.size else totalCardsCount

        val mastery = if (total > 0) (reviewCount.toFloat() / total.toFloat()) * 100.0f else 0.0f
        return E2EArticleProgress(
            articleId = articleId,
            totalCards = total,
            newCards = newCount,
            learningCards = learningCount,
            reviewCards = reviewCount,
            masteryPercentage = mastery,
            lastStudiedTime = reviewLogs.filter { it.cardId.contains(articleId) }.maxOfOrNull { it.reviewTime }
        )
    }
}
