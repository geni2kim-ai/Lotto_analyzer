# Feedback — v1.0.8 (review of evidence_v1.0.7)

대상 저장소: `geni2kim-ai/Lotto_analyzer` / 브랜치: `main`
리뷰한 evidence: `evidence/evidence_v1.0.7.md` (commit `6e7a103`, "docs: add v1.0.7 implementation evidence")
리뷰 방식: evidence 주장 ↔ 실제 커밋 diff ↔ main 최신 소스(raw) 직접 대조 + Actions API로 CI 결론 확인

## §0 검증된 통과 항목 (조치 불필요)

1. **P0 B1 — UNAVAILABLE 종료 가드**: raw 소스에서 양쪽 discovery 루프에
   `DrawAvailability.UNAVAILABLE -> return null` 확인 (각각 LottoRemoteDataSourceImpl.kt L202, L213).
2. **P0 B2 — HTTP 비정상 → UNAVAILABLE**: `if (!response.isSuccessful) return DrawAvailability.UNAVAILABLE`
   (L225), body/data/list null → UNAVAILABLE (L227-228) 확인. HTTP 4xx/5xx가 MISSING으로
   오분류되어 미래 경계를 조용히 확정하는 경로가 없음.
3. **회귀 테스트가 의미 있음**: `incrementalDiscoveryIOExceptionFallsBackToFullListWithoutRetryLoop`,
   HTTP 500 변형 모두 `withTimeout(1_000)` 으로 감싸 무한 재시도 시 테스트가
   실패하도록 설계. `exactQueryCalls == 1` 단언은 discovery가 단 한 번의 probe 후
   즉시 종료됨을 증명. Fake의 `httpErrorDrawNos` 경로 정상.
4. **SyncStatusCard 접근성 시맨틱**: production(`mergeDescendants = true`,
   `liveRegion = LiveRegionMode.Polite`, `stateDescription = presentation.announcement`)과
   테스트 단언이 일치.
5. **UI 후속 작업 4건 모두 코드에 존재**: Analysis `재현 정보` disclosure + 복사,
   Database 검색어 trailing clear 아이콘 → `viewModel::clearSearch`,
   Statistics `계산 기준` disclosure, `SyncStatusPresentation` 추출.
6. **CI green 확인**: Android CI #22 (코드 최종 `182a172f`) SUCCESS,
   #23 (`6e7a103`, evidence) SUCCESS — Actions API로 결론 직접 확인.
7. **보간 체커 진단 문구 복구 확인**: b05fe12 시점에는 진단 출력이
   `Use braced interpolation: + identifier + before Korean suffix text.` 로 깨져 있었으나,
   현재 main은 올바른 f-string(`Use braced interpolation: ${drawNo} ...`)으로 복구됨. 조치 완료.

## 조치 항목 (v1.0.8에서 구현)

### F1 (P1) — SyncStatusCardTest의 하드코딩된 한글 카피
테스트가 `"실패한 7개 회차 다시 시도"`, `"실패 목록 보기"` 같은 실제 UI 문구를 직접
단언하고 있음. 카피가 바뀌면 실제 회귀 없이 테스트가 깨지는 brittle 구조.
`resolveSyncStatusPresentation`이 반환하는 announcement/문구를 테스트가 도출해서
단언하거나, 노드에 testTag를 부여해 카피와 단언을 분리할 것.

### F2 (P2) — evidence 문서와 커밋 코드의 불일치
evidence §5는 "test runner fixed to Robolectric API 34"라고 기술했으나 실제 커밋된
`SyncStatusCardTest.kt`는 `@Config(sdk = [35])`. evidence는 커밋된 코드와 일치하게
기술할 것 (문서 drift 방지).

### F3 (P2) — check_kotlin_interpolation.py 블록 주석 휴리스틱
`stripped.startswith("/*")` 판정은 문자열 리터럴 안의 `/*`에도 반응해 상태머신을
깨뜨릴 수 있음. 문자열 리터럴을 인식하도록 개선하거나, 한계로 문서화할 것.

### F4 (P2, UI) — 큰 글꼴 배율에서의 액션 컨테이너
SyncStatusCard의 "다시 시도 / 실패 목록 보기" 버튼 행은 200% 글꼴 배율·좁은 화면에서
찧힘. Row→Column으로 전환되는 반응형 액션 컨테이너 primitive 도입을 제안.
(스크린샷/시맨틱 회귀 테스트는 커버리지 확대 후.)

### F5 (P2, UI, backlog) — History 월별 그룹/필터
히스토리가 길어지면 월 그룹·sticky 헤더·알고리즘 버전/EV 사용 필터 추가.
(현재 규모에서는 불필요 — 백로그로 유지)

### F6 (P2, UI, backlog) — 재현 복사 페이로드 확장
`재현 정보 복사`에 recent-N, 데이터 시작/끝 회차, 정규화 가중치 설정을 포함해
실제 디버그용 replay token으로 발전.

## 완료 조건

1. F1~F4 구현 (F5/F6는 backlog로 남겨도 됨).
2. 기존 테스트 전부 green 유지, Android CI green.
3. `evidence/evidence_v1.0.8.md` 생성. evidence에는 F1~F4의 코드 위치·
   테스트 단언·CI run 번호/결론을 lineage와 함께 기록할 것.
4. **UI 디자인 관점에서 개선할 부분이 있으면 제안해줘** — evidence에
   "Additional UI design recommendations" 섹션을 포함할 것 (차단 조건 아님).

## UI 관찰 (이번 리뷰)

- `재현 정보` disclosure는 기본 접힘 + 복사 버튼 제공으로 이전의 장문 상시 노출
  문제를 잘 해결. 다만 복사된 `algorithm=<version>\nseed=<value>` 만으로는
  타임스탬프·앱 버전이 빠져 디버그 재현 시 맥락이 부족 — F6에서 다룸.
- Statistics `계산 기준` disclosure는 표본 정책(1등 null/비양수 제외)을 명시해
  신뢰도를 높임. `유효 표본 N회` 수치는 disclosure와 별개로 항상 보이게 유지할 것
  (현재 유지됨 — 확인).
- SyncStatusCard: 상태 요약은 live region으로 합쳐지고 액션은 네이티브 버튼으로
  남긴 구조가 TalkBack 관점에서 올바름. F4의 반응형 컨테이너가 다음 단계.
