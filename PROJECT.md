# Project: Classical Chinese FSRS Recitation Android App (文言背诵)

## Architecture
- **Platform**: Native Android (Kotlin + Jetpack Compose + Material 3)
- **Minimum SDK**: 26 (Android 8.0), **Target/Compile SDK**: 34 (Android 14)
- **Toolchain**: Microsoft OpenJDK 17 LTS, AGP 8.4.1, Gradle 8.6, Kotlin 1.9.23, Compose BOM 2024.05.00
- **Aesthetic**: Ancient Chinese Cultural Aesthetic (宣纸底色 #F7F4EB, 徽墨灰黑 #232120, 丹砂朱红 #9E2A2B, 苍竹幽绿 #2C4F3D, 经折线装卡片边饰, 朱砂印章)
- **Data Layer**: Android Room 2.6.1 SQLite Database preloaded from assets (`wenyan_recitation.db`)
- **Algorithm Engine**: Pure Kotlin FSRS (Free Spaced Repetition Scheduler v4.5/v5) with 19 parameters, 4 ratings, real-time interval prediction
- **Presentation Layer**: MVI / MVVM with Kotlin Coroutines & StateFlow, Navigation Compose

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Android Project Scaffolding | Gradle multi-file setup, Aliyun Maven mirrors, local.properties SDK configuration | M0 | Survey Env |
| 2 | 11 Modules Organization | Complete 11 modules: 必修上/下, 选择性必修上/中/下, 选修全集, 各本古诗词诵读 | M1 | ORIGINAL_REQUEST R1 |
| 3 | 100 Classical Works Catalog | 100 articles with title, author, dynasty, genre, full text, and GaoKao72 flag | M1 | ORIGINAL_REQUEST R1 |
| 4 | Couplet & Clause Segmentation | Couplets for poetry, 12-35 char clauses for prose, key quotes tagged | M1 | ORIGINAL_REQUEST R1 |
| 5 | Room Offline Asset DB | Prepackaged SQLite database `wenyan_recitation.db` via Room `createFromAsset` | M1 | ORIGINAL_REQUEST R1 |
| 6 | FSRS Mathematical Engine | Formulas for R(t,S), S0, D0, S'_r, S'_f, mean reversion, interval inversion | M2 | ORIGINAL_REQUEST R2 |
| 7 | 4-State Lifecycle Machine | New, Learning, Review, Relearning state machine with 16 transition rules | M2 | ORIGINAL_REQUEST R2 |
| 8 | 4-Tier Queue Scheduler | Daily queue ordering: Relearning -> Learning -> Review (overdue first) -> New | M2 | ORIGINAL_REQUEST R2 |
| 9 | Live Button Interval Prediction | Real-time interval calculations displayed on Again, Hard, Good, Easy buttons | M2 | ORIGINAL_REQUEST R2 |
| 10 | Review History Logging | Persistent ReviewLogEntity logging rating, stability, difficulty, review timestamp | M2 | ORIGINAL_REQUEST R2 |
| 11 | FSRS Unit Test Suite | Comprehensive JUnit tests for formulas, state machine, and edge cases | M2 | ORIGINAL_REQUEST R2 |
| 12 | Ancient Chinese Theme System | Material 3 Xuan paper color palette, typography, seal stamp components | M3 | ORIGINAL_REQUEST R5 |
| 13 | Single-Sentence Flip Cards | 3D card flip animation, front prompt, back full quote + annotation, 4 ratings | M3 | ORIGINAL_REQUEST R4 |
| 14 | Progressive Cloze / Masking | Full-text view with 3-level masking (keywords, phrases, lines) & tap-to-reveal | M3 | ORIGINAL_REQUEST R4 |
| 15 | Chapter Tree Navigation | 11-module textbook tree with mastery progress indicators (% mastered, % due) | M3 | ORIGINAL_REQUEST R3 |
| 16 | Cross-Chapter Random Pool | One-click due cards review pool or random practice pool across modules | M3 | ORIGINAL_REQUEST R3 |
| 17 | E2E Integration & Verification | Seamless integration of UI, DB, and FSRS engine; end-to-end verification | M4 | ORIGINAL_REQUEST R5 |
| 18 | Debug APK Build (`assembleDebug`) | Successful `./gradlew assembleDebug` producing installable APK artifact | M4 | ORIGINAL_REQUEST R5 |
| 19 | Git Initialization & .gitignore | Clean git repo setup ignoring build caches, .gradle, local.properties | M5 | ORIGINAL_REQUEST R6 |
| 20 | GitHub Repository Release | `gh repo create` under `yulaoshizuikeai`, push code, verify remote | M5 | ORIGINAL_REQUEST R6 |
| 21 | Comprehensive README.md | Project architecture, screenshots/ASCII UI, build steps, FSRS math docs | M5 | ORIGINAL_REQUEST R6 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| E2E | E2E Testing Track | Requirement-driven test harness & multi-tier test cases (Tiers 1-4) | none | IN_PROGRESS |
| M0 | Scaffolding & Build Setup | Android project files, Gradle 8.6, AGP 8.4.1, Aliyun mirrors | none | PLANNED |
| M1 | Content Library & Offline DB | 11 modules, 100 articles, Room preloaded SQLite database | M0 | PLANNED |
| M2 | FSRS Engine & Unit Tests | FSRS v4.5/v5 engine, 4 ratings, scheduler, unit tests | M0 | PLANNED |
| M3 | Compose UI & Recitation Modes | Ancient theme, flip cards, progressive cloze, chapter navigation | M1, M2 | PLANNED |
| M4 | Integration & APK Build | Full app wiring, E2E test execution, `./gradlew assembleDebug` | M3, E2E | PLANNED |
| M5 | Git Repo & GitHub Release | Git commit, `gh repo create` under `yulaoshizuikeai`, push, README | M4 | PLANNED |

## Interface Contracts
### Content DB ↔ Presentation / UI
- `ArticleRepository.getModulesWithArticles(): Flow<List<ModuleWithArticles>>`
- `ArticleRepository.getArticleWithSegments(articleId: String): Flow<ArticleWithSegments>`
- `ArticleRepository.getArticlesByModule(moduleId: String): Flow<List<ArticleEntity>>`
- `ArticleRepository.getGaoKao72Articles(): Flow<List<ArticleEntity>>`

### FSRS Engine ↔ Data Layer
- `FSRSEngine.nextState(card: CardEntity, rating: Rating, now: Long): NextStateResult`
  - Returns: `newCardState: CardEntity`, `reviewLog: ReviewLogEntity`, `nextIntervalDays: Double`
- `FSRSEngine.predictIntervals(card: CardEntity, now: Long): Map<Rating, String>`
  - Returns formatted preview strings: `[AGAIN: "1分", HARD: "10分", GOOD: "3天", EASY: "15天"]`

### Card Scheduler ↔ Study Modes
- `CardRepository.getDueCards(filter: CardFilter): Flow<List<CardWithVerse>>`
- `CardRepository.getRandomCards(filter: CardFilter, limit: Int): Flow<List<CardWithVerse>>`
- `CardRepository.recordReview(cardId: Long, rating: Rating)`

## Code Layout
```
文言背诵/
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── assets/
│   │   │   │   └── database/
│   │   │   │       └── wenyan_recitation.db   (Preloaded SQLite DB)
│   │   │   ├── java/com/ancient/wenyan/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── WenYanApp.kt
│   │   │   │   ├── data/                      (Room DB, Entities, DAOs, Seed scripts)
│   │   │   │   ├── domain/                    (FSRS Engine, Models, Repositories)
│   │   │   │   ├── presentation/              (ViewModels, MVI State)
│   │   │   │   └── ui/                        (Compose UI, Theme, FlipCard, ClozeView, ChapterTree)
│   │   │   └── res/                           (Icons, Themes, Values)
│   │   └── test/java/com/ancient/wenyan/      (JUnit Tests: FSRS math, scheduler, DB)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties
├── gradlew
├── gradlew.bat
├── .gitignore
├── README.md
├── PROJECT.md
└── TEST_INFRA.md
```
