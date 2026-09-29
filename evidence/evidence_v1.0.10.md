# Evidence — v1.0.10

## 1. Lineage

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- Required feedback read before implementation:
  - `feedback/feedback_v1.0.10.md`
  - `feedback/feedback_v1.0.9.md`
- v1.0.9 evidence commit: `00def99dfd8a1519557e1b06d138e887ea35531c`
- v1.0.10 feedback/base commit: `415974fc56ab8d71a2ce33fea584a6cc870d5c23`
- v1.0.10 implementation commit: `1a88b690af36e3f5d1b0516b95219dbc96b62eae`
- Android version: `versionName = "1.0.10"`, `versionCode = 11`

The v1.0.9 original directives G1~G3 and G4/G5 backlog notes were re-read and compared against the v1.0.10 review before implementation. H1/H2 are implemented. G4/G5 remain backlog as instructed.

## 2. H1 — ResponsiveActionContainer width-threshold boundary tests

**Status: PASS**

### Production contract

The production decision remains intentionally unchanged:

```kotlin
availableWidth < stackBelowWidth || fontScale >= stackAtFontScale
```

Default threshold:
- `stackBelowWidth = 360.dp`
- `stackAtFontScale = 1.6f`

Therefore the protected width boundary at font scale 1.0 is:
- 359dp → Column
- 360dp → Row
- 361dp → Row

Production file:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/components/ResponsiveActionContainer.kt`
- blob: `3c50b88ed89dcde1d1fbd391daec027da42ab686`

No production threshold change was required; v1.0.10 hardens the contract with boundary tests.

### Compose / Robolectric boundary tests

File:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/ResponsiveActionContainerTest.kt`
- blob: `e76d6268bd7bcbc936f862234ea8e8d88fe81441`

New tests:

1. `width359dpStacksActionsVertically`
   - `@Config(sdk = [34], qualifiers = "w359dp-h800dp")`
   - fontScale fixed to 1.0
   - asserts `responsive-actions-column` exists
   - asserts `responsive-actions-row` does not exist
   - directly asserts `shouldStackActions(359.dp, 1f) == true`

2. `width360dpKeepsActionsHorizontal`
   - `@Config(sdk = [34], qualifiers = "w360dp-h800dp")`
   - fontScale fixed to 1.0
   - asserts `responsive-actions-row` exists
   - asserts `responsive-actions-column` does not exist
   - directly asserts `shouldStackActions(360.dp, 1f) == false`

3. `width361dpKeepsActionsHorizontal`
   - `@Config(sdk = [34], qualifiers = "w361dp-h800dp")`
   - fontScale fixed to 1.0
   - asserts `responsive-actions-row` exists
   - asserts `responsive-actions-column` does not exist
   - directly asserts `shouldStackActions(361.dp, 1f) == false`

The helper preserves the Robolectric-configured density while overriding only `fontScale = 1f`, so the width qualifier remains the independent variable.

### Existing responsive coverage retained

Pre-existing tests remain:
- 200% font scale → Column
- 420dp / fontScale 1.0 pure decision → Row

Together with the v1.0.9 SyncStatusCard font-scale matrix:
- 1.0 → Row
- 1.3 → Row
- 1.6 → Column
- 2.0 → Column

the width and font-scale thresholds are now independently protected.

## 3. H2 — Robolectric qualifier whitespace normalization

**Status: PASS**

The current v1.0.9 final source already contained the normalized qualifier:

```kotlin
qualifiers = "w600dp-h800dp"
```

The reviewed feedback text referred to a whitespace-padded form, but that form was not present in the current `main` source at the start of this implementation.

v1.0.10 therefore makes the normalization explicit rather than inventing a behavior change:
- the class-level `SyncStatusCardTest` annotation is formatted as a dedicated multiline `@Config`
- a comment records that qualifiers must not have leading/trailing whitespace
- the exact value remains `"w600dp-h800dp"`
- all three new H1 method-level qualifiers are also whitespace-free:
  - `"w359dp-h800dp"`
  - `"w360dp-h800dp"`
  - `"w361dp-h800dp"`

File:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/SyncStatusCardTest.kt`
- blob: `ea149f7bd577258597125073faac51f45f2e9b30`

This closes the documentation/hygiene item without claiming a whitespace bug existed in the v1.0.9 final tree when it did not.

## 4. G4 — replay payload schema versioning

**Status: BACKLOG / DESIGN NOTE**

No implementation change in v1.0.10.

The acceptance contract remains the one recorded in v1.0.9 evidence:
- explicit schema identifier such as `replay-v1`
- stable/self-describing field semantics
- version-aware parsing
- future additions must not silently change older payload meaning

G4 remains coupled to the future expanded replay payload work.

## 5. G5 — durable last-successful-sync receipt

**Status: BACKLOG**

No implementation change in v1.0.10.

The UI must not infer operational “last successful sync” state from draw timestamps. A durable sync receipt model should exist first.

## 6. Version

**Status: PASS**

- `versionName = "1.0.10"`
- `versionCode = 11`

File:
- `app/build.gradle.kts`
- blob: `1738581e1a3f77055c4ecfb7b16fddc2f4d9269f`

## 7. Android CI / verification

Final code-bearing commit:
- `1a88b690af36e3f5d1b0516b95219dbc96b62eae`

Android CI:
- Run number: **#35**
- Run id: `36530844002`
- Conclusion: **SUCCESS**

Steps:
- Python interpolation checker unit tests: **SUCCESS**
- repository interpolation convention gate: **SUCCESS**
  - log: `Kotlin interpolation convention check passed.`
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - `BUILD SUCCESSFUL in 3m 25s`
- `./gradlew --no-daemon testDebugUnitTest`: **SUCCESS**
  - `BUILD SUCCESSFUL in 50s`
- `feature:ui:testDebugUnitTest`: executed successfully

Executed test modules include:
- `core:model:testDebugUnitTest`
- `core:data:testDebugUnitTest`
- `core:engine:testDebugUnitTest`
- `core:network:testDebugUnitTest`
- `feature:ui:testDebugUnitTest`

Existing non-blocking warning:
- AGP 8.2.2 recommends a newer Android Gradle Plugin for `compileSdk = 36`.

## 8. Final checklist

- H1 production threshold read/confirmed: **PASS**
- H1 359dp → Column tag + pure decision: **PASS**
- H1 360dp → Row tag + pure decision: **PASS**
- H1 361dp → Row tag + pure decision: **PASS**
- H1 font scale isolated at 1.0: **PASS**
- H2 class-level qualifier normalized: **PASS**
- H2 new method-level qualifiers normalized: **PASS**
- G4 replay schema versioning: **BACKLOG**
- G5 durable sync receipt: **BACKLOG**
- version 1.0.10 / code 11: **PASS**
- Android build: **PASS**
- unit tests: **PASS**
- Android CI #35: **SUCCESS**

## Additional UI design recommendations

These recommendations are non-blocking.

### Analysis

- The responsive action behavior is now protected on both axes (width and font scale). The next responsive weak point is the fixed two-column provenance/status-chip layout; consider a wrapping layout at narrow widths or 1.6x+ font scale.
- Keep the single `재현 정보 복사` action outside `ResponsiveActionContainer` until it gains a genuine peer action.
- When G4 is implemented, show a short replay fingerprint in the disclosure while copying the full versioned payload.

### History

- Keep month grouping/filtering backlog until list density justifies it, but define filtering and sticky-header interaction together.
- Future exact replay must remain distinct from current-data recomputation and should require immutable historical input binding.
- When multiple algorithm versions accumulate, algorithm-version filtering will provide more value than exposing raw run IDs.

### Statistics

- Keep `prizeSampleCount` always visible.
- If the EV summary later gains a second peer action, reuse the responsive pair primitive rather than creating a screen-specific breakpoint.
- Any future rank-delta indicator should display its comparison baseline explicitly.

### Database

- Preserve exact-draw jump as the primary search path.
- If sync failure actions exceed two, move tertiary actions into the detail surface rather than extending the responsive pair into a three-button row.
- Do not expose “last successful sync” until G5 introduces a durable receipt.

### Common / accessibility

- Consider adding device-width matrix coverage beyond the immediate 359/360/361 boundary only if real-device telemetry or design QA reveals another problematic range.
- The next useful regression layer is screenshot/visual coverage at the combined worst case: narrow width plus 2.0 font scale.
- Keep responsive thresholds centralized in the shared primitive and tests; avoid screen-local copies of `360.dp` or `1.6f`.
