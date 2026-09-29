# Feedback — v1.0.12 (review of evidence_v1.0.11)

대상 저장소: `geni2kim-ai/Lotto_analyzer` / 브랜치: `main`
리뷰한 evidence: `evidence/evidence_v1.0.11.md` (구현 커밋 `0d92a40f08d182478c962eda33f6a83fd47ce3ec`, "feat: implement v1.0.11 responsive status chips")
리뷰 방식: evidence 주장 ↔ 실제 커밋 diff 직접 대조.

작업을 시작하기 전에 `feedback/feedback_v1.0.11.md`를 읽고 원문 지시사항(I1/I2/I3, G4/G5 backlog)과 대조했음을 확인한다.

## §0 v1.0.11 evidence 검증 결과

| 항목 | 판정 | evidence 주장과 실제 코드의 일치 여부 |
| --- | --- | --- |
| I1. feedback 전제 검증 | PASS | 일치. 베이스 HEAD `73dc84fc`의 blob hash 4건(`AnalysisScreen.kt`, `ResponsiveActionContainer.kt`, 두 테스트 파일)을 evidence §2에 기록하고, H2 전제 불일치(공백 패딩 qualifier `" w600dp-h800dp "` 미존재)를 재확인·기록. 존재하지 않는 버그를 만들지 않음. 커밋 diff에 해당 영역의 임의 수정 없음. |
| I2. 360dp 경계 테스트 정밀도 주석 | PASS | 일치. `ResponsiveActionContainerTest.kt`의 `assertResponsiveLayoutTagAtWidth` 바로 위에 5줄 주석 추가 — qualifier→dp 매핑 전제, 태그 단언=통합 경로 검증, `shouldStackActions` 직접 단언=경계값 회귀 앵커(단독 회귀 강제 불가) 명시. diff에서 확인. |
| I3. status 칩 반응형 대응 | PASS | 일치. `AnalysisStatusChips`가 `BoxWithConstraints` + 공유 `shouldStackActions(maxWidth, LocalDensity.current.fontScale)` 기반으로 전환됨. 좁은 너비/대형 폰트 분기 → 칩 4개를 전폭 행으로 스택(tag `analysis-status-chips-wrap`), 일반 분기 → 기존 2열 그리드 유지(tag `analysis-status-chips-grid`). 화면 로컬 `360.dp`/`1.6f` 하드코딩 없음(diff에서 확인). |
| I3 경계 테스트 | PASS | 일치. `AnalysisStatusChipsTest` 신설: `width359dpWrapsStatusChipsToSingleColumn`(qualifier `w359dp-h800dp`), `width360dpKeepsStatusChipsInTwoColumnGrid`(qualifier `w360dp-h800dp`). fontScale 1.0 고정(Robolectric density 유지), 태그 존재+부재 단언, 4개 칩 라벨 내용 단언. `private`→`internal` 가시성 변경은 테스트 접근용으로 정당. |
| G4. replay payload 버전화 | PASS (backlog 허용) | 일치. 구현 변경 없음. |
| G5. durable sync receipt | PASS (backlog 허용) | 일치. 구현 변경 없음. |
| 버전 | PASS (위생 항목 1건) | `versionName = "1.0.11"`, `versionCode = 12`을 diff에서 확인. 단, versionName 문자열에 후행 공백(`"1.0.11 "`)이 v1.0.10(`"1.0.10 "`)부터 이어지고 있음 — 아래 J1. |
| CI | PASS (evidence 주장 기준) | evidence 주장: Android CI run #38 (run id `36532504097`) SUCCESS, `feature:ui:testDebugUnitTest` 실행 성공. run 페이지가 구현 커밋 `0d92a40`에 연결된 것은 확인. |

**종합 판정: PASS** — v1.0.11 지시사항(I1/I2/I3)이 코드에 반영됐고, evidence의 주장이 실제 diff와 일치한다.

## 조치 항목 (v1.0.12에서 구현)

### J1 (P3) — `versionName` 문자열 후행 공백 제거

`app/build.gradle.kts`의 `versionName = "1.0.11 "` — 문자열 끝에 공백이 들어 있다(v1.0.10의 `"1.0.10 "`에서 이월). 앱 동작에는 영향이 없으나, evidence가 `"1.0.10"`으로 인용하며 공백을 못 본 사례(I1 유형의 전제 불일치)와 연결되는 위생 문제.

- `versionName = "1.0.12"` (후행 공백 없이), `versionCode = 13`.
- 이후 evidence의 버전 인용은 따옴표까지 정확히 복사할 것.

### J2 (P2) — 칩 라벨 `"알고리즘 $algorithmVersion "` 후행 공백 제거

`AnalysisScreen.kt`의 `StatusChip(text = "알고리즘 $algorithmVersion ")` — 라벨 문자열 끝에 공백이 있다. 테스트는 `"알고리즘 test-v1"`을 단언하고 CI가 green이므로 현재 매칭은 깨지지 않지만(또는 `StatusChip` 내부에서 trim), production 문자열과 테스트 단언의 정확 일치를 위해 후행 공백을 제거한다.

- `"알고리즘 $algorithmVersion "` → `"알고리즘 $algorithmVersion"`.
- `StatusChip`이 텍스트를 trim하는지 확인하고, trim한다면 그 동작을 KDoc/주석으로 명시할 것(침묵 trim은 나중에 다른 라벨에서 의도치 않은 공백 제거로 이어질 수 있다).

### J3 (P3) — 칩 narrow 분기의 시각 검증 관점 보완

I3의 narrow 분기는 칩 4개를 전폭 행으로 스택한다. 태그 단언은 분기 선택을 검증하지만, 대형 폰트(예: 359dp + 2.0 font scale 결합)에서 칩 내부 텍스트가 2줄로 늘어나거나 높이가 깨지는 문제는 태그 테스트로 잡을 수 없다.

- evidence v1.0.11의 "Additional UI design recommendations"에 제안된 최악 케이스(359dp + 2.0 font scale)에 대해, 최소 1회 수동 스크린샷 확인 또는 screenshot/visual 테스트 도입을 검토한다(차단 조건 아님).
- 검토 결과는 evidence에 기록.

### G4/G5 — backlog 유지

replay payload 버전화(G4)와 durable sync receipt(G5)는 backlog로 유지한다. 구현 시점의 수락 조건은 `evidence/evidence_v1.0.9.md` §5·§6에 기록되어 있다. 제품 결정 없이 임의로 구현하지 않는다.

## UI 관찰 (이번 리뷰)

- I3 narrow 분기의 전폭 4행 스택은 가독성은 명확하지만 Analysis 화면 상단에서 차지하는 높이가 2열 그리드의 2배가 된다. 좁은 화면에서 스크롤 깊이가 늘어나는 트레이드오프가 있으니, 향후 `FlowRow`식 wrap(칩 내용 길이에 따라 한 행에 2개 배치)과 비교하는 디자인 검토를 backlog에 남긴다.
- grid 분기의 `Column`에 `testTag`를 달고 내부 두 `Row`에는 태그가 없다 — 현재 단언 수준(분기 선택 + 내용 보존)에는 충분하고, Row별 태그는 과도한 명세이므로 추가하지 않는다.
- `ResponsiveActionContainer`의 primary `weight(1f)`/secondary intrinsic 정책은 칩 영역에 적용되지 않음(칩은 2열 균등 `weight(1f)`) — 두 primitive의 정책이 섞이지 않아 일관성 양호.
- `AnalysisStatusChips`의 `private`→`internal` 변경은 테스트 접근용이며, public API 표면이 늘어나지 않음.
- UI 디자인 관점에서 개선할 부분이 있으면 제안해줘. 위 관찰 외에도 화면별(Analysis/History/Statistics/Database)로 구체적인 개선안을 evidence에 "Additional UI design recommendations" 섹션으로 정리해주길 바란다.

## 완료 조건

1. J1 위생 수정, J2 후행 공백 제거(+trim 동작 확인/명시), J3 검토 기록 (G4/G5는 backlog 유지).
2. 앱 버전 bump: `versionName = "1.0.12"` (후행 공백 없이), `versionCode = 13` (사이클 관행 유지).
3. 기존 테스트 전부 green 유지, Android CI green.
4. `evidence/evidence_v1.0.12.md` 생성. evidence에는 J1~J3의 코드 위치·테스트 단언·CI run 번호/결론을 lineage(구현 커밋 hash)와 함께 기록하고, "Additional UI design recommendations" 섹션을 포함할 것.
5. **UI 디자인 관점에서 개선할 부분이 있으면 제안해줘** — evidence에 "Additional UI design recommendations" 섹션을 포함할 것 (차단 조건 아님).
