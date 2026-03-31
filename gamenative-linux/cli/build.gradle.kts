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
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("com.google.protobuf:protobuf-java:4.29.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
    testImplementation(kotlin("test"))
}

val copyCliRuntimeClasspath by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copies CLI runtime classpath jars for RPM launcher packaging."
    dependsOn(tasks.named("jar"))
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("packaging/runtime-libs"))
}

tasks.register<JavaExec>("runCli") {
    group = "application"
    description = "Runs the GameNative interactive CLI."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.gamenative.linux.cli.CliMainKt")
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform()
}
