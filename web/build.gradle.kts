plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "courtcompanion"
        browser {
            commonWebpackConfig { outputFileName = "courtcompanion.js" }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.ui)
        }
    }
}
