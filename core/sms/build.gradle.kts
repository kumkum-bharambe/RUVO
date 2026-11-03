// Plain Kotlin: the parser, merchant dictionary and duplicate merging. The Android SMS receiver lives in core/data.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:engine"))
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
