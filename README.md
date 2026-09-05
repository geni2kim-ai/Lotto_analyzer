# Lotto Insight (AI Maestro)

Lotto Insight is a modern Android application designed for lottery number analysis, statistical modeling, and customized recommendations.

## Overview
- **Application ID**: `com.aimaestro.lottoanalyzer`
- **Platform**: Android (Kotlin, Jetpack Compose, Android Architecture Components)
- **Architecture**: Multi-module Clean Architecture (`app`, `core`, `feature`)
- **Database**: Local Room Database (User preferences, historical statistics, generated combinations)

## Prerequisites
- Android Studio Ladybug / Iguana or newer
- JDK 17 or higher
- Android SDK (API 34+ target)

## Project Setup
1. **Clone the repository**:
   ```bash
   git clone https://github.com/geni2kim-ai/LottoInsightAndroid.git
   cd LottoInsightAndroid
   ```
2. **Local Properties Configuration**:
   - Copy `local.properties.example` to `local.properties`
   - Set your local `sdk.dir` path and keystore signing configuration (for release builds).
3. **Firebase / Google Services Setup**:
   - Copy `app/google-services.json.example` to `app/google-services.json`
   - Configure with your authorized Firebase project credentials.

## Build and Run
- **Debug Build**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Run Unit Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Install on Connected Device**:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

## Security & Hygiene
- Signing keystores (`*.jks`, `*.keystore`), local credentials (`local.properties`), and private service files (`google-services.json`) are strictly excluded from version control via `.gitignore`.
- See cluster knowledge document `WIKI-KNW-021` for git root registration and patch workflows.

## License
Copyright © 2026 Genie Kim / AI Maestro. All rights reserved.
