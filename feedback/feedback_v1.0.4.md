# Feedback — v1.0.4 (lotto_analyzer)

대상: geni2kim-ai/Lotto_analyzer (public), main 브랜치, Lotto Insight Android 앱 (Kotlin + Jetpack Compose, 멀티모듈 Clean Architecture).

리뷰 기준: 2026-09-29 doldol 심층 코드 리뷰 — `evidence/evidence_v1.0.3.md`와 구현 커밋(`6f799d4`), CI 보정 커밋(`6447d7d`)의 실제 diff를 원문 정독.

## 0. v1.0.3 evidence 검증 결과 (모두 확인)

- A1: `fetchLatestDrawNo(existingMaxDrawNo)` 계약 확장, 공식 API(`getDraws("all")`의 `ltEpsd`) 우선 + HTML 폴백, `MAX_PLAUSIBLE_DRAW_NO=5000`·`MAX_REASONABLE_SYNC_GAP=200` 검증, HTML 정규식 `d-trigger_txt.{0,80}?([0-9]{1,5})` 교정 — 모두 코드로 확인.
- A2: `fetchDrawRange`의 `coroutineScope + async/awaitAll`, `Semaphore(6)`, `AtomicInteger`, progress callback, `syncDraws`→`fetchAllDraws` 전달, `StatisticsViewModel`·`DatabaseViewModel`의 `syncMessage`(완료/전체/백분율)와 `syncCompleted`/`syncTotal` 보존 — 모두 확인.
- A3: `fallbackGames`의 `usedTickets` 바인딩·`usedTickets.add` 중복 제외·`comparisonTickets` 기준 `maxOverlap` 기록·`maxAttempts` 상한 — 확인.
- A4: null/비양수 `firstPrize` 회차를 EV 분모와 prize sample에서 제외하는 정책이 KDoc에 명시되고 코드에 정확히 구현됨 (`prizeEligibleDraws` 필터 + `requireNotNull` 안전). `appearanceCount`/`appearanceRate`는 전체 회차 기준 서술 통계로 유지. 확인.
- A5: 누적합 weighted selection이 `NumberGenerator.weightedSample`으로 단일화되고 `AnalysisEngineImpl`이 이를 사용, 구 sentinel 방식 제거, 총 가중치 0 이하 시 균등 random fallback — 확인.
- B1: 모듈 루트 구버전 `.kt` 48개 삭제, `core/engine` 루트에 `src`+`build.gradle.kts`만 남음 — 확인.
- B2: `LegacyRemoteDrawResponseValidator` / `NewRemoteDrawResponseValidator` 네이밍 정리 및 참조 정리 — 확인.
- B3: README clone URL(`lotto_analyzer.git`), `settings.gradle.kts`의 `rootProject.name = "lotto_analyzer"`, `app/build.gradle.kts`의 namespace/applicationId 의도 주석, versionName `1.0.3` / versionCode `4` — 확인.
- C1: `.github/workflows/android-ci.yml` 추가, run #2(`36488468298`) Status **SUCCESS** (assembleDebug + testDebugUnitTest) — Actions 페이지에서 직접 확인.
- C2: `PrizeIndexCalculatorTest` — EV=당첨금 유효 표본 출현율×평균 당첨금(500.0/1000.0 정확 검증), null-prize 회차 제외 정책, appearanceRate=2/3 유지, normalized 0..1, rank 1..45 — 의미 있는 검증. 확인.
- C3: `AnalysisEngineTest` — 다양한 번호 분포 fixture, 입력 순서 무관 최근-N 선택, 게임 uniqueness, 각 게임의 `maxOverlap`이 앞선 게임과의 실제 최대 교집합과 일치, score finite, seeded 재현성 — 확인.
- D1: `computeStatistics`의 가짜 `CalendarStatistics` 0값 생성 제거, 미구현 상태는 `calendarStats = null`로 명시 — 확인.
- D2: 화면별(Analysis/History/Statistics/Database/공통) UI 개선 제안이 evidence에 정리됨 — 확인.
- 저장소 metadata 제안(description/topics)은 owner action으로 evidence에 기록됨 — 확인.

## A. 코드 정확성 · 엣지 케이스

### A1. `fetchDrawRange` 폴백 경로의 전체 실패 문제 (중요)
- `core/network/.../datasource/LottoRemoteDataSourceImpl.kt`의 `fetchDrawRange`: 결과 중 단 하나의 `AppResult.Error`라도 있으면 전체를 `AppResult.Error(firstError)`로 반환하고, 성공적으로 받은 회차 데이터도 전부 버림.
- 폴백 경로는 수백 개 회차의 개별 HTTP 요청이므로, 한 회차의 일시적 실패(서버 5xx, 타임아웃)가 전체 동기화를 무효화하는 것은 과도하게 엄격함.
- 보완: 실패한 회차를 수집하되 성공분은 반환하는 부분 성공(partial success) 형태로 변경하고, 스킵된 회차 번호 목록을 함께 보고할 것. 예) `SyncReport(successful: List<Draw>, failedDrawNos: List<Int>)` 형태의 결과형 도입, 또는 `AppResult.Success`에 경고 메타 포함. UI에서는 "N건 중 M건 성공, K건 실패(회차 목록)"로 표시하고 재시도 진입점을 제공할 것.

### A2. `fetchAndSaveLatestDraws`의 순차 요청 잔존
- `core/data/.../repository/LottoRepositoryImpl.kt`의 `fetchAndSaveLatestDraws`: `fetchAndPersistSingleDraw`를 for 루프로 순차 호출함. A2 보완 취지(병렬화)와 같은 동기화 경로이므로 `fetchDrawRange`를 재사용하거나 병렬 구조로 전환할 것. 사용처가 항상 소수 회차라면 주석으로 의도를 명시할 것.

### A3. 장기 미동기 시 최신 회차 감지 불가
- `fetchLatestDrawNo`의 `isPlausibleLatest`: 기존 로컬 최신 회차 대비 200회차 초과 차이를 전부 거부함. 수년간 미동기한 사용자는 공식 API(`getDraws("all")`)가 정상을 반환해도 최신 회차를 절대 찾지 못하고 `ParseError("Latest draw number was not found in a plausible range")`로 종료됨.
- 보완: 공식 API는 신뢰 소스이므로 해당 경로에서는 gap 제한을 완화(또는 별도 에러 `SyncGapTooLarge`로 구분해 전체 범위 재동기화 안내로 연결)하고, 엄격한 gap 검증은 HTML 폴백에만 적용할 것.

### A4. `fallbackGames` 경로의 테스트 부재
- A3에서 수정한 fallback 중복 제거 로직은 사실상 도달이 극히 어려운 방어 경로(후보 조합 풀이 약 800만 개)이며 단위 테스트가 없음. 수정 코드가 검증되지 않은 채로 유지되고 있음.
- 보완: `fallbackGames`를 `internal`로 노출해 직접 테스트하거나, 도달 불가능한 방어 코드라면 주석으로 명시할 것. 테스트는 "이미 선택된 티켓과 동일한 fallback 티켓이 제외되고, 앞선 fallback 티켓과의 maxOverlap이 정확히 기록되는지"를 검증할 것.

## B. 테스트

### B1. `fetchDrawRange` 부분 실패 허용 테스트
- A1 보완 후: 일부 회차 실패 시 성공분 반환 + 스킵 회차 목록 보고를 검증하는 테스트 추가.

### B2. 장기 gap 시나리오 테스트
- `existingMaxDrawNo` 대비 200 초과 gap 상황에서 공식 API 경로가 최신 회차를 정상 반환하는지 검증하는 테스트 추가.

## C. UI 관찰

v1.0.3 D2 제안 중 이번 버전에 구현을 요청하는 항목:

### C1. 동기화 진행률 LinearProgressIndicator 연결
- `StatisticsViewModel`/`DatabaseViewModel`에 `syncCompleted`/`syncTotal` 상태가 이미 준비되어 있으나 화면은 텍스트 백분율만 표시 중. 동기화 중에 `LinearProgressIndicator`를 연결해 진행 상태를 시각적으로 표시할 것.

### C2. 동기화 실패 시 inline 재시도
- 현재 동기화 실패는 "동기화 실패"/"수집 실패" 문구만 노출됨. Database 화면의 동기화 영역과 Statistics 화면에 실패 시 inline retry action을 함께 제공할 것.

### C3. EV 항목에 당첨금 유효 표본 수 표시
- Statistics 화면의 EV 항목에 `prizeSampleCount`를 함께 보여 A4의 null-prize 정책을 사용자에게 투명하게 전달할 것.

### C4. UI 디자인 개선 제안 환영
- UI 디자인 관점에서도 개선할 부분이 있으면 제안해줘. 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 정리해줘.

## 완료 조건

1. 위 항목(A1~~A4, B1~B2, C1~C4)을 수행하고, 추가 UI 개선 제안은 evidence에 정리.
2. `evidence/evidence_v1.0.4.md` 파일을 만들어 완료 내용을 항목별로 정리.
3. 작업을 시작하기 전에 이 feedback 파일을 읽었다는 것을 먼저 확인한다.
