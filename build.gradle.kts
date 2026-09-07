import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

group = "app.wicked"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material.kolor)
}

compose.desktop {
    application {
        mainClass = "app.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe
            )
            packageName = "WickedApp"
            packageVersion = "1.0.0"
            description = "WickedApp"
            vendor = "3plz"
        }
    }
}
