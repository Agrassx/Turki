import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        withJava()
    }
    
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "admin-web.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jvmMain by getting
        val jsMain by getting

        val commonMain by getting {
            dependencies {
                implementation(libs.bundles.kotlin.common)
                implementation(libs.koin.core)
            }
        }

        jvmMain.dependencies {
            implementation(projects.core)
            implementation(compose.desktop.currentOs)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.bundles.ktor.server)
            implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.get()}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")
            implementation(libs.bundles.ktor.client)
            implementation(libs.ktor.serialization.json)
            implementation(libs.koin.compose)
            implementation(libs.koin.ktor)
            implementation(libs.logback.classic)
            implementation(libs.kotlinx.coroutines.core)
        }
        
        jsMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.web.core)
            implementation(compose.web.svg)
            implementation(libs.koin.compose)
            implementation(libs.ktor.client.core)
            implementation("io.ktor:ktor-client-js:${libs.versions.ktor.get()}")
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)
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

tasks.register<Jar>("adminWebJar") {
    dependsOn(":core:jar", "jsBrowserProductionWebpack", "jvmMainClasses")
    archiveBaseName.set("admin-web")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "com.turki.admin.webserver.AdminServerKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.getByName("jvmRuntimeClasspath").map { if (it.isDirectory) it else zipTree(it) })
    from("${layout.buildDirectory.get()}/dist/js/productionExecutable") {
        into("static")
    }
    from("${layout.buildDirectory.get()}/classes/kotlin/jvm/main")
}

tasks.named("jsBrowserProductionWebpack") {
    doLast {
        copy {
            from("${layout.buildDirectory.get()}/dist/js/productionExecutable")
            into("${layout.buildDirectory.get()}/resources/main/static")
        }
    }
}
