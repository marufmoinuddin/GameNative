plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.21"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    testImplementation(kotlin("test"))
}

tasks.register<JavaExec>("runRuntimePrototype") {
    group = "application"
    description = "Runs the runtime orchestration prototype demo and writes session artifacts."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.gamenative.linux.runtime.demo.RuntimePrototypeMainKt")
}

tasks.register<JavaExec>("runRuntimeDiagnostics") {
    group = "application"
    description = "Runs runtime diagnostics CLI and writes JSON snapshot artifact."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.gamenative.linux.runtime.demo.RuntimeDiagnosticsMainKt")
}

tasks.register<JavaExec>("runRuntimeStartupDecision") {
    group = "application"
    description = "Runs startup decision resolver and writes a recommendation report."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.gamenative.linux.runtime.demo.RuntimeStartupDecisionMainKt")
}

tasks.register<JavaExec>("runRuntimeProof") {
    group = "verification"
    description = "Runs strict Wine+Box64 runtime proof checks and fails if prerequisites or smoke launch are not satisfied."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.gamenative.linux.runtime.demo.RuntimeProofMainKt")
}

tasks.test {
    useJUnitPlatform()
}
