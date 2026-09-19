# E2E Test Infra: Classical Chinese FSRS Recitation App

## Test Philosophy
- Opaque-box, requirement-driven. No dependency on implementation design.
- Verification channels: Gradle test runner, database query assertions, pure Kotlin execution scripts, and APK package inspection.
- Methodology: Category-Partition + Boundary Value Analysis + Pairwise Interactions + Real-World Student Workload Scenarios.

## Feature Inventory Mapping
| # | Feature | Requirement | Tier 1 | Tier 2 | Tier 3 |
|---|---------|-------------|:------:|:------:|:------:|
| 1 | 11 Curriculum Modules | ORIGINAL_REQUEST R1 | 5 | 5 | ✓ |
| 2 | 100 Articles & Segmentation | ORIGINAL_REQUEST R1 | 5 | 5 | ✓ |
| 3 | Room SQLite Offline Asset | ORIGINAL_REQUEST R1, R5 | 5 | 5 | ✓ |
| 4 | FSRS v4.5/v5 Formulas | ORIGINAL_REQUEST R2 | 5 | 5 | ✓ |
| 5 | 4-State Lifecycle Machine | ORIGINAL_REQUEST R2 | 5 | 5 | ✓ |
| 6 | Real-time Interval Predictions | ORIGINAL_REQUEST R2 | 5 | 5 | ✓ |
| 7 | Review Logging & Due Queue | ORIGINAL_REQUEST R2 | 5 | 5 | ✓ |
| 8 | Single-Sentence Flip Cards | ORIGINAL_REQUEST R4 | 5 | 5 | ✓ |
| 9 | Progressive Cloze / Masking | ORIGINAL_REQUEST R4 | 5 | 5 | ✓ |
| 10 | Chapter Tree Navigation | ORIGINAL_REQUEST R3 | 5 | 5 | ✓ |
| 11 | Cross-Chapter Random Pool | ORIGINAL_REQUEST R3 | 5 | 5 | ✓ |
| 12 | Debug APK Assembly (`assembleDebug`) | ORIGINAL_REQUEST R5 | 5 | 5 | ✓ |
| 13 | Git & GitHub Remote Release | ORIGINAL_REQUEST R6 | 5 | 5 | ✓ |

## Test Architecture
- **E2E Test Runner**: Standalone JVM test runner or Gradle test suite (`./gradlew test`) executing JVM-side integration and unit tests, plus automated scripts verifying APK outputs and Git/GitHub status.
- **Directory Layout**:
  `app/src/test/java/com/ancient/wenyan/`
  `app/src/test/java/com/ancient/wenyan/fsrs/`
  `app/src/test/java/com/ancient/wenyan/content/`
  `app/src/test/java/com/ancient/wenyan/e2e/`

## Real-World Application Scenarios (Tier 4)
| # | Scenario | Features Exercised | Complexity |
|---|----------|--------------------|------------|
| 1 | Gao Kao Student Daily Due Review Session | 11 Modules, Due Queue, 4 Ratings, FSRS State Transitions, DB Persistence | High |
| 2 | Long Prose Progressive Cloze Study (《赤壁赋》) | 100 Articles, Semantic Segmentation, 3 Masking Levels, Reveal Check | High |
| 3 | Cross-Module Cramming (必修上 + 必修下 Random Pool) | Cross-Chapter Random Pool, Card Filter, Real-Time Prediction Badges | Medium |
| 4 | Initial First Launch Offline Smoke Test | Zero-network startup, Asset DB copy, Zero-latency query of all 11 modules | High |
| 5 | Full Repository Release & Packaging Verification | `./gradlew assembleDebug`, APK size & manifest verification, `gh repo view` | High |

## Coverage Thresholds
- Tier 1: ≥5 test cases per feature (13 features * 5 = ≥65 tests)
- Tier 2: ≥5 boundary cases per feature (≥65 boundary tests)
- Tier 3: Pairwise combinations of state changes and user interactions
- Tier 4: 5 comprehensive real-world workload application tests
- Total threshold: ≥140 test assertions
