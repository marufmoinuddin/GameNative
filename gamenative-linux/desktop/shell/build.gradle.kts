plugins {
    kotlin("jvm")
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
    implementation(project(":core:domain"))
    implementation(project(":core:runtime"))
    implementation(project(":core:store-steam"))
    implementation("io.github.joshuatam:javasteam:1.8.0.1-13-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    testImplementation(kotlin("test"))
}

tasks.register<JavaExec>("runDesktopShell") {
    group = "application"
    description = "Runs the Linux desktop shell prototype UI."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.gamenative.linux.desktop.shell.DesktopShellMainKt")
}

tasks.test {
    useJUnitPlatform()
}
