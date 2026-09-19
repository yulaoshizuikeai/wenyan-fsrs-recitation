# 文言背诵 (WenYan Recitation)

> 基于 **FSRS-5 (Free Spaced Repetition Scheduler)** 间隔重复记忆算法的高中语文古诗文原生安卓闪卡背诵记忆系统。
> 完整收录古文岛高中课标全部 11 个教材模块共 100 篇经典诗文（包含教育部高考必背 72 篇），提供双轨记忆体验（单句翻转闪卡 + 整篇渐进遮挡背诵）。

---

## 🌟 核心特性 (Features)

### 1. 📚 完整高中语文课标体系（11 册教材 · 100 篇经典篇目）
- **必修阶段**：必修上册（15篇）、必修上册古诗词诵读（4篇）、必修下册（13篇）、必修下册古诗词诵读（4篇）。
- **选择性必修阶段**：选必上册（6篇）、选必上册诵读（4篇）、选必中册（4篇）、选必中册诵读（4篇）、选必下册（13篇）、选必下册诵读（4篇）。
- **选修阶段**：中国古代诗歌散文欣赏（29篇）。
- **高考 72 篇专项**：精准标注教育部统编教材必背 72 篇古诗文，支持一键专项抽查背诵。

### 2. 🧠 FSRS-5 间隔重复记忆引擎
- 采用先进的 **FSRS (Free Spaced Repetition Scheduler v5)** 记忆模型，克服传统 SM-2 算法局限。
- 维护每张闪卡的三维记忆指标：
  - **稳定性 (Stability, $S$)**：记忆保留率降至 90% 所经历的时间。
  - **难度 (Difficulty, $D$)**：诗文句子的内在记忆难度，取值 $[1.0, 10.0]$。
  - **可提取性 (Retrievability, $R$)**：根据艾宾浩斯遗忘幂律曲线实时计算当下记忆保留概率。
- 四档评分入口实时预测下次复习间隔：
  - **Again (重来)**：进入短期重学队列（如 $<10$ 分钟）。
  - **Hard (困难)**：记忆较为生疏，适度延长时间（如 $1$ 天）。
  - **Good (良好)**：正常回忆成功，动态推进复习间隔（如 $3$ 天）。
  - **Easy (简单)**：记忆牢固，大幅跳增间隔（如 $15$ 天）。

### 3. 🎋 双轨交互记忆模式
- **单句/名句翻转闪卡 (Flip Flashcard)**：
  - 正面呈现出句题面、朝代与体裁提示；
  - 轻触或点击“显示答案”执行平滑翻转，背面展现对句全句、重点字词注解与白话译文；
  - 底部四档 FSRS 评分按钮附带即时预估间隔徽标。
- **长篇文言渐进遮挡背诵 (Progressive Cloze)**：
  - 针对《劝学》《赤壁赋》《离骚》《师说》《阿房宫赋》等长篇文赋；
  - 支持 **5 级遮挡难度**阶梯式背诵：
    - `L0 原文`：全篇无遮挡诵读；
    - `L1 关键词`：核心名句与高频考点挖空（⟦ __ ⟧）；
    - `L2 半句`：出句提示、对句遮挡；
    - `L3 首字`：保留每句首字骨架与标点，辅助回想；
    - `L4 全盲`：全篇下划线遮挡，自测通篇默写。
  - 支持轻触右上角随时对照原文即时核验。

### 4. 🔀 多维学习路径
- **章节篇目树状学习**：按教材册次逐课精细背诵，实时显示掌握百分比进度条。
- **跨篇目随机背诵**：自由勾选教材范围（必修/选必/选修），设定单次抽卡数量（10/20/30/50 题），高频交叉打乱强化。
- **今日智能复习队列**：每日一键抽取到期复习卡片与新卡配额。

### 5. 🎨 典雅中国古典文韵视觉
- 设计语言融合传统文化意象：**宣纸底色 (#F7F4EB)**、**徽墨黑 (#232120)**、**丹砂朱红 (#9E2A2B)** 与 **苍竹青绿 (#2C4F3D)**。
- 界面点缀经折线装边饰与“熟读成诵”朱砂印章，纯净无广告干扰，全功能 100% 离线可用。

---

## 📱 界面架构与技术栈

- **平台**：Native Android (Min SDK 26, Target/Compile SDK 34)
- **开发语言**：Kotlin 1.9.23 + Java 17 LTS
- **UI 框架**：Jetpack Compose (Material 3 BOM 2024.05.00)
- **本地存储**：Room 2.6.1 + 本地内置完整教材离线数据源
- **异步处理**：Kotlin Coroutines & StateFlow MVI 架构

```
app/src/main/java/com/ancient/wenyan/
├── MainActivity.kt               // 应用入口与全局路由控制
├── WenYanApp.kt                  // Application 基础类
├── data/
│   ├── CurriculumDataSource.kt  // 11 册教材与 100 篇诗文完整全量离线数据库
│   └── WenYanRepository.kt      // 统一状态仓储与复习队列调度
├── domain/
│   ├── cloze/
│   │   └── ClozeEngine.kt       // 5 级渐进遮挡生成引擎
│   ├── fsrs/
│   │   ├── FSRSEngine.kt        // FSRS-5 核心数学算法
│   │   ├── FSRSModels.kt        // 状态、日志与调度数据结构
│   │   ├── CardState.kt         // 卡片生命周期状态枚举
│   │   └── Rating.kt            // 4 档评分定义
│   └── model/
│       └── CurriculumModels.kt  // 诗文篇目、段落分句与闪卡领域模型
└── ui/
    ├── screens/
    │   ├── DashboardScreen.kt   // 首页仪表盘与记忆看板
    │   ├── ChapterTreeScreen.kt // 11 册教材章节导航
    │   ├── FlipCardScreen.kt    // 闪卡翻转背诵界面
    │   ├── ClozeRecitationScreen.kt // 渐进遮挡背诵界面
    │   └── RandomReviewScreen.kt // 跨篇随机背诵配置页
    └── theme/
        ├── Color.kt
        └── Theme.kt             // 宣纸古典主题系统
```

---

## 🛠 构建与运行指南 (Build & Run)

### 前置环境
- JDK 17 (Microsoft OpenJDK 或 Temurin)
- Android SDK (API 34)

### 编译 APK
在项目根目录下执行：
```bash
./gradlew assembleDebug
```
构建产物位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

### 执行单元测试
项目包含 80 个高覆盖度端到端与边界测试用例（覆盖教材 11 册模块、100 篇目解析、FSRS 数学引擎、状态流转、遮挡等级等）：
```bash
./gradlew testDebugUnitTest
```

---

## 🧮 FSRS 算法数学原理

记忆保留率 $R(t, S)$：
$$R(t, S) = \left(1 + \text{factor} \cdot \frac{t}{S}\right)^{-0.5}, \quad \text{where } \text{factor} = 0.9^{-2} - 1 \approx 0.23457$$

下次复习间隔 $I(S, R_{\text{target}})$：
$$I(S) = \frac{S}{\text{factor}} \cdot \left(R_{\text{target}}^{-2} - 1\right)$$

难度与稳定性迭代依据艾宾浩斯复习日志实时回归调整，确保长篇文言文长久牢固掌握。

---

## 📄 开源许可证

本项目基于 MIT License 开源协议。
