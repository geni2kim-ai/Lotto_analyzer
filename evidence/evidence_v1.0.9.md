# Evidence — v1.0.9

## 1. Lineage

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- Required feedback read before implementation:
  - `feedback/feedback_v1.0.9.md`
  - `feedback/feedback_v1.0.8.md`
- v1.0.8 evidence commit: `f9c8a9919c9a269958f37504e3bd62feeb679fba`
- v1.0.9 feedback/base commit: `dd15268a3d68966160b0efd63dfb00433b4e0039`
- v1.0.9 implementation commit: `d1fafad1bb789026dd28bdddff29a0a94ab88910`
- Android version: `versionName = "1.0.9"`, `versionCode = 10`

The v1.0.8 original directives F1~F4 and F5/F6 backlog were re-read and compared against the v1.0.9 review before implementation. G1~G3 are implemented. G4/G5 remain design/backlog items as explicitly allowed.

## 2. G1 — interpolation checker escape handling

**Status: PASS**

### Defect

The normal-string and character-literal scanners compared one-character `current` with a two-character backslash string:

- old: `if current == "\\\\":`
- because `current` is a single character, that condition was dead and escape tracking never activated.

That could terminate a string/char literal too early and let a later `/*` sequence corrupt block-comment state, causing a false negative on subsequent Kotlin interpolation violations.

### Implementation

Both literal branches now compare against exactly one backslash character:

- `if current == "\\":`

File:
- `tools/check_kotlin_interpolation.py`
- blob: `cc88717a8f0d7ffcfdda908d2c597f11f3aa1ef6`

### Regression assertions

File:
- `tools/test_check_kotlin_interpolation.py`
- blob: `268647bf388880b694712204da30a617fa57465c`

New tests:

1. `test_escaped_quote_does_not_turn_following_block_marker_into_comment`
   - input contains `\"` followed by literal `/* not comment`
   - next line contains `"$z개"`
   - assertion: next-line violation is detected as `z`

2. `test_escaped_apostrophe_in_char_literal_preserves_following_scan`
   - input contains escaped apostrophe `\'` inside a Kotlin char literal
   - next line contains `"$z개"`
   - assertion: next-line violation is detected as `z`

All pre-existing checker tests remain and passed in CI.

CI:
- `Test Kotlin interpolation checker`: **SUCCESS**
- `Check Kotlin interpolation convention`: **SUCCESS**
- log: `Kotlin interpolation convention check passed.`

## 3. G2 — ResponsiveActionContainer in Analysis

**Status: PASS**

The v1.0.8 responsive primitive is now reused in Analysis where primary/secondary hierarchy is clear.

### Changed action group

`ConfigSummaryCard` previously used a fixed horizontal `Row` for:
- `설정 변경`
- `다시 생성`

It now uses `ResponsiveActionContainer`.

Hierarchy:
- primary: `다시 생성` / analysis regeneration
- secondary: `설정 변경`

Therefore the same shared policy applies:
- width < 360dp → Column
- fontScale >= 1.6f → Column
- otherwise Row
- in Column mode actions receive full width
- in Row mode the primary action receives weighted space

Analysis file:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
- blob: `49e56e8c052849be321acefa8ee163f1182f80f2`

Shared primitive:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/components/ResponsiveActionContainer.kt`
- blob: `3c50b88ed89dcde1d1fbd391daec027da42ab686`

### Usage rule documented

`ResponsiveActionContainer` now includes KDoc stating:
- use for a clear primary/secondary action pair that competes for horizontal space
- do not wrap a single action merely for consistency
- a native full-width button is preferred for single-action cases

This preserves the design principle from the v1.0.9 review.

## 4. G3 — SyncStatusCard font-scale semantics matrix

**Status: PASS**

`SyncStatusCardTest` now verifies the responsive action layout at the requested font scales:

- 1.0 → `responsive-actions-row`
- 1.3 → `responsive-actions-row`
- 1.6 → `responsive-actions-column`
- 2.0 → `responsive-actions-column`

Test file:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/SyncStatusCardTest.kt`
- blob: `062fe6e8c56282d6ad6b2f9c0154d5df6fa6668c`

Test setup:
- Robolectric API 34
- `@Config(..., qualifiers = "w600dp-h800dp")` provides a stable wide viewport so width does not independently force Column
- `CompositionLocalProvider(LocalDensity provides Density(..., fontScale = ...))` injects the requested font scale
- the same failed-sync state renders two actions so the responsive container is present

Assertions:
- expected row/column test tag exists
- opposite layout tag does not exist

The original accessibility assertions are retained:
- merged summary remains a Polite live region
- summary node is non-clickable
- retry action remains independently clickable
- failure-details action remains independently clickable

## 5. G4 — replay payload schema versioning

**Status: DESIGN NOTE / BACKLOG**

F6 replay-payload expansion remains backlog.

When it is implemented, the acceptance contract should include:
- explicit first-line schema identifier such as `replay-v1`
- stable field names/order or a self-describing structured format
- parser behavior defined per schema version
- future additions must not silently alter the meaning of an older token

Recommended initial payload fields:
- schema version
- algorithm version
- deterministic seed
- applied recent-N
- data start draw
- data end draw
- normalized weights/config
- app version

The current v1.0.9 implementation does not change replay payload semantics.

## 6. G5 — durable last-successful-sync receipt

**Status: BACKLOG**

No timestamp is inferred from draw timestamps.

Future implementation should introduce a durable sync receipt containing at least:
- successful completion timestamp
- latest confirmed draw number
- sync result summary / failure count
- source/protocol version if useful for diagnostics

Only after that durable model exists should the compact status UI display “last successful sync”.

## 7. Version

**Status: PASS**

- `versionName = "1.0.9"`
- `versionCode = 10`

File:
- `app/build.gradle.kts`
- blob: `911afca8ab39d7d3125b908819535a5c9fbc3a0e`

## 8. Android CI / verification

Final code-bearing commit:
- `d1fafad1bb789026dd28bdddff29a0a94ab88910`

Android CI:
- Run number: **#32**
- Run id: `36525028999`
- Conclusion: **SUCCESS**

Steps:
- Python interpolation checker unit tests: **SUCCESS**
- repository interpolation convention gate: **SUCCESS**
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - `BUILD SUCCESSFUL in 3m 30s`
- `./gradlew --no-daemon testDebugUnitTest`: **SUCCESS**
  - `BUILD SUCCESSFUL in 52s`
- `feature:ui:testDebugUnitTest`: executed successfully

Executed test modules include:
- `core:model:testDebugUnitTest`
- `core:data:testDebugUnitTest`
- `core:engine:testDebugUnitTest`
- `core:network:testDebugUnitTest`
- `feature:ui:testDebugUnitTest`

Existing non-blocking warning:
- AGP 8.2.2 recommends a newer Android Gradle Plugin for `compileSdk = 36`.

## 9. Final checklist

- G1 escape handling dead code fixed in both literal scanners: **PASS**
- G1 escaped-quote regression test: **PASS**
- G1 escaped-apostrophe regression test: **PASS**
- G2 Analysis two-action row migrated to ResponsiveActionContainer: **PASS**
- G2 primary/secondary usage rule documented in KDoc: **PASS**
- G3 font scale 1.0 row assertion: **PASS**
- G3 font scale 1.3 row assertion: **PASS**
- G3 font scale 1.6 column assertion: **PASS**
- G3 font scale 2.0 column assertion: **PASS**
- G4 replay schema versioning: **BACKLOG / DESIGN NOTE**
- G5 durable sync receipt: **BACKLOG**
- Android build: **PASS**
- Existing unit tests: **PASS**
- Android CI #32: **SUCCESS**

## Additional UI design recommendations

These recommendations are non-blocking.

### Analysis

- Apply responsive action layout only to genuine action pairs. Do not migrate the single “재현 정보 복사” button unless another peer action is introduced.
- The provenance/status-chip area still uses fixed two-column rows. At 1.6x–2.0x font scales, a wrapping flow layout would be more robust than forcing two equal-width chips.
- When G4/F6 replay payload arrives, show a short fingerprint (for example 8–12 chars) in the disclosure and keep the full versioned payload behind Copy.

### History

- Keep month grouping/filtering in backlog until history density warrants it, but define filter interaction before adding sticky month headers so filtered empty groups never remain visible.
- If exact replay is added, separate `동일 조건 재현` from `현재 데이터로 재계산` visually and semantically; the former should require an immutable historical input snapshot.
- A compact algorithm-version filter will become more useful once multiple algorithm generations accumulate.

### Statistics

- The Top-10 / full-list action is a candidate for the responsive primitive only if a second peer action is introduced; do not over-apply the component to single buttons.
- Preserve `prizeSampleCount` as always-visible evidence even if more EV explanation moves behind disclosure.
- If rank deltas are later added, include an explicit comparison baseline such as “직전 EV 계산 대비”.

### Database

- Preserve exact-draw jump as the fast primary search path even if range search is added later.
- If sync failure actions grow beyond two, move tertiary operations into the failure-details surface instead of forcing three actions into the responsive pair container.
- G5 should precede any “last sync” timestamp UI; do not infer operational state from draw data timestamps.

### Common / accessibility

- Extend the font-scale matrix to other shared two-action components after the SyncStatusCard pattern proves stable.
- Add width-boundary tests around 359dp / 360dp / 361dp in a later tranche to protect the second responsive threshold independently from font scale.
- Keep stable behavior tags for semantics tests while deriving user-facing announcement expectations from presentation models rather than localized copy.
