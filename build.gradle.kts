plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("android") version "2.2.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    kotlin("plugin.compose") version "2.2.21" apply false
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.3" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("app.cash.paparazzi") version "2.0.0-alpha02" apply false
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.21")
        }
    }
}

// Same coordinates whether built here or pulled into another build with includeBuild (Bonjour).
allprojects { group = "fr.clrk.reveil" }

// The gate every PR and every commit must pass: linters, scenarios, coverage. `./gradlew gate`
tasks.register("gate") {
    dependsOn(
        subprojects.map { "${it.path}:detekt" },
        ":reveil-core:test", ":reveil-core:koverVerify",
        ":app-logic:test", ":app-logic:koverVerify",
        ":reveil-youtube:test",
        ":reveil-ring:lintDebug", ":app:lintDebug",
    )
}
