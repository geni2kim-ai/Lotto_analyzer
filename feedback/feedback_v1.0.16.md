# Feedback — v1.0.16 (v2.5 SHADOW dogfood review)

대상 저장소: `geni2kim-ai/Lotto_analyzer`  
검토 기준 브랜치/HEAD: `main` / `59292715a83c0dafca40821fd7fcc17b057cb2a8`  
리뷰 기준선: `geni2kim-ai/PR-with-codediff-finder` v2.5 stable SHADOW baseline

## 1. Review authority / scope

이번 리뷰는 실제 Lotto Analyzer 소스를 대상으로 v2.5의 운영 원칙을 적용했다.

- deterministic evidence: 기준 HEAD 고정 + changed-path/lineage 추적
- L1: lifecycle, repository, network, analysis engine, ViewModel/UI state를 정적 검토
- L2: 사용자 상태와 데이터 계보에 영향을 주는 finding만 재검토
- Adversarial: 별도 보안/거버넌스/권한 변경이 없어 이번 사이클에서는 요구하지 않음
- G4 replay payload versioning / G5 durable sync receipt는 기존 backlog를 유지하며 임의 구현하지 않음

v1.0.15의 F1/U1/U2 구현도 재확인했다.
- F1: `androidxFragmentVersion=1.8.9`가 `gradle.properties`의 SSOT로 이동했고 CI가 해당 값을 읽음.
- U1: 359dp/2.0x FlowRow geometry evidence를 기록하고 CI에서 출력함.
- U2: semantics 순서와 래핑 후 visual order를 비교하는 테스트가 존재함.

## 2. Findings

### H1 — MainActivity가 ViewModelStore를 우회하여 ViewModel을 직접 생성

파일: `app/src/main/java/com/example/lottoinsight/MainActivity.kt`

현재 Activity가 4개의 ViewModel을 생성자로 직접 만든다. 이 인스턴스들은 Android `ViewModelStore`에 등록되지 않으므로 configuration change/final destroy 시 lifecycle 기반 `onCleared()` 보장이 없다. 특히 History/Statistics/Database ViewModel은 장기 Flow collect를 `viewModelScope`에서 실행하므로 Activity 재생성 때 이전 collector가 남아 중복 관찰·동기화·DB 참조 누적으로 이어질 수 있다.

**조치:** 하나의 lazy `ViewModelProvider.Factory`를 사용하고 Activity의 `ViewModelStore`에서 4개 ViewModel을 조회한다. factory의 DB/Retrofit/repository 구성도 lazy로 만들어 configuration recreation에서 기존 ViewModel이 재사용될 때 불필요한 새 graph를 만들지 않는다.

### M1 — ExpectedValue 저장 lineage의 algorithmVersion이 현재 엔진 버전과 불일치

파일: `core/data/.../ExpectedValueRepositoryImpl.kt`

ExpectedValueRunEntity 저장 시 `algorithmVersion = "1.0.0"`이 하드코딩되어 있으나 현재 canonical 엔진 버전은 `Constants.ALGORITHM_VERSION = "lotto-analysis-deterministic-seed-1.1"`이다. 저장된 EV run의 provenance가 실제 계산 기준과 달라진다.

**조치:** `Constants.ALGORITHM_VERSION`을 사용하고 DAO fake를 이용한 regression test로 고정한다.

### M2 — 분석 결과 생성 성공 후 history 저장 실패가 사용자에게 숨겨짐

파일: `feature/analysis/.../AnalysisViewModel.kt`

분석은 성공해 결과를 화면에 표시한 뒤 `saveAnalysisRun`을 호출하지만, repository가 `AppResult.Error`를 반환해도 아무 상태도 갱신하지 않는다. 사용자는 성공 메시지만 보고 기록이 저장된 것으로 오해할 수 있다.

또한 `isLoading`이 coroutine 내부에서 설정되어 빠른 연속 호출 두 개가 worker 시작 전에 동시에 enqueue될 수 있다.

**조치:** launch 전에 loading reservation을 동기적으로 설정하고, save Error/Loading을 명시적으로 UI state에 반영한다. 두 동작을 unit test로 고정한다.

### M3 — draw dataset 갱신 후 EV 화면 provenance가 stale saved-run처럼 남음

파일: `feature/statistics/.../StatisticsViewModel.kt`

`calculateAndSaveExpectedValues` 성공 후 `expectedValueRunId/recentN`가 저장된다. 이후 draw Flow가 갱신되면 `computeStatistics`는 `prizeIndexes`를 **전체 draw dataset**으로 다시 계산하지만 saved-run metadata는 남긴다. 따라서 UI의 "최근 N회 기준 저장 run" 라벨과 실제 표시 점수의 계산 basis가 달라질 수 있다.

또한 `isExpectedValueSaving`이 계산 이후에야 true가 되어 매우 빠른 중복 호출이 두 개의 저장 작업을 enqueue할 수 있다.

**조치:** draw dataset 재계산 시 saved-run provenance를 clear하고, saving reservation을 launch 전에 설정한다. dataset change + duplicate-call 회귀 테스트를 추가한다.

### M4 — one-shot AppResult.Loading이 UI를 영구 loading 상태로 만들 수 있음

파일:
- `feature/analysis/.../AnalysisViewModel.kt`
- `feature/history/.../HistoryViewModel.kt`
- `feature/statistics/.../StatisticsViewModel.kt`
- `feature/statistics/.../DatabaseViewModel.kt`

현재 repository/engine 구현은 one-shot suspend 호출에서 Loading을 반환하지 않지만, 인터페이스 타입은 허용한다. 일부 caller는 Loading을 받으면 `isLoading/isSyncing/isExpectedValueSaving`을 true로 둔 채 종료하여 이후 완료 이벤트가 없는 상태에서 UI가 영구 잠길 수 있다.

**조치:** one-shot Loading을 terminal incomplete result로 취급해 관련 busy flag를 해제하고 재시도 가능한 메시지를 표시한다.

### L1 — 선택 탭이 configuration recreation에서 초기화됨

파일: `app/src/main/java/com/example/lottoinsight/app/ui/MainAppScreen.kt`

선택 탭이 `remember`만 사용해 Activity recreation 시 Analysis 탭으로 돌아간다.

**조치:** `rememberSaveable`로 변경한다.

## 3. Planned implementation

- Android version: `versionName = "1.0.16"`, `versionCode = 17`
- production fixes: H1 / M1 / M2 / M3 / M4 / L1
- regression tests:
  - `ExpectedValueRepositoryImplTest`
  - `AnalysisViewModelTest`
  - `StatisticsViewModelTest`
- v1.0.15 F1/U1/U2 tests/CI gates 유지
- G4/G5 backlog 유지

## 4. Acceptance

1. 기존 unit tests + 신규 regression tests green.
2. `assembleDebug` green.
3. Fragment 1.8.9 resolution gate 유지.
4. U1 FlowRow geometry evidence step 유지.
5. v2.5 SHADOW 기준으로 변경 diff를 L2 재검토.
6. 완료 후 `evidence/evidence_v1.0.16.md`에 final commit/CI run 및 finding resolution 기록.


## 5. Independent Codex cross-check findings

PR #1 independent Codex review on the first candidate commit found two additional P2 issues. Both were accepted as valid higher-layer findings and fixed before merge.

### X1 — analysis save result could overwrite a newer run

The generated result previously cleared `isLoading` before its history save completed. A second generation could therefore start, and a late save failure from the older run could overwrite the newer run's save/message state.

Resolution:
- keep the generation reservation active through persistence;
- set the final success/error/incomplete state only when that run's save finishes;
- add `generationRemainsReservedUntilHistorySaveCompletes` regression coverage.

### X2 — stale EV completion message survived provenance reset

When the draw dataset changed, `expectedValueRunId` and the displayed basis were cleared, but an old `기대값 계산 완료: 최근 N회 기준` message could remain visible.

Resolution:
- clear the EV completion message when the associated saved-run provenance is invalidated;
- preserve unrelated active sync messages;
- extend the statistics regression test to assert the stale completion message is removed.


## 6. Verification history / dogfood backdata

The candidate was intentionally not declared complete after the first patch. Each new HEAD was re-bound to its own evidence.

- `caf28542...`: first candidate.
- Independent Codex review found X1/X2 (both P2); both accepted and fixed.
- `45ea9e30...`: state-race/provenance fixes. Independent Codex re-review: no major issues.
- CI #63 on `45ea9e30...`: **FAIL** at `assembleDebug` because the intended `Constants.ALGORITHM_VERSION` migration lacked the actual `Constants` import.
- `4cc63b30...`: import fixed and source re-fetched to verify import + canonical constant use.
- CI #64 on `4cc63b30...`: `assembleDebug` PASS, unit-test compilation **FAIL** because two new fake repositories used `?.let(AppResult::Success)`.
- `a4a80365...`: both test constructor references changed to `?.let { AppResult.Success(it) }` and re-fetched to verify no bad reference remains.
- CI #65 on `a4a80365...`: **SUCCESS** — Fragment resolution gate, `assembleDebug`, all debug unit-test tasks, and U1 geometry evidence all passed.

Dogfood failure-family candidates for Leonardo/backdata:
- `REVIEW-INTENT-DRIFT/IMPORT`: code edit references a symbol but required import was not in the actual diff.
- `REVIEW-STATE-RACE/ASYNC-SAVE`: prior asynchronous save outcome can corrupt newer UI state.
- `REVIEW-PROVENANCE/STALE-MESSAGE`: provenance fields reset while explanatory UI text remains stale.
- `TEST-HARNESS/KOTLIN-CONSTRUCTOR-REF`: production build passes but newly added test fake fails to compile.

These are evidence-generation cases, not model-weight training claims.


## 7. X3 follow-up — concurrent draw refresh during EV save

A later independent Codex review of `a4a80365...` found another P2:

- while `saveExpectedValueRun` is suspended, another tab can finish draw refresh;
- the draw observer updates statistics/provenance for the new dataset;
- the old EV save then completes and could restore old indexes/runId over the new dataset.

Resolution:
1. after EV persistence completes, re-read the current draw snapshot;
2. compare it with the exact snapshot used for EV calculation;
3. if they differ, do not restore EV indexes/run provenance;
4. delete the just-created stale EV run so it cannot remain as the newest persisted record from that race;
5. ask the user to recalculate;
6. regression-test the race with a suspended fake save and a draw-flow update.

Commits:
- `80301f95...`: freshness guard + concurrent-refresh regression.
- `e22d3b46...`: stale persisted-run cleanup + DAO/repository regression.

Verification:
- CI #67 on `80301f95...`: SUCCESS.
- CI #68 on `e22d3b46...`: SUCCESS.

Independent latest-head re-review was requested after #67, but GitHub Codex returned a usage-limit message instead of a review. This is recorded as reviewer-capacity BLOCKED, not PASS.
