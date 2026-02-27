// Kaero runtime module (Ktor integration + routing + standardized responses).

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion: String by project
val serializationVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

kotlin {
    jvmToolchain(25)
}

group = "dev.kaero"
version = (project.findProperty("kaeroVersion") as String?) ?: "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(kotlin("reflect"))

    api("io.ktor:ktor-server-core-jvm:$ktorVersion")
    api("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    api("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    api("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    api("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    api("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    api("io.ktor:ktor-server-compression-jvm:$ktorVersion")

    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
