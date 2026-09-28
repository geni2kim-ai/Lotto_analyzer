# Feedback — v1.0.3 (lotto_analyzer)

대상: geni2kim-ai/Lotto_analyzer (public), main 브랜치, Lotto Insight Android 앱 (Kotlin + Jetpack Compose, 멀티모듈 Clean Architecture).
리뷰 기준: 2026-09-29 doldol 심층 코드 리뷰 (engine / data / network / model / feature 레이어 원문 정독).

## 0. v1.0.2 피드백 처리 상태 (이월)

feedback_v1.0.2.md의 항목별 현재 상태:

- F1 CI 파이프라인: **미완료** — `.github/workflows`가 여전히 없음. 아래 C1로 이월.
- F2 README 클론 URL: **미완료** — 여전히 `LottoInsightAndroid.git`로 표기. 실제 저장소명은 `lotto_analyzer`. 아래 B3에 포함.
- F3 테스트: **부분** — 단위 테스트 4개 존재(`AnalysisEngineTest`, `DrawTest`, `RemoteDrawResponseValidatorTest`, `AnalysisImageClipboardTest`)를 확인. 그러나 CI가 없어 통과 여부는 미확인. 아래 C2·C3으로 보완.
- F4 민감 파일 제외: **완료 확인** — `.gitignore`에 `google-services.json`, `**/google-services.json`, `local.properties`, `*.jks`, `*.keystore` 모두 존재. 저장소에는 `.example` 파일만 커밋되어 있음. 추가 조치 불필요.
- F5 저장소 메타데이터: **owner 작업** — 설명·토픽이 비어 있음. ChatGPT가 변경 불가하므로 권장 설명문/토픽을 evidence에 제안 형태로 정리.

## A. 코드 정확성 · 엣지 케이스

### A1. `fetchLatestDrawNo`의 HTML 정규식 파싱이 깨지기 쉬움
- `core/network/.../datasource/LottoRemoteDataSourceImpl.kt`
- 최신 회차를 `opt_val[^0-9]{0,50}([0-9]{1,5})` 같은 정규식으로 HTML에서 추출하고, 매칭된 후보 중 `max`를 선택함.
- 문제점:
  1. 동행복권 사이트 개편 시 정규식이 바로 동작 중단됨.
  2. `maxOrNull()`은 페이지 내 무관한 숫자(연도, 다른 회차 표기 등)를 최신 회차로 오인할 수 있음.
- 보완: 공식 API(`getDraws`) 응답에서 최신 회차를 직접 얻는 방식을 우선하고, HTML 파싱은 폴백으로 두되 파싱 결과에 상식적인 범위 검증(예: 기존 로컬 최대 회차보다 지나치게 크지 않은지)을 추가할 것.

### A2. `fetchAllDraws` 폴백 경로의 순차 요청
- 같은 파일. 신규 API 실패 시 `fetchLatestDrawNo` + `fetchDrawRange`로 회차별 순차 요청을 수행하는데, 회차가 ~1200개에 달하면 수백~수천 회의 순차 HTTP 요청이 발생함.
- 보완: 코루틴 병렬화(동시 요청 수 제한 포함) 또는 배치 조회로 전환하고, 동기화 진행률을 UI에 노출할 것.

### A3. `fallbackGames`의 중복 검사 누락
- `core/engine/.../AnalysisEngine.kt`의 `generateGames`: 후보 풀이 부족해 `fallbackGames`로 넘어가면 이미 선택된 게임과의 중복 검사를 하지 않음. 극단적 케이스에서 동일한 6개 번호 티켓이 2장 생성될 수 있음.
- 보완: fallback에서도 `unique` 집합 기준으로 중복 티켓을 제외할 것.

### A4. `PrizeIndexCalculator`의 null 1등 당첨금 처리 정책 부재
- `core/engine/.../calculator/PrizeIndexCalculator.kt`: `firstPrize`가 null인 회차는 `appearanceCount`에는 반영되지만 당첨금 평균에서는 제외되어, 해당 번호의 EV(`appearanceRate * averagePrize`)가 0 쪽으로 깎임.
- 보완: null 처리 정책을 명시할 것. 예) 당첨금 정보가 없는 회차는 EV 계산 대상에서 제외하거나, 표본 수(`prizeSampleCount`) 기준 가중치를 문서화.

### A5. 가중치 샘플링 로직 중복
- `AnalysisEngineImpl.weightedSample`은 동작은 정확하나 선택 조건문(`selectedIndex == items.lastIndex` 트릭)이 가독성이 낮음.
- `core/engine/.../generator/NumberGenerator`에 별도의 샘플링 구현이 존재하고, 엔진 본체는 이를 사용하지 않음(테스트에서만 사용).
- 보완: 샘플링 로직을 하나로 통합하고, `weightedSample`의 선택 부분을 명확한 누적합 방식으로 정리할 것.

## B. 저장소 위생

### B1. 모듈 루트의 구버전 소스 파일 중복
- 각 모듈 루트에 `src/main/java/...`와 내용이 다른 구버전 파일이 함께 커밋되어 있음.
  예: `core/engine/AnalysisEngine.kt`(3.9KB) vs `core/engine/src/.../AnalysisEngine.kt`(12.2KB),
  `feature/ui/statistics/StatisticsScreen.kt`(4.0KB) vs src 버전(6.1KB),
  `core/common/Constants.kt`(762B) vs src 버전(832B) 등.
- Gradle 기본 sourceSet(`src/main/java`)이라면 이 파일들은 컴파일되지 않는 죽은 파일임.
- 보완: `build.gradle.kts`의 sourceSet 커스터마이징 여부를 확인하고, 기본값이라면 모듈 루트의 중복 파일을 전부 삭제할 것.

### B2. 검증기 네이밍 불일치
- `RemoteDrawResponseValidator`(레거시 API용)와 `NewRemoteDrawResponseValidator`(신규 API용).
- 보완: `Legacy`/`New` 또는 `V1`/`V2`처럼 일관된 네이밍으로 통일할 것.

### B3. 저장소명 불일치 (F2 이월 + 추가)
- README의 clone URL이 `LottoInsightAndroid.git` → `https://github.com/geni2kim-ai/lotto_analyzer.git`로 수정.
- `settings.gradle.kts`의 `rootProject.name = "LottoInsightAndroid"`도 실제 저장소명과 불일치 → `lotto_analyzer` 또는 프로젝트 표시명 정책에 맞게 정리.
- 참고: `applicationId`(com.aimaestro.lottoanalyzer)와 `namespace`(com.example.lottoinsight)도 서로 다르니, 의도된 것인지 주석으로 명시할 것.

## C. 테스트 · CI

### C1. CI 파이프라인 추가 (F1 이월)
- `.github/workflows/android-ci.yml`: JDK 17 설정, `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` 실행. main 푸시/PR에 동작.

### C2. `PrizeIndexCalculator` 단위 테스트 추가
- EV 계산은 앱의 핵심 로직인데 테스트가 없음. 출현율×평균 당첨금 계산, null 당첨금 처리(A4에서 정한 정책), 정규화 범위(0~1), 순위 부여를 검증하는 테스트 추가.

### C3. `AnalysisEngineTest` 픽스처 다양화
- 현재 테스트가 전부 동일한 번호(`1..6`) 10개 회차로, 통계가 degenerate한 상태에서 최근-N 선택만 검증함.
- 보완: 번호 분포가 다양한 픽스처로 점수화·후보 생성·중복 완화(`maxOverlap`) 로직이 의도대로 동작하는지 검증하는 테스트 추가.

## D. UI 관찰

### D1. `CalendarStatistics` 스텁 데이터
- `feature/statistics/.../StatisticsViewModel.kt`의 `computeStatistics`가 `CalendarStatistics`를 전부 `0.0`/빈 리스트로 하드코딩함.
- `StatisticsScreen`에 이 값이 노출되고 있다면 실제 계산을 구현하거나, 구현 전까지 해당 섹션을 숨길 것.

### D2. UI 디자인 개선 제안 환영
- UI 디자인 관점에서도 개선할 부분이 있으면 제안해줘. 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 정리해줘.

## 완료 조건

1. 위 항목(A1~A5, B1~B3, C1~C3, D1)을 수행하고, D2 제안은 evidence에 정리.
2. `evidence/evidence_v1.0.3.md` 파일을 만들어 완료 내용을 항목별로 정리.
3. 작업을 시작하기 전에 이 feedback 파일을 읽었다는 것을 먼저 확인한다.
