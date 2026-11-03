plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ruvo.design"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    api(project(":core:engine"))
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui.tooling.preview)
    debugApi(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.core.ktx)
}

// The brand typeface, Anek Latin (SIL Open Font License), is downloaded once on the build machine
// instead of being committed. It includes the ₹ sign and has width and weight axes.
val anekFont = layout.projectDirectory.file("src/main/res/font/anek_latin.ttf").asFile
val fetchFonts by tasks.registering {
    description = "Downloads the Anek Latin variable font into res/font if it is missing."
    outputs.file(anekFont)
    onlyIf { !anekFont.exists() }
    doLast {
        anekFont.parentFile.mkdirs()
        val url = "https://github.com/google/fonts/raw/main/ofl/aneklatin/AnekLatin%5Bwdth,wght%5D.ttf"
        try {
            java.net.URI(url).toURL().openStream().use { input -> anekFont.outputStream().use { input.copyTo(it) } }
        } catch (e: Exception) {
            anekFont.delete()
            throw GradleException(
                "Could not download Anek Latin ($url). Download the variable TTF from " +
                    "https://fonts.google.com/specimen/Anek+Latin and save it as $anekFont",
                e,
            )
        }
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(fetchFonts) }
