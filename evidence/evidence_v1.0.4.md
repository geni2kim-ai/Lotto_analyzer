# Evidence — v1.0.4

## 1. Lineage / scope

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- Feedback read before implementation: `feedback/feedback_v1.0.4.md`
- Feedback/base commit: `b4f073dd9000c0cec7e73c792d457874a049a047`
- Implementation commit: `982696a0192043dec19d39db8244ba1bccd4adf6`
- Android version: `versionName = "1.0.4"`, `versionCode = 5`
- Scope completed: A1~A4, B1~B2, C1~C3 implemented; C4 design recommendations recorded below.

## 2. A — 코드 정확성 / 엣지 케이스

### A1. `fetchDrawRange` 부분 성공 + 실패 회차 보존 + targeted retry — 완료

신규 결과형:
- `core/network/.../datasource/DrawFetchReport.kt`
  - blob: `9719fd66f186bd9a6122356e7b2161508c1c0eb7`
  - `successful: List<Draw>`
  - `failedDrawNos: List<Int>`
  - `attemptedCount`, `isPartialSuccess`

수정:
- `LottoRemoteDataSource.kt`
  - blob: `a476dd84d79c42a1e3143099e3e39845b3b07e1e`
- `LottoRemoteDataSourceImpl.kt`
  - blob: `64e68f8ef668631ba92cdabac56aa796d10e441a`

동작:
1. `fetchDrawRange`는 `fetchDraws(drawNos)`에 위임.
2. 요청은 기존과 동일하게 `Semaphore(6)` 제한 병렬 처리.
3. 일부 회차가 실패해도 성공 회차가 하나 이상 있으면 전체를 `Error`로 폐기하지 않고 `AppResult.Success(DrawFetchReport)`로 반환.
4. 실패 회차 번호는 `failedDrawNos`에 정렬된 상태로 보존.
5. 전부 실패한 경우에는 기존 fail-closed 의미를 유지하여 첫 `AppError`를 `AppResult.Error`로 반환.
6. `fetchDraws(drawNos)`를 별도 계약으로 추가하여 UI 재시도가 누락 회차만 정확히 요청할 수 있도록 함.

data layer:
- `DrawSyncReport.kt`
  - blob: `e4d3ad366803e955c095e6ecfd59cc91346610ae`
- `LottoRepository.kt`
  - blob: `f4fbbb04bc6ef5b846767ad78449b0c8d8f1b282`
- `LottoRepositoryImpl.kt`
  - blob: `1a371b0b6500f94894d54276f83c046e22eecbfd`

`syncDraws`는 성공분을 즉시 DB에 upsert하고 `DrawSyncReport(successfulCount, failedDrawNos)`를 반환한다. 따라서 중간 회차 하나가 실패해도 나머지 성공 데이터가 손실되지 않는다.

재시도:
- repository에 `retryDraws(drawNos)` 추가.
- Statistics/Database ViewModel은 부분 성공의 `syncFailedDrawNos`를 상태에 보존.
- inline retry는 전체 동기화를 다시 시작하는 대신 **실패한 회차만** `retryDraws`로 재요청.
- 이는 부분 성공 후 높은 회차가 저장되어 local max가 올라가도 중간 누락 회차가 영구적으로 건너뛰어지는 문제를 방지한다.
- UI 상태 문구 예: `N건 중 M건 성공, K건 실패 (회차 목록)`; 목록은 화면 과밀 방지를 위해 최대 6개를 먼저 표시하고 나머지 건수를 요약.

### A2. `fetchAndSaveLatestDraws` 순차 요청 제거 — 완료

이전:
- `for` 루프에서 `fetchAndPersistSingleDraw`를 회차별 순차 호출.

현재:
- `latestLocalDrawNo + 1 .. latestLocalDrawNo + fetchCount` 범위를 한 번에 `remoteDataSource.fetchDrawRange`에 전달.
- 따라서 A1/A2의 동일한 제한 병렬화(`Semaphore(6)`) 경로를 재사용.
- 성공분을 batch `upsertDraws`하고 기존 `AppResult<Int>` 계약에는 실제 저장 성공 건수를 반환.
- 사용되지 않게 된 순차 helper `fetchAndPersistSingleDraw` 제거.

### A3. 장기 미동기 gap 처리 — 완료

`fetchLatestDrawNo`의 plausibility 정책을 source별로 분리했다.

공식 JSON API:
- `isOfficialLatestPlausible`
- 절대 범위 `1..5000` 유지.
- 기존 local max보다 작은 값은 거부.
- **200회 gap 제한은 적용하지 않음.**
- 따라서 예: local 100회 / official 450회도 정상적으로 450회를 최신으로 인정.

HTML 폴백:
- `isHtmlLatestPlausible`
- 공식 경로의 기본 검증 + `MAX_REASONABLE_HTML_SYNC_GAP = 200`.
- 구조가 깨지기 쉬운 HTML 정규식 후보에만 엄격한 gap 방어를 유지.

결과적으로 신뢰도가 높은 공식 JSON 응답은 장기 미동기 사용자를 막지 않고, HTML fallback의 오탐 방어는 유지된다.

### A4. `fallbackGames` 방어 경로 직접 테스트 가능화 — 완료

수정:
- `NumberGenerator`
  - blob: `cca8ab6f194e6c4b715158d6191e138112cdcd8c`
  - class를 `open`, `weightedSample`을 `internal open`으로 만들어 테스트에서 scripted sampler 주입 가능.
- `AnalysisEngineImpl`
  - blob: `f3a966713b50cf812dac3053efc7beb67e7e40eb`
  - `AnalysisStats`, `buildStats`, `fallbackGames`를 module-internal로 노출.
  - public API 표면은 확장하지 않음.

테스트에서는 실제 후보 풀이 고갈되기를 기다리지 않고 scripted sampler가
1. 이미 선택된 티켓,
2. 신규 fallback A,
3. fallback A 중복,
4. 신규 fallback B
순서로 반환하도록 하여 방어 경로를 직접 검증한다.

검증:
- 기존 티켓과 동일한 fallback 후보 제외.
- 앞서 채택된 fallback과 동일한 후보도 제외.
- 첫 fallback의 `maxOverlap`이 기존 선택 티켓 기준으로 정확.
- 두 번째 fallback의 `maxOverlap`이 **기존 티켓 + 앞선 fallback** 전체 기준으로 정확.

## 3. B — 테스트

### B1. `fetchDrawRange` 부분 실패 테스트 — 완료

신규:
- `core/network/src/test/java/com/example/lottoinsight/core/network/datasource/LottoRemoteDataSourceImplTest.kt`
- blob: `5a4586d7512c65f3702d4b25371066b6b857392c`

시나리오:
- 1~3회 요청.
- 2회만 신규/레거시 API 모두 `IOException`을 발생시키는 fake service 사용.
- 기대 결과:
  - `AppResult.Success`
  - `successful.drawNo == [1, 3]`
  - `failedDrawNos == [2]`
  - `attemptedCount == 3`
  - `isPartialSuccess == true`
  - progress callback은 총 3건 완료를 보고.

### B2. 장기 gap 공식 API 테스트 — 완료

같은 `LottoRemoteDataSourceImplTest`에서:
- `existingMaxDrawNo = 100`
- 공식 API 최신 `ltEpsd = 450`
- 차이 350회 > 과거 제한 200

기대 결과:
- `AppResult.Success(450)`
- HTML result page 호출 횟수 `0`
- 즉 official API가 정상이면 장기 gap이 HTML fallback 제한에 의해 거부되지 않음을 검증.

### A4 관련 추가 테스트

수정:
- `core/engine/src/test/java/com/example/lottoinsight/core/engine/AnalysisEngineTest.kt`
- blob: `07fd90f90eed1fc2de08c1c8f26f4f4a49e8fa4e`

신규 test:
- `fallbackGamesRejectsDuplicatesAndTracksOverlapAgainstEarlierFallbacks`

기존 최근-N, 결과 uniqueness, `maxOverlap`, finite score, seeded repeatability 테스트도 유지.

## 4. C — UI 구현

### C1. 동기화 `LinearProgressIndicator` 연결 — 완료

수정:
- `StatisticsScreen.kt`
  - blob: `a83f3b16078b74233824aa28c00f9639b3e2f85d`
- `DatabaseScreen.kt`
  - blob: `ddb0e7db04f59236904ef3ca9698a11dddef8664`

동작:
- `isSyncing == true`일 때 `LinearProgressIndicator` 표시.
- `syncCompleted / syncTotal`을 0..1로 clamp하여 determinate progress에 연결.
- 기존 텍스트 `완료 / 전체 / %`도 함께 유지하여 색상이나 막대만으로 상태를 전달하지 않음.

상태:
- `StatisticsUiState.kt` blob: `73c9ac39f5d5f20297283544c05fb2a80a89c337`
- `DatabaseUiState.kt` blob: `35c5686aedcee2b925f35a2c90cede796dadea2b`
- 기존 progress fields에 `syncFailed`, `syncFailedDrawNos`를 추가.

### C2. 실패/부분 실패 inline retry — 완료

ViewModel:
- `StatisticsViewModel.kt`
  - blob: `ca2271182f9847b8948442bbd475a954e5f22377`
- `DatabaseViewModel.kt`
  - blob: `9169afb46f588c27f38155f4c5805b1a87052599`

UI:
- 실패 시 화면 내 `OutlinedButton` 표시.
- 전체 실패: `동기화 다시 시도`.
- 부분 실패: `실패한 K개 회차 다시 시도`.
- 부분 실패 버튼은 저장된 `syncFailedDrawNos`만 repository `retryDraws`로 전달.
- 재시도 자체가 또 부분 실패하면 남은 실패 목록을 다시 보존하므로 반복 복구 가능.
- 실패/부분 실패 상태의 sync message는 error color로 표시.

### C3. EV 당첨금 유효 표본 수 노출 — 완료

Statistics EV 각 항목의 보조 텍스트:
- 평균 1등 금액
- **유효 표본 `prizeSampleCount`회**
- normalized score

따라서 v1.0.3 A4의 정책(당첨금 null/비양수 회차는 EV 표본에서 제외)을 결과 화면에서도 사용자가 확인할 수 있다.

## 5. C4 — 추가 UI 디자인 개선 제안

이번 사이클의 요구 구현(C1~C3)은 적용했으며, 아래 항목은 다음 UI tranche 제안으로만 기록한다.

### Analysis

- 설정 카드가 결과 생성 후에도 화면 상단을 크게 점유하므로, 결과가 존재할 때는 `분석 설정 요약` 한 줄 + expand/collapse 형태로 축소하는 것이 좋다.
- 추천 결과 상단에 `최신 회차 / 분석 범위 / EV 사용 / 알고리즘 버전`을 동일 규격 status chips로 표시하면 결과 provenance를 빠르게 파악할 수 있다.
- GAME별 4개 점수는 긴 숫자 문장보다 `종합 점수`를 1차로 두고, 빈도/연속/홀짝/EV는 expandable detail 또는 compact meter로 분리하는 편이 정보 위계가 명확하다.
- 분석 실패 시 현재 메시지 영역에 `데이터 동기화` 또는 `다시 분석`과 같은 다음 행동을 바로 제공하는 것이 좋다.

### History

- 카드 전체 tap으로만 펼쳐지는 상태는 발견성이 낮으므로 trailing chevron과 `상세` label을 추가하는 것이 좋다.
- run id보다 `실행 시각 / 기준 회차 / 최근 N회 / 게임 수`를 primary metadata로 올리고 run id는 secondary metadata로 낮추는 것이 사용자 관점에서 의미가 크다.
- 이력이 누적되면 `기간 / 기준회차 / EV 사용 여부` filter와 정렬이 필요하다.
- 두 실행을 선택해 번호 overlap, 설정 차이, score 차이를 비교하는 compare mode가 장기 사용 가치가 높다.

### Statistics

- 45개 EV 항목을 처음부터 모두 동일한 카드로 표시하기보다 `요약 → Top 5/10 → 전체 보기`로 progressive disclosure하는 것이 좋다.
- 이번에 추가한 `유효 표본 수`가 너무 작은 번호에는 `표본 적음` 보조 상태를 표시할 수 있다. 단, 임의의 신뢰도 등급 대신 명시적 표본 수와 정책 기준을 사용해야 한다.
- Frequency / EV / 패턴 통계를 segmented control 또는 tabs로 분리하면 한 화면의 스크롤·인지 부하를 줄일 수 있다.
- 동기화 상태 영역은 향후 reusable `SyncStatusCard`로 추출해 Database와 동일한 UI/문구/재시도 정책을 공유하는 것이 좋다.

### Database

- 현재 최근 300회 목록에 회차 번호 검색/점프를 추가하면 오래된 데이터 접근성이 크게 개선된다.
- 상단을 `저장 회차 / 최신 회차 / 마지막 성공 동기화 / 현재 상태`의 compact status card로 정리하는 것이 좋다.
- 각 회차 row는 번호를 primary로 두고 당첨자/당첨금 상세는 expand 또는 detail sheet로 이동하면 스캔 밀도가 높아진다.
- 부분 실패가 발생한 경우 현재처럼 회차 목록을 텍스트로 제공하되, 추후 `실패 K건 보기`를 누르면 bottom sheet/detail list로 전체 실패 회차를 확인할 수 있게 하면 6개 초과 실패도 탐색하기 쉽다.

### 공통

- Statistics와 Database에 중복된 sync progress/error/retry UI를 공통 Compose component로 추출하면 상태 표현 drift를 방지할 수 있다.
- progress/status는 `role=status`에 대응하는 접근성 semantics와 상태 설명을 함께 제공하는 것이 좋다.
- 오류는 색상만으로 구분하지 않고 icon + 문구 + action을 함께 유지해야 한다.
- Dynamic Type에서 상단 Row의 제목/버튼이 충돌하는 화면은 좁은 폭에서 Column로 전환하는 responsive rule을 두는 것이 좋다.
- 모든 주요 action은 최소 48dp touch target을 유지한다.

## 6. CI / 검증

GitHub Actions:
- Workflow: `Android CI`
- Implementation run: **#5**
- run id: `36490700663`
- head: `982696a0192043dec19d39db8244ba1bccd4adf6`
- conclusion: **SUCCESS**

세부:
- JDK 17 setup: SUCCESS
- Android SDK / API 36: SUCCESS
- `./gradlew --no-daemon assembleDebug`: **SUCCESS**
  - log: `BUILD SUCCESSFUL in 3m 24s`
- `./gradlew --no-daemon testDebugUnitTest`: **SUCCESS**
  - `core:engine:testDebugUnitTest`: executed
  - `core:network:testDebugUnitTest`: executed
  - 기존 `core:model`, `feature:ui` unit-test tasks도 executed
  - log: `BUILD SUCCESSFUL in 40s`

비차단 경고:
- 기존 AGP 8.2.2 / compileSdk 36 조합에 대해 더 최신 Android Gradle Plugin을 권장하는 경고가 유지된다.
- GitHub runner의 Node 20 deprecation에 따라 `actions/checkout@v4`, `android-actions/setup-android@v3`가 Node 24로 강제 실행된다는 경고가 있다.
- 두 경고 모두 이번 run의 build/test 성공을 막지 않았으며 dependency/toolchain upgrade는 별도 tranche가 적절하다.

## 7. 변경 파일 핵심 blob SHA

- `DrawFetchReport.kt`: `9719fd66f186bd9a6122356e7b2161508c1c0eb7`
- `LottoRemoteDataSource.kt`: `a476dd84d79c42a1e3143099e3e39845b3b07e1e`
- `LottoRemoteDataSourceImpl.kt`: `64e68f8ef668631ba92cdabac56aa796d10e441a`
- `DrawSyncReport.kt`: `e4d3ad366803e955c095e6ecfd59cc91346610ae`
- `LottoRepository.kt`: `f4fbbb04bc6ef5b846767ad78449b0c8d8f1b282`
- `LottoRepositoryImpl.kt`: `1a371b0b6500f94894d54276f83c046e22eecbfd`
- `AnalysisEngine.kt`: `f3a966713b50cf812dac3053efc7beb67e7e40eb`
- `NumberGenerator.kt`: `cca8ab6f194e6c4b715158d6191e138112cdcd8c`
- `LottoRemoteDataSourceImplTest.kt`: `5a4586d7512c65f3702d4b25371066b6b857392c`
- `AnalysisEngineTest.kt`: `07fd90f90eed1fc2de08c1c8f26f4f4a49e8fa4e`
- `StatisticsViewModel.kt`: `ca2271182f9847b8948442bbd475a954e5f22377`
- `DatabaseViewModel.kt`: `9169afb46f588c27f38155f4c5805b1a87052599`
- `StatisticsScreen.kt`: `a83f3b16078b74233824aa28c00f9639b3e2f85d`
- `DatabaseScreen.kt`: `ddb0e7db04f59236904ef3ca9698a11dddef8664`
- `app/build.gradle.kts`: `ec20b6aa865e70bbc250c400107b058c4e2a52af`

## 8. 완료 상태

- A1: 완료 — 부분 성공 결과 + 실패 회차 보고 + targeted retry
- A2: 완료 — `fetchAndSaveLatestDraws` 병렬 range 재사용
- A3: 완료 — 공식 API 장기 gap 허용 / HTML에만 200 gap 제한
- A4: 완료 — fallback 직접 단위테스트 가능화 및 검증
- B1: 완료 — 부분 실패 성공분/실패 목록 테스트
- B2: 완료 — 공식 API 200 초과 장기 gap 테스트
- C1: 완료 — Statistics/Database LinearProgressIndicator
- C2: 완료 — 전체/부분 실패 inline retry
- C3: 완료 — EV `prizeSampleCount` 표시
- C4: 화면별 추가 UI 제안 기록 완료
- Android CI run #5: SUCCESS
- `assembleDebug`: SUCCESS
- `testDebugUnitTest`: SUCCESS
