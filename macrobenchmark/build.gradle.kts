plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.jonecx.ibex.macrobenchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}

afterEvaluate {
    tasks.register("grantBenchmarkStoragePermission") {
        dependsOn(":app:installBenchmark")
        doLast {
            val sdkDir = rootProject.file("local.properties")
                .readLines()
                .firstOrNull { it.startsWith("sdk.dir=") }
                ?.substringAfter("=")
                ?: System.getenv("ANDROID_HOME")
                ?: error("Cannot find Android SDK")
            val adb = File(sdkDir, "platform-tools/adb")

            ProcessBuilder(
                adb.absolutePath, "shell", "appops", "set",
                "com.jonecx.ibex", "MANAGE_EXTERNAL_STORAGE", "allow",
            ).inheritIO().start().waitFor()
            logger.lifecycle("Granted MANAGE_EXTERNAL_STORAGE to com.jonecx.ibex")
        }
    }

    tasks.named("connectedBenchmarkAndroidTest") {
        dependsOn("grantBenchmarkStoragePermission")
    }

    tasks.register("benchmarkCheck") {
        dependsOn("connectedBenchmarkAndroidTest")
        doLast {
            val benchDir = rootProject.file("benchmarks")
            fun runScript(vararg args: String) {
                ProcessBuilder(*args)
                    .directory(rootProject.projectDir)
                    .inheritIO().start().waitFor()
            }

            logger.lifecycle("Collecting benchmark results...")
            runScript("bash", File(benchDir, "benchmark_result_collect.sh").absolutePath)

            val resultDirs = File(benchDir, "results")
                .listFiles { f -> f.isDirectory && f.name != ".gitkeep" }
                ?.sortedBy { it.name } ?: emptyList()

            if (resultDirs.size >= 2) {
                logger.lifecycle("Comparing against previous run...")
                runScript("python3", File(benchDir, "benchmark_result_compare.py").absolutePath, "--latest")
            }

            logger.lifecycle("Generating benchmark report...")
            runScript("python3", File(benchDir, "benchmark_result_chart.py").absolutePath)
        }
    }
}
