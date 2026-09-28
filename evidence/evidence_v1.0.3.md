# Evidence — v1.0.3

## 1. Lineage / scope

- Repository: `geni2kim-ai/Lotto_analyzer`
- Branch: `main`
- Feedback consumed before implementation: `feedback/feedback_v1.0.3.md`
- Feedback/base commit: `e252d567f0dbd3ab79f871a2b8054b893ef3860e`
- Main implementation commit: `6f799d4ec86d84b6ce4cd880c8b16daaf1e5c311`
- CI correction commit: `6447d7d24f31c4ce7c2149e4f66c1e96d6096fc4`
- Scope: feedback A1~A5, B1~B3, C1~C3, D1 implemented; D2 recorded as design recommendations below.

## 2. A — 코드 정확성 / 엣지 케이스

### A1. 최신 회차 조회의 API 우선화 + HTML 폴백 검증 — 완료

수정:
- `core/network/src/main/java/com/example/lottoinsight/core/network/datasource/LottoRemoteDataSourceImpl.kt`
  - blob: `f994543bb1ddc31858bd01e97c0f3a47c9a21381`
- `LottoRemoteDataSource.fetchLatestDrawNo`가 `existingMaxDrawNo`를 받을 수 있도록 계약 확장.

동작:
1. `getDraws("all")` 공식 JSON 응답의 `ltEpsd`를 최신 회차의 1차 근거로 사용.
2. JSON 경로를 사용할 수 없을 때만 결과 페이지 HTML 정규식 파싱으로 폴백.
3. HTML/JSON 후보 모두 양수, 절대 상한(`MAX_PLAUSIBLE_DRAW_NO = 5000`), 기존 로컬 최신 회차 대비 합리적 차이(`MAX_REASONABLE_SYNC_GAP = 200`)를 검증.
4. 커밋 직전 HTML 폴백의 raw-string 정규식 이스케이프를 재검토해 `d-trigger_txt.{0,80}?([0-9]{1,5})`로 교정.

### A2. 폴백 범위 조회 병렬화 + 진행률 노출 — 완료

수정:
- `LottoRemoteDataSource.fetchAllDraws/fetchDrawRange`에 선택적 progress callback 추가.
- `LottoRepository.syncDraws`가 progress callback을 data source까지 전달.
- `LottoRemoteDataSourceImpl.fetchDrawRange`:
  - `coroutineScope + async/awaitAll`
  - `Semaphore(6)`로 동시 HTTP 요청 수 제한.
  - `AtomicInteger`로 완료 건수 집계.
- 신규 API 일괄 응답을 파싱하는 경로도 동일 progress callback을 발생시킴.
- `StatisticsViewModel`, `DatabaseViewModel`에서 `완료 / 전체 (백분율)`을 `syncMessage`로 상태에 반영.
- 기존 `StatisticsScreen`, `DatabaseScreen`이 `syncMessage`를 렌더링하므로 진행률이 화면에 노출됨.
- `syncCompleted`, `syncTotal`도 각 UI state에 보존하여 향후 progress bar에 바로 연결 가능.

### A3. fallback 게임 중복 방지 — 완료

수정:
- `core/engine/src/main/java/com/example/lottoinsight/core/engine/AnalysisEngine.kt`
  - blob: `99516ce148f08febe7d00ace6f4e68b62a6f7a21`

동작:
- fallback 진입 시 이미 선택된 티켓을 `usedTickets` set에 바인딩.
- 신규 fallback 티켓이 기존/앞선 fallback 티켓과 동일하면 제외.
- fallback 티켓도 이전 선택 티켓들과 실제 교집합을 계산하여 `maxOverlap` 기록.
- 무한 루프 방지를 위해 fallback 시도 횟수에 상한을 둠.

### A4. null 1등 당첨금 정책 명시 및 계산 수정 — 완료

수정:
- `core/engine/src/main/java/com/example/lottoinsight/core/engine/calculator/PrizeIndexCalculator.kt`
  - blob: `04257cb1694d50051b69738043ceab12502724ea`

정책:
- `appearanceCount / appearanceRate`: 저장된 전체 회차에 대한 서술 통계로 유지.
- EV(`rawIndex`) 계산: `firstPrize == null` 또는 비양수인 회차는 **당첨금 유효 표본의 분모와 번호별 prize sample에서 모두 제외**.
- 따라서 당첨금 메타데이터가 누락된 회차가 EV를 인위적으로 0 방향으로 낮추지 않음.
- 코드 KDoc에 정책을 명시.

### A5. 가중치 샘플러 단일화 — 완료

수정:
- `NumberGenerator.kt` blob: `3468607d333a9cc61a097d9030ca6ce5d9b91cca`
- `AnalysisEngine.kt` blob: `99516ce148f08febe7d00ace6f4e68b62a6f7a21`

동작:
- 누적합 weighted selection을 `NumberGenerator.weightedSample` 한 곳으로 통합.
- `AnalysisEngineImpl`이 같은 구현을 사용하도록 변경.
- 기존 `selectedIndex == items.lastIndex` sentinel 방식 제거.
- 0 이하 총 가중치에서는 균등 random fallback.

## 3. B — 저장소 위생

### B1. 모듈 루트 구버전 Kotlin 중복 제거 — 완료

확인:
- 변경 전 저장소 전체에서 `sourceSets` 커스터마이징 검색 결과 없음.
- 각 Android/Kotlin 모듈은 Gradle 기본 `src/main/java` sourceSet 사용.

조치:
- `/src/` 밖 모듈 루트에 남아 있던 구버전 Kotlin 파일 **48개** 삭제.
- 구현 후 Git tree 재검증에서 `/src/` 밖 `.kt` blob은 **0개**.
- 별도로 활성 레거시 validator 파일 1개는 B2에 따라 rename 처리됨.

### B2. validator 네이밍 정리 — 완료

- 레거시 validator:
  - `RemoteDrawResponseValidator`
  - → `LegacyRemoteDrawResponseValidator`
  - blob: `a7d604907aa66671ed1b881f467a3a0dfc07a3f0`
- 신규 API validator:
  - `NewRemoteDrawResponseValidator`
  - blob: `cd193b07ea3395066f46d1e83d6cb40dda6e526c`
- data source 및 unit test의 참조도 새 이름으로 정리.

### B3. 저장소명 / 표시 정책 정리 — 완료

- README clone URL:
  - `https://github.com/geni2kim-ai/lotto_analyzer.git`
  - README blob: `93067fb34460e24e1a0127f9e99b57b3a56c57fa`
- `settings.gradle.kts`:
  - `rootProject.name = "lotto_analyzer"`
  - blob: `85c7f11531f22e6ed43a6151b73c589a7592495d`
- `app/build.gradle.kts`:
  - `namespace = com.example.lottoinsight`는 소스/package 호환성용,
  - `applicationId = com.aimaestro.lottoanalyzer`는 설치/스토어 identity라는 의도를 주석으로 명시.
  - versionName `1.0.3`, versionCode `4`로 갱신.
  - blob: `e03a80a049ed01c35fc5f296fbe1633278dd3cb2`

## 4. C — 테스트 / CI

### C1. Android CI 추가 — 완료

파일:
- `.github/workflows/android-ci.yml`
- 최종 blob: `e77e5b6bf505d6a4af0b0eaeec8ca98680a3cf80`

구성:
- main push / PR
- JDK 17
- Android SDK + API 36
- CI용 `google-services.json.example` 복사
- `./gradlew --no-daemon assembleDebug`
- `./gradlew --no-daemon testDebugUnitTest`

검증:
- 최초 run #1 (`36488359054`)은 **코드 단계 이전** `android-actions/setup-android`에서 실패.
- 원인: action 기본 package 목록의 obsolete `tools` package를 최신 sdkmanager가 찾지 못함.
- 보정: `packages: platform-tools` 명시 + `actions/setup-java@v5`.
- 보정 commit: `6447d7d24f31c4ce7c2149e4f66c1e96d6096fc4`.
- 최종 run #2: `36488468298` — **SUCCESS**.
  - Set up JDK 17: success
  - Set up Android SDK: success
  - Install Android 36: success
  - `assembleDebug`: success, `BUILD SUCCESSFUL in 3m 6s`
  - `testDebugUnitTest`: success, `BUILD SUCCESSFUL in 35s`

비차단 경고:
- AGP 8.2.2가 compileSdk 36에 대해 더 최신 AGP 사용을 권장하는 경고가 있으나 이번 빌드/테스트는 성공함.
- 별도 dependency/toolchain 업그레이드는 이번 feedback 범위 밖으로 유지.

### C2. PrizeIndexCalculator 단위 테스트 — 완료

추가:
- `core/engine/src/test/java/com/example/lottoinsight/core/engine/calculator/PrizeIndexCalculatorTest.kt`
- blob: `9b1968f2d9eaa50fb73aeefd305a4ee4e266e908`

검증 범위:
- 유효 prize 회차 기준 EV = 출현율 × 평균 당첨금.
- null prize 회차가 EV denominator/sample에서 제외되는 정책.
- 전체 회차 기반 `appearanceCount / appearanceRate` 유지.
- normalized score가 0..1 범위.
- 45개 번호에 대해 rank 1..45 부여.

### C3. AnalysisEngineTest 다양화 — 완료

수정:
- `core/engine/src/test/java/com/example/lottoinsight/core/engine/AnalysisEngineTest.kt`
- blob: `af0c597c3e422e7fcac96d07fd952be66dd9790f`

검증 범위:
- 입력 순서와 무관한 최근-N 범위 선택.
- 다양한 번호 분포 fixture.
- 생성 게임 간 ticket uniqueness.
- 각 게임의 `maxOverlap`이 앞선 선택 게임과의 실제 최대 교집합과 일치.
- score가 finite.
- seeded `NumberGenerator` 결과 재현성.

추가 network validator test도 Legacy/New 명명과 신규 API mapping에 맞춰 갱신.

## 5. D — UI

### D1. CalendarStatistics 스텁 노출 제거 — 완료

- `StatisticsViewModel.computeStatistics`에서 모든 필드를 0/빈 리스트로 채우던 가짜 `CalendarStatistics` 생성을 제거.
- 구현 전 상태는 `calendarStats = null`로 명시.
- 현재 `StatisticsScreen`은 `calendarStats`를 직접 렌더링하지 않으므로 사용자가 가짜 상관계수/그룹값을 보지 않음.
- 향후 실제 계산기가 추가될 때만 해당 섹션을 연결하는 방향으로 유지.

### D2. 화면별 UI 디자인 개선 제안 — 제안만, 미구현

#### Analysis
- 현재 `WeightConfigCard`가 결과와 함께 계속 큰 면적을 점유하므로, 결과 생성 후에는 **설정 요약 + 펼치기/접기** 구조로 축소.
- `추천 번호 조합 생성`을 화면의 명확한 primary action으로 유지하고, 분석 중에는 작은 spinner + "분석 중" 텍스트를 함께 표시.
- 결과 상단에 **데이터 범위 / 최신 회차 / 알고리즘 버전 / EV 사용 여부**를 status chips로 묶어 결과 provenance를 한눈에 확인 가능하게 구성.
- 각 GAME의 종합/빈도/연속/홀짝 점수는 숫자 나열만 하지 말고, 짧은 bar 또는 expandable "점수 상세"로 정보 위계를 정리.
- "이미지 복사"는 결과 생성 전에는 숨기고, 생성 후 secondary action으로 유지.

#### History
- 카드 전체 클릭만으로 확장되는 현재 구조에 **chevron + "상세" affordance**를 추가해 확장 가능성을 명확히 표시.
- 실행 번호보다 **날짜 + 기준회차 + 분석범위**를 1차 정보로, raw run id는 보조 정보로 낮춤.
- 기간/기준회차 필터와 정렬, 두 분석 run을 비교하는 진입점을 추가하면 장기 사용 시 탐색성이 크게 개선됨.
- 좁은 화면에서 날짜와 제목이 한 Row에서 충돌하지 않도록 responsive wrapping 또는 2행 header 권장.

#### Statistics
- 45개 EV 카드가 긴 단일 리스트이므로 **요약 KPI → Top N → 전체 순위**의 점진적 공개 구조 권장.
- Frequency / EV / 패턴을 segmented control 또는 tabs로 분리하여 한 화면의 인지 부하를 줄임.
- EV 항목에 `prizeSampleCount`를 함께 보여 **당첨금 유효 표본 수**와 A4의 null-prize 정책을 사용자에게 투명하게 전달.
- normalized score는 소수점 텍스트만보다 0..1 bar를 병행하되, 색상만으로 서열을 표현하지 않도록 숫자/rank도 유지.
- 동기화 중에는 현재 텍스트 백분율에 더해 `LinearProgressIndicator`를 연결할 수 있음(`syncCompleted/syncTotal` 상태는 이미 준비됨).

#### Database
- 현재 최근 300회 고정 목록에 **회차 검색/점프**를 추가해 오래된 회차 접근성을 개선.
- 상단 동기화 영역을 "최신 회차 / 저장 건수 / 동기화 상태 / 마지막 성공" status card로 정리.
- 긴 리스트는 기본 행을 더 compact하게 하고, 당첨금/당첨자 상세는 expand/detail surface로 분리.
- 동기화 실패 시 일반 오류 문구만 노출하기보다 inline retry action을 함께 제공.
- 동기화 중에는 Statistics와 동일한 progress component를 재사용해 상태 표현을 통일.

#### 공통
- transient 성공/실패 알림은 Snackbar, 지속 상태는 화면 내 status 영역으로 역할을 분리.
- Bottom navigation의 icon `contentDescription`은 현재 영문 문자열보다 화면 label과 동일한 한국어 의미로 정리 권장.
- Dynamic Type에서 긴 텍스트가 잘리지 않도록 Row→wrap/Column 전환 기준을 두고, 터치 타깃은 최소 48dp 유지.
- primary/error 상태를 색상 하나에만 의존하지 말고 icon/문구를 병행.
- Analysis/Statistics/Database의 카드 padding, section spacing, loading/empty/error 패턴을 공통 component/token으로 통일.

## 6. 저장소 metadata 제안 (owner action)

현재 repository description/topics가 비어 있어 코드 변경과 별도로 다음을 권장한다.

- Description:
  - `Android lottery analysis app built with Kotlin, Jetpack Compose, Room, and multi-module Clean Architecture.`
- Topics:
  - `android`
  - `kotlin`
  - `jetpack-compose`
  - `room`
  - `clean-architecture`
  - `lottery`
  - `statistics`
  - `multi-module`

현재 사용 가능한 저장소 도구에는 repository metadata 수정 action이 없어 **제안만 기록하고 적용하지 않음**.

## 7. 최종 상태

- A1~A5: 완료
- B1~B3: 완료
- C1~C3: 완료
- D1: 완료
- D2: 화면별 제안 기록 완료
- 최종 Android CI run #2: SUCCESS
- `assembleDebug`: SUCCESS
- `testDebugUnitTest`: SUCCESS
- 죽은 모듈 루트 Kotlin 파일: 48개 제거, 현재 0개
- CalendarStatistics 가짜 0값 데이터: 제거/비노출
