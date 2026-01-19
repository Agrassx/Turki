plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

dependencies {
    api(libs.bundles.kotlin.common)
    api(libs.bundles.exposed)
    api(libs.sqlite.jdbc)
    implementation(libs.koin.core)
}
