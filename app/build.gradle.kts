import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.screenshot)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
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

        testInstrumentationRunner = "com.jonecx.ibex.HiltTestRunner"

        buildConfigField("String", "POSTHOG_API_KEY", "\"${localProperties.getProperty("POSTHOG_API_KEY", localProperties.getProperty("posthog.apiKey", ""))}\"")
        buildConfigField("String", "POSTHOG_HOST", "\"${localProperties.getProperty("POSTHOG_HOST", localProperties.getProperty("posthog.host", "https://us.i.posthog.com"))}\"")
        buildConfigField("boolean", "SKIP_PERMISSION_CHECK", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

ksp {
    arg("correctErrorTypes", "true")
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
    
    // Coil for image/video thumbnails
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    
    // Material Icons Extended
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // DataStore
    implementation(libs.datastore.preferences)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Tink (encryption for credentials)
    implementation(libs.tink.android)

    // SMB client
    implementation(libs.jcifs.ng)
    testFixturesImplementation(libs.jcifs.ng)

    // Media3 (ExoPlayer + Compose UI)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui.compose)
    implementation(libs.media3.ui.compose.material3)

    // Baseline Profile
    implementation(libs.profileinstaller)
    baselineProfile(project(":macrobenchmark"))

    // Logging & Analytics
    implementation(libs.timber)
    implementation(libs.posthog)
    
    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesImplementation(libs.androidx.compose.ui)
    testFixturesImplementation(libs.coil.compose)
    testFixturesImplementation(libs.media3.exoplayer)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
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
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    screenshotTestImplementation(testFixtures(project(":app")))
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}