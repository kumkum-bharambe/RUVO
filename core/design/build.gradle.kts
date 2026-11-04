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
// committed. Fraunces sets numbers and headlines; Space Grotesk sets the interface. Both include the rupee sign.
val brandFonts = mapOf(
    "fraunces.ttf" to ("Fraunces" to "https://raw.githubusercontent.com/google/fonts/main/ofl/fraunces/Fraunces%5BSOFT,WONK,opsz,wght%5D.ttf"),
    "space_grotesk.ttf" to ("Space Grotesk" to "https://raw.githubusercontent.com/google/fonts/main/ofl/spacegrotesk/SpaceGrotesk%5Bwght%5D.ttf"),
)
val fontDir = layout.projectDirectory.dir("src/main/res/font").asFile
val fetchFonts by tasks.registering {
    description = "Downloads the Fraunces and Space Grotesk variable fonts into res/font if they are missing."
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
