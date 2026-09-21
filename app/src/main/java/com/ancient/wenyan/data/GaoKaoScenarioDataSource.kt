package com.ancient.wenyan.data

import com.ancient.wenyan.domain.gaokao.GaoKaoScenarioQuestion

/**
 * 历年高考情境化理解性默写真题与高频名校模拟题库 (Curated GaoKao Scenario Question Bank).
 * 全量严格对齐普通高中统编教材核心必背篇目 (CurriculumDataSource)。
 */
object GaoKaoScenarioDataSource {

    val QUESTIONS: List<GaoKaoScenarioQuestion> = listOf(
        // ==============================================================
        // 1. 劝学 (荀子) - art_bx1_12
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_quanxue_01",
            articleId = "art_bx1_12",
            articleTitle = "劝学",
            author = "荀子",
            prompt = "荀子在《劝学》中强调君子并非天赋异禀，而是善于借助外力来成就自我的总结性名句是：",
            answer = "君子生非异也，善假于物也。",
            keyPoints = listOf("生 (通假字，通‘性’，天资)", "善假于物也 (‘假’借也)"),
            explanation = "全国卷超高频真题，‘生’通‘性’是历年字形与通假考查核心。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_quanxue_02",
            articleId = "art_bx1_12",
            articleTitle = "劝学",
            author = "荀子",
            prompt = "《劝学》中通过劣马虽走得慢，但只要坚持不懈也能走完全程的比喻，论证坚持重要性的句子是：",
            answer = "驽马十驾，功在不舍。",
            keyPoints = listOf("驽马 (‘驽’注意偏旁)", "功在不舍"),
            explanation = "对比论证的典范，说明主观坚持能弥补先天不足。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_quanxue_03",
            articleId = "art_bx1_12",
            articleTitle = "劝学",
            author = "荀子",
            prompt = "《劝学》中以蚯蚓为例，说明用心专一才能成功的名句是：",
            answer = "蚓无爪牙之利，筋骨之强，上食埃土，下饮黄泉，用心一也。",
            keyPoints = listOf("爪牙之利 (定语后置)", "埃土", "用心一也"),
            explanation = "高考经典定语后置句式与‘专一’治学主旨考题。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_quanxue_04",
            articleId = "art_bx1_12",
            articleTitle = "劝学",
            author = "荀子",
            prompt = "荀子在《劝学》中指出整天空想不如片刻学习收获巨大的句子是：",
            answer = "吾尝终日而思矣，不如须臾之所学也。",
            keyPoints = listOf("须臾之所学 (‘须臾’勿误写为‘须予’)", "终日而思"),
            explanation = "强调知行合一、亲身实践学习的必要性。"
        ),

        // ==============================================================
        // 2. 师说 (韩愈) - art_bx1_13
        // ==============================================================
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
            prompt = "韩愈在《师说》中阐释师生辩证关系，提出弟子不必不如师的深刻见解的句子是：",
            answer = "是故弟子不必不如师，师不必贤于弟子，闻道有先后，术业有专攻，如是而已。",
            keyPoints = listOf("闻道有先后", "术业有专攻 (‘攻’勿写为‘功’)", "如是而已"),
            explanation = "经典教育哲学命题，历年高考理解性默写压轴常客。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_shishuo_03",
            articleId = "art_bx1_13",
            articleTitle = "师说",
            author = "韩愈",
            prompt = "韩愈在《师说》开篇即开门见山提出全篇中心论点，阐述从师学习必要性的句子是：",
            answer = "古之学者必有师。师者，所以传道受业解惑也。",
            keyPoints = listOf("学者 (古今异义：求学的人)", "传道受业 (‘受’通‘授’)"),
            explanation = "中华尊师重道文化的基石名句，‘受’通‘授’为必考点。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_shishuo_04",
            articleId = "art_bx1_13",
            articleTitle = "师说",
            author = "韩愈",
            prompt = "《师说》中韩愈批评当时士大夫阶层‘耻学于师’，以官位和地位为借口拒不从师的丑态是：",
            answer = "位卑则足羞，官盛则近谀。",
            keyPoints = listOf("位卑则足羞", "官盛则近谀 (‘谀’为阿谀谄媚)"),
            explanation = "深刻针砭时弊，考查对古文讽刺笔法的领悟。"
        ),

        // ==============================================================
        // 3. 赤壁赋 (苏轼) - art_bx1_14
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_chibifu_01",
            articleId = "art_bx1_14",
            articleTitle = "赤壁赋",
            author = "苏轼",
            prompt = "苏轼在《赤壁赋》中以精妙比喻，感叹个体生命短暂渺小如沧海一粟、蜉蝣寄生的句子是：",
            answer = "寄蜉蝣于天地，渺沧海之一粟。",
            keyPoints = listOf("蜉蝣 (虫字旁注意别写成浮游)", "沧海之一粟 (‘粟’勿写为‘栗’或‘票’)"),
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
            id = "gk_chibifu_03",
            articleId = "art_bx1_14",
            articleTitle = "赤壁赋",
            author = "苏轼",
            prompt = "苏轼在《赤壁赋》中描写秋夜江面月光皎洁、雾气水天相连朦胧壮阔景象的句子是：",
            answer = "白露横江，水光接天。纵一苇之所如，凌万顷之茫然。",
            keyPoints = listOf("白露横江", "纵一苇之所如", "凌万顷之茫然"),
            explanation = "融情入景的千古名段，‘苇’与‘凌’常为考查字形。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_chibifu_04",
            articleId = "art_bx1_14",
            articleTitle = "赤壁赋",
            author = "苏轼",
            prompt = "《赤壁赋》中用夸张与侧面烘托手法，形容洞箫之声如怨如慕、令潜蛟起舞孤舟寡妇落泪的句子是：",
            answer = "舞幽壑之潜蛟，泣孤舟之嫠妇。",
            keyPoints = listOf("幽壑之潜蛟", "嫠妇 (‘嫠’字形极其易错，勿写为‘厘’或‘娌’)"),
            explanation = "高考极度偏爱的极易写错字‘嫠’，历年失分率居高不下。"
        ),

        // ==============================================================
        // 4. 阿房宫赋 (杜牧) - art_bx2_12
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_epangfu_01",
            articleId = "art_bx2_12",
            articleTitle = "阿房宫赋",
            author = "杜牧",
            prompt = "杜牧在《阿房宫赋》末尾深刻警示后人，若只知哀叹而不引以为鉴，终将重蹈覆辙的句子是：",
            answer = "后人哀之而不鉴之，亦使后人而复哀后人也。",
            keyPoints = listOf("哀之而不鉴之 (‘鉴’意动用法，以……为借鉴)", "复哀后人"),
            explanation = "全文画龙点睛之笔，借古讽今之核心旨归。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_epangfu_02",
            articleId = "art_bx2_12",
            articleTitle = "阿房宫赋",
            author = "杜牧",
            prompt = "《阿房宫赋》中描写阿房宫依附地势而建，宫殿连绵错落、走廊盘旋如水波曲折的建筑奇观是：",
            answer = "廊腰缦回，檐牙高啄；各抱地势，钩心斗角。",
            keyPoints = listOf("廊腰缦回 (‘缦’勿写为‘慢’)", "檐牙高啄", "钩心斗角 (古今异义)"),
            explanation = "‘钩心斗角’在文言文与现代汉语中的词义演变常考点。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_epangfu_03",
            articleId = "art_bx2_12",
            articleTitle = "阿房宫赋",
            author = "杜牧",
            prompt = "杜牧在《阿房宫赋》中总结秦朝灭亡的根本原因在于秦朝自身而非天命的句子是：",
            answer = "灭六国者六国也，非秦也；族秦者秦也，非天下也。",
            keyPoints = listOf("族秦者秦也 (‘族’名词作动词，使……灭族)", "非天下也"),
            explanation = "提纲挈领的史论名句，考查词类活用‘族’。"
        ),

        // ==============================================================
        // 5. 琵琶行并序 (白居易) - art_bx1_08
        // ==============================================================
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
            id = "gk_pipaxing_03",
            articleId = "art_bx1_08",
            articleTitle = "琵琶行并序",
            author = "白居易",
            prompt = "白居易在《琵琶行》中抒发自己与琵琶女同病相怜、天涯沦落的千古感慨句子是：",
            answer = "同是天涯沦落人，相逢何必曾相识！",
            keyPoints = listOf("天涯沦落人", "相逢何必曾相识"),
            explanation = "全诗主旨句，诗人与艺人命运交织的千古绝唱。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_pipaxing_04",
            articleId = "art_bx1_08",
            articleTitle = "琵琶行并序",
            author = "白居易",
            prompt = "《琵琶行》中生动描摹琵琶女在千呼万唤中出场时含羞带怯神态的传神诗句是：",
            answer = "千呼万唤始出来，犹抱琵琶半遮面。",
            keyPoints = listOf("千呼万唤始出来", "半遮面"),
            explanation = "极富生活气息的人物出场白描。"
        ),

        // ==============================================================
        // 6. 蜀道难 (李白) - art_xb3_04
        // ==============================================================
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
            id = "gk_shudaonan_02",
            articleId = "art_xb3_04",
            articleTitle = "蜀道难",
            author = "李白",
            prompt = "李白在《蜀道难》中以行人在高空抚胸喘息、伸手可摸星辰的夸张细节描写蜀道之高的诗句是：",
            answer = "扪参历井仰胁息，以手抚膺坐长叹。",
            keyPoints = listOf("扪参历井 (‘扪’摸也，‘参’‘井’皆为星宿名)", "抚膺 (‘膺’胸膛也，字形易混)"),
            explanation = "历年高考极易丢分字‘扪’与‘膺’，考查古典星宿天文与字形。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_shudaonan_03",
            articleId = "art_xb3_04",
            articleTitle = "蜀道难",
            author = "李白",
            prompt = "《蜀道难》中描写剑阁关隘地势极其险要，只要一人守关哪怕万人进犯也攻不破的兵家要地名句是：",
            answer = "剑阁峥嵘而崔嵬，一夫当关，万夫莫开。",
            keyPoints = listOf("峥嵘而崔嵬 (‘崔嵬’高峻也)", "一夫当关，万夫莫开"),
            explanation = "成语‘一夫当关，万夫莫开’之出处，常考地势之险。"
        ),

        // ==============================================================
        // 7. 登高 (杜甫) - art_bx1_07
        // ==============================================================
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
            id = "gk_denggao_02",
            articleId = "art_bx1_07",
            articleTitle = "登高",
            author = "杜甫",
            prompt = "杜甫在《登高》中描绘秋风萧瑟、落叶纷纷而长江水奔流不息之雄浑壮阔景色的名句是：",
            answer = "无边落木萧萧下，不尽长江滚滚来。",
            keyPoints = listOf("无边落木萧萧下 (‘萧萧’草木凋零声)", "不尽长江滚滚来"),
            explanation = "宏大开阔之秋景，沉郁顿挫之诗魂。"
        ),

        // ==============================================================
        // 8. 短歌行 (曹操) - art_bx1_04
        // ==============================================================
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
            id = "gk_duangexing_02",
            articleId = "art_bx1_04",
            articleTitle = "短歌行",
            author = "曹操",
            prompt = "曹操在《短歌行》中化用《诗经》典故，深情表达对贤才热切渴慕与思念的名句是：",
            answer = "青青子衿，悠悠我心。但为君故，沉吟至今。",
            keyPoints = listOf("青青子衿 (‘衿’衣领)", "悠悠我心", "沉吟至今"),
            explanation = "求贤若渴的代表诗句，‘子衿’喻指才俊。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_duangexing_03",
            articleId = "art_bx1_04",
            articleTitle = "短歌行",
            author = "曹操",
            prompt = "曹操在《短歌行》末尾以大山不嫌泥土多、大海不拒细流纳百川为喻，展现博大胸怀与归附天下之心的诗句是：",
            answer = "山不厌高，海不厌深。周公吐哺，天下归心。",
            keyPoints = listOf("山不厌高，海不厌深", "周公吐哺 (‘哺’口中嚼碎的食物)"),
            explanation = "一代政治家胸襟气度的最高体现。"
        ),

        // ==============================================================
        // 9. 念奴娇·赤壁怀古 (苏轼) - art_bx1_09
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_niannujiao_01",
            articleId = "art_bx1_09",
            articleTitle = "念奴娇·赤壁怀古",
            author = "苏轼",
            prompt = "苏轼在《念奴娇·赤壁怀古》中描绘周瑜年轻指挥有方、笑语晏晏之间大破曹军水师的名句是：",
            answer = "羽扇纶巾，谈笑间，樯橹灰飞烟灭。",
            keyPoints = listOf("羽扇纶巾 (‘纶’勿写为‘纶’的别意，音guān)", "樯橹灰飞烟灭 (‘樯橹’代指曹军战船，‘樯’桅杆，‘橹’船桨)"),
            explanation = "儒将风范的生动描摹，‘樯橹’字形为经典高频易错题。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_niannujiao_02",
            articleId = "art_bx1_09",
            articleTitle = "念奴娇·赤壁怀古",
            author = "苏轼",
            prompt = "苏轼在词尾将人生虚无之感融于旷达江月之中，洒酒祭月的抒情名句是：",
            answer = "人生如梦，一尊还酹江月。",
            keyPoints = listOf("一尊还酹江月 (‘尊’通‘樽’，酒杯；‘酹’以酒洒地祭奠)"),
            explanation = "‘酹’字形极易漏笔画，苏轼豪放旷达思想之体现。"
        ),

        // ==============================================================
        // 10. 永遇乐·京口北固亭怀古 (辛弃疾) - art_bx1_10
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_yongyule_01",
            articleId = "art_bx1_10",
            articleTitle = "永遇乐·京口北固亭怀古",
            author = "辛弃疾",
            prompt = "辛弃疾在《永遇乐·京口北固亭怀古》中追忆当年刘裕挥师北伐、驰骋中原之雄伟气概的句子是：",
            answer = "想当年，金戈铁马，气吞万里如虎。",
            keyPoints = listOf("金戈铁马", "气吞万里如虎"),
            explanation = "豪放派词人代表性金句，反衬南宋朝廷之屈辱偷安。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_yongyule_02",
            articleId = "art_bx1_10",
            articleTitle = "永遇乐·京口北固亭怀古",
            author = "辛弃疾",
            prompt = "辛弃疾在词作结尾以老将廉颇自比，慨叹报国无门、英雄垂暮之悲愤的名句是：",
            answer = "凭谁问：廉颇老矣，尚能饭否？",
            keyPoints = listOf("廉颇老矣", "尚能饭否 (‘饭’名词作动词，吃饭)"),
            explanation = "用典深沉，抒发壮志难酬之无尽遗恨。"
        ),

        // ==============================================================
        // 11. 归园田居·其一 (陶渊明) - art_bx1_05
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_guiyuan_01",
            articleId = "art_bx1_05",
            articleTitle = "归园田居·其一",
            author = "陶渊明",
            prompt = "陶渊明在《归园田居·其一》中用鸟儿恋旧林、鱼儿思故渊的比喻，表达自己思念故土田园、渴望摆脱羁绊的心情的诗句是：",
            answer = "羁鸟恋旧林，池鱼思故渊。",
            keyPoints = listOf("羁鸟恋旧林 (‘羁’束缚)", "池鱼思故渊"),
            explanation = "对偶工整的比喻句，表达对自由田园生活的向往。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_guiyuan_02",
            articleId = "art_bx1_05",
            articleTitle = "归园田居·其一",
            author = "陶渊明",
            prompt = "陶渊明在《归园田居·其一》中说明自己天性喜欢丘山自然、从小没有适应世俗官场的诗句是：",
            answer = "少无适俗韵，性本爱丘山。",
            keyPoints = listOf("少无适俗韵 (‘韵’气质风度)", "性本爱丘山"),
            explanation = "开宗明义表明归隐田园的本心初衷。"
        ),

        // ==============================================================
        // 12. 梦游天姥吟留别 (李白) - art_bx1_06
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_mengyou_01",
            articleId = "art_bx1_06",
            articleTitle = "梦游天姥吟留别",
            author = "李白",
            prompt = "李白在《梦游天姥吟留别》末尾昂首长啸，展现绝不向权贵屈服妥协、追求人格独立与尊严的千古名句是：",
            answer = "安能摧眉折腰事权贵，使我不得开心颜！",
            keyPoints = listOf("安能摧眉折腰 (‘摧眉’低眉，‘折腰’弯腰)", "事权贵", "开心颜"),
            explanation = "李白傲岸不羁独立人格的最高赞歌。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_mengyou_02",
            articleId = "art_bx1_06",
            articleTitle = "梦游天姥吟留别",
            author = "李白",
            prompt = "李白在诗中描写天门洞开时雷电轰鸣、山峦崩塌的梦境高潮景象是：",
            answer = "洞天石扉，訇然中开。青冥浩荡不见底，日月照耀金银台。",
            keyPoints = listOf("石扉 (‘扉’门扇)", "訇然中开 (‘訇’巨响声)", "青冥浩荡"),
            explanation = "瑰丽浪漫的仙界想象，‘訇’字形常考。"
        ),

        // ==============================================================
        // 13. 归去来兮辞并序 (陶渊明) - art_xb3_11
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_guiqu_01",
            articleId = "art_xb3_11",
            articleTitle = "归去来兮辞并序",
            author = "陶渊明",
            prompt = "陶渊明在《归去来兮辞》中表明过去做错的官场经历已无法挽回，但明智的未来依然可追的觉醒之句是：",
            answer = "悟已往之不谏，知来者之可追。",
            keyPoints = listOf("悟已往之不谏 (‘谏’挽回匡正)", "知来者之可追"),
            explanation = "决绝辞官归隐的心灵觉醒之语。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_guiqu_02",
            articleId = "art_xb3_11",
            articleTitle = "归去来兮辞并序",
            author = "陶渊明",
            prompt = "陶渊明在辞中描写归家后倚着南窗舒展心怀、寄傲于简陋居室感到无比满足的名句是：",
            answer = "倚南窗以寄傲，审容膝之易安。",
            keyPoints = listOf("倚南窗以寄傲", "审容膝之易安 (‘审’明了知晓，‘容膝’仅容下双膝，极言居室简陋)"),
            explanation = "考查安贫乐道、傲岸不屈之精神境界。"
        ),

        // ==============================================================
        // 14. 兰亭集序 (王羲之) - art_xb3_10
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_lanting_01",
            articleId = "art_xb3_10",
            articleTitle = "兰亭集序",
            author = "王羲之",
            prompt = "王羲之在《兰亭集序》中直接批评把死和生等同、把长寿和短命等量齐观的虚妄荒谬观点的句子是：",
            answer = "固知一死生为虚诞，齐彭殇为妄作。",
            keyPoints = listOf("一死生 (‘一’意动用法，把……看作一样)", "虚诞", "齐彭殇 (‘殇’未成年死)", "妄作"),
            explanation = "王羲之对虚无主义老庄清谈的深刻批驳，词类活用核心考点。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_lanting_02",
            articleId = "art_xb3_10",
            articleTitle = "兰亭集序",
            author = "王羲之",
            prompt = "《兰亭集序》中描写兰亭集会虽无管弦乐队之盛，但借着引流水行酒觞、咏诗抒怀也足以畅叙幽深情怀的名句是：",
            answer = "虽无管弦之盛，一觞一咏，亦足以畅叙幽情。",
            keyPoints = listOf("管弦之盛", "一觞一咏 (‘觞’酒杯)", "畅叙幽情"),
            explanation = "文人雅集的生活剪影，常考‘觞’字含义。"
        ),

        // ==============================================================
        // 15. 锦瑟 (李商隐) - art_xb2_recite_03
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_jinse_01",
            articleId = "art_xb2_recite_03",
            articleTitle = "锦瑟",
            author = "李商隐",
            prompt = "李商隐在《锦瑟》中巧妙融合庄周化蝶与望帝啼鹃两个神话典故，寄托迷茫执着深情的诗句是：",
            answer = "庄生晓梦迷蝴蝶，望帝春心托杜鹃。",
            keyPoints = listOf("庄生晓梦迷蝴蝶", "望帝春心托杜鹃"),
            explanation = "晚唐朦胧诗典范，典故精准互文。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_jinse_02",
            articleId = "art_xb2_recite_03",
            articleTitle = "锦瑟",
            author = "李商隐",
            prompt = "李商隐在《锦瑟》颈联中运用通感与典故，营造出明月清冷、日暖生烟迷离优美意境的名句是：",
            answer = "沧海月明珠有泪，蓝田日暖玉生烟。",
            keyPoints = listOf("沧海月明珠有泪", "蓝田日暖玉生烟"),
            explanation = "意象空灵凄清，诗意极度丰富。"
        ),

        // ==============================================================
        // 16. 离骚(节选) (屈原) - art_xb3_02
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_lisao_01",
            articleId = "art_xb3_02",
            articleTitle = "离骚(节选)",
            author = "屈原",
            prompt = "屈原在《离骚》中表明即使遭受严刑粉身碎骨、也绝不改变高洁追求的千古名句是：",
            answer = "亦余心之所善兮，虽九死其犹未悔。",
            keyPoints = listOf("亦余心之所善兮", "虽九死其犹未悔 (‘犹’勿误作‘由’)"),
            explanation = "中华民族矢志报国、百折不挠之崇高精神象征。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_lisao_02",
            articleId = "art_xb3_02",
            articleTitle = "离骚(节选)",
            author = "屈原",
            prompt = "屈原在《离骚》中表现自己深切关切人民疾苦、哀叹民生艰难的长叹名句是：",
            answer = "长太息以掩涕兮，哀民生之多艰。",
            keyPoints = listOf("长太息以掩涕兮 (‘掩涕’拭泪)", "哀民生之多艰"),
            explanation = "屈原忧国忧民情怀的集中写照。"
        ),

        // ==============================================================
        // 17. 蜀相 (杜甫) - art_xb3_05
        // ==============================================================
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

        // ==============================================================
        // 18. 将进酒 (李白) - art_xb1_recite_03
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_jiangjinjiu_01",
            articleId = "art_xb1_recite_03",
            articleTitle = "将进酒",
            author = "李白",
            prompt = "李白在《将进酒》中自信自负、展现天生我材必能施展、千金散尽还能复来的狂放豪情诗句是：",
            answer = "天生我材必有用，千金散尽还复来。",
            keyPoints = listOf("天生我材必有用", "千金散尽还复来"),
            explanation = "极度自信的盛唐气象与狂士风度。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_jiangjinjiu_02",
            articleId = "art_xb1_recite_03",
            articleTitle = "将进酒",
            author = "李白",
            prompt = "李白在诗开篇以黄河之水自天而降奔流入海、悲叹高堂明镜青丝转瞬成雪的起兴豪放诗句是：",
            answer = "君不见，黄河之水天上来，奔流到海不复回。君不见，高堂明镜悲白发，朝如青丝暮成雪。",
            keyPoints = listOf("奔流到海不复回", "高堂明镜", "朝如青丝暮成雪"),
            explanation = "排山倒海、雷霆万钧的开篇起势。"
        ),

        // ==============================================================
        // 19. 论语十二章 (孔子弟子) - art_xb1_01
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_lunyu_01",
            articleId = "art_xb1_01",
            articleTitle = "论语十二章",
            author = "孔子弟子",
            prompt = "曾子在《论语十二章》中表明读书人士不可以不弘大刚毅、因为承担重任而且道路漫长的名言是：",
            answer = "士不可以不弘毅，任重而道远。",
            keyPoints = listOf("弘毅 (‘弘’宽广，‘毅’刚强)", "任重而道远"),
            explanation = "士人修身立志的核心担当，‘弘毅’内涵常考。"
        ),
        GaoKaoScenarioQuestion(
            id = "gk_lunyu_02",
            articleId = "art_xb1_01",
            articleTitle = "论语十二章",
            author = "孔子弟子",
            prompt = "孔子在《论语十二章》中阐明学与思辩证关系，强调只学习不思考就会迷惘、只思考不学习就会危险的名言是：",
            answer = "学而不思则罔，思而不学则殆。",
            keyPoints = listOf("罔 (‘罔’迷惑迷惘)", "殆 (‘殆’疑惑危险)"),
            explanation = "经典求学治学方法论，‘罔’‘殆’字义。"
        ),

        // ==============================================================
        // 20. 登泰山记 (姚鼐) - art_bx1_15
        // ==============================================================
        GaoKaoScenarioQuestion(
            id = "gk_dengtaishan_01",
            articleId = "art_bx1_15",
            articleTitle = "登泰山记",
            author = "姚鼐",
            prompt = "姚鼐在《登泰山记》中登上泰山日观峰后，描写雪后积雪覆盖群山、在阳光映照下如同蜡烛照耀南天壮丽雪景的句子是：",
            answer = "苍山负雪，明烛天南。",
            keyPoints = listOf("苍山负雪 (‘负’背负)", "明烛天南 (‘烛’名词作动词，照亮)"),
            explanation = "桐城派‘以字点睛’之代表写景名句。"
        )
    )

    /**
     * 获取所有覆盖的篇目名称列表（首项为“全部篇目”）。
     */
    fun getAvailableArticles(): List<String> {
        val uniqueTitles = QUESTIONS.map { it.articleTitle }.distinct()
        return listOf("全部篇目") + uniqueTitles
    }

    /**
     * 根据篇目筛选题目，如果为空或“全部篇目”则返回全量。
     */
    fun getQuestionsByArticle(articleTitle: String?): List<GaoKaoScenarioQuestion> {
        if (articleTitle.isNullOrBlank() || articleTitle == "全部篇目") {
            return QUESTIONS
        }
        return QUESTIONS.filter { it.articleTitle == articleTitle }
    }
}
