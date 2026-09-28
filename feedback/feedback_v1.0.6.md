# Feedback — v1.0.6 (lotto_analyzer)

대상: geni2kim-ai/Lotto_analyzer (public), main 브랜치, Lotto Insight Android 앱 (Kotlin + Jetpack Compose, 멀티모듈 Clean Architecture).

리뷰 기준: 2026-09-29 doldol 심층 코드 리뷰 — `evidence/evidence_v1.0.5.md`와 구현 커밋(`f3bf84f`), 후속 수정 커밋(`4d351bc`, `8776be6`)의 실제 diff를 원문 정독. CI run #12(`8776be6`) SUCCESS를 Actions 페이지에서 직접 확인.

작업을 시작하기 전에 `feedback/feedback_v1.0.5.md`를 읽고 원문 지시사항(항목 1~8)과 대조했음을 확인한다.

## 0. v1.0.5 evidence 검증 결과 — 전체 PASS

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
|---|---|---|
| 1. 도달 불가 Error 분기 정리 | PASS | 일치. `fetchDrawRange`/`fetchDraws`가 `DrawFetchReport`를 직접 반환하도록 계약 변경, `LottoRepositoryImpl`의 도달 불가 `AppResult.Error/Loading` 분기 제거, 인터페이스에 KDoc 명시. |
| 2. 증분 동기화 최적화 | PASS | 일치. `discoverLatestIncrementally` = 지수 탐색으로 상한 경계 확정 → 이진 탐색으로 최신 회차 확정. 경계 신호는 `AppError.InvalidDraw`(미래 회차는 new API "Requested draw was not returned" → InvalidDraw)가 담당하고, 그 외 transport/parser/server 실패는 null 반환 → 기존 공식 전체 목록 경로로 fallback. fallback 경로에서도 로컬 max 이후 항목만 파싱/upsert. 테스트에서 `all` 쿼리 0건을 검증. |
| 3. 결정적 분석 seed | PASS | 일치. `deterministicSeed`가 SHA-256으로 알고리즘 버전·가중치·EV on/off·recentN·gameCount·candidateCount·정렬된 회차/당첨번호/firstPrize를 해싱하고 상위 8바이트를 seed로 사용. `Random().nextLong()` 제거, 알고리즘 버전 `lotto-analysis-deterministic-seed-1.1`로 상향. 테스트가 입력 순서 변경 시 동일 seed·동일 게임, 설정 변경 시 다른 seed를 검증하며 의미 있는 검증이다. |
| 4. 공통 SyncStatusCard | PASS | 일치. 신규 `SyncStatusCard.kt`; Statistics/Database 양 화면의 중복 progress/error/retry 블록 제거 후 공통 컴포넌트 사용. |
| 5. 접근성 semantics | PASS | 일치. `liveRegion = Polite` + `stateDescription` + `mergeDescendants = true`. 아이콘/문구/액션이 병행되어 색상 단독 구분이 아니다. |
| 6. EV progressive disclosure | PASS | 일치. 요약 카드(Top 3 + 표본 제외 고지) → 기본 Top 10 → "전체 N개 보기" 토글. `prizeSampleCount` 명시 유지, 임의 신뢰도 등급 미도입. |
| 7. Database 검색/점프 + 실패 detail | PASS | 일치. `searchQuery`(숫자만·최대 5자)/`searchResult`/`searchMessage` 상태, 로컬 캐시만 조회(네트워크 요청 없음), 300건 내 회차는 스크롤 점프, 그 외는 상단 검색 결과 카드. 실패 6건 초과 시 `실패 목록 보기` → AlertDialog 전체 목록. |
| 8. Analysis/History 개선 | PASS | 일치. Analysis: 결과 생성 후 설정 카드가 한 줄 요약 + "설정 변경" expand, 최신/범위/EV/알고리즘 status chips, 재현 seed 표시 + 정책 설명, GAME 카드 종합 점수 우선 + "세부 점수 보기" expand. History: 실행 시각이 primary title, 기준/범위/게임 수 메타데이터, trailing "상세"/"닫기" + chevron, run id·EV·seed·알고리즘 버전 보조 표시. |
| 버전 | PASS | `versionName = "1.0.5"`, `versionCode = 6` 확인. |
| CI | PASS | Android CI run #12(`8776be6`) SUCCESS 직접 확인. run #10은 `$added건` 인터폴레이션 컴파일 실패 → `4d351bc` 수정, 정적 점검에서 `$EV_PREVIEW_COUNT만` 동일 패턴 발견 → `8776be6` 수정 후 green. |

**종합 판정: PASS** — v1.0.5 지시사항 전 항목이 코드로 확인되며, evidence의 주장·커밋 hash·CI 결과가 실제와 일치한다.

## 1. 새로 발견된 문제점 및 후속 보완 항목

1. `SyncStatusCard`의 `mergeDescendants = true` 접근성 trade-off — 카드 내부의 "실패한 N개 회차 다시 시도"(OutlinedButton)와 "실패 목록 보기"(TextButton)의 개별 semantics가 부모 노드로 병합되어, TalkBack에서 버튼이 별도 포커스로 읽히지 않을 수 있다. 병합 범위를 조정하거나 `customActions`로 대체하는 방안을 검토할 것.
2. Database 검색의 stale 결과 — `updateSearchQuery`가 `searchMessage`는 지우지만 `searchResult`는 유지하므로, 새 쿼리를 입력하는 동안 이전 검색 결과 카드가 계속 표시된다. 쿼리 변경 시 `searchResult`를 무효화할 것.
3. Seed 정책 용어 정합 — evidence는 seed 입력에 "effective recent-N"이 포함된다고 서술하나, 실제 코드는 `config.recentN`(요청값)과 실제 `targetDraws` 목록을 해싱한다. 동작은 결정적이므로 버그는 아니나, 문서와 코드의 용어를 통일할 것(요청값 vs 실제 적용값 구분 명시).
4. 문자열 인터폴레이션 버그 재발 방지 — `$added건`, `$EV_PREVIEW_COUNT만` 2회 연속 발생. 한국어 조사·접미사가 식별자에 바로 이어지는 경우 `${}` 중괄호를 강제하는 코딩 규칙을 둘 것.
5. 증분 탐색의 probe 비용 — 경계 탐지 과정에서 missing draw마다 new API + legacy API 2회 호출이 발생한다(지수+이진 탐색 ≈ 4·log2(gap) 요청). 현재 규모에서는 허용 가능하나, 경계 탐지 전용 경량 경로(legacy 스킵 옵션)를 선택적 최적화 후보로 둘 것.
6. `AnalysisScreen`의 `expandedGames`가 `remember`(비saveable) — 화면 회전 시 게임별 "세부 점수 보기" 펼침 상태가 소실된다. `rememberSaveable` 전환을 검토할 것.

## 2. UI 디자인 관찰

- Statistics: EV 요약 → Top 10 → 전체 보기 위계가 잘 잡혔다. 다음 단계로 "표본 적음" 상태를 도입한다면 임계값을 숫자로 명시할 것(v1.0.4 C4에서 제기된 원칙 유지). EV 요약 카드의 Top 3 표기에 순위 변동(이전 계산 대비) 표시는 아직 없으며, 시계열 비교 의미론이 정의된 뒤에 추가할 것.
- Database: 검색/점프 추가로 탐색성이 크게 개선됐다. "마지막 성공 동기화" 타임스탬프는 아직 회차 타임스탬프로부터 추론하는 형태이므로, durable한 sync receipt 모델이 생기면 compact 상태 카드에 명시할 것.
- Analysis: 설정 요약 카드 + status chips + 재현 seed 표시로 결과 화면의 정보 위계가 정리됐다. seed 잠금/새 seed expert control은 결정적 기본값을 유지한 채 추후 추가를 검토할 것.
- History: 시각 중심 타이틀과 "상세" affordance가 잘 적용됐다. 이력이 길어지면 월별 sticky header 그룹핑을 장기 과제로 둘 것.
- 공통: 동기화 상태 문구가 `SyncStatusCard`로 통합됐으므로, 화면이 더 늘어나면 상태 문구를 공유 presentation model로 추출할 것. Dynamic Type·좁은 폭에서는 상단 action Row의 responsive Column 전환 규칙을 유지할 것.

- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 정리해주길 바란다.

## 3. 완료 조건

위 항목(1~6)을 수행한다. 추가 UI 개선 제안은 evidence에 정리한다.

작업을 마친 뒤 `evidence/evidence_v1.0.6.md` 파일을 만들어 완료 내용을 항목별로 정리하고 저장소에 커밋할 것. evidence에는 구현 커밋 hash, CI 실행 결과(run 번호·결론), 항목별 PASS/FAIL과 코드 식별자(blob SHA 또는 파일 경로)를 포함한다.
