// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    id("com.diffplug.spotless") version "6.21.0" apply false
}

apply(from = "spotless.gradle")

tasks.register("sanityCheck") {
    description = "Runs Spotless, unit tests, screenshot tests, then instrumentation tests"
    group = "verification"
    dependsOn("spotlessApply")
    dependsOn(":app:test")
    dependsOn(":app:validateDebugScreenshotTest")
    dependsOn(":app:connectedDebugAndroidTest")
}

gradle.projectsEvaluated {
    project(":app").tasks.named("test") { mustRunAfter(rootProject.tasks.named("spotlessApply")) }
    project(":app").tasks.named("validateDebugScreenshotTest") { mustRunAfter(project(":app").tasks.named("test")) }
    project(":app").tasks.named("connectedDebugAndroidTest") { mustRunAfter(project(":app").tasks.named("validateDebugScreenshotTest")) }
}