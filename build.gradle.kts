// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    id("com.diffplug.spotless") version "6.21.0" apply false
}

apply(from = "spotless.gradle")
// ./gradlew updateDebugScreenshotTest
tasks.register("sanityCheck") {
    description = "Runs Spotless, unit tests, screenshot tests, then instrumentation tests"
    group = "verification"
    dependsOn("spotlessApply")
    dependsOn(":app:test")
    dependsOn(":app:connectedDebugAndroidTest")
}

gradle.projectsEvaluated {
    val appTasks = project(":app").tasks
    appTasks.named("test") { mustRunAfter(rootProject.tasks.named("spotlessApply")) }

    appTasks.findByName("validateDebugScreenshotTest")?.let {
        rootProject.tasks.named("sanityCheck") { dependsOn(it) }
        it.mustRunAfter(appTasks.named("test"))
        appTasks.named("connectedDebugAndroidTest") { mustRunAfter(it) }
    } ?: run {
        appTasks.named("connectedDebugAndroidTest") { mustRunAfter(appTasks.named("test")) }
    }
}