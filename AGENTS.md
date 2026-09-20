# Agent Guidelines & Development Rules (AGENTS.md)

## 核心开发与 UI 设计准则 (Core UI & Component Principles)

在进行 Android 客户端开发时，严格遵循以下原则：

### 1. 优先使用现有成熟组件库与 UI 库 (Library-First Principle)
- **避免重复造轮子**：严禁自行手写、重新实现已有标准库能够提供的组件。优先复用官方标准库与成熟开源生态方案。
- **Jetpack Compose 官方组件优先**：
  - **骨架与容器**：统一采用 `Scaffold`、`TopAppBar` / `MediumTopAppBar` / `CenterAlignedTopAppBar`、`NavigationBar` / `NavigationBarItem`。
  - **卡片与表面**：使用 `Card`、`ElevatedCard`、`OutlinedCard`，禁止自行堆叠无语义的 `Box` + 手写复杂 `drawBehind` / `shadow`。
  - **列表条目与分割**：使用 `ListItem`、`HorizontalDivider`、`VerticalDivider`。
  - **交互按钮**：使用 `Button`、`FilledTonalButton`、`OutlinedButton`、`TextButton`、`IconButton`、`FloatingActionButton`。
  - **芯片与胶囊**：使用 `FilterChip`、`AssistChip`、`SuggestionChip`、`Badge`、`BadgedBox`，避免手写带有复杂 padding / border 的 Row 替代方案。
  - **弹窗与输入选择**：使用标准 M3 `AlertDialog`、`ModalBottomSheet`、`TimePicker` / `TimePickerDialog`、`DatePicker` / `DatePickerDialog`。
  - **进度与状态**：使用 `LinearProgressIndicator`、`CircularProgressIndicator`。
  - **搜索栏**：使用 M3 `SearchBar` / `DockedSearchBar`。
- **第三方成熟库选型**：
  - 粒子与庆祝动效：`nl.dionsegijn:konfetti-compose`
  - 图标资源：`androidx.compose.material:material-icons-extended`（Google Material Symbols）
  - 图片异步加载（如需）：`io.coil-kt:coil-compose`

### 2. 全面遵循 Material 3 Design 设计规范 (Material 3 Design Alignment)
- **Token 驱动颜色系统**：
  - 严禁在 Composable 内部随意硬编码随机 Hex 颜色。
  - 统一通过 `MaterialTheme.colorScheme`（如 `primary`, `onPrimary`, `surface`, `surfaceVariant`, `outline`, `error` 等）引用主题色彩。
- **阶梯式排版系统 (Typography)**：
  - 严格引用 `MaterialTheme.typography`（`displayLarge`, `headlineMedium`, `titleMedium`, `bodyMedium`, `labelSmall` 等），保持字体层级统一、规范、呼吸感一致。
- **形态与圆角规范 (Shapes)**：
  - 遵循 `MaterialTheme.shapes`（`small`, `medium`, `large`, `extraLarge`），符合 M3 的 Squircle 与标准弧度。
- **微交互与动效 (Motion)**：
  - 遵循 Material 3 动效时序与缓动曲线（如 `FastOutSlowInEasing`），状态过渡使用 Compose 标准 `AnimatedVisibility`、`animateContentSize`、`Crossfade`，手势支持系统级预测性返回（Predictive Back）。

### 3. 强制要求：安卓开发必须调用官方与 Awesome-Android-Skills 开发技能 (Mandatory Android Skills Execution)
在进行任何 Android 相关开发（包括架构设计、ViewModel/UI 编写、异步并发、网络请求、数据存储、构建配置、性能调优或测试）时，**必须严格调用并遵循**已配置的 Android 技能体系：

#### 技能矩阵与触发映射 (Skill Matrix)
| 开发任务领域 | 核心技能名称 | 核心规范与准则要求 |
| :--- | :--- | :--- |
| **整体架构与依赖注入** | `android-architecture` | 遵循 Clean Architecture 规范，划分 UI / Domain / Data 层；依赖注入统一使用 Hilt，严禁高层模块直接依赖底层具体实现。 |
| **UI 与状态管理** | `compose-ui`, `android-viewmodel` | 遵循 Unidirectional Data Flow (UDF)，ViewModel 暴露 `StateFlow`/`SharedFlow`，UI 层 Composable 保持 Stateless，状态必须提升 (State Hoisting)；严禁将 ViewModel 实例向下深层传递给纯展示组件。 |
| **主题与组件系统** | `compose-styles`, `android-adaptive` | 严格遵守 Material 3 Token 系统；自适应不同设备窗格（List-Detail, Two-Pane），严禁硬编码尺寸与固定 Hex 色值。 |
| **全面屏与手势适配** | `edge-to-edge` | 统一启用 Edge-to-Edge，精确处理 Status Bar、Navigation Bar、IME 软键盘 Insets，禁止产生任何系统栏遮挡或留白 bug。 |
| **导航架构** | `navigation-3`, `compose-navigation` | 采用类型安全导航（Type-Safe Navigation）与状态驱动导航，统一管理 BackStack 与返回手势处理。 |
| **异步并发与网络** | `android-coroutines`, `kotlin-concurrency-expert`, `android-retrofit` | 严禁硬编码 `Dispatchers.IO`（需依赖注入）；严禁用 GlobalScope；网络请求统一由 Retrofit + Coroutines 承载并做好统一异常捕获与重试机制。 |
| **数据持久化与离线支持** | `android-data-layer` | 采用 Repository Pattern 统一管理本地与远程数据，Room 数据库操作必须遵循单向数据流与响应式 Flow 输出，构建 Offline-First 架构。 |
| **图片异步加载** | `coil-compose` | 严格使用 `io.coil-kt:coil-compose` 的 `AsyncImage` 或 `SubcomposeAsyncImage`，配置 Crossfade 与占位/错误占位图。 |
| **无障碍标准** | `android-accessibility` | 所有可交互元素必须具备合法语义、至少 48dp 触摸区域（Touch Targets）与合规的对比度。 |
| **性能审计与优化** | `compose-performance-audit`, `r8-analyzer`, `gradle-build-performance` | 严禁无意义的重组风暴（利用 `derivedStateOf`、`remember` 稳定化与 Immutable 标签）；构建脚本开启配置缓存与优化。 |
| **测试与基建** | `android-testing`, `testing-setup`, `android-emulator-skill` | 编写单元测试（JUnit5, MockK, Turbine）、Compose 界面交互测试（Compose Test Rule），确保核心业务状态可测。 |
| **多媒体与系统特性** | `camerax`, `media3-cast-integration`, `android-intent-security` | 遵循最新的 CameraX、Media3 API 与 Intent 安全防护准则（组件导出控制、敏感权限最小化）。 |

#### 执行原则与检查机制 (Pre-Execution Protocol)
1. **先查技能，后写代码**：在编写或重构相关 Android 代码前，必须先调用或查阅对应的 `SKILL.md`，确认最新的官方与行业推荐模式。
2. **拒绝陈旧反模式 (Anti-Patterns)**：严格禁止使用已被 Android 官方废弃或强烈不推荐的模式（如已弃用的旧版 NavController 字符串路由传参反模式、裸用 Thread/Handler、硬编码调度器、阻塞主线程等）。

