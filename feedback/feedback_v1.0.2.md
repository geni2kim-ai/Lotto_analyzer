# Feedback — v1.0.2 (lotto_analyzer)

대상: geni2kim-ai/lotto_analyzer (public), Lotto Insight Android 앱 (Kotlin + Jetpack Compose, 멀티모듈).
최신 소스: "feat(lotto): upgrade targetSdk to 36 and bump version to 1.0.2 (code 3)" (2026-09-06).

## F1. CI 파이프라인 추가
- .github/workflows 가 없고 Actions 실행 기록이 0건임.
- `.github/workflows/android-ci.yml` 을 추가: JDK 17 설정, `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` 실행.
- main 푸시/PR에 동작해야 함.

## F2. README 저장소명 불일치 수정
- README의 git clone URL이 "LottoInsightAndroid.git"로 되어 있으나 실제 저장소명은 lotto_analyzer임.
- 실제 클론 URL(https://github.com/geni2kim-ai/lotto_analyzer.git)로 수정.

## F3. 테스트 현황 확인 및 보완
- README는 testDebugUnitTest를 안내하지만 테스트 존재·통과 상태가 미확인임.
- 단위 테스트 파일 존재 여부를 확인하고, 없거나 부족하면 핵심 로직(번호 생성·통계)에 대한 단위 테스트를 추가.
- CI에서 테스트가 실제로 통과해야 함.

## F4. 민감 파일 제외 검증
- README는 google-services.json·키스토어가 .gitignore로 제외된다고 주장함.
- .gitignore에 해당 항목이 실제로 있는지 대조하고, 누락 시 추가.
- 이미 커밋된 민감 파일이 있는지 git 이력에서 확인.

## F5. 저장소 메타데이터 (owner 작업)
- 저장소 설명·토픽·웹사이트가 비어 있음. 이는 GitHub 설정에서 소유자가 직접 입력해야 함.
- ChatGPT는 설정 변경이 불가하므로, 권장 설명문/토픽 목록을 evidence에 제안 형태로 정리.

## 완료 조건
위 항목(F1~F4)을 수행하고 `evidence/evidence_v1.0.2.md` 파일을 만들어 완료 내용을 정리한다.
시작하기 전에 이 feedback 파일을 읽었다는 것을 먼저 확인한다.
