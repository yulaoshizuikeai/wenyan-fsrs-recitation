# 文言背诵 (WenYan Recitation) - 设计规范 (DESIGN.md)

> **设计定位**：极简现代学习风（Vercel 级纯粹灰阶质感 + Anki/Readwise 学习蓝点缀 + 现代无衬线排版 + 纯正百词斩极简节奏）。  
> **核心原则**：告别泛黄宣纸风，拥抱现代高对比、清爽明朗、零视觉杂讯的学习生产力界面；保留印章与文言典雅神韵，但通过纯粹现代骨架承载。

---

## 一、 视觉设计哲学 (Design Philosophy)

1. **Vercel / Raycast 式极简灰阶**：
   - 彻底摒弃米黄/做旧宣纸底色，改用纯净白底 `#FFFFFF` 与极浅灰基底 `#F8FAFC`。
   - 边框采用细致的单像素发丝线（1dp `#E2E8F0`），卡片平铺无厚重投影，仅保留微浮雕阴影（0-1dp elevation）。
2. **极简学习蓝（Study Navy / Tech Blue）**：
   - 主行动色（Primary CTA）采用深沉沉稳的深海学习蓝 `#0F172A` / `#1E293B` 或 科技海蓝 `#2563EB`。
   - 交互强调色采用纯正克制的 `#3B82F6` 与高光底色 `#EFF6FF`。
3. **百词斩活力火种（Streak Flame Accent）**：
   - 打卡连胜使用高饱和活力橙红 `#F97316` / `#EF4444`，与冷调蓝白形成极致对比，强化打卡自豪感。
4. **全套现代无衬线（Modern Sans-Serif）**：
   - 替换 Serif（衬线字体），采用系统现代无衬线（Roboto / Inter / MiSans），排版呼吸感强，行距紧凑利落。
5. **英文状态标签（Modern Status Tags）**：
   - 状态采用行业标准规范：`NEW`、`LEARNING`、`REVIEW`、`RELEARNING`，搭配低饱和胶囊微标签（Capsule Badges）。

---

## 二、 调色板系统 (Color Palette)

### 1. 基础表面与背景色 (Surfaces & Backgrounds)
| Token 名称 | 色值 Hex | 用途说明 |
| :--- | :--- | :--- |
| `BgCanvas` | `#F8FAFC` (Slate 50) | 全局底层背景，清爽现代冷灰底 |
| `BgSurface` | `#FFFFFF` | 卡片主体底色、弹窗容器底色 |
| `BgSurfaceMuted`| `#F1F5F9` (Slate 100) | 次级区域背景、徽章底色、进度底槽 |
| `BorderSubtle` | `#E2E8F0` (Slate 200) | 卡片 1dp 发丝边框、分隔线 |
| `BorderFocus` | `#CBD5E1` (Slate 300) | 交互高亮与按压状态描边 |

### 2. 文字与排版灰阶 (Typography Contrast)
| Token 名称 | 色值 Hex | 用途说明 |
| :--- | :--- | :--- |
| `TextPrimary` | `#0F172A` (Slate 900) | 一级标题、大号连胜数字、核心字句 |
| `TextSecondary`| `#475569` (Slate 600) | 二级标题、主要注解、列表正文 |
| `TextTertiary` | `#94A3B8` (Slate 400) | 次要说明、辅助标签、时间戳 |

### 3. 功能交互色 (Brand & Accents)
| Token 名称 | 色值 Hex | 用途说明 |
| :--- | :--- | :--- |
| `StudyBluePrimary`| `#1E293B` (Dark Navy) | 核心学习主按钮背景（沉稳如墨，坚实可靠） |
| `StudyBlueAccent` | `#2563EB` (Blue 600) | 超链接、聚焦态、进行中指示 |
| `StudyBlueLight`  | `#EFF6FF` (Blue 50) | 选中态胶囊底色、复习量指示底色 |
| `FlameOrange`     | `#F97316` (Orange 500)| 连胜天数打卡火焰、连续纪录强调 |
| `SuccessGreen`    | `#10B981` (Emerald 500)| 已掌握/完成、今日打卡已完成标记 |
| `AlertRed`        | `#EF4444` (Red 500) | 到期复习提醒、印章边缘高光 |

---

## 三、 排版与字体系统 (Typography & Fonts)

- **字体族**：统一采用 `FontFamily.SansSerif`（替代原有的 `FontFamily.Serif`）。
- **字阶比例**：
  - **Hero Streak 数字**：`40sp` ~ `44sp`，`FontWeight.Black`，紧凑字距 `-1.sp`。
  - **一级页面标题**：`22sp`，`FontWeight.Bold`，颜色 `TextPrimary`。
  - **模块卡片标题**：`16sp`，`FontWeight.SemiBold`。
  - **常规正文与诗文释义**：`14sp`，`FontWeight.Normal`，行高 `20.sp`。
  - **标签与胶囊数据**：`11sp` ~ `12sp`，`FontWeight.Medium`，全大写字母紧凑排版。

---

## 四、 核心组件设计规范 (Component Specifications)

### 1. 百词斩极简打卡横幅卡片 (Streak Banner Card)
- **容器**：`RoundedCornerShape(16.dp)`，底色 `#FFFFFF`，边框 `1.dp #E2E8F0`。
- **左侧**：
  - 双重视觉焦点：`FlameOrange` (`#F97316`) 火焰图标 + 超大数字（如 `14` 天），紧随 `DAYS STREAK` 微小副标。
  - 若 `streak == 0`，显示灰色火苗底座与鼓励文案（`READY TO START`）。
- **右侧**：
  - 动态状态芯片（Status Pill）：
    - 未打卡状态：显示深蓝小按钮「去打卡 ➔」，点击直接进入今日复习队列。
    - 已打卡状态：显示绿色勾选「TODAY DONE ✓」，微底色 `#ECFDF5`。

### 2. 今日看板 (Daily Stats Board)
- 放弃古典宣纸框格，改为 4 列等宽无缝现代统计列：
  - `DUE` (到期)：数字醒目深红色或深蓝色。
  - `LEARNING` (学习中)：浅蓝色或石板灰。
  - `REVIEW` (已掌握)：祖母绿。
  - `RETENTION` (保持率)：以百分比精简呈现（如 `96%`）。

### 3. 主行动按钮 (Primary Review CTA)
- 采用深海军蓝 `#0F172A`，高度 `56.dp`，倒角 `12.dp`。
- 文本字重 `FontWeight.Bold`，白色文字，带弹性点击微缩放动画（`scale: 0.96f`）。

### 4. 研墨足迹 GitHub 热力图 (Activity Heatmap)
- 采用冷色调纯净像素风格：
  - 等级 0（空）：`#F1F5F9`
  - 等级 1：`#BFDBFE` (Blue 200)
  - 等级 2：`#60A5FA` (Blue 400)
  - 等级 3：`#2563EB` (Blue 600)
  - 等级 4：`#1E3A8A` (Blue 900)
- 格点圆角 `2.5.dp`，间距 `3.dp`，视觉风格与 GitHub 个人主页 Contribution Graph 100% 对齐。

### 5. 功能导航列表 (Minimal Nav Cards)
- 移除复杂的古典图标外衬底，采用统一的浅灰白底色卡片，内嵌 20dp 极简双色/纯色线性图标，右侧搭配精致轻量 ChevronRight 图标。

---

## 五、 现代设置与提醒交互 (Settings & Reminders)
- **入口**：在功能导航卡片底部常设「打卡提醒设置 (Daily Reminder)」。
- **时间设置弹窗**：采用标准现代 Material 3 `TimePickerDialog`，默认推荐每日 `21:00`。
- **通知机制**：通过 Android `WorkManager` 每日准时唤醒，发出极简无干扰的常驻打卡推送。
