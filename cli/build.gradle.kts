import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    application
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
    // Skia, which draws the group images; no window is opened.
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}

application {
    mainClass = "com.robert.courtcompanion.cli.CliKt"
    applicationName = "courtcompanion-cli"
    applicationDefaultJvmArgs = listOf("-Djava.awt.headless=true")
}
