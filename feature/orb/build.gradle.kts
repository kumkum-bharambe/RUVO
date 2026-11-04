import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Optional: live AI answers. Put RUVO_CLAUDE_API_KEY=... in local.properties (never committed).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val claudeKey: String = localProps.getProperty("RUVO_CLAUDE_API_KEY", "")

android {
    namespace = "com.ruvo.feature.orb"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "RUVO_CLAUDE_API_KEY", "\"$claudeKey\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.mlkit.translate)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
