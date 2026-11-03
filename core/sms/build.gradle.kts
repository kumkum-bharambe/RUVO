// Plain Kotlin: the parser, merchant dictionary and duplicate merging. The Android SMS receiver lives in core/data.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    // Target Java 17 bytecode with whatever JDK Android Studio ships; no separate JDK 17 install needed.
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core:engine"))
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
