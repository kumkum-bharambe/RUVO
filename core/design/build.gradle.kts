import java.net.URI

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

// The brand typefaces (SIL Open Font License) are downloaded once on the build machine instead of being
// committed. Outfit is the brand face; Anek Latin supplies the ₹ sign, which Outfit lacks.
val brandFonts = mapOf(
    "outfit.ttf" to ("Outfit" to "https://github.com/google/fonts/raw/main/ofl/outfit/Outfit%5Bwght%5D.ttf"),
    "anek_latin.ttf" to ("Anek Latin" to "https://github.com/google/fonts/raw/main/ofl/aneklatin/AnekLatin%5Bwdth,wght%5D.ttf"),
)
val fontDir = layout.projectDirectory.dir("src/main/res/font").asFile
val fetchFonts by tasks.registering {
    description = "Downloads the Outfit and Anek Latin variable fonts into res/font if they are missing."
    outputs.files(brandFonts.keys.map { fontDir.resolve(it) })
    onlyIf { brandFonts.keys.any { !fontDir.resolve(it).exists() } }
    doLast {
        fontDir.mkdirs()
        brandFonts.forEach { (file, info) ->
            val (name, url) = info
            val target = fontDir.resolve(file)
            if (target.exists()) return@forEach
            try {
                URI(url).toURL().openStream().use { input -> target.outputStream().use { input.copyTo(it) } }
            } catch (e: Exception) {
                target.delete()
                throw GradleException(
                    "Could not download $name ($url). Download the variable TTF from Google Fonts and save it as $target",
                    e,
                )
            }
        }
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(fetchFonts) }
