# Evidence — v1.0.5

## 1. Lineage / scope

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- Feedback read before implementation: `feedback/feedback_v1.0.5.md`
- Required predecessor feedback re-read and compared: `feedback/feedback_v1.0.4.md`
- v1.0.5 feedback/base commit: `b3deb7dd6c34465287daefbfb0d23b5e9dd0de4c`
- Main implementation commit: `f3bf84ffbb87889d1a1e13175d13e5848048cf50`
- Compile-fix commit: `4d351bc2bb44f338092d1e4ea8761669213df460`
- Static interpolation-fix commit: `8776be62b41f48369589f08b658bd4322b2743ad`
- Android version: `versionName = "1.0.5"`, `versionCode = 6`
- Scope: feedback items 1~8 implemented; additional UI recommendations recorded below.

## 2. Item 1 — unreachable range/retry Error branches

**Status: PASS**

### Contract cleanup

`LottoRemoteDataSource.fetchDrawRange` and `fetchDraws` now return `DrawFetchReport` directly rather than `AppResult<DrawFetchReport>`.

Rationale:
- once a requested range/target set is already known, individual request failures are aggregate data, not a range-level transport decision;
- per-draw failures are represented in `failedDrawNos`;
- callers can persist successes and retry failures without unreachable `AppResult.Error`/Loading branches.

Code:
- `core/network/.../LottoRemoteDataSource.kt`
  - blob: `114fb0b3486d309f26bbb72942dce4a527c4a9fb`
- `core/network/.../LottoRemoteDataSourceImpl.kt`
  - blob: `c5189916c502e9f7b71109bfcb79583c8cad951e`

Repository simplification:
- `fetchAndSaveLatestDraws` directly receives a `DrawFetchReport`, persists success rows, returns success count.
- `retryDraws` directly receives a `DrawFetchReport`, persists successes, returns `DrawSyncReport`.
- unreachable Error/Loading branches for these two aggregate operations are removed.

Code:
- `core/data/.../LottoRepositoryImpl.kt`
  - blob: `349bd070fd5c6423ee8ff45f8b7115808fe269c5`

Regression:
- `LottoRepositoryImplTest.retryDrawsUsesAggregateReportWithoutUnreachableErrorBranch`
- blob: `b6f4ae8781af74aea655b542cf99daaa388bef45`

## 3. Item 2 — full synchronization download optimization

**Status: PASS**

### Incremental exact-probe fast path

When a local latest draw already exists, `fetchAllDraws(existingMaxDrawNo)` no longer begins by downloading the official `"all"` payload.

New policy:
1. Probe exact draw `existing + 1`.
2. If it exists, use exponential probing to establish an upper boundary.
3. Use binary search between the last existing draw and first missing draw.
4. Once the latest boundary is known, fetch only the missing range with the existing bounded parallel path.
5. If probing fails for a transport/parser/server reason rather than a normal missing-draw boundary, fall back to the established official full-list path.
6. Initial installation/no local max still uses the full official list.
7. Even on full-list fallback, only entries newer than the local max are parsed/upserted.

This preserves the robust old path while avoiding full historical re-download during ordinary incremental sync.

Code:
- `LottoRemoteDataSourceImpl.discoverLatestIncrementally`
- `LottoRemoteDataSourceImpl.fetchAllFromOfficialList`
- blob: `c5189916c502e9f7b71109bfcb79583c8cad951e`

Tests:
- `existingDatabaseUsesIncrementalDiscoveryInsteadOfDownloadingAllDraws`
  - local 100, server latest 102
  - success rows 101/102
  - `allQueryCalls == 0`
  - result page not called
- `existingDatabaseWithNoNewDrawAvoidsAllDownload`
  - local/server latest both 100
  - empty delta
  - `allQueryCalls == 0`

Test file:
- `core/network/.../LottoRemoteDataSourceImplTest.kt`
- blob: `09a83a7ec23841708c87ec6b8953c11407d3870b`

## 4. Item 3 — deterministic/reproducible analysis seed

**Status: PASS**

Previous behavior:
- `Random().nextLong()` created a new non-deterministic seed for each analysis.

New reproducibility policy:
- seed is deterministically derived with SHA-256 from every input used by the recommendation path:
  - algorithm version
  - frequency/consecutive/parity weights
  - EV on/off
  - effective recent-N
  - game count
  - candidate count
  - sorted target draw numbers
  - sorted winning numbers
  - first-prize value/null marker
- first 8 SHA-256 bytes are converted to the persisted `Long` seed.
- same data + same effective config + same algorithm version → same seed and same generated games.
- changed effective config/data/version → seed changes.

Code:
- `AnalysisEngineImpl.deterministicSeed`
- blob: `5e1107bd620ebeadef6954d9c58f45e40901fd48`

Algorithm identifier was advanced:
- `lotto-analysis-deterministic-seed-1.1`
- `Constants.kt`
- blob: `fe1a50036ebe72d0f5b6e85bd3cc92d88544ef8e`

Persistence:
- existing analysis-run persistence already stores `randomSeed`; no schema migration was required.

UI:
- Analysis result displays `재현 seed` and explains the deterministic policy.
- History displays stored seed and algorithm version for each run.

Test:
- `analysisUsesDeterministicSeedForSameInputsAndConfig`
  - reordered input produces same seed and identical games
  - changed config produces a different seed
- `AnalysisEngineTest.kt`
- blob: `267e836efcd5797248d0f07d19c398068354259d`

## 5. Item 4 — common SyncStatusCard

**Status: PASS**

New common component:
- `feature/ui/.../components/SyncStatusCard.kt`
- blob: `50d67e98ded1701dc72a82cc2614349719dc4cc8`

Centralizes:
- syncing state title/icon
- determinate/indeterminate progress
- success/partial-failure/failure message
- retry button
- targeted failed-draw count
- optional failed-list detail action

Statistics:
- blob: `e6c8571994c986aa03583474b13fc8069a5ee40b`

Database:
- blob: `f971d4e307284de8e9a00017da709fd457ab5b1a`

The former duplicated progress/error/retry blocks are removed from both screens.

## 6. Item 5 — accessibility semantics / live status announcement

**Status: PASS**

`SyncStatusCard` adds Compose status semantics:
- `liveRegion = LiveRegionMode.Polite`
- `stateDescription = announcement`
- `mergeDescendants = true`

This is the Compose equivalent used for status/live announcements; Compose does not expose an HTML-style literal `role=status` enum.

The underlying Material `LinearProgressIndicator` retains its progress semantics:
- unknown total → indeterminate
- known total → determinate progress

Visual status is not color-only:
- status icon
- title
- message
- progress
- retry/detail action where applicable

Code:
- `SyncStatusCard.kt`
- blob: `50d67e98ded1701dc72a82cc2614349719dc4cc8`

## 7. Item 6 — EV progressive disclosure

**Status: PASS**

Statistics no longer renders all 45 EV cards as the only default view.

New hierarchy:
1. EV summary card
   - Top 3 number summary
   - explicit reminder that missing-prize draws are excluded from EV valid samples
2. default Top 10 ranking
3. `전체 45개 보기` action
4. user can collapse back to Top 10

Existing explicit `prizeSampleCount` remains visible for every displayed EV row; no opaque confidence grade was introduced.

Code:
- `StatisticsScreen.kt`
- final blob: `e6c8571994c986aa03583474b13fc8069a5ee40b`

## 8. Item 7 — Database search/jump + full failure detail surface

**Status: PASS**

### Draw search/jump

Database state now contains:
- `searchQuery`
- `searchResult`
- `searchMessage`

Code:
- `DatabaseUiState.kt`
- blob: `8f50ac0d6c3e47e3430bf5eaff932dd7c67af1ec`

ViewModel:
- maintains the complete locally observed draw list, while the normal list UI still displays recent 300
- numeric query sanitization
- exact draw lookup from local DB state
- no implicit network request for search
- result/message state

Code:
- `DatabaseViewModel.kt`
- final blob: `9bfc49b9e482ed8edd6cee56ebff0c5593954a67`

UI:
- numeric `회차 번호` field
- `검색/점프` action
- if the draw is in recent 300, LazyList scrolls to that row
- older stored results remain visible in a dedicated search-result card above the recent list

### Failure detail surface

If failed draw count exceeds 6:
- common SyncStatusCard exposes `실패 목록 보기`
- Database opens an `AlertDialog` with the complete failed draw-number list
- retry remains available independently

Code:
- `DatabaseScreen.kt`
- blob: `f971d4e307284de8e9a00017da709fd457ab5b1a`

## 9. Item 8 — Analysis / History inherited UI improvements

**Status: PASS for requested v1.0.5 scope**

### Analysis

Implemented:
- after result creation, large settings panel collapses to a one-line summary card
- `설정 변경` expands it again
- result header includes non-interactive status chips for:
  - latest draw
  - analysis range
  - EV enabled/disabled
  - algorithm version
- deterministic seed displayed beneath provenance status
- GAME card makes total score primary
- frequency/consecutive/parity/EV details are behind `세부 점수 보기` expand/collapse

Code:
- `AnalysisScreen.kt`
- blob: `6e7aad970252393bb5db52b9dfd044e16b48d244`

### History

Implemented:
- timestamp promoted to primary title
- base draw/recent-N/game count promoted to meaningful metadata
- trailing `상세` / `닫기` control with chevron
- run id moved to secondary metadata
- EV status, seed and algorithm version shown
- existing whole-card expansion behavior retained

Code:
- `HistoryScreen.kt`
- blob: `f41fdb977e72429488ad61d6fb0db2bbd231b1bc`

Per feedback, History filter/sort/compare mode remains a longer-term item rather than being added to this tranche.

## 10. Version

**Status: PASS**

- `versionName = "1.0.5"`
- `versionCode = 6`
- `app/build.gradle.kts`
- blob: `5af3af4123a95fb14910a802128b13b19a217562`

## 11. Tests / CI

### Added or strengthened tests

Network:
- partial and all-failure aggregation retained
- long-gap official latest behavior retained
- existing DB incremental sync avoids `all` query
- up-to-date DB avoids `all` query

Blob:
- `LottoRemoteDataSourceImplTest.kt`
- `09a83a7ec23841708c87ec6b8953c11407d3870b`

Data:
- range fetch reuse / partial persistence retained
- aggregate retry contract test added after unreachable Error branch cleanup

Blob:
- `LottoRepositoryImplTest.kt`
- `b6f4ae8781af74aea655b542cf99daaa388bef45`

Engine:
- deterministic seed/game reproducibility test added
- existing fallback duplicate/maxOverlap tests retained

Blob:
- `AnalysisEngineTest.kt`
- `267e836efcd5797248d0f07d19c398068354259d`

### Negative attempts / corrections

CI run #10:
- head: `f3bf84ffbb87889d1a1e13175d13e5848048cf50`
- conclusion: **FAILURE**
- cause: one Kotlin string used `$added건`, parsed as identifier `added건`
- correction commit: `4d351bc2bb44f338092d1e4ea8761669213df460`

Additional static pass found an equivalent interpolation hazard:
- `$EV_PREVIEW_COUNT만`
- corrected to braced interpolation before relying on the next CI
- correction commit: `8776be62b41f48369589f08b658bd4322b2743ad`

Final code-bearing CI:
- Workflow: `Android CI`
- Run: **#12**
- Run ID: `36495509152`
- Head: `8776be62b41f48369589f08b658bd4322b2743ad`
- Conclusion: **SUCCESS**

Build:
- `./gradlew --no-daemon assembleDebug`
- **SUCCESS**
- `BUILD SUCCESSFUL in 3m 25s`

Tests:
- `./gradlew --no-daemon testDebugUnitTest`
- **SUCCESS**
- `BUILD SUCCESSFUL in 44s`
- executed:
  - `core:model:testDebugUnitTest`
  - `core:data:testDebugUnitTest`
  - `core:engine:testDebugUnitTest`
  - `core:network:testDebugUnitTest`
  - `feature:ui:testDebugUnitTest`

Non-blocking existing warning:
- AGP 8.2.2 recommends a newer plugin for `compileSdk = 36`.

## 12. Additional UI design recommendations

These are follow-up recommendations, not blockers for v1.0.5.

### Analysis
- A future explicit “seed 잠금/새 seed” expert control could support intentional scenario branching while keeping the current default deterministic.
- The four provenance status chips could move to a wrapping layout when the project upgrades to a Compose version where a stable FlowRow fits the supported baseline.
- Consider separating “추천 생성” from “결과 저장/공유” in a persistent bottom action area on tall result lists.

### History
- Feedback-designated long-term work remains valuable: period/base-draw/EV filters, sort order, and two-run comparison.
- A future compare view should prioritize changed config, seed, number overlap and score deltas rather than raw JSON/run metadata.
- For very long history lists, group by month with sticky headers.

### Statistics
- Add an explicit user-visible sample-policy help action explaining exactly when a prize sample is excluded.
- If a “표본 적음” hint is later introduced, define and display the numeric threshold rather than a hidden confidence heuristic.
- Consider a compact rank-delta/score distribution visualization only after the underlying time-series comparison semantics are defined.

### Database
- Add a compact “last successful sync” timestamp when the data model has a durable sync receipt rather than inferring it from draw timestamps.
- Search could later support a direct range query such as `1000-1050`, but exact draw jump is safer for the current version.
- For failed-sync detail, a future bottom sheet can replace AlertDialog if richer per-draw error reasons are persisted.

### Common
- Extract status wording into a shared presentation model if more screens adopt synchronization.
- Add Compose UI accessibility tests for live-region/state-description semantics when semantics test infrastructure is expanded.
- For Dynamic Type and narrow width, migrate top action rows to a measured responsive Row/Column component rather than screen-specific breakpoints.
- Keep error/success state redundant across icon, text and action; do not rely on color alone.

## 13. Final checklist

- Item 1: PASS — unreachable range/retry Error branches removed by aggregate-report contract
- Item 2: PASS — ordinary existing-DB sync uses incremental exact discovery; full-list path is fallback/initial-load only
- Item 3: PASS — deterministic SHA-256 seed policy; persisted and displayed; reproducibility test
- Item 4: PASS — shared `SyncStatusCard`
- Item 5: PASS — polite live-region + state semantics
- Item 6: PASS — EV summary → Top 10 → full list
- Item 7: PASS — draw search/jump + >6 failure detail surface
- Item 8: PASS — Analysis collapse/provenance/detail + History timestamp/detail affordance
- Android build: PASS
- Unit tests: PASS
