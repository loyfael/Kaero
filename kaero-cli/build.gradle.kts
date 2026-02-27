// Kaero CLI module (cross-platform JVM app).
// Provides `kaero init` to scaffold a new project from anywhere.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
}

kotlin {
    // Use Java 25 for building/running the CLI itself.
    jvmToolchain(25)
}

group = "dev.kaero"
version = (project.findProperty("kaeroVersion") as String?) ?: "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    // Keep bytecode compatible with JVM 17.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("dev.kaero.cli.MainKt")
    applicationName = "kaero"
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
