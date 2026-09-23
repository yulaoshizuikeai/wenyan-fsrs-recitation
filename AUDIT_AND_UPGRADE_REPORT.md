# 《文言背诵》代码缺陷 (Bugs) 与未完成升级点深度审计报告

本报告针对《文言背诵》项目当前的工程实现、架构规范（[AGENTS.md](AGENTS.md)）、演进路线图（[ROADMAP.md](ROADMAP.md)）以及最新发布日志（[CHANGELOG.md](CHANGELOG.md)）展开端到端独立复核、实测验证与深度穿透审计。

---

## 目录
1. [致命构建、测试与 CI 缺陷 (Build, Testing & CI Defects)](#一致命构建测试与-ci-缺陷)
2. [数据持久化与一致性缺陷 (Data Persistence & Storage Defects)](#二数据持久化与一致性缺陷)
3. [核心算法与业务逻辑缺陷 (Algorithm & Business Logic Defects)](#三核心算法与业务逻辑缺陷)
4. [UI/UX、生命周期、主题与无障碍缺陷 (UIUX-Lifecycle--Theming)](#四uiux生命周期主题与无障碍缺陷)
5. [架构违规与未完成升级点 (Architecture & Unfinished Roadmap)](#五架构违规与未完成升级点)
6. [修复与升级优先级排期表 (Priority Action Plan)](#六修复与升级优先级排期表)

---

## 一、致命构建、测试与 CI 缺陷

### 1.1 标准 Gradle 测试任务因类路径污染全量失败 (`ClassNotFoundException`)
- **定位代码**：[`app/build.gradle.kts: 177-186`](app/build.gradle.kts#L177-L186)
- **命令验证**：运行 `.\gradlew.bat testDebugUnitTest`：
  ```text
  com.ancient.wenyan.ActiveSessionPersistenceTest > initializationError FAILED
      java.lang.ClassNotFoundException at BuiltinClassLoader.java:641
  com.ancient.wenyan.FSRSOptimizerTest > initializationError FAILED
      java.lang.ClassNotFoundException at BuiltinClassLoader.java:641
  ...
  11 tests completed, 11 failed
  Execution failed for task ':app:testDebugUnitTest'.
  ```
- **根因分析**：
  1. 在 AGP 8.4 + Hilt 环境下，类字节码经过 `transformDebugUnitTestClassesWithAsm` ASM 变换，输出在 `app/build/intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs`。
  2. `build.gradle.kts` 中通过 `afterEvaluate` 强行追加了类路径：
     ```kotlin
     testClassesDirs += files("$buildDir/tmp/kotlin-classes/debugUnitTest")
     classpath += files("$buildDir/tmp/kotlin-classes/debugUnitTest", "$buildDir/tmp/kotlin-classes/debug")
     ```
     导致 Gradle 测试扫描器在 `tmp/kotlin-classes` 找到了类文件，但在启动子进程 `TestWorker` 时类加载器上下文隔离错位，全量触发 `ClassNotFoundException`。
  3. 开发者为此注册了自定义任务 `runInProcessTests`（[`app/build.gradle.kts: 188-240`](app/build.gradle.kts#L188-L240)），通过反射手建 `URLClassLoader` 运行用例。这导致 Android Studio、标准 CI/CD 或运行 `./gradlew check` 时测试全军覆没。

### 1.2 Windows / OneDrive 环境下 `kspReleaseKotlin` 目录被锁导致 Release 构建失败
- **定位代码**：[`app/build.gradle.kts: 7-9`](app/build.gradle.kts#L7-L9)
- **命令验证**：执行 `.\gradlew.bat assembleRelease`：
  ```text
  > Task :app:kspReleaseKotlin FAILED
  FAILURE: Execution failed for task ':app:kspReleaseKotlin'.
  > java.io.IOException: Unable to delete directory 'D:\OneDrive\Desktop\文言背诵\app\build\generated\ksp\release'
      Failed to delete some children. This might happen because a process has files open or has its working directory set in the target directory.
      - D:\OneDrive\Desktop\文言背诵\app\build\generated\ksp\release\kotlin
  ```
- **系统属性证据**：PowerShell 检查该目录：
  ```powershell
  Attributes : ReadOnly, Directory, Archive, ReparsePoint
  Mode       : dar--l
  ```
- **根因分析**：工程置于 OneDrive 同步路径下，Release 变体生成的 KSP 代码目录被系统 OneDrive 客户端挂载为符号重解析点 (`ReparsePoint`) 并标记为只读 (`ReadOnly`)。KSP 任务在尝试清空重写该目录时抛出 `IOException`。

### 1.3 单元测试包含真实外网 HTTP 请求与硬编码 API Key 泄露
- **定位代码**：
  - 测试用例：[`app/src/test/java/com/ancient/wenyan/TypeSafeDiagnosisTest.kt: 11-37`](app/src/test/java/com/ancient/wenyan/TypeSafeDiagnosisTest.kt#L11-L37)
  - 引擎实现：[`app/src/main/java/com/ancient/wenyan/domain/ai/TypeSafeDiagnosisEngine.kt: 33`](app/src/main/java/com/ancient/wenyan/domain/ai/TypeSafeDiagnosisEngine.kt#L33)
- **缺陷分析**：
  1. `TypeSafeDiagnosisTest` 在执行单元测试时，通过 `TypeSafeDiagnosisEngine.diagnose` 真实发起网络请求至 `https://api.typesafe.ai/v1/systemone`。
  2. 单元测试违背了 Hermetic/Offline 原则。一旦网络波动、断网或服务端限流，测试必定因 15 秒超时而挂起失败。
  3. `TypeSafeDiagnosisEngine.kt` 第 33 行明文硬编码了真实可用的 Bearer Token：
     ```kotlin
     private const val DEFAULT_API_KEY = "apikey_2254aa16e56061fe4f9c868636572350faf5_c76c129f715a90ed4d03581f969094d5670dc723133a40701257ecf9a8844e64"
     ```
     存在严重的凭据泄露安全风险，并且每次跑测试都在消耗在线 API 配额。

---

## 二、数据持久化与一致性缺陷

### 2.1 每日学习打卡记录未持久化至 Room SQLite (`daily_study_records`)
- **定位代码**：[`WenYanRepository.kt: 400-420`](app/src/main/java/com/ancient/wenyan/data/WenYanRepository.kt#L400-L420)、[`WenYanDao.kt: 61-79`](app/src/main/java/com/ancient/wenyan/data/db/dao/WenYanDao.kt#L61-L79)
- **代码调用链**：
  ```kotlin
  // WenYanRepository.kt line 400
  val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
  val currentDayCount = synchronized(dailyReviewMap) {
      val count = (dailyReviewMap[todayStr] ?: 0) + 1
      dailyReviewMap[todayStr] = count
      count
  }
  saveDailyReviewToPrefs(todayStr, currentDayCount)
  ```
- **缺陷分析**：
  - 用户每背诵完成一张卡片，系统仅更新内存中的 `dailyReviewMap` 并写入 SharedPreferences。
  - 全工程全局检索 `dailyRecordDao`，发现仅在冷启动时的 [`DatabaseMigrationHelper.kt: 120`](app/src/main/java/com/ancient/wenyan/data/db/DatabaseMigrationHelper.kt#L120) 导入过一次历史数据，后续没有任何一处代码调用 `database.dailyRecordDao().upsertRecord()`。
  - 一旦用户卸载重装、清理应用数据或仅依赖 Room 备份，SQLite 里的打卡足迹完全处于远古状态。

### 2.2 WebDAV 与本地 JSON 备份恢复丢失复习日志与打卡热力图数据
- **定位代码**：[`WebDavBackupManager.kt: 70-87, 96-136`](app/src/main/java/com/ancient/wenyan/domain/sync/WebDavBackupManager.kt#L96-L136)
- **缺陷分析**：
  1. `createBackupJson` 导出的 JSON 包含了 `cards` 与 `reviewLogs`。
  2. 但在 `restoreFromJson`（第 96-136 行）中，**仅解析了 `"cards"`，对 `"reviewLogs"` 视而不见**！
  3. 恢复后用户的复习日志条数为 0，导致背诵留存率重置为 100%，且因为复习日志条数不足 10 条，FSRS-5 核心寻优算法（`FSRSOptimizer`）彻底罢工。
  4. 此外，备份格式中甚至没有包含 `dailyReviewMap` 字段，跨设备同步后，连胜火焰（Streak）与打卡热力图直接归零。

### 2.3 伪废除 SharedPreferences 字符串拼接序列化 (Dual Storage Anti-Pattern)
- **定位代码**：[`WenYanRepository.kt: 873-884`](app/src/main/java/com/ancient/wenyan/data/WenYanRepository.kt#L873-L884)
- **缺陷分析**：
  - ROADMAP 1.2 声称已“彻底废除 SharedPreferences 管道符分割字符串拼接存储机制”。
  - 实际上，每次卡片提交评分时，`persistCardStates` 仍在遍历内存拼装大字符串并写入 `PREF_CARD_STATES_KEY`：
    ```kotlin
    val sb = StringBuilder()
    for (c in nonNewCards) {
        sb.append("${c.cardId}|${c.state.name}|${c.step ?: ""}|${c.stability}|${c.difficulty}|${c.elapsedDays}|${c.scheduledDays}|${c.reps}|${c.lapses}|${c.lastReviewTime ?: ""}|${c.dueTime}\n")
    }
    sp.edit().putString(PREF_CARD_STATES_KEY, sb.toString()).apply()
    ```
  - 这造成了双重存储（Room 与 SharedPreferences 双写），违背了单一数据源（Single Source of Truth, SSOT）原则。

### 2.4 Room 破坏性迁移隐患与未导出 Schema
- **定位代码**：[`AppDatabase.kt: 21, 39`](app/src/main/java/com/ancient/wenyan/data/db/AppDatabase.kt#L21)
- **缺陷分析**：
  - `exportSchema = false`，未配置 Schema 输出目录，无法利用 `androidx.room.testing:room-testing` 进行自动化迁移单元测试。
  - `fallbackToDestructiveMigration()`：一旦未来为了扩展功能将版本号从 1 升到 2 且漏写 Migration，Room 会无提示抹掉用户全部本地数据。

---

## 三、核心算法与业务逻辑缺陷

### 3.1 顽固漏卡 (Leech) 违反调度契约，未被隔离反而被前置优先刷屏
- **定位代码**：[`WenYanRepository.kt: 481-484`](app/src/main/java/com/ancient/wenyan/data/WenYanRepository.kt#L481-L484)
- **代码实现**：
  ```kotlin
  val reviewQueue = cardsStateMap.values
      .filter { it.state == CardState.REVIEW && it.dueTime <= endOfTodayMillis && it.cardId in allFlashcards }
      .sortedWith(compareByDescending<CardFsrsState> { it.lapses }.thenBy { it.dueTime })
  ```
- **缺陷分析**：
  - ROADMAP 3.2 规定：“当单张卡片累计遗忘达到阈值（连续 4 次评分 Again）时，自动标记为‘Leech 顽固卡’。**暂停在常规队列中频繁刷屏，单独归入‘难点攻坚顽固卡库’**”。
  - 代码中**完全没有过滤 `!it.isLeech`**；甚至排序逻辑采用了 `compareByDescending { it.lapses }`，使得遗忘次数最多、最难背诵的顽固卡（Lapses >= 4）**被顶在整个常规复习队列的最前列强制刷屏**，完全违背了熔断契约。

### 3.2 默写评测 LCS 算法无法识别错字替换 (`DiffType.SUBSTITUTION`)
- **定位代码**：[`RecitationDiffEngine.kt: 6-11, 84-135`](app/src/main/java/com/ancient/wenyan/domain/speech/RecitationDiffEngine.kt#L84-L135)
- **缺陷分析**：
  - 声明了枚举 `DiffType.SUBSTITUTION`（错字 / 形近错读），但在 DP 回溯中，遇到字符不等时仅做 `dp[i-1][j] >= dp[i][j-1]` 二选一判定，只能产生 `EXTRA`（多字）或 `MISSING`（漏字）。
  - `RecitationEvaluationResult` 中的 `wrongCount` 被硬编码为 `0`（第 130 行）。
  - 实际效果：当学生将“驽马十驾”误写为“弩马十驾”时，算法输出为“多了一个‘弩’字，少了一个‘驽’字”，既没有命中错字替换语义，又导致错误惩罚扣分加倍。

### 3.3 FSRS Fuzz 离散扰动因 HashCode 确定性而退化失效
- **定位代码**：[`FSRSEngine.kt: 81-87`](app/src/main/java/com/ancient/wenyan/domain/fsrs/FSRSEngine.kt#L81-L87)
- **缺陷分析**：
  - `applyFuzz` 采用 `val hash = kotlin.math.abs(cardId.hashCode())`。
  - 对于给定的卡片 ID，其哈希值完全固定。当一张卡片在后续复习周期中再次被计算到相同天数间隔时，计算出的 `delta` 始终一模一样，丧失了引入伪随机离散以防止同一篇目卡片在多轮复习中再次扎堆碰撞的初衷。应结合 `reps`（复习轮次）或当前系统日期生成伪随机扰动。

---

## 四、UI/UX、生命周期、主题与无障碍缺陷

### 4.1 深色模式下 `containerColor = StudyNavy` 低对比度白底白字缺陷
- **定位代码**：
  - [`FSRSConfigDialog.kt: 788`](app/src/main/java/com/ancient/wenyan/ui/components/FSRSConfigDialog.kt#L788)
  - [`FSRSConfigDialog.kt: 424-426`](app/src/main/java/com/ancient/wenyan/ui/components/FSRSConfigDialog.kt#L424-L426)
  - [`StreakBannerCard.kt: 172`](app/src/main/java/com/ancient/wenyan/ui/components/StreakBannerCard.kt#L172)
  - [`RandomReviewScreen.kt: 232`](app/src/main/java/com/ancient/wenyan/ui/screens/RandomReviewScreen.kt#L232)
- **缺陷分析**：
  - [`Theme.kt: 41, 59`](app/src/main/java/com/ancient/wenyan/ui/theme/Theme.kt#L41) 中，`DarkStudyNavy` 定义为 `Color(0xFFE2E8F0)`（浅亮灰色），原本是为文字标题暗色高亮设计的。
  - 但在 `FSRSConfigDialog.kt: 788`，“保存并生效”按钮直接指定了 `containerColor = StudyNavy`，按钮文字为默认纯白；在第 425 行，`FilterChip` 显式设置了 `selectedContainerColor = StudyNavy, selectedLabelColor = Color.White`。
  - 深色模式下底色为浅白灰（#E2E8F0），文字为纯白（#FFFFFF），对比度低于 1.2:1，严重破坏可读性。

### 4.2 多处关键背诵与专项练习屏幕在配置变更时丢失内部进度
- **定位代码**：
  - [`SnowballRecitationScreen.kt: 51-52`](app/src/main/java/com/ancient/wenyan/ui/screens/SnowballRecitationScreen.kt#L51-L52)：`currentStageIndex` 与 `maskHistory` 采用 `remember { mutableIntStateOf(0) }`。用户在背诵长篇古文滚雪球到第 10 联时，一旦旋转手机或切深色模式，进度直接被重置回第 1 联。
  - [`GaoKaoScenarioScreen.kt: 64-67`](app/src/main/java/com/ancient/wenyan/ui/screens/GaoKaoScenarioScreen.kt#L64-L67)：`currentIndex`、`userInput`、`isRevealed` 均使用 `remember`，旋转屏幕后作答文字与当前题序直接被清空。
  - [`ClozeRecitationScreen.kt: 57`](app/src/main/java/com/ancient/wenyan/ui/screens/ClozeRecitationScreen.kt#L57)：`revealedTokenIds` 为 `remember`，旋转后已刮开的词格全部重新遮蔽。
  - [`PracticeScreen.kt: 47, 62-64`](app/src/main/java/com/ancient/wenyan/ui/screens/PracticeScreen.kt#L47)：Tab 选定索引与抽测设置均为 `remember`，旋转后重置为默认第 0 项。

### 4.3 弹窗组件未规范使用 Material 3 标准容器
- **定位代码**：[`BookSelectionDialog.kt: 46`](app/src/main/java/com/ancient/wenyan/ui/components/BookSelectionDialog.kt#L46)、[`FSRSConfigDialog.kt: 108`](app/src/main/java/com/ancient/wenyan/ui/components/FSRSConfigDialog.kt#L108)、[`FeedbackPreferencesDialog.kt: 40`](app/src/main/java/com/ancient/wenyan/ui/components/FeedbackPreferencesDialog.kt#L40)、[`OnboardingTutorialDialog.kt: 106`](app/src/main/java/com/ancient/wenyan/ui/components/OnboardingTutorialDialog.kt#L106)、[`ReminderSettingsDialog.kt: 72`](app/src/main/java/com/ancient/wenyan/ui/components/ReminderSettingsDialog.kt#L72)
- **缺陷分析**：全部使用底层 `androidx.compose.ui.window.Dialog { Card(...) }` 方式自行堆叠，违背了 [AGENTS.md](AGENTS.md) 核心开发准则（“弹窗与输入选择：使用标准 M3 AlertDialog、ModalBottomSheet”），并且在平板或大屏横屏模式下缺少 M3 标准弹窗的自适应宽高约束与安全内边距保护。

### 4.4 结业文牒成就海报掌握度硬编码 100% 且缺少真实图像/PDF 导出
- **定位代码**：[`CertificateAndCopybookScreen.kt: 50, 87-108`](app/src/main/java/com/ancient/wenyan/ui/screens/CertificateAndCopybookScreen.kt#L50)
- **缺陷分析**：
  1. 调用 `CertificateAndCopybookGenerator.generateCertificate(article, 100f)` 时参数写死 `100f`，即便学生该篇课文尚未背过，也无条件展示“掌握度 100%”。
  2. 点击右上角分享按钮，仅构造了一个纯文本分享意图（`Intent.EXTRA_TEXT`），没有任何将文牒或田字格渲染为 Bitmap 图片保存或导出 PDF 的功能。

### 4.5 桌面微件 (AppWidget) 缺少深色主题适配与联动更新时机
- **定位代码**：[`widget_wenyan_today.xml: 6`](app/src/main/res/layout/widget_wenyan_today.xml#L6)、[`colors.xml: 5`](app/src/main/res/values/colors.xml#L5)、[`WenYanRepository.kt: 426, 991`](app/src/main/java/com/ancient/wenyan/data/WenYanRepository.kt#L426)
- **缺陷分析**：
  - 微件布局背景硬编码引用 `@color/xuan_paper_card` (`#FFFDF8`)，且工程中的 `res/values-night/` 目录下仅有 `themes.xml`，不存在 `colors.xml`。系统切深色模式时，桌面微件依然亮白刺眼。
  - 微件更新 `updateAllWidgets` 仅在卡片提交评分和重置数据时触发，切换课本教材范围、午夜跨天、系统开机广播时均未联动刷新。

### 4.6 Edge-to-Edge 边到边窗口 Insets 处理不完整
- **定位代码**：[`MainActivity.kt: 190`](app/src/main/java/com/ancient/wenyan/MainActivity.kt#L190)
- **缺陷分析**：全局仅在 `MainActivity.kt`、`FlipCardScreen.kt` 与 `GaoKaoScenarioScreen.kt` 处理了 Insets。其余页面如 `DashboardScreen`、`ChapterTreeScreen`、`SettingsScreen`、`PracticeScreen` 等均未正确结合 `safeDrawingPadding` 或 `Scaffold(contentWindowInsets = ...)`，在特定折叠屏设备、挖孔屏或开启三键导航栏的机型上存在被导航栏或状态栏遮挡的隐患。

---

## 五、架构违规与未完成升级点

### 5.1 伪 ViewModel 实例化反模式：`remember(repository) { ViewModel(repository) }`
- **定位代码**：
  - [`DashboardScreen.kt: 93-97`](app/src/main/java/com/ancient/wenyan/ui/screens/DashboardScreen.kt#L93-L97)
  - [`ChapterTreeScreen.kt: 42-44`](app/src/main/java/com/ancient/wenyan/ui/screens/ChapterTreeScreen.kt#L42-L44)
  - [`SettingsScreen.kt: 45-47`](app/src/main/java/com/ancient/wenyan/ui/screens/SettingsScreen.kt#L45-L47)
  - [`FootprintScreen.kt: 42-44`](app/src/main/java/com/ancient/wenyan/ui/screens/FootprintScreen.kt#L42-L44)
  - [`MainActivity.kt: 46, 259, 440, 473, 501`](app/src/main/java/com/ancient/wenyan/MainActivity.kt#L46)
- **典型实现反模式**：
  ```kotlin
  @Composable
  fun DashboardScreen(
      repository: WenYanRepository? = null,
      viewModel: DashboardViewModel = if (repository != null) {
          remember(repository) { DashboardViewModel(repository) }
      } else {
          hiltViewModel()
      },
      ...
  )
  ```
- **架构违规透视**：
  1. `MainActivity` 声明了 `@Inject lateinit var repository: WenYanRepository`，并将 `repository` 显式透传给每个 Composable。
  2. 由于 `repository` 始终非空，每个 Screen **无条件执行 `remember(repository) { ...ViewModel(repository) }`，`hiltViewModel()` 从未被执行**！
  3. 这些 ViewModel 只是挂在 Composable `remember` 下的普通堆对象，未挂载到 Activity / Navigation 的 `ViewModelStore` 中。Activity 重建时这些 ViewModel 连同其内部状态一并被销毁，完全丢失了 ViewModel 跨配置变更持久存在的核心能力。
  4. 该做法违背了 Clean Architecture、`android-viewmodel` 与 `android-architecture` 核心规范。

### 5.2 `FlipCardViewModel` 沦为未使用的死代码，且 `FlipCardScreen` 直接穿透 Repository
- **定位代码**：[`FlipCardViewModel.kt: 12-29`](app/src/main/java/com/ancient/wenyan/ui/viewmodel/FlipCardViewModel.kt#L12-L29)、[`FlipCardScreen.kt: 55-65`](app/src/main/java/com/ancient/wenyan/ui/screens/FlipCardScreen.kt#L55-L65)
- **缺陷分析**：
  - `FlipCardViewModel` 仅有 30 行，内部甚至写出了 `fun getRepository(): WenYanRepository = repository` 这种违背分层隔离的泄露反模式。
  - 在全工程任何 UI 处均未引用 `FlipCardViewModel`。`FlipCardScreen` 的参数中直接要求传入 `repository: WenYanRepository`，背诵卡片直接穿透至仓储层。

### 5.3 Domain 业务层污染 Android 框架依赖（`android.util.Base64`）
- **定位代码**：[`WebDavBackupManager.kt: 210`](app/src/main/java/com/ancient/wenyan/domain/sync/WebDavBackupManager.kt#L210)
- **缺陷分析**：
  - 在 `domain/sync/WebDavBackupManager.kt` 中引入了 `android.util.Base64.encodeToString(...)`。
  - 按照 Clean Architecture 与 `android-architecture` 规范，`domain` 领域层必须是纯 Kotlin 模块，严禁依赖 Android Framework 类库。若在没有 Robolectric 的 JVM 单元测试中执行 WebDAV 逻辑，该行代码会直接崩溃（`Method ... not mocked`）。应改用 Java 8 原生 `java.util.Base64` 或多平台纯 Kotlin 方案。

### 5.4 遗留废弃的死代码文件 (Zombie Code)
- **文件清单**：
  1. [`app/src/main/java/com/ancient/wenyan/ui/screens/RandomReviewScreen.kt`](app/src/main/java/com/ancient/wenyan/ui/screens/RandomReviewScreen.kt)（302 行）：在专项练习重构整合后已被完全废弃，全工程 0 处引用。
  2. [`app/src/main/java/com/ancient/wenyan/ui/components/StreakBannerCard.kt`](app/src/main/java/com/ancient/wenyan/ui/components/StreakBannerCard.kt)（215 行）：已被 `DashboardScreen` 自带的今日目标与连胜卡片替代，全工程 0 处引用。
  3. [`FlipCardViewModel.kt`](app/src/main/java/com/ancient/wenyan/ui/viewmodel/FlipCardViewModel.kt)（30 行）：全工程 0 处调用。

### 5.5 缺少声明式类型安全导航 (Navigation 3 / Compose Navigation 未落地)
- **定位代码**：[`MainActivity.kt: 64-176`](app/src/main/java/com/ancient/wenyan/MainActivity.kt#L64-L176)、[`app/build.gradle.kts: 144`](app/build.gradle.kts#L144)
- **现状分析**：
  - 虽然依赖了 `androidx.navigation:navigation-compose:2.7.7`，但 `MainActivity` 完全未使用 `NavHost` 或现代 Navigation 3。
  - 目前使用手工编写的 `OverlayScreen` 密封类与 `OverlayScreenStateHolder` 配合 `AnimatedContent` 进行切页模拟。返回栈依赖手写的单个 `BackHandler`，无法原生支持 Android 14/15 的预测性返回（Predictive Back）手势过渡动效与 DeepLink 路由解析。

### 5.6 路线图中已规划但尚未启动的特性 (Unstarted Roadmap Features)
1. **智能语音 ASR 评测 (Speech Recognition)**：ROADMAP 阶段二与目录 `domain.speech` 命名承诺了 ASR，但目前仅有字符级 LCS 文本匹配，无任何录音、实时音频采集或语音转文本实现。
2. **穿戴设备联动 (Wear OS)**：ROADMAP 阶段四规划，目前无任何 Wear OS 模块或数据同步通信协议。
3. **现代构建工具链与 Kotlin 2.x 升级**：当前停留在 AGP 8.4.1、Kotlin 1.9.23、编译插件 Compose Compiler 1.5.11，未升级至 Kotlin 2.x（官方 Compose 编译器 Gradle 插件）与 Android 15/16 SDK。

---

## 六、修复与升级优先级排期表

| 优先级 | 问题域 | 任务描述 | 影响文件 |
| :--- | :--- | :--- | :--- |
| **P0** | 构建与 CI | 修复 Gradle 标准测试类路径，移除 `afterEvaluate` 污染，废除 `runInProcessTests` 反射黑魔法 | `app/build.gradle.kts` |
| **P0** | 安全与单测 | 将硬编码 Bearer Token 移出源码，单测断网 Mock 处理，禁止联网消耗 API | `TypeSafeDiagnosisEngine.kt`, `TypeSafeDiagnosisTest.kt` |
| **P0** | 数据一致性 | 打卡记录在提交时同步写入 Room `daily_study_records`，避免单依赖 SharedPreferences | `WenYanRepository.kt` |
| **P0** | 数据恢复 | 补全 WebDAV 恢复中的 `reviewLogs` 与每日打卡数据解析，避免 FSRS 算法失效与连胜归零 | `WebDavBackupManager.kt` |
| **P0** | 核心调度 | 修复 Leech 顽固卡未隔离反被排序刷屏的违背契约 bug | `WenYanRepository.kt` |
| **P1** | 架构规范 | 重构伪 ViewModel 模式，全面采用 `hiltViewModel()`，杜绝 Activity 重建丢状态 | `DashboardScreen.kt`, `MainActivity.kt` 等 |
| **P1** | 状态持久化 | 将滚雪球、填空、情景默写等界面的内部进度由 `remember` 升级为 `rememberSaveable` | `SnowballRecitationScreen.kt` 等 |
| **P1** | 主题适配 | 修正深色模式下 `containerColor = StudyNavy` 白底白字缺陷，遵循 Material 3 Token | `FSRSConfigDialog.kt` 等 |
| **P1** | 架构解耦 | 消除 Domain 层对 `android.util.Base64` 的依赖，保持 JVM 纯净度 | `WebDavBackupManager.kt` |
| **P2** | 代码整洁 | 清理 `RandomReviewScreen.kt`、`StreakBannerCard.kt`、`FlipCardViewModel.kt` 等死代码 | 多处文件 |
| **P2** | UI 规范 | 将手写 `Dialog { Card }` 重构为官方 M3 `AlertDialog` / `ModalBottomSheet` | 5 个弹窗组件 |
| **P2** | 现代导航 | 引入声明式类型安全 Navigation 架构，支持系统预测性返回手势 | `MainActivity.kt` |
