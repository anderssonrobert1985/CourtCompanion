import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":shared"))
    // With arguments, the launcher runs the command line tool instead of the window (e.g. "courtcompanion mcp").
    implementation(project(":cli"))
    implementation(compose.desktop.currentOs)
    // Dispatchers.Main for the view models (Android has its own built in).
    implementation(libs.kotlinx.coroutines.swing)

    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.robert.courtcompanion.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "courtcompanion"
            packageVersion = "0.1.0"
            description = "Divide basketball practice players into groups"
            linux {
                shortcut = true
                menuGroup = "Sports"
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}
