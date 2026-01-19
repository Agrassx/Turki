plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.turki.bot.MainKt")
}

dependencies {
    implementation(projects.core)
    implementation(libs.bundles.kotlin.common)
    implementation(libs.bundles.ktor.server)
    implementation(libs.telegram.bot)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)
    implementation(libs.quartz)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.turki.bot.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(":core:jar")
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
