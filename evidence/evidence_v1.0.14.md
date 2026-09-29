# Evidence — v1.0.14

## 1. Lineage and prior-feedback comparison

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- v1.0.14 feedback base: `c1845c969b57512f757ef599beec01f0893d18fb`
- Final v1.0.14 implementation HEAD: `e85ee5a6470b89f682372e43b4f45f946b1a3794`
- Final implementation commit message: `fix: pin fragment to compatible stable 1.8.9`
- Android version: `versionName = "1.0.14"`, `versionCode = 15`

Before implementation, `feedback/feedback_v1.0.13.md` was re-read and compared with `feedback/feedback_v1.0.14.md`.

The v1.0.13 directives were confirmed as:
- K1 — pixel-level screenshot infrastructure review: completed as REVIEWED / BACKLOG / non-blocking.
- K2 — narrow Analysis status chips use `FlowRow` with 359dp/360dp and 359dp + 2.0x layout coverage: completed and preserved.
- K3 — algorithm label uses one-line ellipsis while preserving the full source value in semantics/detail/copy paths: completed and preserved.
- G4 — replay payload schema versioning: backlog.
- G5 — durable sync receipt: backlog.

The v1.0.14 feedback marks K1/K2/K3 PASS and requires G4/G5 to remain backlog. This cycle preserved those constraints.

## 2. P-1 — Fragment 1.1.0 transitive dependency remediation

**Status: PASS with compatibility-bounded version selection**

### 2.1 Original transitive path

The dependency source was identified before the remediation using Android CI #47 (run id `36542496742`) with:

```bash
./gradlew --no-daemon :app:dependencyInsight \
  --dependency androidx.fragment:fragment \
  --configuration debugRuntimeClasspath

./gradlew --no-daemon :app:dependencies \
  --configuration debugRuntimeClasspath
```

The observed inverse dependency path was:

```text
debugRuntimeClasspath
└─ com.google.firebase:firebase-analytics:22.1.0
   └─ com.google.android.gms:play-services-measurement:22.1.0
      └─ com.google.android.gms:play-services-basement:18.4.0
         └─ androidx.fragment:fragment:1.1.0
```

The Firebase Analytics version is selected through `com.google.firebase:firebase-bom:33.2.0`.

Fragment is not directly used by the application source and was not previously declared directly in `app/build.gradle.kts`.

### 2.2 Latest-stable investigation and negative attempt

Android Developers Fragment release notes were checked during this cycle:
- https://developer.android.com/jetpack/androidx/releases/fragment
- latest overall stable observed on 2026-09-30: `1.9.1` (released 2026-09-23)
- latest stable in the 1.8.x line: `1.8.9`

A deliberate `1.9.1` attempt was made to satisfy the strongest interpretation of “latest stable”.

Attempt lineage:
- `240136974ebdeba8bc6dbb432f3142cd1f7ee3c9` — app-scoped Fragment 1.9.1 force (workflow syntax was subsequently repaired)
- `7d4064ca4a00be77260c8155790c429755815392` — repaired dependency verification workflow
- Android CI #51 / run id `36595282884`

CI #51 proved:
- `androidx.fragment:fragment:1.1.0 -> 1.9.1` resolved successfully.
- The build then failed at `:app:checkDebugAarMetadata`.

Observed incompatibility:
- Fragment 1.9.1 caused newer Compose/Lifecycle artifacts to enter the resolved graph.
- examples from the failed build:
  - `androidx.compose.ui:ui-text-android:1.9.0`
  - `androidx.compose.ui:ui-graphics-android:1.9.0`
  - `androidx.compose.runtime:runtime-saveable-android:1.9.0`
  - `androidx.lifecycle:lifecycle-runtime-compose-android:2.10.0`
  - `androidx.lifecycle:lifecycle-viewmodel-compose-android:2.10.0`
- these artifacts require Android Gradle Plugin 8.6.0 or higher.
- the repository currently uses AGP 8.2.2.

Adopting 1.9.1 would therefore require a broader AGP / Compose / Lifecycle upgrade and would violate the feedback requirement to use the minimum scope while preserving compatibility with the existing Compose BOM / Activity line.

### 2.3 Final selected version and implementation

Selected version:
- `androidx.fragment:fragment:1.8.9`
- reason: latest stable in the 1.8.x line that removes the Play Console-reported 1.1.0 resolution while remaining compatible with the current build stack.

Production build file:
- `app/build.gradle.kts`
- final blob: `002ac5ebb5cc5766ba362715167eff7b1198b754`

Resolution policy:

```kotlin
configurations.configureEach {
    resolutionStrategy {
        force("androidx.fragment:fragment:1.8.9")
    }
}
```

The override is scoped to app configurations. No Firebase BOM, Compose BOM, Activity, Kotlin, or AGP family upgrade was introduced.

Verification workflow:
- `.github/workflows/android-ci.yml`
- final blob: `05ac98a036c05a2d8bae61275cd174f99f3fb9c4`

The CI gate executes `:app:dependencyInsight` and requires:
- a resolved `androidx.fragment:fragment:1.8.9`
- the original request to appear as `androidx.fragment:fragment:1.1.0 -> 1.8.9`

### 2.4 Compose / Activity compatibility

Final CI #52 dependency output showed:
- `androidx.fragment:fragment:1.8.9 (forced)`
- `androidx.fragment:fragment:1.1.0 -> 1.8.9`
- `androidx.compose:compose-bom:2024.02.00` remains in the graph
- `androidx.activity:activity-compose:1.8.2` remains in the graph

No 1.9.1-era Compose/Lifecycle escalation occurred in the accepted configuration.

The accepted combination then passed both build and full unit tests.

## 3. L1 — localization stress test

**Status: PASS**

Test file:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/analysis/AnalysisStatusChipsTest.kt`
- final blob: `d13206a35f7dfb3ff357e3b07383c3851dff212e`

Added test:

`localizationStressAt359dpAnd2_0FontScaleKeepsFlowItemsVisibleAndSeparate`

Configuration:
- Robolectric qualifier: `w359dp-h800dp`
- font scale: `2.0f`
- `latestDrawNo = Int.MAX_VALUE`
- `recentN = Int.MAX_VALUE`
- `usePrizeIndex = false` → longer Korean label `EV 미사용`
- long algorithm identifier:
  `analysis-engine-production-localized-2026-09-29-build-abcdef1234567890`

`Int.MAX_VALUE` is used because the composable accepts `Int` for both numeric fields and applies no narrower presentation-layer bound; it is the strongest structurally representable numeric-label stress in this surface.

The test reuses the existing FlowRow worst-case assertions:
- wrap tag exists
- grid tag is absent
- all four expected full semantic labels exist
- all four labels are displayed
- each label has positive semantic width and height
- every label bound is contained within the wrap container
- every pair of label bounds is non-overlapping

The pixel-snapshot limitation remains K1 backlog and is not treated as a v1.0.14 blocker.

## 4. StatusChip documentation hygiene

**Status: PASS**

Production file:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/analysis/AnalysisScreen.kt`
- final blob: `c9219eb4982c087c8a386826ba15b9bce8c9741a`

The existing verbatim-render KDoc now also states that `maxLines` / `overflow` constrain visual layout only and do not mutate source text. This preserves the v1.0.12 whitespace-observability contract while explaining why K3 ellipsis does not destroy the full semantic value.

## 5. G4/G5 backlog

**Status: BACKLOG / intentionally unchanged**

No implementation was made for:
- G4 — replay payload schema versioning
- G5 — durable sync receipt

Their acceptance conditions remain in `evidence/evidence_v1.0.9.md` §5–§6. No product decision was invented in this cycle.

## 6. Version

**Status: PASS**

File:
- `app/build.gradle.kts`
- final blob: `002ac5ebb5cc5766ba362715167eff7b1198b754`

Exact values:

```kotlin
versionCode = 15
versionName = "1.0.14"
```

There is no trailing whitespace inside `versionName`.

## 7. CI and verification

### Dependency-discovery run

Android CI #47:
- run id: `36542496742`
- conclusion: **SUCCESS**
- purpose: run `:app:dependencyInsight` and `:app:dependencies` before P-1 implementation
- result: identified `play-services-basement:18.4.0 -> fragment:1.1.0` through Firebase Analytics / Play Services Measurement

### Initial implementation / workflow correction history

- `061877f5852ef4982471d4f1975599bf7a337dae`
  - initial v1.0.14 source implementation: version bump, Fragment 1.8.9 resolution attempt, L1 test, KDoc update
  - Android CI #48 failed before jobs because the workflow grep line was malformed
- `f7b4927c714e9820818443394e80812871d973c4`
  - repaired workflow
  - Android CI #49 / run id `36543313605`: **SUCCESS**
  - proved the original 1.8.9 compatibility path before the resumed cycle

### Latest-overall-stable negative attempt

- `240136974ebdeba8bc6dbb432f3142cd1f7ee3c9`
  - attempted Fragment 1.9.1
  - Android CI #50 failed before jobs due the same unsafe regex-anchor workflow encoding pattern
- `7d4064ca4a00be77260c8155790c429755815392`
  - repaired workflow using fixed-string grep
  - Android CI #51 / run id `36595282884`: **FAILURE**
  - Fragment resolution step: **SUCCESS**
  - `assembleDebug`: **FAILURE**
  - reason: 1.9.1 escalated Compose/Lifecycle artifacts requiring AGP 8.6.0+

This negative attempt is retained as evidence that 1.9.1 was not rejected speculatively.

### Final accepted implementation

Final implementation HEAD:
- `e85ee5a6470b89f682372e43b4f45f946b1a3794`

Android CI #52:
- run id: `36595783887`
- conclusion: **SUCCESS**

Verified steps:
- interpolation checker tests: **SUCCESS**
- Kotlin interpolation convention gate: **SUCCESS**
- Fragment dependency resolution gate: **SUCCESS**
  - `androidx.fragment:fragment:1.8.9 (forced)`
  - `androidx.fragment:fragment:1.1.0 -> 1.8.9`
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - `BUILD SUCCESSFUL in 2m 20s`
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
- GitHub runner reports Node.js 20-targeting actions being forced to Node.js 24 where applicable

## 8. Final checklist

- feedback_v1.0.13 re-read and K1/K2/K3 + G4/G5 compared before implementation: **PASS**
- P-1 original fragment 1.1.0 path identified using Gradle dependency tooling: **PASS**
- P-1 latest overall stable 1.9.1 evaluated rather than assumed: **PASS**
- P-1 1.9.1 incompatibility demonstrated by actual CI: **PASS**
- P-1 minimum-scope compatible resolution selected: **Fragment 1.8.9**
- P-1 original 1.1.0 request resolves to 1.8.9: **PASS**
- Compose BOM 2024.02.00 remains resolved: **PASS**
- activity-compose 1.8.2 remains resolved: **PASS**
- L1 359dp + 2.0x localization/identifier stress test: **PASS**
- L1 visibility / containment / pairwise non-overlap assertions: **PASS**
- G4: **BACKLOG**
- G5: **BACKLOG**
- versionName `"1.0.14"`: **PASS**
- versionCode `15`: **PASS**
- Android build: **PASS**
- full debug unit tests: **PASS**
- final implementation Android CI #52: **SUCCESS**

## Additional UI design recommendations

These items are non-blocking.

### Analysis

- The new L1 case protects semantics/layout bounds, but the most valuable future pixel regression is still the exact `359dp + 2.0x + Int.MAX_VALUE labels + long algorithm identifier` state. If screenshot infrastructure is introduced, use this as the first golden rather than adding a broad matrix.
- FlowRow can leave a visually sparse final row. Before changing spacing rules, check the real device layout with Korean text and preserve the current semantic order.
- Keep algorithm metadata visually secondary. Ellipsis plus the existing full-value reproducibility/copy path is preferable to shrinking typography.
- If numeric draw/count values ever move from `Int` to a wider domain or formatted grouping separators, add corresponding localization stress cases rather than weakening the current bounds assertions.

### History

- If History later displays algorithm identifiers, reuse the Analysis policy: compact one-line presentation in lists, complete value in detail/copy surfaces.
- Keep filter state and scroll position together so returning from a run detail does not reset month/group position.
- When G4 is implemented, visually separate exact historical replay from recomputation with current payload/schema versions.

### Statistics

- Keep sample-size context adjacent to EV/statistical outputs even under narrow layouts; wrapping secondary context below the metric is preferable to silently truncating it.
- Future comparison deltas should always state the baseline and should not rely on color alone.
- For large-number formatting, test Korean locale grouping and long accessibility strings independently from chart layout.

### Database

- Exact draw-number search/jump remains the clearest primary navigation path.
- Do not surface an authoritative “last successful sync” timestamp until G5 provides a durable receipt.
- Keep sync recovery actions bounded on the primary card; move diagnostics or export actions into a detail surface if action count grows.

### Common / accessibility

- Preserve visual order and semantics traversal order when FlowRow wraps; TalkBack order should remain source order even when items move to additional rows.
- When visual text is ellipsized, keep the complete value available through semantics, detail, or copy.
- Dependency remediation should remain separate from UI library modernization. A future AGP/Compose modernization should be its own reviewed tranche rather than being smuggled into a Play Console warning fix.
