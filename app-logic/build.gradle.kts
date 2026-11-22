// Display logic of the reveil-playlist phone app (turntable, morning light, weather, updates): pure Kotlin,
// kept out of reveil-core so the reusable modules carry no screen of this app.
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.kover")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    api(project(":reveil-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(testFixtures(project(":reveil-core")))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }

kover.reports.verify.rule {
    minBound(95, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE)
    minBound(95, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
}
