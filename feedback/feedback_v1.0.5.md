# Feedback — v1.0.5 (lotto_analyzer)

대상: geni2kim-ai/Lotto_analyzer (public), main 브랜치, Lotto Insight Android 앱 (Kotlin + Jetpack Compose, 멀티모듈 Clean Architecture).

리뷰 기준: 2026-09-29 doldol 심층 코드 리뷰 — `evidence/evidence_v1.0.4.md`(재조정 커밋 `34a8af0` 반영본)와 구현 커밋(`982696a`), 후속 조정 커밋(`64edd66`)의 실제 diff를 원문 정독.

작업을 시작하기 전에 `feedback/feedback_v1.0.4.md`를 읽고 원문 지시사항(A1~A4, B1~B2, C1~C4)과 대조했음을 확인한다.

## 0. v1.0.4 evidence 검증 결과 — 전체 PASS

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
|---|---|---|
| A1. 부분 성공 + 실패 회차 보존 + targeted retry | PASS | 일치. `DrawFetchReport(successful/failedDrawNos/attemptedCount/isPartialSuccess)` 신규. `fetchDraws`는 `Semaphore(6)` 병렬 + 진행 콜백, 성공분 보존, `failedDrawNos` 정렬 보존. 조정 커밋(`64edd66`) 이후 전 회차 실패 시에도 `AppResult.Success(DrawFetchReport)`로 실패 회차 식별자를 보존한다. 초기 evidence의 "전부 실패 시 fail-closed" 서술은 재조정 evidence(`34a8af0`)에서 정정되었다. `syncDraws`는 성공분을 즉시 upsert하고 `DrawSyncReport`를 반환하며, `retryDraws`는 `>0` 필터·distinct·sorted 후 실패 회차만 재요청한다. 두 ViewModel이 `syncFailedDrawNos`를 보존하고 부분 실패 시 실패 회차만 재시도한다. |
| A2. `fetchAndSaveLatestDraws` 순차 요청 제거 | PASS | 일치. 순차 루프가 제거되고 `fetchDrawRange`를 1회 호출한다. 신규 `LottoRepositoryImplTest`가 range `101..103` 단일 요청, single-draw 호출 0건, 부분 성공분 저장, 반환값 2를 검증한다. `fetchCount <= 0` 가드(`AppError.InsufficientData`)도 확인했다. |
| A3. 장기 미동기 gap 처리 | PASS | 일치. `isOfficialLatestPlausible`(1..5000, local max 이상, gap 제한 없음)와 `isHtmlLatestPlausible`(+200 gap 제한)가 분리되어 있다. 테스트에서 local 100 / official 450 → `Success(450)`, HTML 미호출을 검증한다. |
| A4. `fallbackGames` 방어 경로 직접 테스트 가능화 | PASS | 일치. `NumberGenerator`는 `open` + `internal open weightedSample`, `AnalysisEngineImpl`(AnalysisEngine.kt 내 정의)은 module-internal로 노출되어 있다. `ScriptedNumberGenerator` 테스트가 중복 제외·정확한 fallback 번호·`maxOverlap`(1, 2)을 assertion으로 검증하며, 단순 통과가 아닌 의미 있는 검증이다. |
| B1. 부분 실패 테스트 | PASS | 일치. 부분 실패(2회 실패 → successful `[1, 3]`, `failedDrawNos == [2]`, `isPartialSuccess`)와 전 실패(모두 실패 → `failedDrawNos == [1, 2, 3]` 보존) 테스트가 추가되었다. |
| B2. 장기 gap 테스트 | PASS | 일치. 위 A3 테스트와 동일. |
| C1. 동기화 `LinearProgressIndicator` 연결 | PASS | 일치. Statistics/Database 양 화면에서 `isSyncing` 시 표시. total 미확정 시 indeterminate(조정 커밋에서 0% 고정 바 문제 수정), 확정 시 `completed/total` clamp. 텍스트 `%`도 병행 유지된다. |
| C2. 실패/부분 실패 inline retry | PASS | 일치. 실패 시 `OutlinedButton`: 목록 없음 → "동기화 다시 시도", 목록 있음 → "실패한 K개 회차 다시 시도". 재시도 자체가 또 부분 실패하면 남은 목록을 보존하므로 반복 복구 가능하다. 실패 메시지는 error color로 표시된다. |
| C3. EV 당첨금 유효 표본 수 노출 | PASS | 일치. EV 행 보조 텍스트에 "유효 표본 N회"가 표시된다. |
| C4. 추가 UI 디자인 제안 | PASS | 제안 기록 완료. |
| CI | PASS | Android CI run #5(`982696a`) SUCCESS, run #7(`64edd66`) SUCCESS(`assembleDebug` 3m 36s, `testDebugUnitTest` 45s, `core:data` 테스트 신규 실행). 문서 커밋 run #8(`34a8af0`)은 리뷰 시점에 진행 중이었다. |

**종합 판정: PASS** — v1.0.4 지시사항 전 항목이 코드로 확인되며, 재조정 evidence는 현재 main의 코드와 일치한다.

## 1. 새로 발견된 문제점 및 후속 보완 항목

1. 도달 불가 `Error` 분기 정리 — `64edd66` 이후 `fetchDrawRange`/`fetchDraws`는 `Error`를 반환하지 않으므로, `LottoRepositoryImpl.fetchAndSaveLatestDraws`·`retryDraws`의 `AppResult.Error` 분기가 사실상 도달 불가 코드가 되었다. 동작에는 문제 없으나 계약을 명확히 하려면 분기 단순화 또는 KDoc 명시를 검토할 것.
2. 전체 목록 재다운로드 — `fetchAllDraws`의 공식 "all" API 경로가 성공하면 매 동기화마다 전체 회차 목록을 내려받아 upsert한다. 변경분만 요청·저장하는 최적화 여지가 있다(기존 동작 유지, 성능 개선 후보).
3. 분석 시드 비결정성 — `AnalysisEngineImpl.analyzeAndGenerate`가 `Random().nextLong()`을 사용해 결과가 재현되지 않는다. `randomSeed`는 기록되지만 `NumberGenerator`의 seeded repeatability 테스트와 달리 실제 경로는 결정적이지 않다. 재현 가능성 정책(결정적 시드 저장/표시)을 정할 것.
4. 동기화 UI 중복 — Statistics/Database의 sync progress·error·retry UI가 두 화면에 중복 구현되어 있다. 공통 `SyncStatusCard`(또는 동등 컴포넌트)로 추출해 문구·동작 drift를 방지할 것. (v1.0.4 C4 승계)
5. 접근성 — 동기화 진행률과 상태 전환에 `role=status`에 대응하는 semantics와 live announcement를 추가할 것.
6. EV 목록 progressive disclosure — 45개 카드를 한 번에 렌더링하는 대신 요약 → Top 5/10 → 전체 보기로 단계적 공개할 것. `prizeSampleCount`는 명시적 수치로 유지하고, 임의의 신뢰도 등급은 도입하지 말 것.
7. Database 접근성 — 최근 300회 목록에 회차 번호 검색/점프를 추가하고, 실패 회차가 6건을 초과할 때 전체 목록을 볼 수 있는 detail surface를 추가할 것.
8. Analysis/History 개선안 승계 — (a) 분석 결과 생성 후 설정 카드를 요약 한 줄 + expand/collapse로 축소, (b) 결과 상단에 최신 회차/분석 범위/EV 사용/알고리즘 버전 status chips, (c) 게임별 점수는 종합 점수를 우선하고 세부 지표는 expandable detail로 분리, (d) History 카드에 trailing chevron·`상세` affordance와 실행 시각 중심 메타데이터 추가. filter/sort/compare mode는 장기 과제로 둘 것.

## 2. UI 디자인 관찰

- Statistics: 동기화 영역이 텍스트 + 버튼 + progress bar로 구성되어 있으나 상태별 시각적 위계가 평면적이다. 성공/부분 성공/실패를 icon + 색상 + 문구 조합으로 구분하고(색상 단독 구분 금지), 부분 실패 메시지와 재시도 버튼의 간격을 일정하게 유지할 것. EV 카드의 "평균 1등 금액 · 유효 표본 N회 · 점수" 3단 보조 텍스트는 정보 밀도가 적절하다. 표본이 극히 적은 번호에는 "표본 적음(정책 기준 명시)" 보조 상태를 고려할 수 있다.
- Database: 회차 번호가 primary로 잘 보이나, 동기화 상태 영역이 상단에 텍스트 위주라 상태 파악이 한눈에 어렵다. 저장 회차 / 최신 회차 / 마지막 성공 동기화 / 현재 상태를 compact status card로 정리할 것을 제안한다.
- 공통: 두 화면의 동기화 UI 문구는 현재 동일하게 유지되고 있으나 코드가 중복되어 있어 drift 위험이 있다(위 항목 4와 동일). 좁은 폭·큰 글자(Dynamic Type)에서는 상단 Row의 버튼이 겹칠 수 있으므로 responsive Column 전환 규칙을 둘 것. 모든 주요 action은 최소 48dp 터치 타겟을 유지할 것.
- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 정리해주길 바란다.

## 3. 완료 조건

위 항목(1~8)을 수행한다. 추가 UI 개선 제안은 evidence에 정리한다.
작업을 마친 뒤 `evidence/evidence_v1.0.5.md` 파일을 만들어 완료 내용을 항목별로 정리하고 저장소에 커밋할 것. evidence에는 구현 커밋 hash, CI 실행 결과(run 번호·결론), 항목별 PASS/FAIL과 코드 식별자(blob SHA 또는 파일 경로)를 포함한다.
