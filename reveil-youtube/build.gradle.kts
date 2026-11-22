// YouTube adapter: NewPipeExtractor behind the ports of reveil-core, and YouTubeHealth.
// Pure Kotlin: the host gives its own OkHttpClient (certificates, timeouts) through YouTube.init.
plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kover")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    api(project(":reveil-core"))
    api("com.github.TeamNewPipe:NewPipeExtractor:v0.27.1")
    api("com.squareup.okhttp3:okhttp:5.1.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(testFixtures(project(":reveil-core")))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// `-Psmoke` runs YouTubeSmokeTest against the real YouTube (daily workflow); it is skipped otherwise.
tasks.test {
    useJUnitPlatform()
    systemProperty("smoke", (findProperty("smoke") != null).toString())
}
