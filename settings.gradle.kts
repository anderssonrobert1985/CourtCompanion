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

dependencyResolutionManagement {
    // Not FAIL_ON_PROJECT_REPOS: the Kotlin plugin adds its own repositories to download Node.js and Binaryen for
    // the web build.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CourtCompanion"
include(":shared", ":app", ":desktop", ":cli", ":web")
