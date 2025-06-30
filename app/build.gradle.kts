plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

val applicationId: String by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra
val androidCompileSdk: Int by rootProject.extra
val androidMinSdk: Int by rootProject.extra
val appVersionName: String by rootProject.extra
val appVersionCode: Int by rootProject.extra

android {
    compileSdk = androidCompileSdk
    namespace = applicationId
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    defaultConfig {
        applicationId = applicationId
        minSdk = androidMinSdk
        targetSdk = androidCompileSdk
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        base.archivesName.set("CIFSDocumentsProvider-${versionName}")
    }

    buildTypes {
        debug {
            versionNameSuffix = "D"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    signingConfigs  {
        getByName("debug") {
            storeFile = file("$rootDir/debug.keystore")
        }
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
        }
    }

    packaging {
        // For commons-vfs2-jackrabbit2
        resources.excludes.addAll(setOf(
            "META-INF/*",
            "META-INF/versions/*/OSGI-INF/MANIFEST.MF",
        ))
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))
    implementation(project(":common"))
    implementation(project(":presentation"))
    implementation(project(":domain"))

    implementation(libs.androidx.appcompat)
    implementation(libs.hilt.android)
    implementation(libs.androidx.work.runtime)
    ksp(libs.hilt.android.compiler)
    // CIFS INTEGRATION
    implementation("com.hierynomus:smbj:0.11.5")
    implementation("eu.agno3.jcifs:jcifs-ng:2.1.10")
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("androidx.room:room-ktx:2.5.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
}
