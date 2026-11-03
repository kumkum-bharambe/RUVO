// Plain Kotlin: no Android code here, so the maths runs and tests on any machine.
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
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
