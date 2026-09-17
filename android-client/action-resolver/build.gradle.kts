plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

dependencies {
    api(project(":domain"))
    api(project(":registry"))
    implementation(project(":local-ai"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":intent-router"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}
