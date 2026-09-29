# Evidence — v1.0.12

## 1. Lineage and required feedback comparison

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- v1.0.12 feedback/base HEAD verified before implementation: `59edab69f308ab5b8d46e9f3bbe458c44ed6c1db`
- v1.0.12 implementation commit: `6e32f3ec7a0e8dde84cc750d1696b4aa21788d17`
- Implementation commit message: `test: implement v1.0.12 hygiene and worst-case coverage`
- Android version after implementation: `versionName = "1.0.12"`, `versionCode = 13`

Before implementation, `feedback/feedback_v1.0.11.md` was re-read and compared against `feedback/feedback_v1.0.12.md` as required.

The original v1.0.11 directives were confirmed as:
- I1 — verify quoted feedback premises against the exact `main` HEAD before changing code
- I2 — document the Robolectric qualifier → integer-dp precision premise and the difference between integrated tag assertions and the direct decision-function anchor
- I3 — make Analysis provenance/status chips responsive using the shared responsive threshold policy
- G4/G5 — keep replay payload versioning and durable sync receipt as backlog

The v1.0.12 review marks I1/I2/I3 PASS and explicitly keeps G4/G5 in backlog. This cycle preserved those constraints.

## 2. Pre-change premise verification (I1 carried forward)

**Status: PASS**

Before changing source, the exact `main` HEAD `59edab69f308ab5b8d46e9f3bbe458c44ed6c1db` was inspected.

### J1 feedback premise

Feedback quoted:

```kotlin
versionName = "1.0.11 "
```

Actual HEAD content was:

```kotlin
versionName = "1.0.11"
```

Base file:
- `app/build.gradle.kts`
- blob: `5cbcfc4e198e8cd70a6a7a5300a5c0bded674c39`

The value was also checked as an exact line/serialized string, confirming there was no character between `1.0.11` and the closing quote.

Therefore the claimed inherited trailing-space bug did **not** exist in the actual base tree. No fake whitespace defect was introduced and then removed. The required version bump was still performed.

### J2 feedback premise

Feedback quoted:

```kotlin
"알고리즘 $algorithmVersion "
```

Actual HEAD contained both algorithm-label call sites as:

```kotlin
"알고리즘 $algorithmVersion"
```

Base file:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
- blob: `7f419b7a8321d845b62e9d1e3742099f1b19a5d2`

No trailing space existed at either call site.

The base `StatusChip` implementation was also inspected and was:

```kotlin
Text(text = text, ...)
```

There was no `trim()`, `trimEnd()`, or other whitespace normalization. Thus the previous green exact-label test was not being rescued by a hidden trim path.

This is another premise mismatch of the type I1 was created to catch.

## 3. J1 — version hygiene and bump

**Status: PASS**

File:
- `app/build.gradle.kts`
- implementation blob: `918d453e3dcd366266cd7446cbc12e07675627fc`

Final values, copied exactly including quotes:

```kotlin
versionCode = 13
versionName = "1.0.12"
```

There is no trailing whitespace inside the `versionName` string.

## 4. J2 — algorithm chip label / whitespace contract

**Status: PASS with corrected premise**

No production algorithm label needed a whitespace deletion because both call sites were already exactly:

```kotlin
"알고리즘 $algorithmVersion"
```

File:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
- implementation blob: `63231fedc0cd14fd244e9bf9bc23511dabdd5573`

To make the actual whitespace policy explicit, `StatusChip` now documents that it renders the caller-provided label **verbatim** and intentionally does not trim. Whitespace normalization remains a call-site responsibility so accidental leading/trailing spaces remain observable to tests.

Test file:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/analysis/AnalysisStatusChipsTest.kt`
- implementation blob: `e76836f70471db3c3213e88aec2d9db08f325e84`

Existing label assertions still include exactly:

```kotlin
"알고리즘 test-v1"
```

A comment now records that `onNodeWithText` uses exact matching by default (`substring = false`), so leading/trailing whitespace in the rendered semantics text would cause the assertion to fail.

No silent trim behavior was added.

## 5. J3 — combined narrow-width / large-font review

**Status: REVIEWED; automated combined-state coverage added; pixel-level visual check remains backlog**

Repository inspection found no existing screenshot-regression infrastructure:
- no Paparazzi usage
- no `captureToImage` tests
- no maintained screenshot baseline suite

Because J3 is explicitly non-blocking, this cycle did not introduce a new screenshot framework solely for one case.

Instead, the existing Robolectric Compose test was extended with the high-value combined worst case:

`width359dpAt2_0FontScaleKeepsAllStatusChipsInWrapLayout`

Configuration:
- Robolectric qualifier: `w359dp-h800dp`
- preserved Robolectric density
- injected `fontScale = 2.0f`

Assertions:
- `analysis-status-chips-wrap` exists
- `analysis-status-chips-grid` does not exist
- all four chip labels remain present:
  - `최신 1234회`
  - `분석 100회`
  - `EV 사용`
  - `알고리즘 test-v1`

This proves the shared responsive branch and content preservation under the combined threshold stress case.

Limitation:
- semantics/tag tests do not prove pixel-level line wrapping, clipping, chip height, or visual balance.
- a screenshot/visual regression for `359dp + 2.0x` remains a UI-test-infrastructure backlog item rather than a v1.0.12 blocker.

## 6. G4/G5 backlog

**Status: BACKLOG / intentionally unchanged**

No implementation was made for:
- G4 — replay payload schema versioning
- G5 — durable sync receipt

Their acceptance contracts remain in `evidence/evidence_v1.0.9.md` §5–§6. This cycle did not make product decisions on those items.

## 7. Android CI / verification

Implementation commit:
- `6e32f3ec7a0e8dde84cc750d1696b4aa21788d17`

Android CI:
- workflow: **Android CI**
- run number: **#41**
- run id: `36535338635`
- conclusion: **SUCCESS**

Verified steps:
- Python interpolation checker unit tests: **SUCCESS**
- repository interpolation convention gate: **SUCCESS**
  - log: `Kotlin interpolation convention check passed.`
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - `BUILD SUCCESSFUL in 3m 26s`
- `./gradlew --no-daemon testDebugUnitTest`: **SUCCESS**
  - `BUILD SUCCESSFUL in 50s`
- `feature:ui:testDebugUnitTest`: executed successfully

Executed test modules include:
- `core:model:testDebugUnitTest`
- `core:data:testDebugUnitTest`
- `core:engine:testDebugUnitTest`
- `core:network:testDebugUnitTest`
- `feature:ui:testDebugUnitTest`

Non-blocking tooling warnings:
- AGP 8.2.2 recommends a newer Android Gradle Plugin for `compileSdk = 36`
- GitHub runner warns that Node.js 20-targeting actions are being forced onto Node.js 24

## 8. Final checklist

- feedback_v1.0.11 re-read before work: **PASS**
- I1/I2/I3 and G4/G5 backlog compared against v1.0.12 review: **PASS**
- exact base HEAD checked before code changes: **PASS**
- J1 quoted trailing-space premise checked against actual source: **MISMATCH RECORDED**
- J1 version bumped to exact `"1.0.12"` / code 13: **PASS**
- J2 quoted trailing-space premise checked against both actual call sites: **MISMATCH RECORDED**
- J2 `StatusChip` trim behavior inspected: **NO TRIM**
- J2 verbatim whitespace policy documented: **PASS**
- J2 exact algorithm-label test retained: **PASS**
- J3 screenshot infrastructure assessed: **PASS**
- J3 359dp + 2.0x combined automated coverage added: **PASS**
- J3 pixel-level screenshot regression: **BACKLOG / NON-BLOCKING**
- G4: **BACKLOG**
- G5: **BACKLOG**
- Android build: **PASS**
- all debug unit tests: **PASS**
- Android CI #41: **SUCCESS**

## Additional UI design recommendations

These recommendations are non-blocking.

### Analysis

- The 4-row narrow provenance layout is safe but vertically expensive. When screenshot infrastructure exists, compare it against a true `FlowRow` design that lets short chips share a row while long algorithm labels wrap naturally.
- Keep the current shared width/font-scale decision as the coarse safety gate, but avoid making every metadata chip full-width by default if localization shows substantial unused horizontal space.
- For long algorithm-version strings, consider a max visual length plus an accessible full-value detail/copy surface rather than letting a technical identifier dominate the result header.
- Keep `재현 정보 복사` as a standalone action until it has a genuine peer; do not force it into a two-action primitive for cosmetic consistency.

### History

- If filtering is introduced, design filter state, month grouping, sticky headers, and scroll restoration together so returning from detail does not lose the user's place.
- Historical replay should visually distinguish “exact historical replay” from “recompute with current data/settings”; this becomes especially important when G4 is implemented.
- Prefer user-meaningful metadata such as execution time, algorithm version, and analysis range over exposing raw run identifiers as primary information.

### Statistics

- Keep `prizeSampleCount` adjacent to EV-derived metrics so users can judge the sample basis without opening another surface.
- If rank deltas are introduced, always show the baseline being compared; an unlabeled positive/negative delta is ambiguous.
- Continue progressive disclosure for dense tables: summary → top subset → full list is preferable to adding more horizontal columns on small screens.

### Database

- Preserve exact draw-number search/jump as the primary recovery/navigation path.
- Do not display “last successful sync” until G5 supplies a durable receipt; draw timestamps and UI refresh times must not be presented as operational success evidence.
- If sync recovery grows beyond two primary actions, keep tertiary diagnostics in the detail surface instead of expanding the main card into a crowded button cluster.

### Common / accessibility

- Add visual regression only for high-value combined states: narrow width + 2.0x font + longest supported Korean/localized labels.
- Verify TalkBack reading order independently from visual wrapping whenever metadata moves between grid and stacked arrangements.
- Keep layout thresholds centralized and keep text normalization visible at call sites; hidden trimming inside reusable UI primitives makes spacing defects harder to detect.
