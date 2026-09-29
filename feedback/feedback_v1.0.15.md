# Feedback — v1.0.15 (review of evidence_v1.0.14)

대상 저장소: `geni2kim-ai/Lotto_analyzer` / 브랜치: `main`
리뷰한 evidence: `evidence/evidence_v1.0.14.md` (구현 커밋 `e85ee5a6470b89f682372e43b4f45f946b1a3794`, "fix: pin fragment to compatible stable 1.8.9")
리뷰 방식: evidence 주장 ↔ 실제 커밋 diff 직접 대조 (public repo, compare view).

작업을 시작하기 전에 `feedback/feedback_v1.0.14.md`를 읽고 원문 지시사항(P-1, L1, G4/G5 backlog)과 대조했음을 확인한다.
`~/workspace/lotto-loop/pending/pending_items.md`도 확인한다 — 현재 보류 항목 없음 (P-1은 feedback_v1.0.14 포함 → evidence_v1.0.14에서 구현 완료 확인됨).

## §0 v1.0.14 evidence 검증 결과

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
| --- | --- | --- |
| P-1. fragment 1.1.0 해소 | PASS | 일치. 의존 경로 `play-services-basement:18.4.0 → fragment:1.1.0`을 dependencyInsight로 특정. 1.9.1 시도 후 CI #51에서 `:app:checkDebugAarMetadata` 실패(AGP 8.6+ 요구)로 기각 — 추측이 아닌 실제 CI 실패 근거. 최종 `app/build.gradle.kts`에 app-scoped `force("androidx.fragment:fragment:1.8.9")` 적용 (Firebase BOM / Compose BOM / Activity / AGP 변경 없음). CI 워크플로에 dependencyInsight gate 추가 (`1.1.0 -> 1.8.9` 단언). |
| L1. localization stress 테스트 | PASS | 일치. `localizationStressAt359dpAnd2_0FontScaleKeepsFlowItemsVisibleAndSeparate` 추가: 359dp/2.0x, `latestDrawNo`/`recentN` = `Int.MAX_VALUE`, 64자 algorithm identifier. helper를 파라미터화(`latestDrawNo`/`recentN`/`usePrizeIndex`/`algorithmVersion`)하고 기존 케이스는 기본값(1234/100/true/"test-v1") 유지. 기존 단언(존재·가시성·containment·pairwise non-overlap) 재사용. |
| StatusChip KDoc | PASS | 일치. `maxLines`/`overflow`가 시각 레이아웃 제약일 뿐 source text를 변경하지 않음을 명시 — v1.0.12 whitespace-observability 계약과의 관계 설명됨. |
| 버전 | PASS | diff에서 `versionCode 14→15`, `versionName "1.0.13"→"1.0.14"` 확인. |
| CI | PASS (evidence 주장 기준, 불확실성 명시) | evidence 주장: Android CI #52 (run id `36595783887`) SUCCESS. 텍스트 fetch로는 run 결론을 직접 보지 못함 (전 사이클과 동일한 한계). commit 페이지에서 최종 커밋이 grep 기대값 1.9.1→1.8.9 변경을 포함한 것은 확인. |
| G4/G5 | PASS (backlog 유지) | 일치. diff에 변경 없음. |

**종합 판정: PASS** — v1.0.14 지시사항(P-1, L1)이 코드에 반영됐고, evidence의 주장이 실제 diff와 일치한다.

### 관찰 / 미세 지적 (차단 아님)

- R1. `force()`가 `configurations.configureEach`에 있어 app 모듈의 모든 configuration에 적용된다. 현재로선 문제 없으나(fragment를 앱 코드가 직접 쓰지 않음), 의도가 runtime 범위에 한정된 것이라면 주석에 스코프를 명시하거나 `constraints` 블록 대안을 검토할 것.
- R2. CI gate의 `grep -Fq 'androidx.fragment:fragment:1.1.0 -> 1.8.9'`는 고정 문자열 매칭이라 버전 변경 시 workflow도 함께 고쳐야 한다. 의도된 gate이므로 유지하되, 버전 상수를 한 곳에서 관리하면 drift를 방지할 수 있다.
- R3. commit 페이지 텍스트 추출상 최종 force 문자열에 후행 공백처럼 보이는 artifact가 있으나, 공식 compare diff와 CI #52의 정상 해결(`1.1.0 -> 1.8.9`)로 보아 추출 artifact로 판단한다. 이번 사이클에서 `force` 문자열에 후행 공백이 없음을 명시적으로 확인할 것 (Gradle은 버전 문자열의 후행 공백을 용납하지 않으므로).

## 조치 항목 (v1.0.15에서 구현)

### F1 (P1) — fragment 버전 상수화 또는 "두 곳 동시 변경" 체크리스트 명문화

- `app/build.gradle.kts`의 force 버전과 `.github/workflows/android-ci.yml`의 grep 기대값이 같은 문자열 "1.8.9"를 두 곳에 하드코딩하고 있다. 한 곳만 바꾸면 CI가 깨지거나(안전 방향) gate가 무력화된다.
- 조치 (둘 중 하나 선택):
  - (a) 버전을 gradle property 또는 버전 카탈로그 스타일 상수로 빼고, workflow에서 해당 값을 읽어 grep에 사용한다.
  - (b) 현상 유지를 선택하면, evidence에 "fragment 버전 변경 시 build.gradle.kts와 android-ci.yml을 반드시 함께 변경" 체크리스트를 명문화한다.
- 어느 쪽이든 evidence에 선택 근거와 함께 기록한다.

### U1 (P2) — UI: Analysis 칩 영역 실기기 육안 확인 (v1.0.14 이월)

- v1.0.14 피드백 UI 관찰에서 제기: FlowRow 전환 후 마지막 행에 칩 1~2개만 남을 때 우측 여백이 시각적으로 비어 보일 수 있다.
- 조치: 359dp/2.0x 조건에서 실기기 또는 스크린샷으로 확인하고, 문제가 있으면 간격/정렬 조정안을 evidence의 "Additional UI design recommendations"에 기록한다 (구현은 다음 사이클로 이월 가능).

### U2 (P2) — UI: FlowRow 래핑 시 TalkBack 순서 확인 (v1.0.14 이월)

- `AlgorithmStatusChip` ellipsize 후 semantics 순서가 시각 순서(래핑 후)와 일치하는지 확인한다.
- 조치: Robolectric semantics 순서 단언 또는 수동 확인 중 가능한 쪽으로 수행하고 결과를 evidence에 기록한다.

### G4/G5 — backlog 유지

replay payload 버전화(G4)와 durable sync receipt(G5)는 backlog로 유지한다. 수락 조건은 `evidence/evidence_v1.0.9.md` §5·§6에 기록되어 있다. 제품 결정 없이 임의로 구현하지 않는다.

## UI 관찰 (이번 리뷰)

- L1 테스트가 `Int.MAX_VALUE`를 쓰는데, 실제 표시 포맷(천 단위 구분자 등)이 바뀌면 stress 케이스의 라벨도 함께 갱신해야 한다 — v1.0.14 evidence의 권고와 일치.
- `force` 방식은 Play Console 경고 해소에는 최소 스코프이나, fragment를 앱 코드가 직접 쓰지 않으므로 deprecated API 노출 우려는 없다.
- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 "Additional UI design recommendations" 섹션으로 정리해주길 바란다.

## 완료 조건

1. F1 (버전 상수화 또는 체크리스트 명문화), U1/U2 확인 기록 (G4/G5는 backlog 유지).
2. 앱 버전 bump: `versionName = "1.0.15"` (후행 공백 없이), `versionCode = 16` (사이클 관행 유지).
3. 기존 테스트 전부 green 유지, Android CI green.
4. `evidence/evidence_v1.0.15.md` 생성. evidence에는 F1/U1/U2의 코드 위치·단언/확인 결과·CI run 번호/결론을 lineage(구현 커밋 hash)와 함께 기록하고, "Additional UI design recommendations" 섹션을 포함할 것.
5. **UI 디자인 관점에서 개선할 부분이 있으면 제안해줘** — evidence에 "Additional UI design recommendations" 섹션을 포함할 것 (차단 조건 아님).
