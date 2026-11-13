# SmartResult NG

A production-grade Android application for Primary, Secondary, and Tertiary academic results management, custom grading scales, GPA weighting, CSV data onboarding, PDF report generation, and optional Firebase Firestore synchronization.

## Core Features

- **Student & Class Management**: Register students, manage school classes across Primary, JSS, SS, and Tertiary tiers, and organize subject catalogs with credit unit weights.
- **Assessment Score Entry & Ledger**: Record CA and Exam scores with real-time total computation, dynamic grade evaluation, and tabular class ledgers.
- **Custom Grading Scales & GPA Weighting**: Configure custom score ranges (e.g., WAEC/NECO 9-point `A1–F9`, Standard `A–F`, Primary 4-Tier Competency, US/Cambridge `4.0`), GPA point weights, unweighted or credit-unit weighted GPA calculation, and live score simulation.
- **Visual Report Template Designer & PDF Export**: Customize school branding, logos, layout density, and generate individual or bulk school-wide PDF report cards and CSV exports via Android's `FileProvider`.
- **CSV Import & Column Mapping Wizard**: Import student rosters, subject catalogs, and assessment scores from spreadsheets with automatic header matching and row-level validation.
- **Performance Analytics & Cloud Sync Hub**: Visualize class and subject performance metrics and optionally synchronize data with Firebase Firestore.

## Tech Stack

- **Language**: Kotlin (`2.2.x`)
- **UI**: Jetpack Compose + Material Design 3 (Adaptive tablet sidebar & mobile portrait layout, edge-to-edge, hardware back navigation)
- **Persistence**: Android Room (`SQLite`) with KSP
- **Cloud Sync**: Firebase Firestore (`firebase-firestore`)
- **Testing**: JUnit 4, Robolectric, Roborazzi
- **Build System**: Gradle (`9.3.1`), Android Gradle Plugin (`9.1.1`), `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`

## Project Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt              # Edge-to-edge activity host
├── data/
│   ├── AppDatabase.kt           # Room database (v6)
│   ├── Daos.kt                  # Student, Score, Subject, Class, Settings & GradingScale DAOs
│   ├── Models.kt                # Domain entities, grading bands & GPA evaluation logic
│   ├── ResultRepository.kt      # Data repository & initial seed pipeline
│   └── ResultViewModel.kt       # MVVM state flows, PDF/CSV generation & navigation stack
└── ui/
    ├── SmartResultApp.kt        # Main Compose screens, report renderer & CSV import wizard
    ├── GradingScaleScreen.kt    # Custom grading scale & GPA configuration UI
    └── theme/                   # Material 3 color palette, typography & theme
```

## Security & Environment Configuration

1. Copy `.env.example` to `.env` locally (never commit `.env` or real API keys).
2. Firebase Cloud Sync credentials can be configured dynamically inside the in-app **Cloud Sync Hub** or provided securely at build time.
3. Release signing relies on environment variables or CI secrets (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) — never commit `.jks` or `.keystore` files to version control.

## Build & Test Instructions

### Prerequisites

- **JDK 21**
- **Android SDK** (Platform 36, Build-Tools 36)
- **Gradle 9.3.1** (or Android Studio)

### Run Unit Tests

```bash
gradle :app:testDebugUnitTest
```

### Build Debug APK

```bash
gradle :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Signed Release APK

Provide your release upload keystore and credentials via environment variables:

```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password

gradle :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

## CI/CD (GitHub Actions)

The included `.github/workflows/android-build.yml` workflow automatically runs unit tests, builds APKs, uploads workflow artifacts, and publishes releases on `v*` tags when configured with the repository secrets:

- `SIGNING_KEYSTORE_BASE64` (Base64-encoded release `.jks` file)
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- `DEBUG_KEYSTORE_BASE64` (optional, if overriding debug signing)
