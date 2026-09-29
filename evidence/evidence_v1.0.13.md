# Evidence — v1.0.13

## 1. Lineage and required feedback comparison

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- v1.0.13 feedback/base HEAD verified before implementation: `2b9ab342e5ea122dac8a7f872f7b7960baeaf20b`
- v1.0.13 implementation commit: `6dd222f58aac12b6f089804afb71f473d6c35355`
- Implementation commit message: `feat: implement v1.0.13 flow chips and label bounds`
- Android version: `versionName = "1.0.13"`, `versionCode = 14`

Before implementation, `feedback/feedback_v1.0.12.md` was re-read and compared against `feedback/feedback_v1.0.13.md`.

The prior-cycle directives remain correctly closed/carried:
- J1 — exact version-string hygiene and version bump: completed in v1.0.12
- J2 — algorithm chip whitespace/verbatim contract: completed in v1.0.12
- J3 — 359dp + 2.0 font-scale combined-state review: completed in v1.0.12, with pixel-level limitation explicitly left as non-blocking backlog
- G4 — replay payload schema versioning: remains backlog
- G5 — durable sync receipt: remains backlog

The v1.0.13 feedback §0 marks J1/J2/J3 PASS and keeps G4/G5 as backlog. This implementation preserved those boundaries.

## 2. K1 — pixel-level visual verification infrastructure review

**Status: REVIEWED / BACKLOG / NON-BLOCKING**

The current repository and CI were inspected before making this decision.

Current state:
- no Paparazzi dependency or Gradle plugin
- no `captureToImage` usage
- no golden/snapshot baseline directory
- `.github/workflows/android-ci.yml` runs JVM/build verification only:
  - `assembleDebug`
  - `testDebugUnitTest`
- no emulator/instrumentation job is currently provisioned

### Option review

**Paparazzi**
- benefit: deterministic JVM screenshot regression and baseline diffs without an emulator
- cost for this repository today:
  - new Gradle plugin/dependency surface
  - baseline image lifecycle/review policy
  - binary golden churn in Git
  - CI task integration and update workflow
  - maintenance overhead disproportionate to the current 1–2 targeted chip cases

**compose-ui-test `captureToImage`**
- benefit: closer to existing Compose UI test APIs
- limitation in this repository:
  - the current test lane is Robolectric/JVM, not a maintained instrumentation/emulator screenshot lane
  - adopting it as a stable pixel-regression gate would require screenshot storage/comparison policy and CI execution support beyond the current test setup

### K1 conclusion

No pixel-baseline framework was introduced in v1.0.13.

This is intentionally non-blocking per feedback. The minimum useful trigger for adoption is:
1. multiple screens require repeatable visual regression, or
2. a real defect escapes semantics/layout tests, or
3. screenshot review becomes part of normal UI acceptance.

Until then, K2 extends the existing Robolectric Compose coverage to the strongest semantics/layout assertions available without introducing a new golden-image system.

## 3. K2 — narrow FlowRow implementation

**Status: PASS**

Production file:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
- implementation blob: `9b6fc13e8da632ed4a4b6996a49495bc942849be`

Changes:
- narrow / large-font branch changed from a four-row full-width `Column` to `FlowRow`
- existing branch tag preserved:
  - `analysis-status-chips-wrap`
- shared responsive decision remains unchanged:
  - `shouldStackActions(maxWidth, LocalDensity.current.fontScale)`
- no local copy of `360.dp` or `1.6f` was introduced
- FlowRow spacing:
  - horizontal: `6.dp`
  - vertical: `4.dp`
- short chips can now share an available row instead of forcing four full-width rows

The normal-width branch remains the established two-column weighted grid.

### K2 tests

Test file:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/analysis/AnalysisStatusChipsTest.kt`
- implementation blob: `5beb23ccef463647e0f55004707fa184818e997b`

Updated / retained cases:

1. `width359dpUsesFlowWrapLayout`
   - qualifier `w359dp-h800dp`
   - fontScale 1.0
   - wrap tag exists/displayed
   - grid tag absent
   - all four labels exist/displayed

2. `width360dpKeepsStatusChipsInTwoColumnGrid`
   - qualifier `w360dp-h800dp`
   - fontScale 1.0
   - grid tag exists/displayed
   - wrap tag absent
   - all four labels exist/displayed

3. `width359dpAt2_0FontScaleKeepsAllStatusChipsInWrapLayout`
   - qualifier `w359dp-h800dp`
   - injected fontScale 2.0
   - wrap tag exists/displayed
   - grid tag absent
   - all four labels exist/displayed
   - each label semantics bound has positive width/height
   - each label semantics bound stays within the wrap container bounds
   - every label-bound pair is asserted not to overlap via `Rect.overlaps`

These assertions do not replace pixel-level screenshot testing, but they extend K2 to the strongest semantics/layout guarantee practical in the existing CI lane: selected branch, visibility, container containment, and pairwise non-overlap.

## 4. K3 — algorithm label length cap

**Status: PASS**

Production file:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
- implementation blob: `9b6fc13e8da632ed4a4b6996a49495bc942849be`

A dedicated `AlgorithmStatusChip` now applies:
- `maxLines = 1`
- `overflow = TextOverflow.Ellipsis`

This applies in both:
- narrow FlowRow branch
- normal two-column grid branch

The ordinary `StatusChip` remains reusable and now accepts optional `maxLines` / `overflow` parameters while preserving existing defaults for non-algorithm chips.

### Full-value accessibility

The compact header can visually ellipsize a long algorithm version, but the source string is not truncated.

Full value remains available through two paths:
1. Compose Text semantics retain the full source text used by the chip.
2. Existing `ReproducibilityInfo` exposes the complete algorithm version in its disclosure and includes it in the copy payload.

New test:
- `longAlgorithmVersionRemainsAvailableInSemantics`
- qualifier `w359dp-h800dp`
- fontScale 2.0
- uses a deliberately long algorithm version
- exact full text `"알고리즘 <long-version>"` remains present in semantics and displayed

Existing exact assertion for `"알고리즘 test-v1"` remains intact, so K3 does not weaken the short-label contract.

## 5. G4/G5 backlog

**Status: BACKLOG / intentionally unchanged**

No implementation change was made for:
- G4 — replay payload schema versioning
- G5 — durable sync receipt

Acceptance criteria remain in `evidence/evidence_v1.0.9.md` §5–§6.

## 6. Version

**Status: PASS**

File:
- `app/build.gradle.kts`
- implementation blob: `86bec7a761b3728316a7ed389438240f8b6906c0`

Exact values:
```kotlin
versionCode = 14
versionName = "1.0.13"
```

There is no trailing whitespace inside `versionName`.

## 7. Android CI / verification

Implementation commit:
- `6dd222f58aac12b6f089804afb71f473d6c35355`

Android CI:
- workflow: **Android CI**
- run number: **#44**
- run id: `36539995784`
- conclusion: **SUCCESS**

Verified steps:
- Python interpolation checker unit tests: **SUCCESS**
- repository interpolation convention gate: **SUCCESS**
  - log: `Kotlin interpolation convention check passed.`
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - `BUILD SUCCESSFUL in 3m 34s`
- `./gradlew --no-daemon testDebugUnitTest`: **SUCCESS**
  - `BUILD SUCCESSFUL in 53s`
- `feature:ui:testDebugUnitTest`: executed successfully

Executed test modules include:
- `core:model:testDebugUnitTest`
- `core:data:testDebugUnitTest`
- `core:engine:testDebugUnitTest`
- `core:network:testDebugUnitTest`
- `feature:ui:testDebugUnitTest`

Non-blocking tooling warnings:
- AGP 8.2.2 recommends a newer Android Gradle Plugin for `compileSdk = 36`
- GitHub runner reports Node.js 20-targeting actions being forced onto Node.js 24 where applicable

## 8. Final checklist

- feedback_v1.0.12 re-read before implementation: **PASS**
- J1/J2/J3 and G4/G5 backlog comparison maintained: **PASS**
- K1 pixel-level infrastructure options reviewed: **PASS**
- K1 no premature screenshot framework introduced: **PASS / NON-BLOCKING BACKLOG**
- K2 narrow branch converted to FlowRow: **PASS**
- K2 existing wrap tag retained: **PASS**
- K2 359dp / 360dp branch tests retained/updated: **PASS**
- K2 359dp + 2.0x visibility/containment/non-overlap assertions: **PASS**
- K3 algorithm chip maxLines=1: **PASS**
- K3 TextOverflow.Ellipsis: **PASS**
- K3 full long value remains in semantics: **PASS**
- K3 full value remains available in ReproducibilityInfo view/copy path: **PASS**
- G4: **BACKLOG**
- G5: **BACKLOG**
- version 1.0.13 / code 14: **PASS**
- Android build: **PASS**
- all debug unit tests: **PASS**
- Android CI #44: **SUCCESS**

## Additional UI design recommendations

These recommendations are non-blocking.

### Analysis

- After moving to FlowRow, the next useful visual check is not another breakpoint matrix but a localization stress case: longest supported Korean labels plus a long algorithm identifier at 359dp/2.0x. If screenshot infrastructure is later added, make this the first golden case.
- Consider making the provenance chips ordered by user relevance rather than technical origin: latest draw → analysis range → EV state → algorithm version is currently appropriate; keep algorithm metadata last and visually secondary.
- The algorithm label is now safely ellipsized, but avoid adding a tooltip-only recovery mechanism on mobile. The existing reproducibility disclosure/copy surface is a better full-value destination.
- If FlowRow still produces excessive vertical height on real devices, evaluate smaller horizontal padding before reducing typography size; preserving readable type is preferable to compressing text.

### History

- If algorithm-version filtering is introduced, reuse the same display-normalization policy as Analysis: compact/ellipsized list presentation with a full value in detail.
- Combine month grouping, filters, and scroll restoration in one state model so returning from an expanded run does not reset position.
- Exact replay should eventually expose immutable-input provenance near the action, not only inside a deep detail section.

### Statistics

- Keep sample-size context adjacent to EV values; if horizontal space gets constrained, move secondary labels below the metric rather than ellipsizing the sample count.
- Future comparison deltas should include explicit baseline text and avoid color-only meaning.
- For dense rank lists, progressive disclosure remains preferable to multi-column compression on narrow widths.

### Database

- Exact draw-number jump should remain the primary local navigation affordance.
- G5 must land before any “last successful sync” timestamp becomes authoritative UI.
- If sync recovery accumulates more actions, keep only the two most important actions in the status surface and move diagnostics/export into the detail surface.

### Common / accessibility

- When a visible value is ellipsized, always retain a full-value path through semantics, detail, or copy; do not make visual truncation destructive.
- Keep FlowRow semantics order aligned with visual order so TalkBack traversal remains predictable after wrapping.
- Introduce screenshot/golden infrastructure only when there is enough recurring UI surface to justify baseline maintenance; semantics/layout tests should continue covering deterministic branch and accessibility contracts.
