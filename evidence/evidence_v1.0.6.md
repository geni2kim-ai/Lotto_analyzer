# Evidence — v1.0.6

## 1. Lineage / scope

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- Read before implementation:
  - `feedback/feedback_v1.0.6.md`
  - `feedback/feedback_v1.0.5.md`
- v1.0.5 original directives 1~8 were re-read and compared against v1.0.6 §0; the review reports all eight as PASS.
- v1.0.6 feedback/base commit: `dd478d1c56b250cba8afc7ac20b78d4c0949bc2c`
- Implementation commit: `2148b0907993e4c049d385948fe411bc414e67cb`
- Android version: `versionName = "1.0.6"`, `versionCode = 7`
- Scope completed: §1 items 1~6.

## 2. Item 1 — SyncStatusCard accessibility merge trade-off

**Status: PASS**

Problem:
- v1.0.5 placed `mergeDescendants = true` on the whole `Card`.
- The card also contains interactive retry and failed-list buttons, so those controls could be absorbed into the parent semantics node instead of remaining independent TalkBack focus targets.

Implementation:
- Removed semantics merging from the outer card.
- Added a dedicated non-interactive status-summary `Column`.
- `mergeDescendants = true`, `LiveRegionMode.Polite`, and `stateDescription` now apply only to this summary region:
  - status icon/title
  - message
  - progress indicator/progress text
- Retry `OutlinedButton` and failed-list `TextButton` remain outside the merged summary semantics subtree and therefore preserve their native, separately focusable button semantics.

Decision on `customActions`:
- Not adopted. Native Material buttons already expose appropriate button role/action semantics and are more discoverable than moving both actions into parent custom actions.
- Scoped merging preserves the concise status announcement without sacrificing individual controls.

Code:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/components/SyncStatusCard.kt`
- blob: `b8d11974963a369654cfd1de15f6d253d254a43d`

## 3. Item 2 — Database stale search result

**Status: PASS**

`DatabaseViewModel.updateSearchQuery` now clears both:
- `searchResult = null`
- `searchMessage = null`

As soon as the query text changes, the previous result card is invalidated and cannot remain visible under a new query.

Code:
- `feature/statistics/src/main/java/com/example/lottoinsight/feature/statistics/DatabaseViewModel.kt`
- blob: `4f3cac586e4245065a0290474b354f46a4f3130a`

## 4. Item 3 — deterministic seed terminology alignment

**Status: PASS**

Terminology is now explicit in production KDoc.

Policy:
- **requested recent-N** = value requested by the user/config before clamping.
- **applied/effective recent-N** = `min(requested recent-N, available draw count)`.
- `analyzeAndGenerate` creates `effectiveConfig = config.copy(recentN = effectiveRecentN)`.
- `deterministicSeed(targetDraws, effectiveConfig)` hashes the **applied/effective recent-N**, not the original oversized request.
- The exact `targetDraws` set is also hashed.
- Therefore two requests that clamp to the same applied recent-N and use the same target draw data intentionally produce the same seed/results.

Code:
- `core/engine/src/main/java/com/example/lottoinsight/core/engine/AnalysisEngine.kt`
- blob: `95e13026dd97b6c6d320874246926a5da107dd61`

Regression test:
- `oversizedRequestedRecentNMatchesSameAppliedRecentNSeed`
- Example: request recent-N 100 with only 12 draws vs request recent-N 12.
- Asserts applied recent-N = 12, identical seed, identical generated games.

Test:
- `core/engine/src/test/java/com/example/lottoinsight/core/engine/AnalysisEngineTest.kt`
- blob: `48ba7d38c8467b94a4e243fb87a11b9632ae0fb6`

## 5. Item 4 — Kotlin interpolation recurrence prevention

**Status: PASS**

A repository coding rule and CI gate were added.

Rule:
- When a Kotlin interpolated identifier is immediately followed by Korean suffix text/counter/particle, braces are mandatory.
- Required examples:
  - `"${added}건"`
  - `"${previewCount}만 보기"`
  - `"${drawNo}회"`
- Forbidden:
  - `"$added건"`
  - `"$previewCount만 보기"`
  - `"$drawNo회"`

Documentation:
- `CONTRIBUTING.md`
- blob: `a9e8594d198ba9412786c52317eddd80521a4373`

Static gate:
- `tools/check_kotlin_interpolation.py`
- scans Kotlin files and rejects unbraced `$identifier` immediately followed by Hangul.
- ignores build/.gradle/.git trees.
- blob: `4f758f2f756a573dce1257159a2e3b150c58cc5e`

CI:
- `.github/workflows/android-ci.yml`
- new step: `Check Kotlin interpolation convention`
- runs before `assembleDebug`.
- blob: `4d938f59821077390198a1de4ed725b52e78ec86`

CI run #15 reports:
- `Kotlin interpolation convention check passed.`

This directly guards against recurrence of the v1.0.5 `$added건` and `$EV_PREVIEW_COUNT만` failures.

## 6. Item 5 — incremental latest-boundary probe cost

**Status: PASS**

The optional optimization candidate was implemented.

Previous probe behavior:
- `discoverLatestIncrementally` called `fetchDraw(candidate)`.
- A missing candidate could trigger new API followed by legacy API, doubling boundary-probe calls.

New lightweight boundary policy:
- `discoverLatestIncrementally` calls `probeDrawAvailability`.
- Boundary probe uses the structured new API only.
- It never calls the legacy endpoint.
- Results:
  - validated exact draw present → `PRESENT`
  - explicit non-null empty list / no matching draw → `MISSING`
  - HTTP/network/parser/malformed response → `UNAVAILABLE`
- `UNAVAILABLE` returns null from boundary discovery so the caller uses the established full-list fallback rather than treating an anomaly as the latest boundary.
- Actual payload collection still uses `fetchDraw`, so legacy fallback remains available for real draw downloads.

Code:
- `core/network/src/main/java/com/example/lottoinsight/core/network/datasource/LottoRemoteDataSourceImpl.kt`
- `probeDrawAvailability`
- `DrawAvailability`
- updated `discoverLatestIncrementally`
- blob: `5bfed7f9c40fe5e5d12c25f78f671ccbef56d699`

Regression:
- existing incremental-sync test now tracks `legacyQueryCalls`.
- local latest 100 / server latest 102 asserts:
  - full `all` query = 0
  - result-page query = 0
  - legacy query = 0
  - exact new-API queries > 0

Test:
- `core/network/src/test/java/com/example/lottoinsight/core/network/datasource/LottoRemoteDataSourceImplTest.kt`
- blob: `89d57d7231c1101a1e992c9f7312e4dde1bcd71c`

## 7. Item 6 — Analysis game-detail state survives recreation

**Status: PASS**

Previous:
- `expandedGames = remember { mutableStateMapOf<Int, Boolean>() }`
- state was lost on activity recreation/rotation.

Current:
- `expandedGameIds` uses `rememberSaveable(result?.randomSeed)`.
- saveable value is an `IntArray` of expanded game indexes.
- expand/collapse updates the saved array.
- using result seed as the input resets stale expansion state when a materially different analysis result arrives, while preserving it across recreation for the same result.

Code:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
- blob: `62378fc02829517ca97f5d11fb72fd2583d0833a`

## 8. Version

**Status: PASS**

- `versionName = "1.0.6"`
- `versionCode = 7`
- `app/build.gradle.kts`
- blob: `582e320c5cc2404db17dd3023aade4860a21149e`

## 9. CI / verification

Workflow:
- `Android CI`
- final code-bearing run: **#15**
- run id: `36499028441`
- head: `2148b0907993e4c049d385948fe411bc414e67cb`
- conclusion: **SUCCESS**

Steps:
- Kotlin interpolation convention: **SUCCESS**
  - `Kotlin interpolation convention check passed.`
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - `BUILD SUCCESSFUL in 3m 39s`
- `./gradlew --no-daemon testDebugUnitTest`: **SUCCESS**
  - `BUILD SUCCESSFUL in 46s`

Executed test modules include:
- `core:model:testDebugUnitTest`
- `core:data:testDebugUnitTest`
- `core:engine:testDebugUnitTest`
- `core:network:testDebugUnitTest`
- `feature:ui:testDebugUnitTest`

Existing non-blocking warning:
- AGP 8.2.2 recommends a newer plugin for `compileSdk = 36`.

## 10. Additional UI design recommendations

The requested v1.0.6 UI/accessibility fixes are implemented. The following are follow-up recommendations, not blockers.

### Analysis

- Add a compact “재현 정보” disclosure row that groups algorithm version + seed, with an explicit copy action for support/debug sharing rather than keeping long seed text always prominent.
- If expert “새 seed” behavior is introduced later, keep deterministic replay as the default and label the alternate mode clearly as a new randomized scenario.
- For narrow width / large font, make provenance status chips use a wrapping flow layout rather than fixed two-column rows.
- Preserve the newly saveable game-detail expansion state, but consider an “모두 펼치기/접기” control only when game count is high enough to justify it.

### History

- Long-term monthly sticky headers remain appropriate once history volume grows.
- Add a “동일 조건 재현” action only after the product defines whether replay should use the historical draw snapshot or current database; do not silently mix those semantics.
- Filter/sort/compare mode should prioritize meaningful user dimensions: date, base draw, EV usage, and algorithm version.

### Statistics

- If “표본 적음” is added, show the numeric rule directly (for example, “유효 표본 < N”) and keep the actual `prizeSampleCount` visible.
- A small “계산 기준” disclosure under the EV summary could explain recent-N and null-prize exclusion without making every EV card denser.
- Rank movement should wait until a stable previous-run comparison model exists, as requested in feedback.

### Database

- Add a clear-search affordance inside or next to the draw-number field so the user can immediately leave search mode after a jump.
- A durable “마지막 성공 동기화” receipt remains preferable to inferring state from draw timestamps; once modeled, surface it in the compact DB status area.
- If failure detail eventually records per-draw causes, migrate the simple list dialog to a bottom sheet/detail screen with retry status per draw.

### Common / accessibility

- Add Compose semantics tests for `SyncStatusCard` proving retry/detail buttons remain independently discoverable while the summary is merged and live-announced.
- As more status cards appear, move title/message/icon selection into a shared presentation model while keeping actions as native controls.
- Introduce a reusable responsive action container that switches Row → Column at constrained widths/large font scales.
- Continue using icon + text + action redundancy; never make color the sole status carrier.

## 11. Final checklist

- Item 1: PASS — semantics merging scoped to non-interactive summary; native buttons remain separate
- Item 2: PASS — query edits invalidate stale search result
- Item 3: PASS — requested vs applied/effective recent-N terminology/code/test aligned
- Item 4: PASS — braced interpolation rule + CI enforcement
- Item 5: PASS — lightweight new-API-only boundary probe; legacy skipped during discovery
- Item 6: PASS — game detail expansion moved to `rememberSaveable`
- Version 1.0.6 / code 7: PASS
- Kotlin interpolation static gate: PASS
- Android build: PASS
- Unit tests: PASS
