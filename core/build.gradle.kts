plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

dependencies {
    implementation(libs.bundles.kotlin.common)
    api(libs.bundles.exposed)
    implementation(libs.sqlite.jdbc)
    implementation(libs.koin.core)
}
