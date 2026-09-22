# 更新历程 (Changelog)

本项目遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/) 规范，所有版本迭代与重要变更均记录于此。

---

## [v1.6.0] - 2026-09-22

### 🏗️ Hilt 依赖注入与 MVVM/MVI 单向数据流 (UDF) 架构重构 (Architecture Modernization)
- **接入 Google Hilt 依赖注入框架 (Hilt DI)**：
  - 应用入口标注 `@HiltAndroidApp`，MainActivity 标注 `@AndroidEntryPoint`；
  - 声明 `AppModule` 提供 `AppDatabase`、`WenYanRepository`、`FSRSEngine` 单例生命周期；
  - 声明 `CoroutineDispatchersModule` 注入协程调度器（`@IoDispatcher`、`@DefaultDispatcher`、`@MainDispatcher`），彻底替换底层硬编码调度器。
- **ViewModel 层全面抽离与 UDF 单向数据流 (MVVM/MVI UDF)**：
  - 打造 4 大核心 `@HiltViewModel`：`DashboardViewModel`、`ChapterTreeViewModel`、`SettingsViewModel`、`FlipCardViewModel`；
  - UI 状态由不可变数据类与 `StateFlow<UiState>` 统一暴露，所有 Composable 屏幕彻底消除 Repository 穿透，实现清晰的 Stateless 架构。

### 💾 设置中心全量数据备份与 WebDAV 云端同步 (Backup & WebDAV Cloud Sync UI)
- **本地 JSON 全量备份导出与恢复 (SAF Export & Import)**：
  - 在设置中心“数据与关于”卡片新增“导出学习进度”与“从本地文件恢复进度”入口；
  - 接入 Android 存储访问框架（SAF `CreateDocument` / `OpenDocument`），一键无缝导出/导入全量卡片学习状态与复习日志。
- **WebDAV 云端同步配置对话框 (WebDAV Cloud Sync Dialog)**：
  - 支持坚果云、Nextcloud 等标准 WebDAV 协议；
  - 包含服务器地址、账号、密码及密码明密文切换，支持一键“上传备份”与“云端恢复”，具备进度指示与震动音效反馈。

### 🎨 Android 12+ 莫奈取色 (Monet Dynamic Color) 与热力图月份排版修复 (Theming & Heatmap Fixes)
- **全量主题莫奈动态取色联动 (Material You Dynamic Theming)**：
  - `WenYanTheme` 接入 Android 12+ `dynamicLightColorScheme` 与 `dynamicDarkColorScheme`，随系统壁纸动态取色；
  - 语义化 Token（`StudyBlueAccent`、`StudyBlueLight`、`DueRed`、`SuccessGreen` 等）全面绑定 `MaterialTheme.colorScheme`，全应用各卡片、按钮、图标自适应莫奈色彩。
- **打卡热力图月份标签重叠与遮挡修复 (Heatmap Month Labels Fix)**：
  - 热力图新增“莫奈”配色方案对齐系统动态取色；
  - 彻底根除月份文字（如“September”或多字符月份）因 13dp 单元格宽度限制产生的严重文字挤压、重叠与显示不全缺陷，改用绝对坐标列对齐排版与优雅的月份展示。

---

## [v1.5.7] - 2026-09-21

### 📝 高考理解性默写专项体验全面革新 (GaoKao Scenario Dictation Upgrade)
- **扩充 60+ 道高考真题与重点名篇题库 (Question Bank Expansion)**：
  - 题库规模大幅扩容，全面覆盖统编课标与历年高考 25+ 篇核心初高中必背古文与诗词（涵盖《劝学》《师说》《赤壁赋》《琵琶行》《阿房宫赋》《蜀道难》《登高》《出师表》《岳阳楼记》《滕王阁序》《归去来兮辞》《逍遥游》等）。
- **新增“篇目专项默写”与全真随机刷题 (Article Filtering & Shuffle)**：
  - 顶部新增篇目专项筛选横滑栏与全量篇目选择抽屉，支持学生选择特定篇目进行针对性靶向攻坚；
  - 新增“🎲 随机乱序”按键，打乱题目出题顺序，满足多样化模拟考场随机抽查需求。
- **软键盘智能联动与一键评测收起 (Keyboard IME Done Integration)**：
  - 输入框支持软键盘回车键（`ImeAction.Done`），一键自动收起软键盘、清除焦点并立即执行端侧 LCS 算法精准评测；
  - 新增输入内容一键清空按钮，彻底告别原先需依靠手势返回才能关闭键盘的操作痛点。
- **彻底根除采分点文字挤压重叠排版缺陷 (FlowRow Responsive Layout)**：
  - 将答案展示区的“核心易错点 · 采分突破”从原先易变形截断的单行 Row 重构为响应式折行 `FlowRow`；
  - 彻底杜绝字与字挤压成团、文字遮挡等布局缺陷；优化题干行高（26sp）与留白呼吸感。

---

## [v1.5.6] - 2026-09-21

### 🎨 设置中心与目标设置弹窗 UI/UX 体验深度重构 (Settings & Goals UI/UX Overhaul)
- **设置列表页去零碎化与卡片收拢 (Card Overkill Resolution)**：
  - 彻底打破“一功能一独立卡片”造成的视觉割裂与信息密度过稀问题，收拢整合为 **3 大语义化分组大卡片**（学习与调度、偏好与教材、数据与关于）；
  - 内部采用 Material 3 标准精致内缩分割线（避开左侧 38dp 图标与文字边缘），界面屏占比大幅提升。
- **信息层级纯净化与防折行控制 (Header Noise & Overflow Control)**：
  - 移除所有分组头副标题与 TopAppBar 冗余副标，归还清爽阅读呼吸感；
  - 精简条目状态描述并增加 `maxLines = 1` 与 `TextOverflow.Ellipsis`，从根本上杜绝“100”与“句”孤立折行。
- **调色板规范化与高危操作阻断提示 (Design Tokens & Danger Indication)**：
  - 统一功能项图标为品牌科技蓝 `StudyBlueAccent`，消除原先五颜六色的视觉噪音；
  - 高危操作“重置所有背诵数据”标题加粗标红（`DueRed`），右侧增加醒目红色“重置”指示标，强化用户操作阻断心智。
- **彻底根除“文字增多把左侧 SVG 挤上去”布局缺陷 (Vertical Center Lock)**：
  - 摒弃 M3 `ListItem` 在多行模式下强制将 `leadingContent` 顶端对齐的黑盒行为；
  - 采用全受控 `Row(verticalAlignment = Alignment.CenterVertically)`，配合 38dp 尺寸锁定与中间 `Modifier.weight(1f)`，确保无论文字多少行，左侧图标始终稳固处在几何垂直居中轴线上。
- **目标设置弹窗升级为自适应 M3 `ModalBottomSheet` (Dialog to BottomSheet)**：
  - 彻底根除居中弹窗底部“出题优先顺序”被屏幕边缘截断遮挡的严重缺陷；
  - 顶部配备标准 Drag Handle，中间区域支持流畅垂直滚动，底部常驻固定“取消 / 保存目标”操作栏，杜绝误触与遮挡。
- **扁平化结构与行内步进器 (Inline Stepper)**：
  - 移除内部多层嵌套卡片（嵌套边框叠加），改用通透的单向垂直线性流；
  - 将加减微调按钮直接合流并入右上角数值栏 `[ − ] 20 句/日 [ + ]`，大幅释放纵向空间，触控热区达标且提供微触感震动反馈。
- **高对比度激活 Chip (High-Contrast Active State)**：
  - 统一新学与复习模块的 Chip 样式：选中态为饱和蓝底白字加粗，未选态为灰底灰字，彻底解决选中态难以辨认的问题。

### 🏛️ Room SQLite 离线数据库底座与数据无损平滑迁移 (Room Offline DB & Uncapped Logs)
- **Room 架构体系落地**：
  - 声明 `CardStateEntity`、`ReviewLogEntity`、`DailyRecordEntity`，建立规范的 `@Dao` 数据访问层；
  - 彻底解耦过往 SharedPreferences 管道符拼接序列化，消除单 XML 膨胀与主线程 I/O 阻塞；
  - 编写 `DatabaseMigrationHelper`，冷启动自动检测并无缝迁移旧版用户卡片状态、复习记录与每日打卡数据；
  - **解除 500 条复习日志硬编码截断**，全面记录全生命周期复习记录，为离线算法参数调优提供真实全量数据支持。
- **无障碍触摸靶与 Edge-to-Edge 视觉规范**：
  - 重构 `RecitationHeatmapCard` 打卡热力图，网格单元交互区域扩展至符合无障碍标准的触摸靶，加入涟漪动效与周次对齐。

### 🎓 高考实战赋能：情境化默写、LCS 智能评测与长文滚雪球 (GaoKao Pedagogy & Snowball Recitation)
- **历年高考情境化理解性默写专项 (`GaoKaoScenarioScreen`)**：
  - 紧扣高考名篇考点，内置高频真题题库与易错通假字、古今异义采分点提点；
  - 卡片正面呈现情境提示，背面高亮考点与失分陷阱，支持答案即时对比与评分提交。
- **端侧轻量 LCS 动态规划比对引擎 (`RecitationDiffEngine`)**：
  - 基于最长公共子序列（Longest Common Subsequence）算法，毫秒级比对学生默写与原文；
  - 精确标记错字（Wrong）、漏字（Missing）、多字（Extra），输出高精度准确率评估。
- **长篇诗文“滚雪球”串联背诵 (`SnowballRecitationScreen`)**：
  - 针对《赤壁赋》、《蜀道难》、《琵琶行》等长篇古文，提供渐进式上下文累加记忆流程：
    - 阶段一：背诵首联
    - 阶段二：遮挡连背 1+2 联
    - 阶段三：遮挡连背 1+2+3 联……直至全篇通背；
  - 彻底攻克“单句会背、通篇连不起来”的断层痛点。

### 🔬 FSRS-5 算法极致攻坚：19 参数 Nelder-Mead 调优、Leech 熔断与 Fuzz 扰动
- **Nelder-Mead 19 参数本地离线联合优化 (`FSRSOptimizer`)**：
  - 废弃网格搜索，引入纯 Kotlin 实现的 Nelder-Mead 多维单纯形寻优算法；
  - 结合二元交叉熵损失（Log Loss）与生理单调性惩罚函数（$w_0 \le w_1 \le w_2 \le w_3$），对全部 19 个参数实现真正的端侧全局拟合；
  - 引入 `Mutex` 互斥锁保护，彻底消除快速连续翻卡时并发调优的竞态写入。
- **顽固漏卡 (Leech) 自动熔断与难点攻坚**：
  - 统计卡片累计遗忘次数（`lapses >= 4`），自动标记为 Leech 顽固卡并熔断常规队列刷屏；
  - 在练习中心开辟“顽固卡专项突破”专区，集中攻坚核心生僻字词。
- **同文防碰撞离散扰动 (Fuzzing Factor)**：
  - 对调度天数 $\ge 3$ 天的卡片施加 $\pm 5\%$ 离散扰动，防止同篇课文同一天集中到期引发“复习雪崩”。

### 🌐 全域生态：纯 Kotlin WebDAV 多端备份与硬笔字帖导出
- **WebDAV / 本地数据全量同步 (`WebDavBackupManager`)**：
  - 支持学生在手机、折叠屏与平板间无缝流转复习进度与打卡热力图；
  - 采用零依赖纯 Kotlin JSON 构建与解析，保障跨平台运行的高性能与轻量性。
- **结业文牒成就海报与米字格硬笔楷书字帖 (`CertificateAndCopybookScreen`)**：
  - 掌握度达标时自动颁发国风结业文牒，附带朱砂印章、宣纸底纹与篆书评语；
  - 一键生成米字格楷书临摹练习帖，打通“眼脑口手”全感官记忆闭环。
- **练习中心 (`PracticeScreen`) 全面重构**：
  - 统一收拢“高考情境默写”、“长文滚雪球”、“楷书字帖临摹”与“顽固卡攻坚”四大核心能力。
- **全面严密的自动化测试基准**：
  - 扩充单元测试至 134 项（10 个完整测试套件），全面覆盖 Room 迁移、Nelder-Mead 调优、LCS 评测、滚雪球流转与 WebDAV 闭环。

---

## [v1.5.5] - 2026-09-21

### 🎯 每日背诵学习目标 (Anki风格) 与全新独立设置中心 (Daily Goals & Settings Center)
- **类似 Anki 的每日学习与复习目标管理体系**：
  - 支持自定义设定**每日新学句子上限**（0 纯复习模式、5、10、20、30、不设限）与**每日复习上限**（20、50、100、200、不设限），防止复习卡片积压滚雪球；
  - 支持配置**出题优先次序偏好**：先复习后新学（`REVIEW_FIRST`，Anki 经典推荐防拖延）、先新学后复习（`NEW_FIRST`）、混合顺承排布（`MIXED`）；
  - 主页（今日背诵）新增**今日目标看板卡片**：双色现代进度条实时呈现新学与复习完成度，双目标达成时展示打卡通关庆祝徽章，并支持一键“调整目标”。
- **全新独立 Material 3 设置中心 (`SettingsScreen`) 与 6 大功能子菜单**：
  - 彻底收敛原先分散在主页顶栏、足迹页底部的零乱入口，构建独立的设置页；
  - 建立 6 大条理分明的子菜单体系：
    1. **学习与复习目标**（新卡上限、复习上限、出题顺序、篇章原序）
    2. **记忆算法与调优**（FSRS-5 算法调优、期望保留率、19项权重矩阵与自适应拟合）
    3. **背诵打卡提醒**（每日背诵通知推送开关与时钟选择）
    4. **视听与触感偏好**（交互音效与 Taptic 触感震动）
    5. **教材与篇目范围**（统编 11 册教材、高考 72 篇必背范围切换）
    6. **应用维护与关于**（新手指南重温、数据重置、离线文库篇目与版本信息）
  - 主页顶栏全面净化，右上角保留精致的“设置中心”齿轮入口与音效切换按钮。

### 🧠 FSRS 队列调度合规化与配置变更状态无损持久化 (Spaced Repetition & Lifecycle Hardening)
- **FSRS 学习队列截断与状态契约修复**：
  - 严格对齐 FSRS / Anki 标准调度规范：`dailyReviewLimit` 仅限流已毕业的正式复习卡（`CardState.REVIEW`），初学卡（`LEARNING`）与遗忘重学卡（`RELEARNING`）不再受复习上限截断，确保当日记忆强化闭环；
  - 修复 `DailyGoalsAndQueueTest` 中多步学习调度（`learningSteps`）到期时间验证，建立 126 项严密且无条件断言的自动化测试基线；
  - 修复混合学习顺承排布（`StudyOrderPreference.MIXED`）在 SRS 优先级与课本顺序下的排序计算逻辑，解决未学新卡 `dueTime = 0L` 对章节紧急程度比对的干扰。
- **屏幕旋转/分屏配置变更状态无损恢复**：
  - 在 `MainActivity` 中引入 `rememberSaveable` 与 `OverlayScreenStateHolder`，实时追踪翻卡背诵进度；
  - 即使在 100% 完成背诵后的结算庆祝界面发生屏幕旋转或系统暗色主题切换，背诵进度与完成态亦能无损保全；
  - 升级 `ClozeRecitationScreen` 的遮挡层级与“一键全览”开关为 `rememberSaveable`。
- **纯复习模式目标判定修复**：
  - 修复 `targetNew = 0` 时 `TodayStudyProgress.isNewGoalReached` 恒为 false 的问题，支持纯复习模式下目标正常达成并点亮当日足迹打卡。
- **技术重构与未来功能演进路线图发布**：
  - 编制并发布 [`ROADMAP.md`](file:///d:/OneDrive/Desktop/文言背诵/ROADMAP.md)，系统规划现代架构底座（Room/Hilt）、高考实战深化、FSRS-5 算法极致攻坚与多端生态全景路径。

---

## [v1.5.3] - 2026-09-21

### 🛡️ 篇目文库崩溃根治与全屏排版留白优化 (Crash Elimination & Layout Insets Optimization)
- **篇目文库切换崩溃彻底根治**：
  - 移除了在非标准导航容器下易因 LifecycleOwner 状态未就绪导致崩溃的 `collectAsStateWithLifecycle()`，改用标准的 Compose `collectAsState()`；
  - 彻底清理 `expandedModuleIds` 中使用 `listSaver<Set<String>, String>` 导致 `SaveableStateRegistry` 注册失败与 Bundle 序列化抛出 `IllegalArgumentException` 的致命隐患；
  - 为文库列表进度条 `LinearProgressIndicator` 增加全面的 `safeMastery`、`NaN` 与 `Infinite` 防御，彻底杜绝底层 Android Canvas 在绘制非有限浮点数时闪退；
  - 内层 `Scaffold` 显式设置 `contentWindowInsets = WindowInsets(0, 0, 0, 0)`，根除双重 Scaffold 嵌套测量冲突。
- **主标题上方双重状态栏留空根治**：
  - 修复根 `Scaffold` 默认将系统状态栏高度计入 `innerPadding` 导致外层下沉一次、子界面 `TopAppBar` 再次消费状态栏而产生的**双重状态栏叠加（80dp+ 巨大留白空白）**；
  - 根 `Scaffold` 的 `contentWindowInsets` 显式设置为 `WindowInsets(0, 0, 0, 0)`，仅保留底部导航栏边距，各子界面 `TopAppBar` 贴顶并精确适配单倍系统状态栏，页面视觉恢复精致紧凑。
- **闪卡切题防剧透与评级防抖**：
  - 卡片 3D 翻转动画与状态使用 `key(currentIndex, currentCard.id)` 实现各卡片严格隔离，切题瞬间新卡必定以 0° 正面呈现，彻底消除上一张卡片 180° 反向旋转导致的瞬时答案剧透；
  - 为“重来”、“困难”、“良好”、“简单”评级按钮加入状态防抖（`isTransitioning`），杜绝快速连击导致的重复扣除或多次提交 FSRS 评级；
  - 优化“重来”卡片的队列插入机制，确保至少间隔 3 张卡片或顺承原序插入，避免瞬间作弊式复现。
- **FSRS-5 算法跨自然日与短周期稳定性加固**：
  - 跨天计算由过去的绝对 24 小时毫秒除法全面升级为自然日历比较（`ChronoUnit.DAYS.between`），解决夜间 23:50 背诵与次日 08:00 晨读被误判定为同一次复习的缺陷；
  - 修正 `shortTermStability` 中 `HARD` 评级约束（`rating.value >= 3` 保持 `>= 1.0`，使困难评级能够正常生效短期惩罚）；
  - 全面防护稳定性与难度入参 `NaN`。
- **深色模式（Dark Theme）全界面高对比度修复**：
  - 统一重构 `BookSelectionDialog`、`ReminderSettingsDialog`、`OnboardingTutorialDialog`、`PracticeScreen`、`DashboardScreen`、`ChapterTreeScreen` 底部确认/主操作按钮颜色，彻底告别深色模式下使用浅灰 `StudyNavy` 配纯白文字所产生的 1.2:1 白底白字可读性灾难，严格对齐 Material 3 主题 `primary` / `onPrimary` 规范。

---

## [v1.5.2] - 2026-09-21

### 🚀 彻底根治滑动闪退与线程并发崩溃 (Root Cause Fix: Gesture & Concurrency Stability)
- **主界面手势与导航解耦重构**：
  - 彻底拔除导致手势冲突与 `SubcomposeLayout` 递归度量崩溃的 `HorizontalPager` 嵌套大架构；
  - 采用现代单向数据流（UDF）驱动的 `AnimatedContent` 方向感知平滑横滑切页，配合轻量级 `detectHorizontalDragGestures` 阻尼阈值手势判断；
  - 完美保障四大主 Tab（今日背诵、篇目文库、专项练习、研墨足迹）在左右跟手滑动手势与底部导航栏点击时如丝般顺滑，彻底杜绝触摸滑动即闪退的顽疾；
  - 纵向 `LazyColumn` 滚动与横向切页手势完全分离，阻尼与触摸斜率自然分流，消除上下滑动时的误判卡死。
- **热力图坐标溢出崩溃修复**：
  - 修复 `RecitationHeatmapCard` 在首帧测量前 `scrollState.maxValue` 为 `Int.MAX_VALUE` 时直接调用 `scrollTo()` 导致坐标溢出奔溃的严重隐患；
  - 恢复并加固响应式 `snapshotFlow` 布局就绪监听，严格在测量得到有效像素值时执行视口平移。
- **仓储层高并发线程安全加固**：
  - 将 `WenYanRepository` 中的卡片状态映射与日志集合全面升级为 `ConcurrentHashMap` 与 `CopyOnWriteArrayList`，彻底根除高并发状态计算下的 `ConcurrentModificationException`。
- **全局非捕获异常兜底防御**：
  - 在 `WenYanApp` 注册全局 `UncaughtExceptionHandler` 与持久化日志记录（`crash.log`），全方位增强极端 OEM 系统环境下的容灾自愈与诊断能力。

---

## [v1.5.1] - 2026-09-21

### 🛡️ 启动崩溃与机型兼容性紧急修复 (Crash & Compatibility Fix)
- **Activity 启动生命周期防护**：调整 `enableEdgeToEdge()` 调用时机至 `super.onCreate()` 之后，并包裹安全捕获与容错降级；防止在各类 OEM 定制系统（MIUI/OriginOS/ColorOS/OneUI）或 Android 12-14 机型上因 DecorView 尚未就绪导致的点开即闪退。
- **SDK 稳态构建环境**：将 `compileSdk` 与 `targetSdk` 稳妥调整为 Android 34（与当前稳定版 AGP 8.4.1 工具链完全对齐），移除临时绕过编译限制标志，彻底杜绝字节码与资源表兼容性崩溃。
- **系统状态栏与导航栏原生透明化**：重构 `themes.xml` 及新增 `values-night/themes.xml`，状态栏与导航栏统一采用透明底，禁用 API 29+ 强制暗色遮罩（`isNavigationBarContrastEnforced = false`），完美呈现 Edge-to-Edge 视觉。

### 🌙 全局深色模式完备沉浸式适配 (Complete True Dark Mode Overhaul)
- **主题 Token 动态响应架构**：将 `BgCanvas`、`BgSurface`、`BgSurfaceMuted`、`BorderSubtle`、`TextPrimary`、`TextSecondary`、`TextTertiary`、`StudyNavy` 等核心视觉 Token 全面重构为 `@Composable get()` 动态计算属性，卡片、弹窗与各个页面在浅色/深色切换时实现 100% 自动适配，彻底告别“白底黑字在深色模式下刺眼”或“黑底黑字无法阅读”的缺陷。
- **深空蓝黑高对比配色体系**：
  - 全局底色：`#0B0F17`（深邃黑蓝，纯净不发灰，低功耗护眼）；
  - 卡片底色：`#151D2A`（层级抬升表面）；
  - 次级底色：`#1E293B`（Slate 800 辅助胶囊底）；
  - 发丝边框：`#334155`（Slate 700 细致描边）；
  - 主副文字：`#F8FAFC` 与 `#CBD5E1`（高可读性明亮阶梯排版）。
- **打卡热力图全面暗色适配**：重构研墨打卡热力图格子色彩映射体系与底部图例，深色模式下 0 记录格子自动适配为深沉底色（`#1E293B`），消除此前浅灰格子在暗色背景下刺眼的视觉缺陷。
- **底部导航栏原生色彩对齐**：底部导航栏容器底色与图标指示器完全接轨 `MaterialTheme.colorScheme`，深浅模式下均具备极高的辨识度与现代质感。

---

## [v1.5.0] - 2026-09-20

### ⏯️ Anki 级断点续背与中途退出无缝恢复 (Breakpoint Session Resumption)
- **中途退出即时保存**：背诵古诗文过程中随时退出到主界面或关闭 App，当前背诵队列、指针位置与掌握进度将毫秒级自动持久化。
- **主页动态续背卡片**：主页首屏动态展现“未完待续 · 点击继续背诵”悬浮卡片，清晰呈现当前篇目名称、句数进度条及已掌握句数，一键继续直达上次卡片；支持一键放弃并清理会话。
- **篇目文库与专项练习联动**：篇目文库（ChapterTreeScreen）与专项练习（PracticeScreen）智能感知进行中的背诵会话，点击相同篇目可选择恢复历史进度或重新开始。
- **动态重来队列保留**：背诵过程中选择“重来 (Again)”动态追加到队尾的复习卡片序列同样完整保存，绝不丢失复习队列。

### ⚙️ 核心算法引擎与逻辑修复 (Algorithm & Stability Fixes)
- **FSRS Easy 间隔单调递增**：彻底修复复习模式下极端步长评定“简单 (Easy)”可能未严格长于“良好 (Good)”的计算边界，确保间隔倍数严格单调递增。
- **掌握句数计数修正**：修复翻卡界面评定“重来 (Again)”时计数器误增的缺陷，仅在判定掌握（Hard/Good/Easy）时正确计数。
- **参数调优后台异步化**：FSRS 参数自动调优算法迁移至后台工作协程非阻塞执行，配合转圈 Loading 状态，彻底杜绝主线程掉帧卡顿。
- **仓储层并发线程安全**：增强内存卡片状态映射与复习记录的并发保护，彻底杜绝多协程读写异常。

### 📱 现代 Android 体验与规范升级 (Modern Android & Material 3 Alignment)
- **Android 15 Edge-to-Edge 全面屏**：升级 `compileSdk = 35`、`targetSdk = 35`，接入官方 `enableEdgeToEdge()` 与 `adjustResize`，适配沉浸式状态栏与导航手势。
- **Material 3 深色模式 (Dark Theme)**：规范构建 `ModernDarkColorScheme`，全局适配系统深色模式切换。
- **无障碍点击区域与无障碍语义**：交互按钮与弹窗关闭按钮均优化至合规触摸区域（$\ge 44\text{dp} \sim 48\text{dp}$）；为挖空词胶囊增加 `Role.Button` 与 TalkBack 读屏标签。

---

## [v1.4.1] - 2026-09-20

### 📜 顺承篇章原序背诵机制 (Sequential Poem Recitation Order & Context Preservation)
- **告别打散诗文上下文**：引入 `RecitationOrderMode.SEQUENTIAL`（顺承篇章原序）并设为默认背诵模式，兼顾间隔重复复习与古诗文“起承转合”的章法韵律。
- **篇目聚类与篇内正序推进**：
  - 复习与练习队列根据篇目整体聚类，杜绝不同课文单句随意乱序穿插；
  - 篇目内部严格按照原文自然行文逻辑（从首句到尾句）正序推进。
- **联次标识与文脉桥接**：
  - 闪卡正面新增联次定位徽标（如 `第 2/4 联 · 顺承原序`），背诵进度心中有数；
  - 自动注入上联/上句提示（`【上承文脉】`），出句对句更自然流畅。
- **篇内就近重温强化**：翻卡背诵评定为“重来 (Again)”时，卡片智能就近插入当前篇章末尾即刻强化温习，绝不跨篇甩到总队列末尾破坏整体语感。
- **专项练习灵活开关**：在“高考 72 篇专项背诵”与“随机背诵”中均提供“顺承篇章原序”快捷开关；算法设置弹窗支持自由切换“顺承篇章原序 / 紧迫度交错优先 / 完全随机乱序”。

### 🌿 界面用语自然化与去学术化 (Natural Language Refinement & Intuitive UX)
- **日常背诵直觉表达**：告别生硬晦涩的书面用词，核心 Tab 与按钮全面统一采用“背诵 / 打卡 / 学习”替代“研读 / 研习”。
- **算法设置通俗化降维**：
  - 去除“二元交叉熵 (Log Loss) 最小化”、“贝叶斯统计”、“自适应调优引擎”等高门槛学术词汇；
  - 改造为“智能参数调优”、“离线本地计算”、“根据你的背诵与复习记录，自动优化最契合你的记忆参数与复习周期”等清晰大白话。
- **微交互与文案打磨**：
  - 精简优化音效测试选项（“简单提示”、“重来提示”、“填空提示”、“背诵达成”）；
  - 规范新手指南四步导引文案，去除多余冗余；
  - 优化按钮布局与高度弹性，杜绝小屏幕或大字号环境下的文字折行截断。

---

## [v1.4.0] - 2026-09-20

### 🧠 FSRS-5 算法设置与自适应调优引擎 (FSRS Optimization Engine)
- **纯本地离线参数调优**：内置 `FSRSOptimizer`，基于学习者真实的背诵轨迹与遗忘率（Again 率），运用贝叶斯统计与二元交叉熵（Log Loss）最小化，自适应校准 19 项 FSRS 核心权重，具备生理安全边界防护。
- **算法调度配置弹窗 (`FSRSConfigDialog`)**：
  - 目标留存率滑动微调（80% ~ 97%），实时提示文言文最佳建议区间（90%~95%）。
  - 背诵遗忘特化系数滑动微调（0.50x ~ 1.00x），适配长句整句遗忘衰减特性。
  - 最大复习间隔胶囊选择（1年、2年、10年、终身）。
  - 一键自适应优化按钮与优化效果统计横幅（显示 Log Loss 拟合提升度与详细变更项）。
  - 19 项 FSRS-5 核心权重矩阵查看器与一键恢复出厂官方参数。
- **持久化与无感微调**：参数全部保存在 `SharedPreferences`，支持开启“背诵时后台自动微调”（每 20 次评分自适应优化一次）。
- **主界面快速入口**：顶部栏新增 `Psychology`（脑力认知）天蓝图标按钮，记忆状态卡片新增“算法调优”微按钮。

### 🎋 单句语境填空多空变体与高频复习强化 (Multi-Cloze & High-Frequency Review)
- **彻底根除句子跨联错位**：重写切分算法，严格基于完整意群（`。`、`！`、`？`、`；`、换行）为单元切分，杜绝了奇数句导致后续全篇排比颠倒的问题。
- **一卡多空与上下文保留**：同一诗联/长句在单句模式下支持派生多张不同挖空点的闪卡，题面保留完整上下文，挖空处以古雅胶囊 `⟦ ________ ⟧` 呈现；诗词名句支持出句对句双向互测。
- **调低学习权重以强化复习**：默认目标保留率提升至 `0.93`，背诵稳定度系数设为 `0.72`，缩短初次掌握后的复习保护期，实现更密集扎实的高频温习。
- **翻卡界面视觉升级**：正面标注空位序号（如 `【语境填空 第 2/3 空】`），背面明确分区（`【填空正解】`、`【整句对照】`、`【译文释义】`）。

---

## [v1.3.2] - 2026-09-20

### 🎨 Material 3 官方标准组件库重构 (Component Standards)
- **全面接入 M3 官方组件**：严格遵循 `AGENTS.md` 准则，废除手写自定义边框与 `Box` 堆叠，全面重构成标准 `OutlinedCard`、`ElevatedCard` 与 `ListItem`。
- **Token 驱动色彩与排版**：统一使用 `MaterialTheme.colorScheme`（`surface`, `onSurface`, `primary`）与 `MaterialTheme.typography`（阶梯式排版体系），消除硬编码字体与样式。
- **弹窗与交互胶囊**：
  - 教材自选弹窗（`BookSelectionDialog`）接入标准 M3 `AlertDialog` 与规范化单选/复选列表项。
  - 视听触觉偏好弹窗（`FeedbackPreferencesDialog`）全面优化选项与即时试听体验。
  - 模式切换胶囊全面升级为 M3 `FilterChip`，交互反馈与圆角更符合 Material 3 规范。

### 🚀 CI/CD 与构建优化
- **动态版本提取**：优化 GitHub Actions 发布流水线（`.github/workflows/release.yml`），自动从 `build.gradle.kts` 中动态提取最新 `versionName`，避免硬编码导致预发布版本号滞后。
- **构建缓存稳定性**：配置 Gradle 与 Kotlin 编译器属性，增强在 Windows 环境及特殊字符路径下的构建稳定性。

---

## [v1.3.1] - 2026-09-20

### 🔊 零延迟原声音效与视觉打磨
- **CC0 零延迟原生音效**：引入高品质低底噪原生音频资源（翻卡、按键、重学与大捷号角），消除播放卡顿与杂音。
- **主界面左右手势滑动**：底层采用 `HorizontalPager` 实现手势平滑左右滑动切换 Tab，并与底部导航栏实现双向无缝联动。
- **沉浸式与布局去杂**：修复状态栏顶部安全间距，精简今日研读信息展示，去除多余调试信息。
- **顶部快速静音**：顶部常驻声音控制开关，支持单键快速切换全局静音/开启。

---

## [v1.3.0] - 2026-09-19

### 🎮 视听触觉综合体验与现代 UI/UX 深度重构
- **触感弹簧微交互 (Haptic Feedback)**：闪卡翻转、按钮点按均具备弹簧物理回弹效果（Spring Physics）与震动触觉。
- **粒子礼花庆祝动效**：接入 `Konfetti` 粒子库，完成一轮背诵复习时触发泥金与朱砂粒子全屏庆祝。
- **研墨足迹 · 背诵热力图 (Heatmap)**：GitHub/多邻国风格打卡足迹，记录近百日每日背诵强度方格与连续连胜天数（Streak）。
- **自选教材书籍联动**：支持自选单册教材或组合预设（必修、选必、高考 72 篇），复习队列全局实时联动。
- **新手研习指南**：4 步卡片式引导，帮助初学者快速掌握 FSRS 算法评分、双轨背诵与热力图。

---

## [v1.2.0] - 2026-09-19

### 🎋 交互式渐进遮挡与强化循环复习
- **渐进遮挡可交互化**：支持挖空句子单字/半句点击即时查验核对。
- **跨篇目随机抽取强化**：新增跨篇目自定义范围随机抽查模式，支持快速冲刺与考前突击。
- **常驻导航控制**：增强顶部导航条与系统返回键的平滑退出控制。

---

## [v1.1.1] - 2026-09-19

### 🛠️ 构建与版本同步
- 同步版本代码与发布脚本，修复构建产物命名与标签一致性。

---

## [v1.1.0] - 2026-09-19

### 📱 架构升级与分区解耦
- **Material 3 底部导航栏**：引入 `Scaffold` + `NavigationBar` + `NavigationBarItem`，界面逻辑彻底解耦。
- **数据流优化**：重构 ViewModel 与 StateFlow 响应式状态流，清除历史遗留测试数据，大幅提升页面切换流畅度。

---

## [v1.0.1] - 2026-09-18

### 🚀 自动化发布流水线
- 建立 GitHub Actions CI/CD 流水线，支持打 Tag 自动编译 Release/Debug APK 并发布到 GitHub Releases。
- 配置 Release 签名证书（永久 Keystore）与 APK SHA-256 校验。

---

## [v1.0.0] - 2026-09-18

### 🌟 初始发布 (Initial Release)
- **高中语文 11 册教材全覆盖**：收录必修、选择性必修与选修共 100 篇诗文，标注文科教育部必背 72 篇篇目。
- **FSRS-5 间隔重复算法**：基于科学记忆规律动态计算稳定性（$S$）、难度（$D$）与可提取性（$R$），四档科学打分预测复习周期。
- **双轨记忆模式**：单句翻转闪卡 + 5 级长篇文言渐进遮挡背诵。
- **中国古典文韵视觉**：宣纸底色、徽墨黑、丹砂朱红与苍竹青绿，100% 离线可用，无网络请求与广告。
