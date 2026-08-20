pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Azmaree SDK modules resolve from Maven Central, same as CI: no mavenLocal, so local
        // builds can never ship bytes that differ from the published version they declare.
        google()
        mavenCentral()
    }
}

rootProject.name = "Ibex"
include(":app")
include(":macrobenchmark")
