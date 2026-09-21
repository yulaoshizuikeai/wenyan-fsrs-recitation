package com.ancient.wenyan.data

import com.ancient.wenyan.domain.gaokao.GaoKaoScenarioQuestion

/**
 * Curated repository of GaoKao contextual understanding recitation questions (历年高考情境化理解性默写题库).
 */
object GaoKaoScenarioDataSource {

    val QUESTIONS: List<GaoKaoScenarioQuestion> = listOf(
        GaoKaoScenarioQuestion(
            id = "gk_duangexing_01",
            articleId = "art_bx1_04",
            articleTitle = "短歌行",
            author = "曹操",
            prompt = "曹操在《短歌行》中运用比喻，感叹人生短暂犹如清晨朝露，过去的日子已经很多的诗句是：",
            answer = "对酒当歌，人生几何！譬如朝露，去日苦多。",
            keyPoints = listOf("譬如朝露", "去日苦多", "对酒当歌"),
            explanation = "本题考查曹操对生命短暂之忧思与慷慨求贤之急迫，为历年高考高频考点。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_shuxiang_01",
            articleId = "art_xb3_05",
            articleTitle = "蜀相",
            author = "杜甫",
            prompt = "杜甫在《蜀相》中高度概括诸葛亮一生辅佐两朝开济功业，却因出师未捷身先死而令后世英雄泪满衣襟的千古名句是：",
            answer = "出师未捷身先死，长使英雄泪满襟。",
            keyPoints = listOf("出师未捷", "长使", "泪满襟"),
            explanation = "赞颂诸葛亮鞠躬尽瘁死而后已的崇高品格，千古同悲。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_shudaonan_01",
            articleId = "art_xb3_04",
            articleTitle = "蜀道难",
            author = "李白",
            prompt = "李白在《蜀道难》中引用五丁开山的神话传说，描写秦蜀开辟通道历尽千难万险、付出巨大牺牲的诗句是：",
            answer = "地崩山摧壮士死，然后天梯石栈相钩连。",
            keyPoints = listOf("地崩山摧", "天梯石栈", "相钩连"),
            explanation = "极富浪漫主义神话色彩，为高考古代诗歌默写经典考题。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_chibifu_01",
            articleId = "art_bx1_14",
            articleTitle = "赤壁赋",
            author = "苏轼",
            prompt = "苏轼在《赤壁赋》中以精妙比喻，感叹个体生命短暂渺小如沧海一粟、蜉蝣寄生的句子是：",
            answer = "寄蜉蝣于天地，渺沧海之一粟。",
            keyPoints = listOf("蜉蝣 (注意虫字旁与浮沉之别)", "沧海之一粟 (‘粟’勿误作‘栗’或‘票’)", "通假与字形考查重点"),
            explanation = "新高考全国卷极高频易错题，‘粟’与‘蜉蝣’为字形易混核心失分点。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_chibifu_02",
            articleId = "art_bx1_14",
            articleTitle = "赤壁赋",
            author = "苏轼",
            prompt = "《赤壁赋》中苏轼用白描手法描摹清风与明月，抒发造物者无尽宝藏、与朋友共享之惬意的句子是：",
            answer = "惟江上之清风，与山间之明月，耳得之而为声，目遇之而成色。",
            keyPoints = listOf("惟 (通‘唯’)", "耳得之而为声", "目遇之而成色"),
            explanation = "体悟苏轼超然旷达之宇宙哲思与审美意趣。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_shishuo_01",
            articleId = "art_bx1_13",
            articleTitle = "师说",
            author = "韩愈",
            prompt = "韩愈在《师说》中打破门第成见，指出择师的标准不在于贵贱长幼，而在于道理所在的句子是：",
            answer = "是故无贵无贱，无长无少，道之所存，师之所存也。",
            keyPoints = listOf("是故", "道之所存，师之所存也"),
            explanation = "韩愈关于从师之道的核心立论，破除魏晋以来的士族偏见。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_shishuo_02",
            articleId = "art_bx1_13",
            articleTitle = "师说",
            author = "韩愈",
            prompt = "韩愈在《师说》中阐释学生与老师辩证关系，提出弟子不必不如师的深刻见解的句子是：",
            answer = "是故弟子不必不如师，师不必贤于弟子，闻道有先后，术业有专攻，如是而已。",
            keyPoints = listOf("闻道有先后", "术业有专攻 (‘攻’勿写为‘功’)", "古今异义考查"),
            explanation = "经典教育哲学命题，历年高考理解性默写压轴常客。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_quanxue_01",
            articleId = "art_bx1_12",
            articleTitle = "劝学",
            author = "荀子",
            prompt = "荀子在《劝学》中强调君子并非天赋异禀，而是善于借助外力来成就自我的总结性名句是：",
            answer = "君子生非异也，善假于物也。",
            keyPoints = listOf("生 (通假字，通‘性’，天资禀赋)", "善假于物也 (‘假’为借助之意)"),
            explanation = "经典通假字考察，全国卷多次考查‘生’通‘性’的深层理解。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_quanxue_02",
            articleId = "art_bx1_12",
            articleTitle = "劝学",
            author = "荀子",
            prompt = "《劝学》中通过劣马虽慢但只要坚持不懈也能走完全程的比喻，论证坚持重要性的句子是：",
            answer = "驽马十驾，功在不舍。",
            keyPoints = listOf("驽马 (‘驽’注意偏旁)", "功在不舍"),
            explanation = "对比论证的典范，说明主观坚持能够克服客观条件不足。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_denggao_01",
            articleId = "art_bx1_07",
            articleTitle = "登高",
            author = "杜甫",
            prompt = "杜甫在《登高》中将长年漂泊的羁旅之悲与晚年抱病的孤苦集于一身，备受后人赞叹的对仗千古奇句是：",
            answer = "万里悲秋常作客，百年多病独登台。",
            keyPoints = listOf("万里 (空间之广)", "百年 (暮年迟暮)", "常作客", "独登台"),
            explanation = "明代胡应麟誉为‘古今独步’之律诗代表作。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_pipaxing_01",
            articleId = "art_bx1_08",
            articleTitle = "琵琶行并序",
            author = "白居易",
            prompt = "白居易在《琵琶行》中描写音乐戛然而止，余味悠长，胜过此时有声的绝妙诗句是：",
            answer = "别有幽愁暗恨生，此时无声胜有声。",
            keyPoints = listOf("幽愁暗恨 (‘幽’勿写为‘忧’)", "此时无声胜有声"),
            explanation = "中国古典音乐通感描摹的巅峰之作。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_pipaxing_02",
            articleId = "art_bx1_08",
            articleTitle = "琵琶行并序",
            author = "白居易",
            prompt = "《琵琶行》中描写乐曲达到最高潮时突然撕裂、气势骤止的两个比喻句是：",
            answer = "银瓶乍破水浆迸，铁骑突出刀枪鸣。",
            keyPoints = listOf("乍破", "水浆迸 (‘迸’字形考查)", "铁骑突出"),
            explanation = "音韵铿锵，极具视觉冲击力与听觉张力。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_epangfu_01",
            articleId = "art_bx2_12",
            articleTitle = "阿房宫赋",
            author = "杜牧",
            prompt = "杜牧在《阿房宫赋》末尾深刻警示后人，若只知哀叹而不引以为鉴，终将重蹈覆辙的句子是：",
            answer = "后人哀之而不鉴之，亦使后人而复哀后人也。",
            keyPoints = listOf("哀之而不鉴之 (‘鉴’为意动用法，以……为借鉴)", "复哀后人"),
            explanation = "全文画龙点睛之笔，借古讽今之核心旨归。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_lisao_01",
            articleId = "art_xb3_02",
            articleTitle = "离骚(节选)",
            author = "屈原",
            prompt = "屈原在《离骚》中表明即使遭受严刑粉身碎骨、也绝不改变高洁志向的千古名句是：",
            answer = "亦余心之所善兮，虽九死其犹未悔。",
            keyPoints = listOf("亦余心之所善兮", "虽九死其犹未悔 (‘犹’勿误作‘由’)"),
            explanation = "中华民族矢志报国、百折不挠之崇高精神象征。"
        )
    )
}
