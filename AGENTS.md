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
