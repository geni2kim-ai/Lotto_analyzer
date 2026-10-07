# Evidence — v1.0.16

Repository: `geni2kim-ai/Lotto_analyzer`  
PR: #1 — `review/lotto-v1.0.16` -> `main`  
Base SHA: `59292715a83c0dafca40821fd7fcc17b057cb2a8`  
Validated code SHA: `e22d3b46772234fba5d921009ccfedc49b0d6a5e`  
Version: `1.0.16` / versionCode `17`  
Review mode: PR-with-codediff-finder v2.5 stable SHADOW dogfood

> This evidence file is documentation written after the validated code SHA. It does not claim that a documentation-only evidence commit changes the validated production code.

## 1. Resolved source findings

### H1 — unmanaged ViewModel lifecycle
Resolved.

`MainActivity` now obtains Analysis/Database/History/Statistics ViewModels through `ViewModelProvider` bound to the Activity `ViewModelStore`, instead of constructing them directly. The dependency graph in the custom factory is lazy.

### M1 — ExpectedValue algorithmVersion lineage mismatch
Resolved.

Expected-value runs now persist `Constants.ALGORITHM_VERSION` rather than stale literal `"1.0.0"`.

Regression:
- `ExpectedValueRepositoryImplTest.saveExpectedValueRunUsesCurrentAlgorithmVersion`

### M2 / X1 — generated analysis save visibility and async state race
Resolved.

- generation reservation is set before launching work;
- it remains active through history persistence;
- save failure/incomplete state is surfaced;
- an older save cannot complete after a newer generation and overwrite its state.

Regressions:
- `saveFailureIsSurfacedWithoutDiscardingGeneratedResult`
- `duplicateRequestBeforeWorkerRunsIsIgnored`
- `generationRemainsReservedUntilHistorySaveCompletes`

### M3 / X2 — stale EV provenance/message after draw refresh
Resolved.

When the draw dataset changes, saved-run provenance is invalidated and the old EV completion message is cleared with it, while unrelated sync messages remain intact.

Regression:
- `savedExpectedValueBasisIsClearedWhenDrawDatasetChanges`

### X3 — stale EV save completion after concurrent draw refresh
Resolved.

If draw data changes while `saveExpectedValueRun` is suspended:

1. current draw data is re-read after persistence;
2. the current selected snapshot is compared with the snapshot used for calculation;
3. a mismatch prevents the old indexes/runId from being restored;
4. the newly persisted stale run is deleted through `deleteExpectedValueRun`;
5. the UI remains bound to the refreshed draw dataset and asks for recalculation.

Regressions:
- `staleSaveCompletionDoesNotRestoreExpectedValueProvenanceAfterDrawRefresh`
- `deleteExpectedValueRunDeletesPersistedRun`

### M4 — one-shot AppResult.Loading could leave permanent busy state
Resolved.

Unexpected `AppResult.Loading` from one-shot calls is treated as an incomplete terminal attempt and busy flags are cleared with a retryable message.

Affected:
- AnalysisViewModel
- HistoryViewModel
- StatisticsViewModel
- DatabaseViewModel

### L1 — selected tab lost on recreation
Resolved.

`MainAppScreen` now stores selected tab with `rememberSaveable`.

## 2. Independent review history

### First independent review
Reviewed early candidate `caf28542...`.

Found two P2 issues:
- X1: analysis save completion race;
- X2: stale EV completion message/provenance.

Both were accepted, fixed, regression-covered, replied to, and their review threads were resolved.

### Second independent review
Reviewed `45ea9e30...`.

Result: **no major issues**.

### Third independent review
Reviewed `a4a80365...`.

Found one P2:
- X3: draw refresh can occur while EV persistence is suspended, then stale save completion can restore old EV provenance.

X3 was accepted and fixed.

### Attempted post-X3 independent re-review
A new `@codex review` was requested after X3 was fixed.

Result: **BLOCKED by Codex code-review usage limit**. No latest-head independent PASS is claimed.

This is treated as reviewer-capacity evidence, not as a code finding and not as a PASS.

## 3. CI history

### CI #63 — FAIL, captured as backdata
Candidate: `45ea9e30...`

- Fragment 1.8.9 resolution gate: PASS
- `assembleDebug`: FAIL
- cause: intended `Constants.ALGORITHM_VERSION` migration referenced `Constants` without the actual import

Failure family:
- `REVIEW-INTENT-DRIFT/IMPORT`

### CI #64 — FAIL, captured as backdata
Candidate: `4cc63b30...`

- Fragment gate: PASS
- `assembleDebug`: PASS
- unit-test compilation: FAIL
- cause: `?.let(AppResult::Success)` in two new fake repositories

Failure family:
- `TEST-HARNESS/KOTLIN-CONSTRUCTOR-REF`

### CI #65 — SUCCESS
Candidate: `a4a80365...`

PASS:
- Kotlin interpolation checker tests
- Kotlin interpolation convention
- Fragment dependency resolution
- `assembleDebug`
- debug unit-test task set
- U1 geometry evidence

### CI #67 — SUCCESS
Candidate: `80301f95...` (X3 freshness guard)

All gates above: PASS.

### CI #68 — SUCCESS
Candidate: `e22d3b46772234fba5d921009ccfedc49b0d6a5e` (X3 stale-run cleanup)

All gates above: PASS.

## 4. Dogfood / Leonardo backdata

Reusable failure-family candidates observed in this real repository review:

- `REVIEW-INTENT-DRIFT/IMPORT` — intended edit and actual compiled diff diverge.
- `REVIEW-STATE-RACE/ASYNC-SAVE` — late async result can corrupt newer UI state.
- `REVIEW-PROVENANCE/STALE-MESSAGE` — provenance reset and explanatory UI text diverge.
- `REVIEW-PROVENANCE/STALE-SNAPSHOT` — old computation result is applied after source dataset changes.
- `REVIEW-PROVENANCE/STALE-PERSISTED-RUN` — stale result is rejected in UI but persists as newest stored record.
- `TEST-HARNESS/KOTLIN-CONSTRUCTOR-REF` — production build passes but newly added test harness does not compile.
- `REVIEW-CAPACITY/CODEX-LIMIT` — independent reviewer unavailable due external quota; must be represented as BLOCKED, never PASS.

These are case-bank / regression / rule-calibration data, not claims of model-weight training.

## 5. v1.0.15 regression preservation

Prior F1/U1/U2 protections remain intact on the final validated code candidate:

- F1 Fragment 1.8.9 SSOT + dependency-resolution gate
- U1 359dp / 2.0x font-scale geometry evidence
- U2 semantic order vs wrapped visual-order regression coverage

## 6. Remaining intentional backlog

Not changed in this review round:

- G4 replay payload versioning
- G5 durable sync receipt

They remain explicit backlog and are not silently marked complete.

## 7. Final SHADOW verdict

For validated production-code SHA `e22d3b46772234fba5d921009ccfedc49b0d6a5e`:

- deterministic/build checks: **PASS**
- debug unit-test task set: **PASS**
- U1 geometry evidence gate: **PASS**
- accepted independent findings X1/X2/X3: **RESOLVED**
- final post-X3 independent review: **BLOCKED — reviewer usage limit**
- security/governance escalation requiring a separate Adversarial authority: not identified in this patch
- merge authorization: **NOT SELF-GRANTED**

**Code validation verdict: PASS.**  
**Independent latest-head review verdict: BLOCKED (capacity), not PASS.**  
**Merge gate: remains a separate authority decision under SHADOW mode.**
