# Evidence — v1.0.8

## 1. Lineage

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- Feedback: `feedback/feedback_v1.0.8.md`
- v1.0.7 evidence commit: `6e7a1031376bd8aa35979d165be08bc2d1b180cb`
- v1.0.8 feedback/base commit: `90995ca8f6e9fa294478d8f7f5ed9ebeb6bc25fe`
- Initial v1.0.8 implementation commit: `673fc2273a1e3c5c22c806b9884409df82b2b507`
- Follow-up compile/test correction commits:
  - `774028633983071c4a4c0b9fec16438eed07e74a`
  - `507a91a2c07c7bae4d5d6b291db9f7db59b82282`
  - `84a42d37d59797b5fb16b97fb3c73464c67a4d7d`
  - `4d5485cd0967da583021cb57bf535b8214ca66cd`
- Final code-bearing head before this evidence: `4d5485cd0967da583021cb57bf535b8214ca66cd`
- Android version: `versionName = "1.0.8"`, `versionCode = 9`

F5/F6 remain backlog by feedback permission; F1~F4 are implemented.

## 2. F1 — decouple SyncStatusCardTest from hard-coded Korean UI copy

**Status: PASS**

Problem:
- v1.0.7 test asserted action controls by literal Korean copy:
  - `실패한 7개 회차 다시 시도`
  - `실패 목록 보기`
- harmless copy changes could therefore fail the semantics regression test.

Implementation:
- action controls now expose stable test tags:
  - `SYNC_RETRY_ACTION_TAG = "sync-retry-action"`
  - `SYNC_FAILURE_DETAILS_ACTION_TAG = "sync-failure-details-action"`
- the semantics test locates actions with `onNodeWithTag(...)`, then asserts they exist and expose click actions.
- the live-region state-description expectation is no longer duplicated as a hard-coded UI sentence. The test derives it through:
  - `resolveSyncStatusPresentation(...).announcement`

Production:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/components/SyncStatusCard.kt`
- blob: `c98a9d0181ebcccee2986dd40d86d81fa5ba80fa`

Test:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/SyncStatusCardTest.kt`
- blob: `87e911e9a8c30602a331295a65511c6c7a778c19`

Key assertions:
- Polite live-region node exists with state description derived from the presentation model.
- live-region node has no click action.
- retry tag node exists and has a click action.
- failed-details tag node exists and has a click action.

Result:
- the test remains coupled to semantic behavior and presentation output, but not to incidental action-button Korean copy.

## 3. F2 — evidence / Robolectric SDK lineage correction

**Status: PASS**

The v1.0.8 review reported an evidence/code mismatch between API 34 and an observed `@Config(sdk = [35])`. The commit lineage explains the discrepancy:

- `315abea8bde450bbcfa61c20a803df95c41b6b79`: `@Config(sdk = [35])`
- `997a035e0ffea5170e99fc00fecbe63756bd1800`: `@Config(sdk = [35])`
- `1f10efcaff29eb488234aada3b776afe633f6f91`: repaired to `@Config(sdk = [34])`
- v1.0.7 evidence head `6e7a103...`: final file remains `@Config(sdk = [34])`
- v1.0.8 final code: `@Config(sdk = [34])`

Current test file:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/SyncStatusCardTest.kt`
- blob: `87e911e9a8c30602a331295a65511c6c7a778c19`

This evidence is explicitly bound to the final committed source: **Robolectric SDK 34**. SDK 35 belongs to intermediate setup commits, not the final v1.0.7/v1.0.8 state.

The new responsive-container tests also use:
- `@Config(sdk = [34])`

File:
- `ResponsiveActionContainerTest.kt`
- blob: `7325abd0d73ac4d65c5ec5e19d407c7ce61f3f28`

## 4. F3 — Kotlin interpolation checker block-comment heuristic

**Status: PASS**

Problem:
- the previous checker tracked block comments using line-prefix logic such as `stripped.startswith("/*")`.
- that was a heuristic rather than lexical recognition and could be confused by comment markers in literal text.

Implementation:
- replaced the prefix heuristic with a small stateful Kotlin-aware lexer.
- `strip_kotlin_comments(...)` now tracks:
  - nested `/* ... */` block comments
  - `//` comments
  - normal string literals
  - character literals
  - multiline triple-quoted strings
- comment markers inside string/char/triple-string literals are preserved rather than changing comment state.
- interpolation scanning is applied after real comments are stripped, so executable Kotlin string interpolation is still checked.

Checker:
- `tools/check_kotlin_interpolation.py`
- blob: `c5b126468ff5db0aa1feedd33b4cbdaaeb1528ca`

Regression tests:
- `tools/test_check_kotlin_interpolation.py`
- blob: `5a83b281107375fa2410a851c9d7d9ec1ed6d2cf`

Python assertions cover:
1. `/* ... */` inside a normal string does not start comment state and interpolation is still detected.
2. real multiline block comments are ignored.
3. nested block comments are ignored.
4. triple-quoted strings preserve comment markers and still check interpolation.
5. inline `//` comments after executable code are ignored.

Documentation:
- `CONTRIBUTING.md`
- blob: `a6a1623fe5b7fd60d685f744ea9ace4de0f41308`
- documents the lexer scope and explicitly states it is intentionally not a complete Kotlin parser.

CI:
- `.github/workflows/android-ci.yml`
- blob: `d8f984500f3ae0bdb79f0b9e4b4652620a479dcc`
- adds `python3 -m unittest tools.test_check_kotlin_interpolation` before the repository interpolation gate.

Final run #29:
- checker unit test step: **SUCCESS**
- repository interpolation convention step: **SUCCESS**

## 5. F4 — responsive SyncStatusCard action container

**Status: PASS**

Problem:
- retry / failure-detail actions were always rendered in a horizontal `Row`.
- large font scale or narrow width could force labels/buttons into an unusable layout.

Implementation:
- introduced reusable `ResponsiveActionContainer`.
- layout decision:
  - Column when `availableWidth < 360.dp`
  - Column when `fontScale >= 1.6f`
  - otherwise Row
- when stacked, both actions receive `fillMaxWidth()`.
- in horizontal mode the primary action receives RowScope `weight(1f)`, while the secondary action keeps natural width.
- `SyncStatusCard` now delegates its retry/detail actions to this primitive.

Production:
- `feature/ui/src/main/java/com/example/lottoinsight/feature/ui/components/ResponsiveActionContainer.kt`
- blob: `3f8d42654afe7a390179d0f4f43b5e209c27ad2a`
- `SyncStatusCard.kt`
- blob: `c98a9d0181ebcccee2986dd40d86d81fa5ba80fa`

Decision function:
- `shouldStackActions(...)`
- used by production layout and directly unit-tested for deterministic width/font-scale policy.

Tests:
- `feature/ui/src/test/java/com/example/lottoinsight/feature/ui/components/ResponsiveActionContainerTest.kt`
- blob: `7325abd0d73ac4d65c5ec5e19d407c7ce61f3f28`

Key assertions:
- 200% font scale causes the Compose container to expose the Column layout test tag.
- at 420dp / 1.0 font scale, `shouldStackActions(...)` returns false, selecting horizontal layout.
- this separates the pure responsive decision from Robolectric root-measurement behavior.

### Negative attempts and corrections

The final implementation retained transparent CI history:

- **Run #25 — FAILURE**
  - cause: inaccessible top-level Compose `weight` import.
  - fix: `774028633983071c4a4c0b9fec16438eed07e74a`
  - RowScope member extension is now used without the inaccessible import.

- **Run #26 — FAILURE**
  - assemble succeeded; unit-test compile failed due an unavailable explicit `assertExists` import.
  - fix: `507a91a2c07c7bae4d5d6b291db9f7db59b82282`

- **Run #27 — FAILURE**
  - assemble succeeded; only `regularFontAndWideWidthKeepHorizontalLayout` failed.
  - cause: Robolectric root constraints made the wide-layout existence assertion environment-dependent.
  - fix:
    - `84a42d37d59797b5fb16b97fb3c73464c67a4d7d`
    - `4d5485cd0967da583021cb57bf535b8214ca66cd`
  - production layout decision was extracted to `shouldStackActions`.
  - large-font Column behavior remains a Compose test; regular wide-layout policy is asserted through the pure decision function.

## 6. Backlog

### F5 — History month grouping / filters

**Status: BACKLOG (allowed by feedback)**

Recommended future scope:
- monthly grouping and sticky headers
- algorithm-version filter
- EV-use filter
- only introduce once history volume makes scan cost meaningful

### F6 — reproduction-copy payload expansion

**Status: BACKLOG (allowed by feedback)**

Recommended future replay/support payload:
- algorithm version
- deterministic seed
- applied recent-N
- data start/end draw numbers
- normalized weight configuration
- app version / payload schema version
- optional timestamp only as context, not as an input to deterministic replay

## 7. Version

**Status: PASS**

- `versionName = "1.0.8"`
- `versionCode = 9`

File:
- `app/build.gradle.kts`
- blob: `12f24ff28664698efbc6898f8f1f5966c282b837`

## 8. Final CI / verification

Final code-bearing head:
- `4d5485cd0967da583021cb57bf535b8214ca66cd`

Android CI:
- Run number: **#29**
- Run id: `36520200925`
- Conclusion: **SUCCESS**

Steps:
- interpolation checker unit tests: **SUCCESS**
- interpolation repository gate: **SUCCESS**
  - log: `Kotlin interpolation convention check passed.`
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - `BUILD SUCCESSFUL in 3m 27s`
- `./gradlew --no-daemon testDebugUnitTest`: **SUCCESS**
  - `BUILD SUCCESSFUL in 49s`
- `feature:ui:testDebugUnitTest`: executed successfully

Existing non-blocking warning:
- AGP 8.2.2 recommends a newer Android Gradle Plugin for `compileSdk = 36`.

## 9. Final checklist

- F1 brittle Korean action-copy assertions removed: **PASS**
- F2 evidence / test-runner SDK lineage aligned to final code: **PASS**
- F3 block-comment heuristic replaced with lexer + regression tests: **PASS**
- F4 responsive Row→Column action primitive + tests: **PASS**
- F5: **BACKLOG**
- F6: **BACKLOG**
- Android build: **PASS**
- Unit tests: **PASS**
- Android CI #29: **SUCCESS**

## Additional UI design recommendations

These are non-blocking follow-up recommendations.

### Analysis
- When F6 is implemented, make the copied reproduction payload visibly versioned (for example `replay-v1`) so future fields can evolve without ambiguous parsing.
- The reproduction disclosure could show a short human-readable fingerprint derived from the full payload, while keeping the full token behind Copy.
- At large font scale, apply the new responsive action-container primitive to any two-button Analysis rows that can wrap poorly.

### History
- Keep F5 backlog until list volume justifies it, but design filters and sticky month headers together so filtered results do not produce confusing empty month sections.
- If exact replay is eventually added, visually distinguish immutable historical replay from “current data recomputation” with different labels and supporting descriptions.

### Statistics
- Keep `prizeSampleCount` permanently visible even if more calculation-policy text moves behind disclosure.
- Consider applying the same responsive action primitive to Top-10 / full-list controls if localized copy grows.
- If rank comparisons are later added, expose the comparison baseline explicitly rather than showing an unexplained up/down indicator.

### Database
- The search field clear affordance is already present; a later exact-range search should preserve the fast exact-draw path as the primary interaction.
- If sync failure actions gain a third action, do not expand the current responsive container into an overcrowded three-button row; stack or move tertiary actions into the details surface.
- Add a durable last-successful-sync receipt before displaying “last sync” timestamps.

### Common / accessibility
- Reuse `ResponsiveActionContainer` for other compact action groups only when the primary/secondary hierarchy is clear; avoid making it a generic layout substitute.
- Add a font-scale matrix (1.0, 1.3, 1.6, 2.0) to screenshot or semantics regression coverage when UI-test infrastructure expands.
- Continue keeping action controls outside merged live-region semantics, with stable test tags for behavior tests and presentation-model assertions for status text.
