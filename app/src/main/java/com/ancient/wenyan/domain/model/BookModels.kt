package com.ancient.wenyan.domain.model

data class BookGroup(
    val id: String,
    val name: String,
    val shortName: String,
    val moduleIds: Set<String>,
    val totalArticles: Int,
    val description: String,
    val tag: String // e.g. "必修", "选必", "选修", "预设"
)

object BookPresets {
    // 6 个高中标准教材单册（包含正文及古诗词诵读）
    val BOOK_BX_1 = BookGroup(
        id = "BOOK_BX_1",
        name = "必修上册",
        shortName = "必修上",
        moduleIds = setOf("MODULE_BX_1", "MODULE_BX_1_RECITE"),
        totalArticles = 19,
        description = "高一上学期 · 经典诗词与山水名赋",
        tag = "必修"
    )

    val BOOK_BX_2 = BookGroup(
        id = "BOOK_BX_2",
        name = "必修下册",
        shortName = "必修下",
        moduleIds = setOf("MODULE_BX_2", "MODULE_BX_2_RECITE"),
        totalArticles = 17,
        description = "高一下学期 · 先秦诸子与史传名篇",
        tag = "必修"
    )

    val BOOK_XB_1 = BookGroup(
        id = "BOOK_XB_1",
        name = "选择性必修上册",
        shortName = "选必上",
        moduleIds = setOf("MODULE_XB_1", "MODULE_XB_1_RECITE"),
        totalArticles = 10,
        description = "高二上学期 · 诸子思想与经典论说",
        tag = "选必"
    )

    val BOOK_XB_2 = BookGroup(
        id = "BOOK_XB_2",
        name = "选择性必修中册",
        shortName = "选必中",
        moduleIds = setOf("MODULE_XB_2", "MODULE_XB_2_RECITE"),
        totalArticles = 8,
        description = "高二学年 · 史家绝唱与经世文赋",
        tag = "选必"
    )

    val BOOK_XB_3 = BookGroup(
        id = "BOOK_XB_3",
        name = "选择性必修下册",
        shortName = "选必下",
        moduleIds = setOf("MODULE_XB_3", "MODULE_XB_3_RECITE"),
        totalArticles = 17,
        description = "高二下学期 · 山水游记与抒怀名篇",
        tag = "选必"
    )

    val BOOK_XX_APPRECIATION = BookGroup(
        id = "BOOK_XX_APPRECIATION",
        name = "选修 · 古代诗歌散文欣赏",
        shortName = "选修欣赏",
        moduleIds = setOf("MODULE_XX_APPRECIATION"),
        totalArticles = 29,
        description = "高三拓展 · 历代名家经典诗文培优",
        tag = "选修"
    )

    // 所有 6 大课本列表
    val ALL_SINGLE_BOOKS: List<BookGroup> = listOf(
        BOOK_BX_1,
        BOOK_BX_2,
        BOOK_XB_1,
        BOOK_XB_2,
        BOOK_XB_3,
        BOOK_XX_APPRECIATION
    )

    // 快捷聚合选项
    val SCOPE_ALL = BookGroup(
        id = "SCOPE_ALL",
        name = "全部 11 册教材",
        shortName = "全部教材",
        moduleIds = emptySet(), // emptySet represents all
        totalArticles = 100,
        description = "高中统编教材全量收录 · 涵盖全部100篇诗文",
        tag = "全库"
    )

    val SCOPE_REQUIRED_ALL = BookGroup(
        id = "SCOPE_REQUIRED_ALL",
        name = "必修全套 (上/下)",
        shortName = "必修全套",
        moduleIds = setOf("MODULE_BX_1", "MODULE_BX_1_RECITE", "MODULE_BX_2", "MODULE_BX_2_RECITE"),
        totalArticles = 36,
        description = "高一必修两册课文及诵读 · 夯实文言背诵基石",
        tag = "必修"
    )

    val SCOPE_SELECTIVE_ALL = BookGroup(
        id = "SCOPE_SELECTIVE_ALL",
        name = "选择性必修全套 (三册)",
        shortName = "选必全套",
        moduleIds = setOf(
            "MODULE_XB_1", "MODULE_XB_1_RECITE",
            "MODULE_XB_2", "MODULE_XB_2_RECITE",
            "MODULE_XB_3", "MODULE_XB_3_RECITE"
        ),
        totalArticles = 35,
        description = "高二选择性必修三册 · 涵盖论说名篇与经典辞赋",
        tag = "选必"
    )
}

/**
 * 研墨打卡热力图统计数据
 */
data class HeatmapStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val activeDays: Int,
    val totalReviews: Int,
    val dailyReviewMap: Map<String, Int> = emptyMap() // "yyyy-MM-dd" -> count
)

data class HeatmapDay(
    val dateStr: String,
    val count: Int,
    val intensityLevel: Int, // 0 to 4
    val isToday: Boolean,
    val dayOfWeek: Int // 1 (Mon) to 7 (Sun)
)
