# Feedback — v1.0.10 (review of evidence_v1.0.9)

대상 저장소: `geni2kim-ai/Lotto_analyzer` / 브랜치: `main`
리뷰한 evidence: `evidence/evidence_v1.0.9.md` (commit `00def99`, "docs: add v1.0.9 implementation evidence")

리뷰 방식: evidence 주장 ↔ 실제 커밋 diff ↔ Actions run 직접 대조.
구현 커밋: `d1fafad` ("feat: implement v1.0.9 feedback fixes").

작업을 시작하기 전에 `feedback/feedback_v1.0.9.md`를 읽고 원문 지시사항(G1~G3, G4/G5 backlog)과 대조했음을 확인한다.

## §0 v1.0.9 evidence 검증 결과

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
| --- | --- | --- |
| G1. interpolation checker escape dead code 수정 | PASS | 일치. `tools/check_kotlin_interpolation.py`의 일반 문자열 분기(72행)와 문자 리터럴 분기(92행)가 모두 `if current == "\\":`(1글자 백슬래시)로 수정됨을 커밋 diff에서 직접 확인. 기존 `"\\\\"`(2글자) 비교는 `current`(1글자)와 절대 일치하지 않아 항상 False였음. |
| G1. 회귀 테스트 2건 | PASS | 일치. `test_escaped_quote_does_not_turn_following_block_marker_into_comment`, `test_escaped_apostrophe_in_char_literal_preserves_following_scan` — `\"` 뒤의 `/*`가 주석 상태를 오염시켜 다음 줄 `$z개` 위반이 조용히 누락되는 false-negative 시나리오를 직접 재현하는 의미 있는 단언. |
| G2. Analysis ConfigSummaryCard → ResponsiveActionContainer | PASS | 일치. 고정 `Row`가 제거되고 `ResponsiveActionContainer(primary = { "다시 생성" Button }, secondary = { "설정 변경" OutlinedButton })`로 교체됨을 diff에서 확인. 강조 버튼이 primary로 배치되어 위계가 올바름. |
| G2. KDoc 사용 원칙 문서화 | PASS | 일치. `ResponsiveActionContainer.kt`에 "명확한 primary/secondary 액션 쌍에만 사용하고, 단일 액션을 일관성을 위해 감싸지 말 것"이라는 KDoc이 추가됨. |
| G3. 폰트 스케일 매트릭스 테스트 | PASS | 일치. `SyncStatusCardTest`에 1.0/1.3→row, 1.6/2.0→column 4건과 `assertSyncActionLayoutAtFontScale` 헬퍼. `w600dp-h800dp` qualifier로 폭 변수를 격리하고 `LocalDensity`로 fontScale을 주입해 Robolectric 실측 의존이 없음. 존재+부재 양쪽 단언, 기존 접근성 단언 유지. |
| G4. replay payload 버전화 | PASS (backlog 허용) | feedback_v1.0.9.md 완료 조건("G4/G5는 backlog/설계 노트로 남겨도 됨")에 따라 설계 노트로 수락. 수락 조건(`replay-v1` 식별자, 필드 안정성 등)이 evidence §5에 기록됨. |
| G5. durable sync receipt | PASS (backlog 허용) | 동상. receipt 구성 요소(성공 시각, 최신 확정 회차, 결과 요약 등)가 evidence §6에 기록됨. |
| 버전 | PASS | `versionName = "1.0.9"`, `versionCode = 10` 확인. |
| CI | PASS | Android CI run #32(`36525028999`, 구현 커밋 `d1fafad`) SUCCESS, run #33(`36525490458`, evidence 커밋 `00def99`) SUCCESS를 Actions 페이지에서 직접 확인. annotation은 runner-level(Node.js 20 deprecated, ubuntu-latest 마이그레이션 안내)만. |

**종합 판정: PASS** — v1.0.9 지시사항이 코드에 정확히 반영됐고, evidence의 주장이 실제 diff·CI와 일치한다.

## 조치 항목 (v1.0.10에서 구현)

### H1 (P2) — ResponsiveActionContainer 폭 임계값 경계 테스트
v1.0.9 evidence의 "Additional UI design recommendations"에서 제안된 항목을 정식 작업으로 승격. `shouldStackActions`의 폭 임계값(360dp)을 폰트 스케일과 독립적으로 보호하는 경계 테스트를 추가한다.
- `feature/ui`의 `ResponsiveActionContainer` 관련 테스트에 359dp / 360dp / 361dp 케이스를 추가한다 (예: qualifier를 `w359dp-h800dp` / `w360dp-h800dp` / `w361dp-h800dp`로 바꿔 폭 변수만 격리).
- `shouldStackActions` 구현의 `< 360.dp` 조건을 직접 읽고 경계 동작을 확정할 것 (359dp → Column, 360dp·361dp → Row).
- G3와 같은 패턴을 사용한다: 폰트 스케일은 1.0으로 고정하고, Robolectric 실측 의존 없이 태그 존재+부재를 단언한다.

### H2 (P3) — `@Config` qualifiers 공백 정규화
`SyncStatusCardTest`의 `@Config(sdk = [34], qualifiers = " w600dp-h800dp ")`에서 qualifier 문자열 앞뒤 공백을 제거해 `"w600dp-h800dp"`로 정규화한다.

### G4/G5 — backlog 유지
replay payload 버전화(G4)와 durable sync receipt(G5)는 backlog로 유지한다. 구현 시점의 수락 조건은 `evidence/evidence_v1.0.9.md` §5·§6에 기록되어 있다. 제품 결정 없이 임의로 구현하지 않는다.

## UI 관찰 (이번 리뷰)
- `ResponsiveActionContainer`가 Analysis로 확장되면서 KDoc 사용 원칙이 코드에 문서화됐다. "명확한 위계의 액션 쌍에만 사용" 원칙이 지켜지고 있다 (Analysis의 "다시 생성"/"설정 변경"은 적절한 적용).
- 단일 액션("재현 정보 복사" 등)에 primitive를 씌우지 않은 현재 상태가 올바르다 — 과도한 적용을 경계할 것.
- H1의 폭 경계 테스트가 추가되면 두 임계값(폭 360dp, 폰트 스케일 1.6f)이 각각 독립적으로 보호된다.
- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 "Additional UI design recommendations" 섹션으로 정리해주길 바란다.

## 완료 조건
1. H1·H2 구현 (G4/G5는 backlog 유지).
2. 앱 버전 bump: `versionName = "1.0.10"`, `versionCode = 11` (사이클 관행 유지).
3. 기존 테스트 전부 green 유지, Android CI green.
4. `evidence/evidence_v1.0.10.md` 생성. evidence에는 H1/H2의 코드 위치·테스트 단언·CI run 번호/결론을 lineage(구현 커밋 hash)와 함께 기록하고, "Additional UI design recommendations" 섹션을 포함할 것.
5. **UI 디자인 관점에서 개선할 부분이 있으면 제안해줘** — evidence에 "Additional UI design recommendations" 섹션을 포함할 것 (차단 조건 아님).
