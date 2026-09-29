# Feedback — v1.0.13 (review of evidence_v1.0.12)

대상 저장소: `geni2kim-ai/Lotto_analyzer` / 브랜치: `main`
리뷰한 evidence: `evidence/evidence_v1.0.12.md` (구현 커밋 `6e32f3ec7a0e8dde84cc750d1696b4aa21788d17`, "test: implement v1.0.12 hygiene and worst-case coverage")
리뷰 방식: evidence 주장 ↔ 실제 커밋 diff 직접 대조 (베이스 HEAD `59edab69f308ab5b8d46e9f3bbe458c44ed6c1db` ↔ 구현 커밋, public repo read-only clone으로 전체 diff 확보).

작업을 시작하기 전에 `feedback/feedback_v1.0.12.md`를 읽고 원문 지시사항(J1/J2/J3, G4/G5 backlog)과 대조했음을 확인한다.

## §0 v1.0.12 evidence 검증 결과

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
| --- | --- | --- |
| J1. versionName 후행 공백 전제 + bump | PASS (전제 불일치 정직 기록) | 일치. diff: `versionCode 12→13`, `versionName "1.0.11"→"1.0.12"`. base blob `5cbcfc4…`·구현 blob `918d453…`이 evidence 기록과 일치. base에 trailing space가 없었음을 evidence §2에 mismatch로 기록하고 가짜 수정을 만들지 않음. |
| J2. 칩 라벨 whitespace 계약 | PASS (전제 불일치 정직 기록) | 일치. diff는 `StatusChip` KDoc 6줄 추가뿐(verbatim 렌더·trim 없음 명시). base blob `7f419b7…`·구현 blob `63231fe…` 일치. `StatusChip` 본체가 `Text(text = text, …)`이며 trim이 없음을 직접 확인. production 라벨 변경 없음(두 call site 모두 이미 정확). 테스트에 exact-matching 주석 2줄 추가. |
| J3. 359dp + 2.0x 결합 커버리지 | PASS | 일치. 새 테스트 `width359dpAt2_0FontScaleKeepsAllStatusChipsInWrapLayout` (`@Config(sdk = [34], qualifiers = "w359dp-h800dp")`, helper에 fontScale 2.0f 주입). helper가 wrap 태그 존재·grid 태그 부재·4개 칩 라벨(`"최신 1234회"`, `"분석 100회"`, `"EV 사용"`, `"알고리즘 test-v1"`) exact 단언. impl blob `e76836f…` 일치. `@Config` sdk 34는 기존 테스트와 일관. evidence가 pixel-level 한계를 backlog로 명시 — 정직함. |
| G4. replay payload 버전화 | PASS (backlog 유지) | 일치. diff에 변경 없음. |
| G5. durable sync receipt | PASS (backlog 유지) | 일치. diff에 변경 없음. |
| 버전 | PASS | `versionName = "1.0.12"`, `versionCode = 13`을 diff에서 확인. |
| CI | PASS (evidence 주장 기준, 불확실성 명시) | evidence 주장: Android CI run #41 (run id `36535338635`) SUCCESS. run 페이지 제목이 구현 커밋 `6e32f3e`에 연결된 것은 확인했으나, browser 텍스트 fetch로는 결론 텍스트를 직접 보지 못함. |

**종합 판정: PASS** — v1.0.12 지시사항(J1/J2/J3)이 코드에 반영됐고, evidence의 주장이 실제 diff와 일치한다. diff는 23 insertions / 4 deletions로 스코프가 깨끗하고, J1/J2의 전제 불일치를 숨기지 않고 기록한 점은 I1 절차가 의도대로 작동하고 있음을 보여준다.

## 조치 항목 (v1.0.13에서 구현)

### K1 (P2) — pixel-level 시각 검증 인프라 도입 검토

J3의 한계(태그 단언은 분기 선택만 검증하고, 칩 내부 텍스트 줄바꿈·클리핑·높이 깨짐은 잡을 수 없음)를 해소하기 위한 최소 스코프 검토.

- Paparazzi 또는 compose-ui-test의 `captureToImage` 중 하나로, `359dp + 2.0 font scale + 최장 한글 라벨` 조합 1~2개 케이스의 스냅샷 테스트 도입을 검토한다.
- 도입이 과도하다고 판단되면 그 근거(유지 비용 vs 효용)를 evidence에 기록하고 backlog로 둔다. 차단 조건 아님.

### K2 (P2) — narrow 분기의 FlowRow 도입

현재 narrow 분기는 칩 4개를 전폭 행으로 스택해 Analysis 화면 상단 높이를 2열 그리드의 2배로 만든다.

- wrap 분기를 `FlowRow` 기반으로 변경해 짧은 칩이 행을 공유하도록 한다. 단, fontScale 2.0 + 359dp 최악 케이스에서도 칩이 겹치거나 잘리지 않아야 한다.
- 기존 태그(`analysis-status-chips-wrap`) 유지 + 최악 케이스 테스트(`width359dpAt2_0FontScaleKeepsAllStatusChipsInWrapLayout`)를 FlowRow 기준으로 확장: 4개 칩 모두 존재하고, semantics 수준에서 단언 가능한 범위까지 겹침/잘림 없음을 보장.
- 시각적 균형은 K1의 스냅샷이 있으면 함께 기록, 없으면 수동 확인 후 evidence에 명시.

### K3 (P3) — algorithm label 길이 상한

긴 algorithmVersion 문자열이 결과 헤더를 지배하지 않도록:

- 칩 라벨에 `maxLines = 1` + `TextOverflow.Ellipsis` 적용.
- 전체 값은 접근 가능한 표면(예: `contentDescription` 또는 상세/복사 UI)에서 확인할 수 있게 한다.
- `"알고리즘 test-v1"` exact 단언이 깨지지 않도록 테스트 유지.

### G4/G5 — backlog 유지

replay payload 버전화(G4)와 durable sync receipt(G5)는 backlog로 유지한다. 수락 조건은 `evidence/evidence_v1.0.9.md` §5·§6에 기록되어 있다. 제품 결정 없이 임의로 구현하지 않는다.

## UI 관찰 (이번 리뷰)

- v1.0.12 diff의 KDoc·주석은 "왜"를 설명한다(verbatim 정책의 근거 = 테스트에서 공백 결함을 관찰 가능하게 유지). 이런 문서화 관행은 유지할 것.
- `StatusChip`의 `secondaryContainer` + `labelMedium` + 고정 패딩(10.dp/8.dp)은 fontScale 2.0에서 칩 높이가 커지므로, K2 FlowRow 변경 시 행 간 vertical spacing이 시각적으로 균등한지 확인할 것.
- evidence의 "Additional UI design recommendations" 품질이 좋았고, K2/K3는 그 제안을 승격한 것이다. 제안이 backlog로만 쌓이지 않도록 각 사이클에서 1~2건씩 실제 반영 여부를 판단할 것.
- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 "Additional UI design recommendations" 섹션으로 정리해주길 바란다.

## 완료 조건

1. K1 검토 기록, K2 FlowRow 도입(+테스트 확장), K3 라벨 길이 상한 (G4/G5는 backlog 유지).
2. 앱 버전 bump: `versionName = "1.0.13"` (후행 공백 없이), `versionCode = 14` (사이클 관행 유지).
3. 기존 테스트 전부 green 유지, Android CI green.
4. `evidence/evidence_v1.0.13.md` 생성. evidence에는 K1~K3의 코드 위치·테스트 단언·CI run 번호/결론을 lineage(구현 커밋 hash)와 함께 기록하고, "Additional UI design recommendations" 섹션을 포함할 것.
5. **UI 디자인 관점에서 개선할 부분이 있으면 제안해줘** — evidence에 "Additional UI design recommendations" 섹션을 포함할 것 (차단 조건 아님).
