# Evidence — v1.0.4

## 1. Lineage / reconciliation

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- Feedback re-read before reconciliation: `feedback/feedback_v1.0.4.md`
- Feedback commit / original v1.0.4 base: `b4f073dd9000c0cec7e73c792d457874a049a047`
- Initial v1.0.4 implementation commit: `982696a0192043dec19d39db8244ba1bccd4adf6`
- Initial evidence commit: `ddb15d565c3d3348f4a00f335502a897c6cc355e`
- Reconciliation / remaining-work commit: `64edd66ff5462f6392e76f769207c7538a40c183`
- Android version: `versionName = "1.0.4"`, `versionCode = 5`

The repository was re-audited from the current `main` source rather than trusting the previous evidence text. A1~A4, B1~B2 and C1~C3 were present, but the reconciliation found two residual behaviors worth closing: all-per-draw failure lost the failed draw-number list, and the progress bar appeared stuck at 0% before the total became known. A direct regression test for A2 was also added.

## 2. A — 코드 정확성 · 엣지 케이스

### A1. `fetchDrawRange` partial success + failed draw reporting — COMPLETE

Core result:
- `core/network/src/main/java/com/example/lottoinsight/core/network/datasource/DrawFetchReport.kt`
- blob: `9719fd66f186bd9a6122356e7b2161508c1c0eb7`
- fields:
  - `successful: List<Draw>`
  - `failedDrawNos: List<Int>`
  - `attemptedCount`
  - `isPartialSuccess`

Contracts:
- `LottoRemoteDataSource.kt`
- blob: `a476dd84d79c42a1e3143099e3e39845b3b07e1e`
- `fetchAllDraws`, `fetchDrawRange`, and targeted `fetchDraws(drawNos)` return `AppResult<DrawFetchReport>`.

Final implementation:
- `LottoRemoteDataSourceImpl.kt`
- blob: `59695531b7106014e59bfc468fd60049fda9cc0f`

Behavior:
1. Requested draw numbers are normalized to positive, distinct, sorted targets.
2. Requests run through `coroutineScope + async/awaitAll` with `Semaphore(6)`.
3. Each completed request advances the progress callback.
4. Successful draws are retained even if other draws fail.
5. Failed draw numbers are retained in `failedDrawNos`.
6. **Reconciliation fix:** even if every individual draw request fails, the aggregate range operation returns a `DrawFetchReport` containing all failed draw numbers instead of discarding those identifiers in a generic `AppResult.Error`.
7. Request-level failures that prevent discovering a range at all (for example, neither the official latest source nor fallback can establish the range) can still return `AppResult.Error`.

Repository reporting:
- `DrawSyncReport.kt`: `e4d3ad366803e955c095e6ecfd59cc91346610ae`
- `LottoRepository.kt`: `f4fbbb04bc6ef5b846767ad78449b0c8d8f1b282`
- `LottoRepositoryImpl.kt`: `1a371b0b6500f94894d54276f83c046e22eecbfd`

Successful draws are batch-upserted, while `failedDrawNos` is propagated to the ViewModels. `retryDraws(drawNos)` retries only the failed draw numbers, preventing internal holes from being permanently skipped after higher draw numbers have already been persisted.

UI status wording was aligned to the feedback:
- `부분 완료: 총 N건 중 M건 성공, K건 실패 (...회차 목록...)`
- Statistics ViewModel blob: `baa09b7efed81ecf3d6ddcf231c5fa2c20c2a9b3`
- Database ViewModel blob: `b8b653bf8d6eb19d25615a68a33172f731b6684b`

### A2. `fetchAndSaveLatestDraws` sequential request removal — COMPLETE

`LottoRepositoryImpl.fetchAndSaveLatestDraws` no longer loops over `fetchDraw`.

Final behavior:
- computes `latestLocalDrawNo + 1 .. latestLocalDrawNo + fetchCount`
- calls `remoteDataSource.fetchDrawRange(start, end)` once
- therefore reuses the same bounded-concurrency range path as A1
- batch-persists successful draws
- returns the actual number of successful draws through the existing `AppResult<Int>` API

The obsolete per-draw sequential helper was removed.

Reconciliation regression test:
- `core/data/src/test/java/com/example/lottoinsight/core/data/repository/LottoRepositoryImplTest.kt`
- blob: `22994aa3e3a10ba3e9bf668747f5f190341ab3e0`
- verifies:
  - local latest = 100
  - `fetchCount = 3`
  - exactly one range request `101..103`
  - no single-draw request
  - partial successful draws 101 and 103 are persisted
  - return value is 2
- `core/data/build.gradle.kts` adds JUnit test dependency; blob `44ff720e5319f3988eb9f96d5d6485f58c0bfd0d`.

### A3. long-unsynced latest draw detection — COMPLETE

`fetchLatestDrawNo` has source-specific plausibility checks.

Official JSON API:
- validates positive/absolute range and does not accept a value below the existing local maximum
- does **not** apply the old 200-draw maximum gap
- a long-unsynced state such as local 100 / official 450 is accepted

HTML fallback:
- keeps the stricter `MAX_REASONABLE_HTML_SYNC_GAP = 200`
- preserves protection against regex/parser false positives from fragile HTML

This separates the trust policy for the structured official API from the HTML fallback.

### A4. `fallbackGames` defensive path testability — COMPLETE

Production support:
- `NumberGenerator.kt` blob: `cca8ab6f194e6c4b715158d6191e138112cdcd8c`
  - `NumberGenerator` is test-subclassable
  - `weightedSample` is `internal open`
- `AnalysisEngine.kt` blob: `f3a966713b50cf812dac3053efc7beb67e7e40eb`
  - fallback-related helpers are module-internal rather than public API

Regression:
- `AnalysisEngineTest.kt` blob: `07fd90f90eed1fc2de08c1c8f26f4f4a49e8fa4e`
- scripted sampler drives the defensive path directly
- verifies:
  - ticket equal to an already selected ticket is skipped
  - duplicate of an earlier fallback ticket is skipped
  - first fallback `maxOverlap` is correct against existing selections
  - later fallback `maxOverlap` includes earlier fallback tickets

## 3. B — tests

### B1. `fetchDrawRange` partial/all failure reporting — COMPLETE

Test:
- `core/network/src/test/java/com/example/lottoinsight/core/network/datasource/LottoRemoteDataSourceImplTest.kt`
- final blob: `a2c7fcacbdf1fb2422baa942216d248583114484`

Partial-failure scenario:
- request draws 1..3
- draw 2 fails in both new and legacy request paths
- expected:
  - successful draw numbers `[1, 3]`
  - failed draw numbers `[2]`
  - attempted count 3
  - partial-success flag true
  - progress callback completes all three targets

Reconciliation all-failure scenario:
- draws 1, 2 and 3 all fail
- expected:
  - aggregate operation remains inspectable as a report
  - successful list empty
  - failed draw numbers `[1, 2, 3]`
  - attempted count 3
- this locks in the identifiers required for targeted retry even at 0/N success.

### B2. long-gap official API scenario — COMPLETE

Same network test suite verifies:
- existing local max = 100
- official JSON API returns latest = 450
- gap = 350 (> old 200 limit)
- result = `AppResult.Success(450)`
- HTML result page is not called

## 4. C — UI observation items

### C1. synchronization `LinearProgressIndicator` — COMPLETE

Statistics:
- `StatisticsScreen.kt`
- final blob: `fa3ed8bcbbd6b4081d319ea6fed1d2b432d7ec46`

Database:
- `DatabaseScreen.kt`
- final blob: `2a9c7516354b63012468fc42030c2484c96279ee`

Behavior:
- while syncing and `syncTotal > 0`: determinate progress = `syncCompleted / syncTotal`
- **reconciliation fix:** while syncing and total is not known yet: indeterminate `LinearProgressIndicator` rather than a visually frozen 0% bar
- textual completed / total / percentage state remains visible once known, so progress is not communicated by graphics alone

### C2. inline retry — COMPLETE

Statistics and Database both render an inline `OutlinedButton` after sync failure.

Behavior:
- no retained failed list: `동기화 다시 시도`
- retained failed list: `실패한 K개 회차 다시 시도`
- targeted retry calls repository `retryDraws(syncFailedDrawNos)`
- repeated partial failure keeps the remaining failed list for another retry

All-per-draw failure now also preserves the draw-number list, so the same targeted path can be used for 0/N success after the range has been established.

### C3. EV valid prize sample count — COMPLETE

Statistics EV rows display:
- average first prize
- `유효 표본 {prizeSampleCount}회`
- normalized score

This exposes the v1.0.3 null/non-positive prize exclusion policy directly in the UI.

## 5. C4 — further UI design recommendations

These are recommendations only; they are not required to close feedback_v1.0.4.

### Analysis
- Collapse the large settings card after a result is generated into a compact summary with an explicit expand action.
- Show provenance/status chips for latest draw, analysis range, EV enabled/disabled and algorithm version.
- Keep overall score primary and move frequency/consecutive/parity/EV component scores into expandable detail or compact meters.
- Provide an actionable recovery control beside analysis errors (retry analysis or sync data), rather than message-only failure state.

### History
- Add a trailing chevron or explicit `상세` affordance instead of relying only on whole-card tap.
- Promote timestamp, base draw, recent-N and game count above raw run ID.
- Add filtering/sorting once history grows (period, base draw, EV use).
- A compare mode for two runs could show number overlap, setting differences and score differences.

### Statistics
- Use progressive disclosure: summary KPIs → Top 5/10 EV numbers → full 45-number ranking.
- Keep explicit `prizeSampleCount`; if a low-sample warning is introduced, use a documented sample threshold instead of an opaque confidence label.
- Separate Frequency / EV / pattern views with tabs or segmented controls to reduce scroll density.
- Extract synchronization state UI into a shared component with Database to prevent behavior/text drift.

### Database
- Add draw-number search/jump for access beyond the recent-300 list.
- Consolidate stored count, latest draw, last successful sync and current sync state into a compact status card.
- Keep winning numbers primary; move winner/prize details into expandable detail for denser scanning.
- For many failed draws, an explicit `실패 K건 보기` detail surface would scale better than a long inline list.

### Common
- Reuse a single `SyncStatusCard`/state component for progress, failure, retry and partial success across Statistics and Database.
- Add accessibility status semantics/live announcements for sync transitions.
- Do not rely on color alone for failure/success; keep icon/text/action combinations.
- Apply responsive Row→Column rules for large font sizes/narrow screens.
- Maintain at least 48dp action touch targets.

## 6. CI / verification

Code-bearing reconciliation run:
- Workflow: `Android CI`
- Run: **#7**
- Run id: `36492948418`
- Head: `64edd66ff5462f6392e76f769207c7538a40c183`
- Conclusion: **SUCCESS**

Build:
- `./gradlew --no-daemon assembleDebug`
- **SUCCESS**
- log: `BUILD SUCCESSFUL in 3m 36s`

Unit tests:
- `./gradlew --no-daemon testDebugUnitTest`
- **SUCCESS**
- log: `BUILD SUCCESSFUL in 45s`
- executed test tasks include:
  - `core:data:testDebugUnitTest` — newly enabled and executed
  - `core:engine:testDebugUnitTest`
  - `core:network:testDebugUnitTest`
  - `core:model:testDebugUnitTest`
  - `feature:ui:testDebugUnitTest`

Non-blocking existing warning:
- Android Gradle Plugin 8.2.2 recommends a newer AGP for `compileSdk = 36`.
- This did not block build or tests and is outside the requested v1.0.4 feedback scope.

## 7. Final completion checklist

- A1: COMPLETE — partial success, all-failure identifier preservation, failed draw reporting, targeted retry
- A2: COMPLETE — sequential latest-fetch removed; bounded range path reused; direct repository regression test added
- A3: COMPLETE — official API long gap allowed; strict gap remains HTML-only
- A4: COMPLETE — fallback defensive path directly tested
- B1: COMPLETE — partial failure plus 0/N failure reporting tests
- B2: COMPLETE — >200 official API gap test
- C1: COMPLETE — indeterminate-before-total and determinate-after-total progress indicators
- C2: COMPLETE — inline full/targeted retry
- C3: COMPLETE — `prizeSampleCount` visible in EV rows
- C4: COMPLETE as requested design recommendations in evidence
- Android build: PASS
- Unit tests: PASS
