import org.gradle.kotlin.dsl.extra

// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Global Definition - Updated for Samsung My Files Root Extension
val applicationId by extra ("com.samsung.android.app.networkstoragemanager")
val javaVersion by extra (JavaVersion.VERSION_21)
val androidCompileSdk by extra (35)
val androidMinSdk by extra (26)
val appVersionName by extra ("2.0.0")
val appVersionCode by extra (2000000001)

plugins {
    id("com.android.application") version "8.7.2" apply false
    id("com.android.library") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("androidx.room") version "2.6.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

task("clean", Delete::class) {
    delete(rootProject.buildDir)
}