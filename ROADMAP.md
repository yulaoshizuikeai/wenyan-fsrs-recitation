# 文言背诵 (WenYan Recitation) 技术重构与未来功能升级路线图 (ROADMAP.md)

本路线图结合项目核心规范（[AGENTS.md](file:///d:/OneDrive/Desktop/文言背诵/AGENTS.md)、[DESIGN.md](file:///d:/OneDrive/Desktop/文言背诵/DESIGN.md)、[PROJECT.md](file:///d:/OneDrive/Desktop/文言背诵/PROJECT.md)）及 Android 官方架构规范与最佳实践制定，旨在将《文言背诵》打造成兼具现代 Android 工程底座、纯正 FSRS-5 算法内核与高考实战备考体验的顶级学习工具。

---

## 全景演进架构 (Architectural Roadmap)

```
┌───────────────────────────────────────────────────────────────────────────┐
│                     文言背诵技术重构与功能演进全景路线图                  │
├───────────────────┬───────────────────┬───────────────────┬───────────────┤
│ 阶段一：底座现代重塑│ 阶段二：高考实战深化│ 阶段三：算法极致攻坚│ 阶段四：全域生态│
│ (Modern Foundation)│  (Pedagogy Drill)  │  (FSRS-5 Engine)  │  (Ecosystem)  │
├───────────────────┼───────────────────┼───────────────────┼───────────────┤
│ • 落地 Room 离线库 │ • 高考情境理解默写│ • 19 参全量离线优化│ • 桌面微件    │
│ • Hilt + MVI UDF  │ • 智能语音 ASR 评测│ • 遗忘漏卡 Leech   │ • 多端 WebDAV │
│ • Navigation 3 路由│ • 篇章长文滚雪球背│ • 篇章防碰撞 Fuzz │ • 结业文牒字帖│
│ • M3 标准弹窗与适配│ • 易错通假字专攻  │ • 队列分级动态限流│ • 穿戴设备联动│
└───────────────────┴───────────────────┴───────────────────┴───────────────┘
```

---

## 阶段一：架构底座重塑与性能规范筑基 (Phase 1: Modern Foundation & Quality Fixes)

> **目标**：彻底消除历史架构硬伤，让测试套件全绿，重构数据层持久化，全面对齐 Material 3 与 Android 现代架构规范。

### 1.1 紧急缺陷修复 (P0)
- [x] **修复 `DailyGoalsAndQueueTest` 失败用例**
  - **问题**：`test04` 与 `test05` 调度时间未推进，导致卡片处于初学第 1 步到期时间在 10 分钟后而被截断过滤。
  - **措施**：修正测试中的到期时间模拟，显式校验 FSRS 状态迁移契约。
- [x] **修正学习中卡片截断违规 (Anki/FSRS Violation)**
  - **问题**：[`WenYanRepository.kt: 404-409`](file:///d:/OneDrive/Desktop/文言背诵/app/src/main/java/com/ancient/wenyan/data/WenYanRepository.kt#L404-L409) 将 `LEARNING` / `RELEARNING` 与 `REVIEW` 混合同步截断。
  - **措施**：`dailyReviewLimit` 仅对已毕业的正式复习卡（`REVIEW` 状态）生效，学习与重学中的卡片不受截断限制，确保当天强化闭环。
- [x] **修复屏幕旋转/分屏状态丢失问题**
  - **问题**：[`MainActivity.kt: 104`](file:///d:/OneDrive/Desktop/文言背诵/app/src/main/java/com/ancient/wenyan/MainActivity.kt#L104) `remember { mutableStateOf<OverlayScreen?>(null) }` 在配置变更后丢失。
  - **措施**：改用 `rememberSaveable` 并配合可序列化路由，防止横竖屏旋转或暗色切换时被强制退出背诵页。

### 1.2 数据持久化与 Room 离线数据库迁移 (P0)
- [x] **构建 Room SQLite 离线体系**
  - 声明 `@Entity`：`CardStateEntity`、`ReviewLogEntity`、`DailyRecordEntity`。
  - 声明 `@Dao`，所有查询与写回通过响应式 `Flow` 输出。
  - 废除 SharedPreferences 管道符分割字符串拼接存储机制，消灭单 XML 键膨胀与主线程全量卡片序列化 I/O 阻塞。
  - 编写 `DatabaseMigrationHelper`，无缝迁移旧版用户进度。
- [x] **解除复习日志 500 条硬编码截断**
  - 使用 SQLite 存储全生命周期复习日志，为离线算法参数调优提供真实全量数据支持。

### 1.3 现代架构迁移：Hilt 依赖注入与 ViewModel UDF (P1)
- [x] **接入 Hilt 依赖注入框架**
  - 提供 `@Singleton` 的 `AppDatabase`、`WenYanRepository`、`FSRSEngine`。
  - 注入协程调度器（`@IoDispatcher CoroutineDispatcher`），彻底替换硬编码的 `Dispatchers.Default` / `Dispatchers.IO`。
- [x] **MVVM / MVI 单向数据流 (UDF) 重构**
  - 拆分独立 ViewModel：`DashboardViewModel`、`FlipCardViewModel`、`ChapterTreeViewModel`、`SettingsViewModel`。
  - 状态由 `StateFlow<UiState>` 统一暴露，Composable 严格 Stateless。

### 1.4 Material 3 弹窗组件化与无障碍整改 (P1)
- [x] **弹窗全面重构为标准 M3 组件**
  - 重构 6 个手写 `Dialog + Card`（今日目标、FSRS 设置、背诵提醒、诗集选择、反馈偏好、引导指引），替换为官方标准 `AlertDialog` 与 `ModalBottomSheet`。
  - 时间选择改用标准 M3 `TimePicker` / `TimePickerDialog`。
- [x] **热力图无障碍 48dp 触摸靶达标**
  - 重构 [`RecitationHeatmapCard.kt`](file:///d:/OneDrive/Desktop/文言背诵/app/src/main/java/com/ancient/wenyan/ui/components/RecitationHeatmapCard.kt) 日历格点击，触摸区域由 15dp 扩大至标准 24~48dp，并加入平滑横向滚动与手势隔离，解决与根容器切 Tab 的手势冲突。
- [x] **规范 Edge-to-Edge 边到边体验**
  - 移除暴力 `WindowInsets(0, 0, 0, 0)` 清空逻辑，采用正确的 `safeDrawingPadding` / `Scaffold innerPadding`，全面适配挖孔屏与软键盘。

---

## 阶段二：高考语文实战深度赋能 (Phase 2: Pedagogical & GaoKao Alignment)

> **目标**：紧扣新高考核心考点，从“死记硬背”跃升到“情境默写”、“长文通背”与“声韵纠错”。

### 2.1 高考情境化理解性默写专项突破
- [x] **新增情境化理解性默写题库**
  - 针对高考核心必背篇目，提供“真题题干 / 情境提示 ➔ 默写对应原句”模式。
  - 卡片正面展示情境考题与易错采分点提点，提供输入即时校验与智能对比评测。
- [x] **专项易错考点库（通假字、古今异义、重点实虚词）**
  - 整理高频易错考点，在答题后解析中呈现详细考点分析与失分陷阱提示。

### 2.2 端侧离线智能默写比对评测
- [x] **引入端侧轻量 LCS 动态规划默写比对引擎**
  - 支持学生默写输入，系统毫秒级比对原文。
  - 实时可视化标出“漏字”、“错字”、“多字”，提供高精度准度百分比反馈。

### 2.3 长篇诗文“滚雪球”串联背诵 (Snowball Paragraph Chaining)
- [x] **长文渐进串联记忆模式**
  - 针对《赤壁赋》、《蜀道难》、《琵琶行》等长篇古文，提供渐进式上下文递增训练：
    - 阶段 1：背诵第 1 联
    - 阶段 2：遮挡连背 1+2 联
    - 阶段 3：遮挡连背 1+2+3 联
  - 彻底攻克“单句会背、通篇连不起来”的断层痛点。

---

## 阶段三：FSRS-5 算法极致攻坚 (Phase 3: Advanced Spaced Repetition)

> **目标**：打造业界最严谨、最贴合汉字篇章记忆机理的间隔重复引擎。

### 3.1 真实 19 参数本地离线参数调优 (Nelder-Mead)
- [x] **重构 FSRSOptimizer 寻优算法**
  - 废弃网格搜索，引入纯 Kotlin 实现的 Nelder-Mead 多维单纯形寻优算法，基于全量复习日志计算 Log Loss（二元交叉熵损失），对全部 19 个参数实现真正的联合调优。
- [x] **消除并发调优竞态**
  - 引入 `Mutex` 互斥保护，防止用户快速连续翻卡时启动多个优化协程发生乱序写入。

### 3.2 顽固漏卡 (Leech) 自动熔断与专项攻克
- [x] **智能识别 Leech 卡片**
  - 统计卡片累计 `lapses`（遗忘次数）。当单张卡片累计遗忘达到阈值（连续 4 次评分 Again）时，自动标记为“Leech 顽固卡”。
  - 暂停在常规队列中频繁刷屏，单独归入“难点攻坚顽固卡库”，支持针对性专项突破。

### 3.3 同文邻近句子防碰撞离散扰动 (Fuzzing Factor)
- [x] **引入 FSRS 标准 Fuzz 扰动**
  - 对同一篇目的相邻句子施加 $\pm 5\%$ 的到期时间离散扰动，防止整篇课文在同一天集中爆发导致到期卡片“雪崩”。

---

## 阶段四：全域生态与效率联动 (Phase 4: Ecosystem & Productivity)

> **目标**：多端流转无缝衔接，传统美学与学习成果外显。

### 4.1 多端 WebDAV 备份与云同步
- [x] **支持 WebDAV / 坚果云自动同步**
  - 支持学生在多设备之间无缝流转复习进度与打卡热力图。
  - 提供本地全量数据一键导出与还原纯 Kotlin JSON 备份。

### 4.2 结业文牒成就海报与硬笔楷书字帖导出
- [x] **国风“结业文牒”成就海报**
  - 单篇或整册文言文达到 100% 掌握时，生成带有传统朱砂印章、宣纸底纹与毛笔题字的高分辨率结业文牒，增强成就感。
- [x] **硬笔书法田字格/米字格字帖导出**
  - 支持将背诵篇目一键转换为带拼音/笔画的米字格楷书临摹字帖，供学生手写练习。

### 4.3 Android 桌面微件 (Glance Widget)
- [ ] **Jetpack Glance 桌面小组件**
  - 桌面直显“连胜火焰”、“今日剩余待复习句数”以及“今日精选名句”，点击直达复习队列。
- [ ] **自适应折叠屏与平板布局**
  - 支持 `ListDetailPaneScaffold` 与 `NavigationRail`，大屏双栏沉浸背诵。

---

## 执行建议与优先级 (Execution Order)

1. **第一步（紧急 P0）**：修复 `DailyGoalsAndQueueTest` 中的时间推进与 `WenYanRepository` 的学习队列截断，使现有测试套件 100% 通过。
2. **第二步（架构 P0）**：设计并落地 Room SQLite 数据库，废除 SharedPreferences 序列化，迁移卡片与日志持久化。
3. **第三步（体验 P1）**：重构 6 处弹窗为 M3 标准组件，修复热力图 48dp 触摸靶和手势冲突，修复 `rememberSaveable` 旋转丢状态。
4. **第四步（演进 P2）**：逐步落地高考情境化默写模式与 FSRS-5 19参数离线优化。
