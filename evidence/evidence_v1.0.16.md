# Evidence — v1.0.16

Repository: `geni2kim-ai/Lotto_analyzer`  
PR: #1 — `review/lotto-v1.0.16` -> `main`  
Base SHA: `59292715a83c0dafca40821fd7fcc17b057cb2a8`  
Validated code SHA: `a4a80365bf6a1f80e916261232b5321df099c56e`  
Version: `1.0.16` / versionCode `17`  
Review mode: PR-with-codediff-finder v2.5 stable SHADOW dogfood

## 1. Final code changes

### H1 — ViewModel lifecycle ownership
Resolved.

`MainActivity` no longer constructs the four ViewModels directly. It obtains them through `ViewModelProvider(this, LottoViewModelFactory(...))`, binding them to the Activity `ViewModelStore`. The dependency graph in the factory is lazy, so a configuration recreation that reuses existing ViewModels does not eagerly construct an unused replacement graph.

Affected:
- `app/src/main/java/com/example/lottoinsight/MainActivity.kt`

### M1 — Expected-value algorithm lineage
Resolved.

`ExpectedValueRunEntity.algorithmVersion` now uses the canonical `Constants.ALGORITHM_VERSION` rather than the stale literal `"1.0.0"`.

Regression:
- `ExpectedValueRepositoryImplTest.saveExpectedValueRunUsesCurrentAlgorithmVersion`

### M2 / X1 — Analysis save visibility + async race
Resolved.

The analysis action is reserved synchronously before launching work, remains reserved until history persistence completes, and surfaces save failure/incomplete state instead of leaving a false success impression. This also prevents an older save result from overwriting the state of a newer run.

Regressions:
- `saveFailureIsSurfacedWithoutDiscardingGeneratedResult`
- `duplicateRequestBeforeWorkerRunsIsIgnored`
- `generationRemainsReservedUntilHistorySaveCompletes`

### M3 / X2 — Expected-value provenance invalidation
Resolved.

When the shared draw dataset changes, the displayed EV values are recomputed for that dataset and saved-run provenance is invalidated. A stale EV completion message is cleared at the same time, while unrelated sync messages are preserved.

Regression:
- `savedExpectedValueBasisIsClearedWhenDrawDatasetChanges`

### M4 — one-shot Loading fail-closed handling
Resolved.

One-shot engine/repository calls no longer leave UI busy flags permanently set when an unexpected `AppResult.Loading` is returned. Callers terminate the attempt with a retryable incomplete/failure message.

Affected:
- AnalysisViewModel
- HistoryViewModel
- StatisticsViewModel
- DatabaseViewModel

### L1 — selected tab recreation
Resolved.

`MainAppScreen` uses `rememberSaveable` for the selected tab.

## 2. Independent review

The first independent Codex review of PR #1 found two P2 findings:

1. analysis save result could overwrite a newer run;
2. stale EV completion text could survive provenance reset.

Both were accepted, fixed in `45ea9e30e30df265802ea16890a532daeab5c72d`, regression-covered, replied to, and the review threads were resolved.

A subsequent Codex review of `45ea9e30...` reported **no major issues**.

After that semantic-review point, later changes were limited to:
- adding the missing `Constants` import required for compilation;
- correcting two constructor-reference expressions in newly added tests.

A fresh Codex re-review was requested on the latest PR after CI #65; its completion is not claimed here until a matching reviewed-commit result is present.

## 3. CI evidence

### Run #63 — failure captured
Candidate: `45ea9e30...`

- Fragment resolution gate: PASS
- `assembleDebug`: FAIL
- root cause: unresolved `Constants` reference in `ExpectedValueRepositoryImpl.kt`
- disposition: fixed; failure retained as dogfood evidence

### Run #64 — failure captured
Candidate: `4cc63b30...`

- Fragment resolution gate: PASS
- `assembleDebug`: PASS
- debug unit tests: FAIL during test compilation
- root cause: `?.let(AppResult::Success)` in newly added fake repositories
- disposition: fixed in both test files; failure retained as dogfood evidence

### Run #65 — final code validation
Candidate: `a4a80365bf6a1f80e916261232b5321df099c56e`

Result: **SUCCESS**

Successful gates:
- Kotlin interpolation checker tests
- Kotlin interpolation convention
- Fragment dependency resolution, using `androidxFragmentVersion=1.8.9`
- `assembleDebug`
- debug unit-test task set
- U1 FlowRow geometry evidence reporting

## 4. v1.0.15 regression preservation

The prior F1/U1/U2 work remains present and its CI gates passed on the validated candidate:

- F1 Fragment 1.8.9 resolution SSOT/gate
- U1 359dp + 2.0x font-scale geometry evidence
- U2 semantic order vs wrapped visual order regression coverage

## 5. SHADOW review assessment

For the validated code SHA:

- deterministic checks: PASS
- build: PASS
- unit tests: PASS
- prior independent findings: RESOLVED
- unresolved review threads: none from the accepted X1/X2 findings
- protected security/governance change requiring Adversarial escalation: not identified in this patch
- G4 replay payload versioning: unchanged backlog
- G5 durable sync receipt: unchanged backlog

**Code verdict: PASS for candidate readiness.**

Because this is the v2.5 SHADOW baseline, this evidence does not self-authorize merge. Merge remains a separate authority decision.
