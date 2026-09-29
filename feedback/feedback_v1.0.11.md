# Feedback — v1.0.11 (review of evidence_v1.0.10)

대상 저장소: `geni2kim-ai/Lotto_analyzer` / 브랜치: `main`
리뷰한 evidence: `evidence/evidence_v1.0.10.md` (commit `102fc8c`, "docs: add v1.0.10 implementation evidence")

리뷰 방식: evidence 주장 ↔ 실제 커밋 diff ↔ Actions run 직접 대조.
구현 커밋: `1a88b69` ("test: implement v1.0.10 responsive boundary coverage").

작업을 시작하기 전에 `feedback/feedback_v1.0.10.md`를 읽고 원문 지시사항(H1/H2, G4/G5 backlog)과 대조했음을 확인한다.

## §0 v1.0.10 evidence 검증 결과

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
| --- | --- | --- |
| H1. 359/360/361dp 폭 경계 테스트 | PASS | 일치. `ResponsiveActionContainerTest`에 3건 추가 + `assertResponsiveLayoutTagAtWidth` 헬퍼를 커밋 diff에서 직접 확인. 각 테스트가 태그 존재+부재 단언과 `shouldStackActions` 순수 결정 단언을 모두 수행. 폰트 스케일은 1.0으로 고정하고 Robolectric density를 유지해 폭 변수만 격리. 프로덕션 임계값(`availableWidth < 360.dp \|\| fontScale >= 1.6f`) 변경 없음. |
| H2. `@Config` qualifier 공백 정규화 | PASS (전제 수정 포함) | 지시문이 인용한 `" w600dp-h800dp "` 공백 패딩 형태는 베이스 커밋 시점 실제 코드에 존재하지 않았음(기존부터 `"w600dp-h800dp"`). 구현이 이 점을 evidence §3에 솔직히 기록하고, 클래스 레벨 `@Config`를 multiline으로 재포맷 + 공백 금지 주석 추가로 명문화한 조치는 diff에서 확인. 값 변경 없음(변경할 것이 없었음). |
| G4. replay payload 버전화 | PASS (backlog 허용) | 일치. 구현 변경 없음. 수락 조건은 `evidence/evidence_v1.0.9.md` §5 기록 유지. |
| G5. durable sync receipt | PASS (backlog 허용) | 일치. 구현 변경 없음. |
| 버전 | PASS | `versionName = "1.0.10"`, `versionCode = 11`을 diff에서 확인. |
| CI | PASS | Android CI run #35(구현 커밋 `1a88b69`) SUCCESS, run #36(evidence 커밋 `102fc8c`) SUCCESS를 Actions 페이지에서 직접 확인. |

**종합 판정: PASS** — v1.0.10 지시사항이 코드에 반영됐고, evidence의 주장이 실제 diff·CI와 일치한다. 단, H2 지시문의 전제(공백 패딩 qualifier의 존재)가 실제 코드와 달랐던 점은 아래 I1의 프로세스 보완 항목으로 남긴다.

## 조치 항목 (v1.0.11에서 구현)

### I1 (P3) — feedback 지시 전제 검증 절차
v1.0.10 H2에서 지시문이 인용한 코드 상태(`" w600dp-h800dp "`)가 베이스 커밋의 실제 상태와 달랐던 사례의 재발 방지.
- 코드 변경을 지시하기 전, 인용하는 스니펫이 베이스 커밋(`main` HEAD)의 실제 파일 내용과 일치하는지 `git show <base>:<path>` 또는 파일 뷰어로 직접 확인할 것.
- 인용 상태와 실제 상태가 다르면 없는 버그를 만들어내지 말고, "전제 불일치"를 evidence/feedback에 기록한 뒤 주석·포맷 명문화 같은 위생 조치로 갈음할 수 있다.

### I2 (P2) — 360dp 경계 테스트의 정밀도 전제 명시
`ResponsiveActionContainerTest.width360dpKeepsActionsHorizontal`은 Robolectric qualifier `w360dp-h800dp`가 `BoxWithConstraints`의 `maxWidth`를 정확히 `360.dp`로 매핑한다는 암묵적 전제에 의존한다(픽셀→dp 변환에서 359.999dp 같은 값이 나오면 `< 360.dp` 조건이 뒤집힌다). 현재 CI에서 안정 통과 중이므로 동작 변경은 불필요.
- 세 경계 테스트(또는 `assertResponsiveLayoutTagAtWidth` 헬퍼)에 주석으로 전제를 명시할 것: "qualifier 너비가 dp 정수 경계에 정확히 매핑됨을 전제. 태그 단언이 통합 경로 검증, `shouldStackActions` 직접 단언이 경계값 회귀 앵커."
- 순수 결정 함수 단언은 프로덕션 함수 자체를 호출하므로 단독으로는 회귀를 강제하지 못한다는 점을 주석에 남길 것.

### I3 (P3) — provenance/status 칩 레이아웃의 좁은 너비 대응
v1.0.10 evidence의 "Additional UI design recommendations"에서 지적된 다음 반응형 약점을 정식 작업으로 승격. 고정 2열 provenance/status 칩 레이아웃이 좁은 너비(≤360dp) 또는 1.6x 이상 폰트 스케일에서 겹치거나 잘릴 가능성이 있다.
- 해당 칩 레이아웃을 `FlowRow`(또는 너비 기반 wrapping 레이아웃)로 교체한다.
- H1과 같은 패턴으로 경계 테스트를 추가한다: 359dp/360dp qualifier에서 칩이 겹치지 않고(또는 의도대로 wrap되어) 표시됨을 태그 기반 단언으로 확인하고, 폰트 스케일은 1.0으로 고정한다.
- 임계값은 `ResponsiveActionContainer`의 공유 primitive에 둔다. 화면별 `360.dp`/`1.6f` 하드코딩을 새로 만들지 않는다.

### G4/G5 — backlog 유지
replay payload 버전화(G4)와 durable sync receipt(G5)는 backlog로 유지한다. 구현 시점의 수락 조건은 `evidence/evidence_v1.0.9.md` §5·§6에 기록되어 있다. 제품 결정 없이 임의로 구현하지 않는다.

## UI 관찰 (이번 리뷰)
- H1 경계 테스트 추가로 두 임계값(폭 360dp, 폰트 스케일 1.6f)이 각각 독립적으로 보호된다. `ResponsiveActionContainer`의 Row/Column 분기에서 primary가 먼저·강조 배치되어 위계가 올바르다.
- Row 분기(`primary`에 `weight(1f)`, secondary 고정 폭)는 360dp를 갓 넘는 너비에서 secondary 라벨이 길어지면 primary가 과도하게 좁아질 수 있다. 현재 사용처(Analysis의 "다시 생성"/"설정 변경")는 짧은 라벨이라 문제없으나, 라벨이 길어지는 화면에 적용할 때는 secondary에도 `weight` 또는 `wrapContentWidth` 정책을 명시할 것.
- 단일 액션("재현 정보 복사" 등)에 primitive를 씌우지 않는 KDoc 원칙이 계속 지켜지고 있다 — 과도한 적용을 경계할 것.
- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 "Additional UI design recommendations" 섹션으로 정리해주길 바란다.

## 완료 조건
1. I1 절차 확인(코드 인용 전 실제 상태 대조), I2 주석 명시, I3 칩 레이아웃 대응 구현 (G4/G5는 backlog 유지).
2. 앱 버전 bump: `versionName = "1.0.11"`, `versionCode = 12` (사이클 관행 유지).
3. 기존 테스트 전부 green 유지, Android CI green.
4. `evidence/evidence_v1.0.11.md` 생성. evidence에는 I1~I3의 코드 위치·테스트 단언·CI run 번호/결론을 lineage(구현 커밋 hash)와 함께 기록하고, "Additional UI design recommendations" 섹션을 포함할 것.
5. **UI 디자인 관점에서 개선할 부분이 있으면 제안해줘** — evidence에 "Additional UI design recommendations" 섹션을 포함할 것 (차단 조건 아님).
