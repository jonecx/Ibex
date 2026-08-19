import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.screenshot)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.sentry.gradle)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    namespace = "com.jonecx.ibex"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.jonecx.ibex"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.jonecx.ibex.IbexTestRunner"
        // Resets the process-global Koin fakes before each instrumented test.
        testInstrumentationRunnerArguments["listener"] = "com.jonecx.ibex.FakeResetRunListener"

        buildConfigField("String", "POSTHOG_API_KEY", "\"${localProperties.getProperty("POSTHOG_API_KEY", localProperties.getProperty("posthog.apiKey", ""))}\"")
        buildConfigField("String", "POSTHOG_HOST", "\"${localProperties.getProperty("POSTHOG_HOST", localProperties.getProperty("posthog.host", "https://us.i.posthog.com"))}\"")
        // Same backends/accounts as Azmaree: copy these keys from Azmaree's local.properties.
        buildConfigField("String", "AXIOM_API_KEY", "\"${localProperties.getProperty("AXIOM_API_KEY") ?: ""}\"")
        buildConfigField("String", "AXIOM_DATASET", "\"${localProperties.getProperty("AXIOM_DATASET") ?: "azmaree-qoe"}\"")
        buildConfigField("String", "AXIOM_HOST", "\"${localProperties.getProperty("AXIOM_HOST") ?: "https://api.axiom.co"}\"")
        buildConfigField("String", "SENTRY_DSN", "\"${localProperties.getProperty("SENTRY_DSN") ?: ""}\"")
        buildConfigField("boolean", "SKIP_PERMISSION_CHECK", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        listOf("benchmark", "nonMinifiedRelease").forEach { name ->
            create(name) {
                initWith(buildTypes.getByName("release"))
                signingConfig = signingConfigs.getByName("debug")
                matchingFallbacks += listOf("release")
                isDebuggable = false
                // Keep the perf/baseline pipeline on un-minified code so its numbers stay comparable.
                // Turning R8 on for benchmark is a deliberate follow-up, not a side effect of this change.
                isMinifyEnabled = false
                isShrinkResources = false
                buildConfigField("boolean", "SKIP_PERMISSION_CHECK", "true")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testFixtures {
        enable = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

composeCompiler {
    stabilityConfigurationFile = project.layout.projectDirectory.file("compose-stability.conf")
}

// Uploads R8/ProGuard mappings + source context on release builds so Sentry stack traces
// stay readable once minification is enabled. No-ops without SENTRY_AUTH_TOKEN (e.g. CI).
val sentryAuthToken = localProperties.getProperty("SENTRY_AUTH_TOKEN") ?: System.getenv("SENTRY_AUTH_TOKEN") ?: ""
sentry {
    org.set(localProperties.getProperty("SENTRY_ORG") ?: System.getenv("SENTRY_ORG") ?: "")
    projectName.set(localProperties.getProperty("SENTRY_PROJECT") ?: System.getenv("SENTRY_PROJECT") ?: "")
    authToken.set(sentryAuthToken)
    // Auto-uploads need auth; gate them so tokenless CI release builds do not fail.
    includeSourceContext.set(sentryAuthToken.isNotEmpty())
    autoUploadProguardMapping.set(sentryAuthToken.isNotEmpty())
    autoUploadSourceContext.set(sentryAuthToken.isNotEmpty())
}

baselineProfile {
    dexLayoutOptimization = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Material 3 Adaptive
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Coil for image/video/gif thumbnails
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)
    
    // Material Icons Extended
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Tink (encryption for credentials)
    implementation(libs.tink.android)

    // SMB client
    implementation(libs.jcifs.ng)
    testFixturesImplementation(libs.jcifs.ng)

    // WorkManager: durable, resumable foreground transfers that survive process death and reboot
    implementation(libs.androidx.work.runtime)

    // Azmaree video player SDK + its SMB byte source (brings the player core + jcifs-ng transitively)
    implementation(libs.azmaree.player)
    implementation(libs.azmaree.source.smb)
    implementation(libs.azmaree.image)

    // Baseline Profile
    implementation(libs.profileinstaller)
    baselineProfile(project(":macrobenchmark"))

    // Logging, analytics, metrics, crash reporting (vendor-agnostic behind adapters)
    implementation(libs.timber)
    implementation(libs.posthog)
    implementation(libs.sentry.android)
    
    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesImplementation(libs.androidx.compose.ui)
    testFixturesImplementation(libs.coil.compose)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.azmaree.player)
    testImplementation(testFixtures(project(":app")))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    androidTestImplementation(testFixtures(project(":app")))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.koin.bom))
    androidTestImplementation(libs.koin.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    screenshotTestImplementation(testFixtures(project(":app")))
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}