plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.turki.admin.web.AdminServerKt")
}

dependencies {
    implementation(projects.core)
    implementation(libs.bundles.kotlin.common)
    implementation(libs.bundles.ktor.server)
    implementation("io.ktor:ktor-server-html-builder:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)
}

tasks.jar {
    dependsOn(":core:jar")
    manifest {
        attributes["Main-Class"] = "com.turki.admin.web.AdminServerKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
