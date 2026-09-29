# Evidence — v1.0.11

## 1. Lineage

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- v1.0.11 feedback/base HEAD verified before code changes: `73dc84fc63d29b442f0331add7feba583d98a1ce`
- v1.0.11 implementation commit: `0d92a40f08d182478c962eda33f6a83fd47ce3ec`
- Implementation commit message: `feat: implement v1.0.11 responsive status chips`
- Android version: `versionName = "1.0.11"`, `versionCode = 12`
- Required review comparison performed before implementation:
  - `feedback/feedback_v1.0.11.md` §0 reviewed: v1.0.10 overall verdict **PASS**
  - `feedback/feedback_v1.0.10.md` original directives H1/H2 and G4/G5 backlog re-read and compared
  - H1/H2 were confirmed as completed in v1.0.10; G4/G5 remain backlog by explicit instruction

## 2. I1 — feedback premise verification before code changes

**Status: PASS**

Before changing code, every snippet relied on by this cycle was checked against the actual `main` HEAD `73dc84fc63d29b442f0331add7feba583d98a1ce` using the repository file viewer/API at that exact ref.

Verified base-state facts:

- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
  - base blob: `49e56e8c052849be321acefa8ee163f1182f80f2`
  - `AnalysisStatusChips` really was a fixed two-row / two-column layout:
    - two `Row` blocks
    - each `StatusChip` used `Modifier.weight(1f)`
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/components/ResponsiveActionContainer.kt`
  - base blob: `3c50b88ed89dcde1d1fbd391daec027da42ab686`
  - shared production policy really was:
    - default `stackBelowWidth = 360.dp`
    - default `stackAtFontScale = 1.6f`
    - decision: `availableWidth < stackBelowWidth || fontScale >= stackAtFontScale`
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/ResponsiveActionContainerTest.kt`
  - base blob: `e76d6268bd7bcbc936f862234ea8e8d88fe81441`
  - actual qualifiers were exactly `w359dp-h800dp`, `w360dp-h800dp`, and `w361dp-h800dp`
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/SyncStatusCardTest.kt`
  - base blob: `ea149f7bd577258597125073faac51f45f2e9b30`
  - actual qualifier was already normalized as `"w600dp-h800dp"`

### Premise mismatch carried forward from H2

The original v1.0.10 H2 directive quoted a whitespace-padded qualifier `" w600dp-h800dp "`, but the actual base source did not contain that state. The v1.0.11 feedback §0 also records this discrepancy.

This cycle therefore did **not** recreate or "fix" a nonexistent whitespace bug. The mismatch is recorded as provenance, and all v1.0.11 changes were based only on snippets verified at the exact main HEAD above.

## 3. I2 — 360dp boundary-test precision premise

**Status: PASS**

File:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/ResponsiveActionContainerTest.kt`
- implementation blob: `ac27dcdffadcacfa5046f0d8b1fbce22fbf6cf1c`

A boundary-precision comment was added directly above `assertResponsiveLayoutTagAtWidth`.

The comment now records all required assumptions:

1. Robolectric `wNNNdp` qualifiers are assumed to map exactly to the same integer-dp boundary observed by `BoxWithConstraints.maxWidth`.
2. Tag assertions are the **integrated layout-path** check.
3. Direct `shouldStackActions` assertions are the **threshold regression anchor**.
4. Because those direct assertions call the production decision function itself, they do **not** independently prove qualifier → `BoxWithConstraints` mapping.

Existing H1 assertions remain unchanged and still protect:
- 359dp → Column tag exists / Row tag absent / direct decision true
- 360dp → Row tag exists / Column tag absent / direct decision false
- 361dp → Row tag exists / Column tag absent / direct decision false
- fontScale fixed to 1.0 in the qualifier-driven integration helper

## 4. I3 — responsive provenance/status chips

**Status: PASS**

### Production layout

File:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
- implementation blob: `7f419b7a8321d845b62e9d1e3742099f1b19a5d2`

Changes:
- fixed two-row chip layout was replaced by a width/font-scale responsive layout
- `AnalysisStatusChips` now uses `BoxWithConstraints`
- the layout calls the existing shared `shouldStackActions(maxWidth, LocalDensity.current.fontScale)`
- **no screen-local `360.dp` or `1.6f` threshold was added**
- narrow / large-font branch:
  - one full-width chip per row
  - tag: `analysis-status-chips-wrap`
- normal branch:
  - existing two-column chip grid retained
  - tag: `analysis-status-chips-grid`

This reuses the same shared responsive policy as `ResponsiveActionContainer`, so future threshold changes remain centralized in that primitive.

### New I3 boundary tests

File:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/analysis/AnalysisStatusChipsTest.kt`
- implementation blob: `2ca8fdc0c9863d7ab0a5c839631277cc36653f8a`

New tests:

1. `width359dpWrapsStatusChipsToSingleColumn`
   - qualifier: `w359dp-h800dp`
   - fontScale explicitly fixed to 1.0 while preserving Robolectric density
   - asserts `analysis-status-chips-wrap` exists
   - asserts `analysis-status-chips-grid` does not exist
   - asserts all four chip labels are present:
     - `최신 1234회`
     - `분석 100회`
     - `EV 사용`
     - `알고리즘 test-v1`

2. `width360dpKeepsStatusChipsInTwoColumnGrid`
   - qualifier: `w360dp-h800dp`
   - fontScale explicitly fixed to 1.0 while preserving Robolectric density
   - asserts `analysis-status-chips-grid` exists
   - asserts `analysis-status-chips-wrap` does not exist
   - asserts the same four chip labels remain present

The tests verify both the selected responsive branch and content preservation at the boundary.

## 5. G4/G5 backlog

**Status: BACKLOG / intentionally unchanged**

No implementation change was made for:
- G4 — replay payload schema versioning
- G5 — durable sync receipt

Their acceptance contracts remain in `evidence/evidence_v1.0.9.md` §5–§6. This cycle did not make a product decision or implement them early.

## 6. Version

**Status: PASS**

File:
- `app/build.gradle.kts`
- implementation blob: `5cbcfc4e198e8cd70a6a7a5300a5c0bded674c39`

Values:
- `versionName = "1.0.11"`
- `versionCode = 12`

## 7. Android CI / verification

Implementation commit:
- `0d92a40f08d182478c962eda33f6a83fd47ce3ec`

Android CI:
- workflow: **Android CI**
- run number: **#38**
- run id: `36532504097`
- conclusion: **SUCCESS**

Verified workflow steps:
- Python interpolation checker unit tests: **SUCCESS**
- repository interpolation convention gate: **SUCCESS**
  - log: `Kotlin interpolation convention check passed.`
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - `BUILD SUCCESSFUL in 3m 22s`
- `./gradlew --no-daemon testDebugUnitTest`: **SUCCESS**
  - `BUILD SUCCESSFUL in 51s`
- `feature:ui:testDebugUnitTest`: executed successfully

Executed test modules include:
- `core:model:testDebugUnitTest`
- `core:data:testDebugUnitTest`
- `core:engine:testDebugUnitTest`
- `core:network:testDebugUnitTest`
- `feature:ui:testDebugUnitTest`

Non-blocking runner/tooling warnings:
- AGP 8.2.2 recommends a newer Android Gradle Plugin for `compileSdk = 36`
- GitHub runner warns that Node.js 20-based actions are being forced onto Node.js 24

## 8. Final checklist

- feedback_v1.0.11 §0 v1.0.10 PASS reviewed: **PASS**
- feedback_v1.0.10 H1/H2 + G4/G5 original directives re-read: **PASS**
- I1 exact main HEAD snippets checked before code changes: **PASS**
- I1 prior H2 premise mismatch explicitly recorded, nonexistent bug not recreated: **PASS**
- I2 qualifier precision/integration-vs-anchor assumptions documented: **PASS**
- I3 narrow-width responsive chip layout implemented: **PASS**
- I3 shared responsive policy reused; no screen-local threshold copy: **PASS**
- I3 359dp wrap tag + content assertions: **PASS**
- I3 360dp grid tag + content assertions: **PASS**
- G4 replay schema versioning: **BACKLOG**
- G5 durable sync receipt: **BACKLOG**
- version 1.0.11 / code 12: **PASS**
- Android build: **PASS**
- all debug unit tests: **PASS**
- Android CI #38: **SUCCESS**

## Additional UI design recommendations

These recommendations are non-blocking and are not part of the v1.0.11 acceptance gate.

### Analysis

- The provenance/status chips now protect the immediate narrow-width failure mode. Add a future visual-regression case for the combined worst case (359dp + 2.0 font scale), because dynamic type and narrow width can expose text-height issues that branch tags alone cannot detect.
- `ResponsiveActionContainer` still gives the primary action `weight(1f)` while the secondary action keeps intrinsic width in horizontal mode. If a future screen introduces a long secondary label, define an explicit equal-weight or max-width policy rather than letting the primary collapse unpredictably.
- Keep the single `재현 정보 복사` action outside the primary/secondary responsive primitive until it actually gains a peer action.

### History

- If history density grows, introduce month grouping and filtering as one interaction design rather than separately; sticky headers, filter state, and scroll restoration should be specified together.
- Any future "replay" affordance should distinguish immutable historical replay from recomputation with current data/settings, especially when G4 is implemented.
- Prefer algorithm-version filtering over exposing raw run IDs as the main browsing control.

### Statistics

- Keep `prizeSampleCount` visible wherever EV-derived values are presented.
- For future comparison/rank-delta UI, always show the comparison baseline (previous draw, selected range, or other explicit reference) next to the delta.
- Continue progressive disclosure for dense EV/statistical lists; avoid placing all secondary metrics at the same visual weight as the primary summary.

### Database

- Preserve exact draw-number search/jump as the primary navigation path and keep it local-cache-only unless the UI explicitly communicates a network action.
- Do not show a "last successful sync" timestamp until G5 provides a durable receipt; a UI timestamp inferred from draw data would overstate operational certainty.
- If recovery actions grow beyond the existing pair, keep the first two actions local and move tertiary diagnostics into the failure-detail surface.

### Common / accessibility

- Keep all width/font-scale thresholds centralized in the shared responsive primitive; do not duplicate `360.dp` or `1.6f` in individual screens.
- Add screenshot/visual tests only for high-value combined states (narrow width + large font + long localized label) rather than duplicating every unit-test matrix point.
- When adding new status chips or compact metadata, verify TalkBack reading order and minimum touch-target requirements separately from visual layout tests.
