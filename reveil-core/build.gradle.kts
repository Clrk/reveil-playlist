// Alarms, recurrence, planning, snooze, playlist, wake queue, prefetch rules, source health.
// Pure Kotlin: no Android, no NewPipe, so any host (phone app, Bonjour) can reuse it.
plugins {
    kotlin("jvm")
    `java-test-fixtures`
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.kover")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testFixturesApi(platform("org.junit:junit-bom:5.13.4"))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }

kover.reports.verify.rule {
    minBound(95, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE)
    minBound(95, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
}

// The scenario runner is shared with the other modules' tests as test fixtures: it is not production code.
kover.reports.filters.excludes.packages("fr.clrk.reveil.core.bdd")
