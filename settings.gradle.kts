pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "reveil"
include(":reveil-core", ":reveil-youtube", ":reveil-ring", ":app-logic", ":app")
