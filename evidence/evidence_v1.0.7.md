# Evidence — v1.0.7

## 1. Lineage / review binding

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- Required feedback read before completion:
  - `feedback/feedback_v1.0.7.md`
  - `feedback/feedback_v1.0.6.md`
- v1.0.6 original items 1~6 were compared against v1.0.7 §0 before completing this cycle.
- v1.0.6 evidence head: `5067becc881493061ce72b44b5cdf63f2d95b70a`
- v1.0.7 feedback commit: `1d870791544af753733029e6de82218bbdf3a79d`
- v1.0.7 regression hardening commit: `b05fe125c6433c718b2bc6655524869e31989b20`
- Compose semantics test repair lineage:
  - `315abea8bde450bbcfa61c20a803df95c41b6b79`
  - `997a035e0ffea5170e99fc00fecbe63756bd1800`
  - `1f10efcaff29eb488234aada3b776afe633f6f91`
- UI / presentation completion commit: `182a172f185f77fd12dd201cc215fc0601068b46`
- Android version: `versionName = "1.0.7"`, `versionCode = 8`

### Resumption note

The earlier v1.0.7 attempt had already placed the two P0 guards in the current source tree and added the requested regression/semantics tests, but had not produced `evidence/evidence_v1.0.7.md` and had not completed the requested UI follow-up. The resumed pass re-read both feedback files, independently verified the P0 source behavior, verified the regression tests and CI, completed the UI/presentation work, and then produced this evidence.

## 2. P0 B1 — UNAVAILABLE must terminate incremental discovery

**Status: PASS**

Required contract:
- anomaly / unavailable probe result must return `null` from `discoverLatestIncrementally`
- caller then uses the established full-list fallback
- the same candidate must not be retried forever

Verified code:
- exponential-search `when`:
  - `DrawAvailability.UNAVAILABLE -> return null`
- binary-search `when`:
  - `DrawAvailability.UNAVAILABLE -> return null`

File:
- `core/network/src/main/java/com/example/lottoinsight/core/network/datasource/LottoRemoteDataSourceImpl.kt`
- blob: `5bfed7f9c40fe5e5d12c25f78f671ccbef56d699`

Regression test:
- `incrementalDiscoveryIOExceptionFallsBackToFullListWithoutRetryLoop`
- wraps `fetchAllDraws(existingMaxDrawNo = 100)` in `withTimeout(1_000)`
- injects IOException for exact draw 101
- asserts:
  - result succeeds via fallback data
  - successful draw numbers = 101, 102
  - `allQueryCalls == 1`
  - `exactQueryCalls == 1`
  - `legacyQueryCalls == 0`
- the timeout makes a same-candidate infinite retry fail the test rather than hang the suite

Test file:
- `core/network/src/test/java/com/example/lottoinsight/core/network/datasource/LottoRemoteDataSourceImplTest.kt`
- blob: `aa4f7ce86c10c7e83b24ddb09cf3a606ea1868cb`

## 3. P0 B2 — HTTP errors must not become MISSING

**Status: PASS**

Verified code:
- `probeDrawAvailability` checks HTTP success before parsing:
  - `if (!response.isSuccessful) return DrawAvailability.UNAVAILABLE`
- only a successful structured response with no matching exact draw is classified as `MISSING`
- HTTP 4xx/5xx therefore cannot silently establish a future boundary

File:
- `LottoRemoteDataSourceImpl.kt`
- blob: `5bfed7f9c40fe5e5d12c25f78f671ccbef56d699`

Regression test:
- `incrementalDiscoveryHttp500IsUnavailableAndFallsBackToFullList`
- injects HTTP 500 for exact draw 101
- wraps the sync in `withTimeout(1_000)`
- asserts:
  - successful fallback result contains draw 101
  - `allQueryCalls == 1`
  - `exactQueryCalls == 1`
  - `legacyQueryCalls == 0`

This locks out the silent-staleness failure mode where HTTP 500 could otherwise be mistaken for “no newer draw”.

Test:
- `LottoRemoteDataSourceImplTest.kt`
- blob: `aa4f7ce86c10c7e83b24ddb09cf3a606ea1868cb`

## 4. P1 — incremental-discovery regression coverage

**Status: PASS**

The two required failure-injection tests are present and executed by `core:network:testDebugUnitTest`:

1. IOException during exponential discovery → exits discovery → full-list fallback
2. HTTP 500 during exact probe → UNAVAILABLE → full-list fallback

Additional retained incremental tests continue to verify:
- normal existing-DB discovery avoids `all` download
- up-to-date DB avoids `all` download
- boundary discovery does not invoke legacy API
- long-gap official API behavior remains supported

File:
- `core/network/src/test/java/com/example/lottoinsight/core/network/datasource/LottoRemoteDataSourceImplTest.kt`
- blob: `aa4f7ce86c10c7e83b24ddb09cf3a606ea1868cb`

## 5. P1 — SyncStatusCard semantics regression test

**Status: PASS**

Production semantics:
- status summary subtree alone uses:
  - `mergeDescendants = true`
  - `LiveRegionMode.Polite`
  - `stateDescription`
- retry and failed-list buttons remain outside that merged subtree and retain native button semantics

Production file:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/components/SyncStatusCard.kt`
- final blob: `605a7200df1b9a888479d8aaee6425c6751de8d2`

Compose/Robolectric test:
- `summaryIsMergedLiveRegionWhileActionsRemainIndependentClickTargets`
- asserts:
  - a Polite live-region node exists with the expected state description
  - that live-region node has no click action
  - `실패한 7개 회차 다시 시도` exists and has a click action
  - `실패 목록 보기` exists and has a click action

Test:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/SyncStatusCardTest.kt`
- blob: `58cf3eb45331d5c03559ac4645f664e394de2bc6`

Test infrastructure:
- `feature/ui/build.gradle.kts`
- Robolectric + Compose UI test dependencies
- Android resources enabled for unit tests
- test runner fixed to Robolectric API 34 after the initial semantics-test setup iterations

## 6. P2 — interpolation checker comment handling

**Status: PASS**

The optional P2 improvement is implemented.

Checker behavior:
- skips comment-only `//` lines
- skips KDoc / block-comment bodies
- continues to scan executable lines, including lines that also have inline comments
- build/.gradle/.git directories remain excluded

Documentation:
- `CONTRIBUTING.md` explicitly documents checker scope and inline-comment behavior
- blob: `4d3eee2f2620715520db6c0ae3394e1a00558b2b`

Checker:
- `tools/check_kotlin_interpolation.py`
- final blob: `659d763e306d14705118e6f813fd8022d7669be0`
- this completion pass also corrected the violation diagnostic so it prints the intended `${identifier}` remediation text

CI continues to execute the interpolation check before Android compilation.

## 7. UI follow-up implemented

### Analysis — reproduction disclosure + copy action

**Status: PASS**

Previous state:
- long seed/policy text was always exposed below result provenance.

Current state:
- compact `재현 정보` disclosure row
- collapsed by default
- expanded content shows:
  - algorithm version
  - seed
  - deterministic-replay explanation
- `재현 정보 복사` copies:
  - `algorithm=<version>`
  - `seed=<value>`
- deterministic replay remains the default; no random “new seed” control was introduced

File:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
- blob: `a5f20258b2a1021df4f90aa5733903ae751c7cb3`

### Database — clear-search affordance

**Status: PASS**

The draw-number `OutlinedTextField` now shows a trailing clear icon whenever the query is non-empty.

Behavior:
- clear button invokes `DatabaseViewModel.clearSearch`
- query, result card, and search message are cleared together
- users can leave search mode immediately after a jump without manual deletion

File:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/database/DatabaseScreen.kt`
- blob: `c4b478301ddb7c0a5cf4339e87892ae5c86b6d80`

### Statistics — EV calculation-criteria disclosure

**Status: PASS**

The EV summary card now contains a `계산 기준` disclosure.

Expanded content explicitly states:
- whether the current display is based on the latest saved recent-N EV run or the stored full draw set
- first-prize null / non-positive draws are excluded from the EV valid-sample denominator and prize average

This keeps the default summary compact while making the sample policy inspectable.

File:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/statistics/StatisticsScreen.kt`
- blob: `45a2f3430fa97fd588c3c31c72e434bb4de2f364`

### Common — shared SyncStatus presentation model

**Status: PASS**

Title / announcement / tone selection was extracted from the composable into:
- `SyncStatusPresentation`
- `resolveSyncStatusPresentation`

This keeps status-copy/tone logic reusable while leaving retry/detail actions as native controls.

Production:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/components/SyncStatusPresentation.kt`
- blob: `cce1b19b91dd1ad4d91f3d6b0f670572f27d8134`

Tests:
- `SyncStatusPresentationTest.kt`
- blob: `8ba3fae0bac69c635738f26e499f595f5d0f9af4`
- verifies failure and active presentation mapping / announcement generation

## 8. History “same conditions replay” semantic decision

**Status: DEFERRED BY DESIGN, NOT A v1.0.7 DEFECT**

No `동일 조건 재현` action was added in this cycle.

Reason:
- current history records run metadata, seed, configuration and generated games, but does not bind an immutable historical input-draw snapshot as a replay artifact
- silently replaying against the current DB could produce a conceptually different operation from replaying the historical analysis input

Defined product semantics for a future implementation:
1. **Exact replay / 동일 조건 재현** should mean historical input snapshot + historical applied config + historical algorithm version.
2. **Current-data recomputation** should be a different action, explicitly labeled `현재 데이터로 재계산`.
3. The two operations must not share the same label.

Until snapshot binding is durable, withholding the exact-replay button is safer and more truthful than exposing an ambiguous action.

Current History UI remains:
- timestamp-first hierarchy
- explicit detail affordance
- stored seed / algorithm metadata

File:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/history/HistoryScreen.kt`
- current path retained; no misleading replay control introduced

## 9. Version

**Status: PASS**

- `versionName = "1.0.7"`
- `versionCode = 8`

File:
- `app/build.gradle.kts`
- blob: `16f5e80c5eaba948ce1491f86666e22c24023807`

## 10. CI / verification

Final code-bearing commit:
- `182a172f185f77fd12dd201cc215fc0601068b46`

Final code-bearing CI:
- Workflow: `Android CI`
- Run number: **#22**
- Run id: `36514958943`
- Conclusion: **SUCCESS**

Steps:
- Kotlin interpolation convention check: **SUCCESS**
  - log: `Kotlin interpolation convention check passed.`
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - `BUILD SUCCESSFUL in 3m 43s`
- `./gradlew --no-daemon testDebugUnitTest`: **SUCCESS**
  - `BUILD SUCCESSFUL in 53s`

Executed test modules include:
- `core:model:testDebugUnitTest`
- `core:data:testDebugUnitTest`
- `core:engine:testDebugUnitTest`
- `core:network:testDebugUnitTest`
- `feature:ui:testDebugUnitTest`

Relevant final-test coverage includes:
- IOException incremental fallback regression
- HTTP 500 incremental fallback regression
- SyncStatusCard Compose semantics regression
- SyncStatusPresentation mapping tests

Non-blocking existing warning:
- AGP 8.2.2 recommends a newer Android Gradle Plugin for `compileSdk = 36`.

## 11. Additional UI design recommendations

These are follow-up recommendations, not blockers for v1.0.7.

### Analysis

- Extend the copied replay payload later to include applied recent-N, data start/end draw numbers, and normalized weight configuration, producing a compact support/debug replay token.
- On narrow width / large font scales, provenance chips should use a wrapping flow layout rather than fixed rows.
- If an expert randomized mode is added, label it explicitly as a new scenario; never replace deterministic replay as the default.

### History

- Before exact replay is implemented, add durable input snapshot binding (or a stable snapshot digest + reconstructable draw IDs).
- When both replay modes exist, visually separate `동일 조건 재현` from `현재 데이터로 재계산`.
- For long history, add month grouping / sticky headers and filters for algorithm version and EV usage.

### Statistics

- Keep `prizeSampleCount` visible even with the new calculation-criteria disclosure.
- If “표본 적음” is introduced, display the numeric threshold in the same disclosure rather than an opaque confidence badge.
- Consider a compact distribution view only after a stable previous-run/time-series comparison contract exists.

### Database

- A durable last-successful-sync receipt should be modeled separately from draw timestamps before adding “last sync” to the status card.
- Range search (for example 1000–1050) is a reasonable later extension, but exact draw jump should remain the primary fast path.
- If per-draw failure reasons are persisted later, promote the simple failure dialog to a detail sheet with reason + retry state.

### Common / accessibility

- Add a responsive action-container primitive that switches Row → Column for large font scales / constrained widths.
- Keep status content in the shared presentation model, but keep actions as native controls so accessibility focus/action semantics stay obvious.
- Consider screenshot/semantics regression coverage for large font scale once Compose UI test coverage expands.

## 12. Final checklist

- P0 B1 — UNAVAILABLE exits both discovery loops: **PASS**
- P0 B2 — HTTP non-2xx becomes UNAVAILABLE: **PASS**
- P1 IOException fallback regression test: **PASS**
- P1 HTTP 500 fallback regression test: **PASS**
- P1 SyncStatusCard semantics test: **PASS**
- P2 comment false-positive handling/documentation: **PASS**
- Analysis reproduction disclosure + copy: **PASS**
- Database clear-search affordance: **PASS**
- Statistics calculation-criteria disclosure: **PASS**
- Common SyncStatus presentation model extraction: **PASS**
- History replay semantics: **DEFINED / implementation intentionally deferred until historical input snapshot binding exists**
- Version 1.0.7 / code 8: **PASS**
- Android build: **PASS**
- Unit tests: **PASS**
