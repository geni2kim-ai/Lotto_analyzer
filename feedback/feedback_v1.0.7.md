## Feedback — v1.0.7 (lotto_analyzer)

대상: geni2kim-ai/Lotto_analyzer (public), main 브랜치, Lotto Insight Android 앱 (Kotlin + Jetpack Compose, 멀티모듈 Clean Architecture).

리뷰 기준: 2026-09-29 doldol 심층 코드 리뷰 — `evidence/evidence_v1.0.6.md`와 구현 커밋(`2148b0907993e4c049d385948fe411bc414e67cb`)의 실제 diff를 원문 정독. CI run #15(`2148b09`, run id 36499028441) SUCCESS를 Actions 페이지에서 직접 확인.

작업을 시작하기 전에 `feedback/feedback_v1.0.6.md`를 읽고 원문 지시사항(항목 1~6)과 대조했음을 확인한다.

## 0. v1.0.6 evidence 검증 결과

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
| --- | --- | --- |
| 1. SyncStatusCard 접근성 merge 범위 | PASS | 일치. 외부 Card의 `mergeDescendants` 제거 → 상태 요약 전용 inner `Column`에만 `mergeDescendants = true` + `LiveRegionMode.Polite` + `stateDescription` 적용. Retry `OutlinedButton`·실패 목록 `TextButton`은 병합 서브트리 밖에 있어 네이티브 버튼 semantics(독립 포커스) 유지. |
| 2. Database 검색 stale 결과 무효화 | PASS | 일치. `DatabaseViewModel.updateSearchQuery`가 `searchResult = null`, `searchMessage = null`을 모두 초기화 — 쿼리 변경 즉시 이전 결과 카드가 사라진다. |
| 3. Seed 용어 정합 | PASS | 일치. KDoc에 requested vs applied/effective recent-N 명시, `deterministicSeed(draws, effectiveConfig)` 파라미터명 변경. 회귀 테스트 `oversizedRequestedRecentNMatchesSameAppliedRecentNSeed`(recentN=100 요청 vs 12건 → applied 12, 동일 seed·동일 게임)가 정책을 의미 있게 검증한다. |
| 4. 인터폴레이션 재발 방지 | PASS | 일치. `tools/check_kotlin_interpolation.py`의 정규식 `(?<!\\)$([A-Za-z_][A-Za-z0-9_]*)(?=[\u3131-\u318E\uAC00-\uD7A3])` 원문 확인 — 이스케이프 `\$` 제외, build/.gradle/.git 스킵, 위반 시 exit 1. CI `android-ci.yml`에 `assembleDebug` 이전 단계로 추가, `CONTRIBUTING.md`에 규칙 문서화. |
| 5. 증분 탐색 probe 경량화 | **FAIL (버그 2건)** | **불일치.** 아래 B1·B2 참조. legacy 미호출 자체는 테스트로 확인되나, 장애 경로 처리가 깨져 있다. |
| 6. Analysis 게임 펼침 상태 보존 | PASS | 일치. `rememberSaveable(result?.randomSeed)` + `IntArray` 저장. 새 결과 도착 시 input key 변경으로 stale 펼침 상태가 리셋되며, 결정적 seed 특성상 동일 입력 → 동일 게임이므로 리셋이 무해하다. |
| 버전 | PASS | `versionName = "1.0.6"`, `versionCode = 7` 확인. |
| CI | PASS | Android CI run #15(`2148b09`) SUCCESS 직접 확인. |

### B1. [치명] `UNAVAILABLE` 분기 누락 — 무한 루프 가능

`discoverLatestIncrementally`의 지수 탐색 `when`과 이진 탐색 `when`은 `DrawAvailability.PRESENT`·`MISSING`만 처리하고 `UNAVAILABLE` 분기(또는 `else`)가 없다. `when`을 statement로 사용했으므로 어느 분기에도 매치되지 않으면 아무 동작 없이 루프가 계속되고, `step`과 경계(`lowerBound`/`upperBound`, `low`/`high`)가 그대로이므로 **같은 candidate를 무한 재시도**한다.

 evidence §6의 "UNAVAILABLE returns null from boundary discovery so the caller uses the established full-list fallback" 주장은 실제 코드와 불일치한다 — 코드는 어떤 경우에도 null을 반환하지 않는다. 일시적 장애는 재시도로 회복될 수 있으나, 장애가 지속되면(네트워크 단절, 해당 엔드포인트의 지속적 500 등) 동기화가 코루틴 취소까지 영원히 멈춘다. `probeDrawAvailability`는 `CancellationException`만 rethrow하므로 취소 자체는 가능하지만, 사용자 관점에서는 멈춰 버린 동기화다.

### B2. HTTP 에러의 오분류 — `response.isSuccessful` 미검사

`probeDrawAvailability`는 `response.isSuccessful`을 검사하지 않는다. HTTP 에러(예: 500) 시 `response.body()`는 null → `items`가 비어 `firstOrNull`이 null → `MISSING`을 반환한다. 즉 **서버 장애가 "미래 경계"로 오인**될 수 있다. 지수 탐색 첫 probe에서 500이 지속되면 `upperBound = existingMaxDrawNo + 1`로 확정 → 이진 탐색을 건너뛰고 `discoveredLatest = existing` 반환 → `fetchAllDraws`가 "신규 회차 없음"으로 조용히 종료되는 **silent staleness**가 발생한다. v1.0.5 이전 코드(`isMissingDrawError` — `AppError.InvalidDraw`만 경계 신호, 그 외 transport/server 실패는 null → fallback)는 HTTP 에러를 fallback으로 처리했으므로 이는 회귀다.

## 1. v1.0.7 보완 항목 (우선순위 순)

1. **[P0] B1 수정** — 두 `when`(지수 탐색·이진 탐색)에 `DrawAvailability.UNAVAILABLE -> return null` 추가 (또는 `else -> return null`). 문서화된 계약("anomalies return null → full-list fallback")과 코드를 일치시킨다.
2. **[P0] B2 수정** — `probeDrawAvailability`에서 응답 파싱 전에 `if (!response.isSuccessful) return DrawAvailability.UNAVAILABLE` 추가.
3. **[P1] 회귀 테스트** — (a) 지수 탐색 중 `IOException` 발생 시 `discoverLatestIncrementally`가 null을 반환하고 full-list 경로(`all` 쿼리 호출 > 0)로 fallback하는지 검증. (b) HTTP 500 응답 시 `UNAVAILABLE`로 분류되는지 검증. 테스트 더블(`LottoApiService` fake)에 실패 주입이 필요하면 확장할 것.
4. **[P1] SyncStatusCard semantics 테스트** — evidence v1.0.6 §10에서 제안된 대로, 요약 영역은 병합·liveRegion으로 읽히고 retry/상세 버튼은 독립 포커스 대상으로 유지됨을 Compose semantics 테스트로 검증한다.
5. **[P2] interpolation checker 주석 오탐 문서화** — 주석 안의 `$식별자`+한글도 위반으로 잡힐 수 있다. 주석 라인을 스킵하거나 `CONTRIBUTING.md`에 이 제약을 명시 (선택).

## 2. UI 디자인 관찰

- Analysis: evidence §10 제안대로 "재현 정보" disclosure row(알고리즘 버전 + seed 묶음) + 복사 액션을 도입해, 긴 seed 텍스트가 항상 노출되지 않게 한다. "새 seed" expert control을 나중에 도입하더라도 결정적 재생을 기본값으로 유지할 것.
- Database: 검색 필드 안에 clear-search affordance를 두어 점프 후 검색 모드에서 즉시 빠져나올 수 있게 한다 (evidence §10).
- Statistics: EV 요약 하단에 "계산 기준" disclosure(recent-N·무효 표본 제외 규칙 설명) — 카드 밀도는 유지하면서 설명 가능성을 확보한다.
- History: "동일 조건 재현" 액션은 과거 draw snapshot 기준인지 현재 DB 기준인지 제품 의미론을 정의한 뒤에만 도입한다 (두 의미론을 조용히 섞지 말 것).
- 공통: 화면이 더 늘어나면 `SyncStatusCard`의 타이틀/메시지/아이콘 선택 로직을 공유 presentation model로 추출한다 (액션은 네이티브 컨트롤 유지).
- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 정리해주길 바란다.

## 3. 완료 조건

위 항목(1~5)을 수행한다. 추가 UI 개선 제안은 evidence에 정리한다.

작업을 마친 뒤 `evidence/evidence_v1.0.7.md` 파일을 만들어 완료 내용을 항목별로 정리하고 저장소에 커밋할 것. evidence에는 구현 커밋 hash, CI 실행 결과(run 번호·결론), 항목별 PASS/FAIL과 코드 식별자(blob SHA 또는 파일 경로)를 포함한다.
