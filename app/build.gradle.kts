import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ruvo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ruvo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The release signing key lives outside the repo, in ~/.ruvo/keystore.properties (never committed).
    // On a machine without it, release builds fall back to the debug key so they still build.
    val keyProps = Properties().apply {
        val f = File(System.getProperty("user.home"), ".ruvo/keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keyProps.getProperty("storeFile") != null) create("ruvo") {
            storeFile = File(keyProps.getProperty("storeFile"))
            storePassword = keyProps.getProperty("storePassword")
            keyAlias = keyProps.getProperty("keyAlias")
            keyPassword = keyProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("ruvo") ?: signingConfigs.getByName("debug")
            // Real phones only (ARM). The x86 libraries are for computer emulators and double the download size.
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }
        // The shareable build for testers (GitHub Releases): the release build without SMS reading, because
        // phones can block sideloaded apps that ask for SMS permissions. Its manifest lives in src/share/.
        create("share") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            // Its own app ID, so it installs next to the full app instead of replacing it (and its data).
            applicationIdSuffix = ".share"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:data"))
    implementation(project(":feature:today"))
    implementation(project(":feature:money"))
    implementation(project(":feature:goals"))
    implementation(project(":feature:guide"))
    implementation(project(":feature:orb"))
    implementation(project(":feature:onboarding"))
    implementation(project(":widget"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Safety check: a shared APK must never carry a private AI key, because anyone can read it out of the APK.
gradle.taskGraph.whenReady {
    val shipping = allTasks.any { t -> listOf("assemble", "bundle", "install", "package").any { t.name.startsWith(it) } && (t.name.contains("Release") || t.name.contains("Share")) }
    val key = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }.getProperty("RUVO_CLAUDE_API_KEY", "")
    if (shipping && key.isNotBlank()) throw GradleException(
        "RUVO_CLAUDE_API_KEY is set in local.properties. Empty it before building a release or share APK, " +
            "or the key would be inside the app for anyone to copy."
    )
}
