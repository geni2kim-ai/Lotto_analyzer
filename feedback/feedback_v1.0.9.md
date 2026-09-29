# Feedback — v1.0.9 (review of evidence_v1.0.8)

대상 저장소: `geni2kim-ai/Lotto_analyzer` / 브랜치: `main`
리뷰한 evidence: `evidence/evidence_v1.0.8.md` (commit `f9c8a99`, "docs: add v1.0.8 implementation evidence")
리뷰 방식: evidence 주장 ↔ 실제 커밋 diff ↔ blob SHA 직접 대조.
구현 커밋: `673fc22` (본 구현) + 후속 수정 `7740286`, `507a91a`, `84a42d3`, `4d5485c`.
9개 변경 파일의 전체 diff를 원문 정독하고, evidence가 기재한 blob SHA 9건을
`git hash-object`으로 전부 대조(전부 일치). 체커 단위 테스트 5건과 저장소
게이트를 로컬에서 직접 실행해 통과 확인.

작업을 시작하기 전에 `feedback/feedback_v1.0.8.md`를 읽고 원문 지시사항(F1~F4, F5/F6 backlog)과 대조했음을 확인한다.

## §0 v1.0.8 evidence 검증 결과

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
|---|---|---|
| F1. SyncStatusCardTest 한글 카피 분리 | PASS | 일치. `onNodeWithTag(SYNC_RETRY_ACTION_TAG)` / `SYNC_FAILURE_DETAILS_ACTION_TAG`로 전환, live-region 기대값은 `resolveSyncStatusPresentation(...).announcement`에서 도출. `"실패한 7개 회차 다시 시도"`·`"실패 목록 보기"` 하드코딩 단언 제거, `assertTextContains` import 삭제 확인. |
| F2. evidence/SDK lineage 정합 | PASS | 일치. `SyncStatusCardTest.kt`·`ResponsiveActionContainerTest.kt` 모두 `@Config(sdk = [34])` (grep 직접 확인). 중간 커밋의 35는 v1.0.7 체인(`315abea`→`1f10efc`에서 34로 복구)의 잔재라는 설명이 git 히스토리와 일치. |
| F3. interpolation checker lexer | PASS (단, G1 잠재 버그 발견) | 구현 자체는 일치. 중첩 블록 주석·triple-quoted·문자 리터럴 처리를 원문으로 확인했고, 단위 테스트 5건 전부 통과·저장소 게이트 통과를 직접 실행으로 검증. 그러나 escape 처리에 dead code 존재 → 아래 G1. |
| F4. ResponsiveActionContainer | PASS | 일치. 신규 `ResponsiveActionContainer.kt`(66줄) 원문 확인: `BoxWithConstraints` + `shouldStackActions`(width < 360.dp 또는 fontScale ≥ 1.6f → Column). `weight(1f)`가 RowScope 멤버 확장으로 사용됨(CI #25 실패의 수정 반영). 테스트는 200% 폰트→Column 태그 단언 + 순수 결정 함수 직접 단언으로 Robolectric 측정 의존을 제거 — #27 실패의 교훈이 반영된 좋은 설계. |
| 버전 | PASS | `versionName = "1.0.8"`, `versionCode = 9` 확인. |
| CI | 조건부 PASS | evidence는 Android CI #29(run id `36520200925`) SUCCESS를 주장. 실패→수정 체인(#25→`7740286`, #26→`507a91a`, #27→`84a42d3`+`4d5485c`)이 git 커밋 메시지와 정확히 일치함을 직접 확인. Actions 페이지 직접 확인은 브라우저 단계에서 수행. |

## 조치 항목 (v1.0.9에서 구현)

### G1 (P1) — `check_kotlin_interpolation.py`의 escape 처리 dead code

`strip_kotlin_comments`의 일반 문자열 분기(72행)와 문자 리터럴 분기(92행)에 있는
`if current == "\\\\":` 비교는 1글자(`current`)와 2글자 문자열(`"\\\\"` → `\\` 2글자)을
비교하므로 **항상 False** — escape 감지가 전혀 동작하지 않는다.

실증: `val s = "a\" /* not comment" // real comment` 다음 줄에
`val t = "$z개"`가 있을 때, `\"` 뒤에서 문자열이 조기 종료되어 `/*`가 진짜 블록
주석으로 오인 → 주석 상태가 다음 줄까지 오염 → `$z개` 위반이 **조용히 누락**
(false negative, 직접 실행으로 재현 확인).

현재 저장소에는 `\"`가 없어 gate 통과는 유효하나, 체커의 존재 이유(위반 누락 방지)와
정면으로 충돌하는 잠재 버그다.

- `"\\\\"` → `"\\"`(1글자 백슬래시)로 수정 (두 분기 모두).
- 회귀 테스트 추가: (a) `\"` 뒤에 `/*`가 와도 주석 상태가 오염되지 않고 다음 줄 위반이 검출됨, (b) char 리터럴의 `\'` 케이스. 기존 5건과 같은 `find_violations_in_lines` 스타일로.

### G2 (P2) — Analysis 화면 2-버튼 행에 ResponsiveActionContainer 적용

evidence "Additional UI"에서 제안된 항목을 정식 작업으로 승격. Analysis 화면의
2-버튼 행(예: 재현 정보 복사 주변 액션)이 큰 글꼴 배율에서 깨질 수 있으므로
`ResponsiveActionContainer` primitive를 적용한다. primary/secondary 위계가
분명한 곳에만 적용한다는 evidence의 사용 원칙을 지킨다.

### G3 (P2) — 폰트 스케일 매트릭스 semantics 회귀 커버리지

evidence 제안을 승격. `SyncStatusCard`를 우선 대상으로 1.0 / 1.3 / 1.6 / 2.0
폰트 스케일에서 Row↔Column 전환 태그(`responsive-actions-row` /
`responsive-actions-column`)를 단언하는 semantics 테스트를 추가한다.
Robolectric 루트 측정 의존을 피하기 위해 G2와 같은 패턴(결정 함수는 순수 단언,
Compose 단언은 `CompositionLocalProvider`로 density 주입)을 사용한다.

### G4 (P2, 설계) — replay payload 버전화

F6(재현 복사 페이로드 확장) 구현 시점에 복사 페이로드에 스키마 버전을 명시한다
(예: 첫 줄 `replay-v1`). 필드가 늘어나도 파싱 모호성이 생기지 않도록 한다.
F6 자체는 backlog 유지, 버전화는 F6의 수락 조건에 포함한다.

### G5 (P3, backlog) — durable last-successful-sync receipt

"마지막 성공 동기화" 타임스탬프를 회차 타임스탬프 추론이 아닌 durable한 sync
receipt 모델로 만든 뒤 compact 상태 카드에 명시한다 (v1.0.6부터 이어진 과제).

## UI 관찰 (이번 리뷰)

- `ResponsiveActionContainer`의 임계값(360.dp / 1.6f)이 파라미터로 노출되어 있어
  조정 가능 — 현재 값 유지, 추후 실측(좁은 폭·대형 폰트 기기) 기반으로 조정.
- Column 모드에서 두 버튼 모두 `fillMaxWidth()`, Row 모드에서 primary만
  `weight(1f)` — primary/secondary 위계가 레이아웃에 그대로 반영되어 올바름.
- secondary가 null인 단일 액션의 경우에도 컨테이너를 재사용할 수 있으나,
  단일 버튼에까지 primitive를 씌우는 것은 과도 — 현재 SyncStatusCard처럼
  2-액션 이상일 때만 사용한다는 원칙을 `ResponsiveActionContainer` KDoc에 명시할 것.
- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도
  화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에
  "Additional UI design recommendations" 섹션으로 정리해주길 바란다.

## 완료 조건

1. G1~G3 구현 (G4/G5는 backlog/설계 노트로 남겨도 됨).
2. 기존 테스트 전부 green 유지, Android CI green.
3. `evidence/evidence_v1.0.9.md` 생성. evidence에는 G1~G3의 코드 위치·
   테스트 단언·CI run 번호/결론을 lineage(구현 커밋 hash)와 함께 기록하고,
   "Additional UI design recommendations" 섹션을 포함할 것.
4. **UI 디자인 관점에서 개선할 부분이 있으면 제안해줘** — evidence에
   "Additional UI design recommendations" 섹션을 포함할 것 (차단 조건 아님).
