// Root Gradle configuration shared by all subprojects.

plugins {
    // Declare Kotlin plugins once at the root to avoid loading the Kotlin Gradle
    // plugin multiple times across subprojects.
    // Keep this version aligned with `gradle.properties` (kotlinVersion).
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
}

subprojects {
    repositories {
        mavenCentral()
    }
}
