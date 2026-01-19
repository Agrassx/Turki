import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        val commonMain by getting {
            dependencies {
                implementation(projects.core)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.bundles.kotlin.common)
                implementation(libs.bundles.ktor.client)
                implementation(libs.ktor.serialization.json)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
            }
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.core)
        }

    }
}

compose.desktop {
    application {
        mainClass = "com.turki.admin.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TurkiAdmin"
            packageVersion = "1.0.0"
        }
    }
}
