# Building

## Prerequisites

- **JDK 21** (Temurin recommended)
- **Android SDK** with API 36 installed
- **Android Studio** Meerkat or later (for Compose preview support)

## Local Properties

Create a `local.properties` file in the project root with your Android SDK path and optional analytics keys:

```properties
sdk.dir=/path/to/Android/sdk

# Optional — PostHog analytics (leave empty to disable)
POSTHOG_API_KEY=
POSTHOG_HOST=https://us.i.posthog.com
```

## Build Variants

| Variant | Description |
|---|---|
| `debug` | Development build with full logging |
| `release` | Minification disabled (ProGuard rules present but `isMinifyEnabled = false`) |
| `benchmark` | Release-based, non-debuggable, skips permission check for macrobenchmark |
| `nonMinifiedRelease` | Same as benchmark, used for baseline profile generation |

## Gradle Tasks

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Full verification (spotless + unit tests + screenshot tests + instrumented tests)
./gradlew sanityCheck

# Code formatting
./gradlew spotlessApply
```

## Dependencies

Dependencies are managed via a Gradle version catalog (`gradle/libs.versions.toml`).

### Core

| Dependency | Version | Purpose |
|---|---|---|
| AGP | 9.0.1 | Android Gradle Plugin |
| Kotlin | 2.2.10 | Language + Compose compiler |
| Compose BOM | 2024.09.00 | Compose UI toolkit |
| Material 3 Adaptive | 1.1.0 | Adaptive layouts |
| Navigation Compose | 2.8.4 | Screen navigation |
| Lifecycle | 2.10.0 | ViewModel + lifecycle-aware components |
| Hilt | 2.59.2 | Dependency injection |
| KSP | 2.2.10-2.0.2 | Kotlin Symbol Processing (for Hilt) |

### Media & Networking

| Dependency | Version | Purpose |
|---|---|---|
| Media3 | 1.9.2 | ExoPlayer + Compose UI |
| Coil | 2.6.0 | Image loading (compose, gif, video) |
| jcifs-ng | 2.1.10 | SMB2/3 client |

### Storage & Security

| Dependency | Version | Purpose |
|---|---|---|
| DataStore | 1.1.1 | Preferences persistence |
| Tink | 1.20.0 | AES-GCM encryption for credentials |
| Kotlinx Serialization | 1.7.3 | JSON serialization for network connections |

### Observability

| Dependency | Version | Purpose |
|---|---|---|
| Timber | 5.0.1 | Logging |
| PostHog | 3.3.2 | Product analytics |

### Testing

| Dependency | Version | Purpose |
|---|---|---|
| JUnit | 4.13.2 | Unit test framework |
| Robolectric | 4.12.2 | Android framework mocking for JVM tests |
| Turbine | 1.1.0 | Flow testing |
| Coroutines Test | 1.8.1 | `TestDispatcher` + `runTest` |
| Espresso | 3.7.0 | UI test assertions |
| Compose UI Test | (BOM) | Compose test rules |
| Hilt Testing | 2.59.2 | `@HiltAndroidTest` support |

### Build Quality

| Dependency | Version | Purpose |
|---|---|---|
| Spotless | 6.21.0 | Code formatting (ktlint) |
| Baseline Profile | 1.5.0-alpha03 | Startup/scroll performance |
| Screenshot Testing | 0.0.1-alpha13 | Compose screenshot validation |

## Compose Stability

A `compose-stability.conf` file is used to configure the Compose compiler's stability inference for better recomposition performance.
