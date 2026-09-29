## Feedback — v1.0.14 (review of evidence_v1.0.13)

대상 저장소: `geni2kim-ai/Lotto_analyzer` / 브랜치: `main`
리뷰한 evidence: `evidence/evidence_v1.0.13.md` (구현 커밋 `6dd222f58aac12b6f089804afb71f473d6c35355`, "feat: implement v1.0.13 flow chips and label bounds")

리뷰 방식: evidence 주장 ↔ 실제 커밋 diff 직접 대조 (diff 확보, public repo).

작업을 시작하기 전에 `feedback/feedback_v1.0.13.md`를 읽고 원문 지시사항(K1/K2/K3, G4/G5 backlog)과 대조했음을 확인한다.

## §0 v1.0.13 evidence 검증 결과

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
| --- | --- | --- |
| K1. pixel-level 시각 검증 인프라 도입 검토 | PASS (REVIEWED / BACKLOG, 비차단) | 일치. Paparazzi vs compose-ui-test `captureToImage` 옵션 검토 기록, 도입 보류 근거(유지 비용 vs 효용) 명시. diff에 프레임워크 도입 없음. |
| K2. narrow 분기 FlowRow 도입 | PASS | 일치. narrow 분기 Column(칩별 fillMaxWidth) → FlowRow(수평 6.dp / 수직 4.dp spacing, fillMaxWidth 제거). 태그 `analysis-status-chips-wrap` 유지. 테스트 확장: 359dp/360dp 분기 유지 + 2.0x 케이스에 semantics bounds containment + pairwise non-overlap 단언 추가 (`Rect.overlaps`, strict 부등식이므로 접하는 경계는 겹침으로 판정하지 않음 — 단언 설계 올바름). |
| K3. algorithm label 길이 상한 | PASS | 일치. `AlgorithmStatusChip` (maxLines=1, TextOverflow.Ellipsis) 양 분기 적용. `StatusChip`에 maxLines/overflow optional param 추가, 기본값(Int.MAX_VALUE/Clip)으로 기존 동작 보존. 긴 version 테스트에서 semantics에 full text 유지. |
| G4/G5 | PASS (backlog 유지) | 일치. diff에 변경 없음. |
| 버전 | PASS | diff에서 `versionCode 13→14`, `versionName "1.0.12"→"1.0.13"` 확인. |
| CI | PASS (evidence 주장 기준, 불확실성 명시) | evidence 주장: Android CI run #44 (run id `36539995784`) SUCCESS. run 페이지 제목이 구현 커밋 `6dd222f`에 연결된 것은 확인했으나, 텍스트 fetch로는 결론 텍스트를 직접 보지 못함. |

**종합 판정: PASS** — v1.0.13 지시사항(K1/K2/K3)이 코드에 반영됐고, evidence의 주장이 실제 diff와 일치한다.

## 조치 항목 (v1.0.14에서 구현)

### P-1 (출처: Play Console 알림 (2026-09-29)) — androidx.fragment:fragment 1.1.0 구버전 경고 해소

- Play Console 알림 내용: "Lotto Insight - 로또 번호 분석 9월 20일 사용 중인 SDK 버전이 오래됨" / 본문: "androidx.fragment:fragment님이 fragment:1.1.0을(를) 오래된 버전으로 신고했습니다. 최신 SDK 버전으로 업데이트하는 것이 좋습니다." 영향 App Bundle: 버전 3 (1.0.2), 출시 "1.0.2 (3) - API 36 대응 비공개 테스트 버전".
- 확인된 사실: `app/build.gradle.kts`에 fragment 직접 의존 없음 — transitive 의존으로 추정. `gradle/libs.versions.toml`은 존재하지 않음(버전 하드코딩 방식).
- 조치:
  1. `./gradlew :app:dependencies` (또는 dependency insight)로 fragment 1.1.0을 끌어오는 의존 경로를 특정한다.
  2. 최신 안정 버전(예: 1.8.x 계열)으로 해결 — `resolutionStrategy { force(...) }` 또는 상위 의존 업그레이드 중 최소 스코프 선택. compose-bom(2024.02.00)/activity-compose(1.8.2) 계열과 충돌 없는지 확인.
  3. 빌드 + 전체 유닛 테스트 통과 확인 → evidence에 의존 경로·선택한 버전·CI 결과를 기록.
- 주의: Fragment API 직접 사용처가 없으면 동작 변경은 없어야 한다. Play SDK 인덱스 경고이므로 필수는 아니나, 구버전 fragment의 알려진 이슈 회피 차원에서 최신으로 올리는 것이 안전하다.

### L1 (P2) — localization stress: 최장 한글 라벨 + 긴 algorithm identifier FlowRow 테스트

- v1.0.13 evidence의 "Additional UI design recommendations"(Analysis 섹션) 제안 승격. 기존 테스트 라벨("최신 1234회", "분석 100회" 등)은 짧은 고정값이라 최악 케이스를 충분히 대변하지 못한다.
- 조치: `359dp + 2.0 font scale` 조건에서 실제 가능한 최장 한글 라벨(큰 회차 번호, 큰 분석 횟수 등)과 긴 algorithm identifier 조합 케이스를 추가. 기존 단언(존재·가시성·containment·non-overlap)을 재사용.
- 차단 조건 아님. 픽셀 스냅샷 도입은 K1 backlog 유지.

### G4/G5 — backlog 유지

replay payload 버전화(G4)와 durable sync receipt(G5)는 backlog로 유지한다. 수락 조건은 `evidence/evidence_v1.0.9.md` §5·§6에 기록되어 있다. 제품 결정 없이 임의로 구현하지 않는다.

## UI 관찰 (이번 리뷰)

- FlowRow 전환으로 narrow 분기 높이가 줄었으나, 마지막 행에 칩 1~2개만 남을 때 우측 여백이 시각적으로 비어 보일 수 있음. 다음 사이클에 359dp/2.0x 실기기(또는 스크린샷)로 육안 확인 권장.
- `AlgorithmStatusChip` ellipsize 후 TalkBack 읽기 순서: FlowRow의 semantics 순서가 시각 순서(래핑 후)와 일치하는지 수동 확인 권장 (evidence "Common/accessibility" 권고와 연결).
- `StatusChip`의 KDoc "verbatim 렌더" 계약(v1.0.12)과 `maxLines = 1` 추가의 관계를 주석에 명시할 것 — 왜 1줄 제한이 verbatim 정책과 충돌하지 않는지(잘림이 아닌 줄 수 제한 + ellipsis, full text는 semantics에 보존).
- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 "Additional UI design recommendations" 섹션으로 정리해주길 바란다.

## 완료 조건

1. P-1 fragment 버전 해결(의존 경로·선택 버전 기록), L1 stress 테스트 추가 (G4/G5는 backlog 유지).
2. 앱 버전 bump: `versionName = "1.0.14"` (후행 공백 없이), `versionCode = 15` (사이클 관행 유지).
3. 기존 테스트 전부 green 유지, Android CI green.
4. `evidence/evidence_v1.0.14.md` 생성. evidence에는 P-1/L1의 코드 위치·테스트 단언·CI run 번호/결론을 lineage(구현 커밋 hash)와 함께 기록하고, "Additional UI design recommendations" 섹션을 포함할 것.
5. **UI 디자인 관점에서 개선할 부분이 있으면 제안해줘** — evidence에 "Additional UI design recommendations" 섹션을 포함할 것 (차단 조건 아님).
