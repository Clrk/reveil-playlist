// Android library that rings: RingService, RingMusic, receivers, VolumeFader, evening download, track cache.
// The host app plugs in its sources and screens through RingHost.
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "fr.clrk.reveil.ring"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    lint {
        // Checked as the phone app targets it; a host app checks its own target.
        targetSdk = 36
        warningsAsErrors = true
        // Checks whose verdict depends on what was published since, not on this code: they would fail an untouched branch.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "InlinedApi", "OldTargetApi", "AndroidGradlePluginVersion")
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    api(project(":reveil-core"))
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    implementation("androidx.core:core-ktx:1.17.0")
    api("androidx.media3:media3-exoplayer:1.8.0")
    api("androidx.work:work-runtime-ktx:2.10.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    api("com.squareup.okhttp3:okhttp:5.1.0")
}
