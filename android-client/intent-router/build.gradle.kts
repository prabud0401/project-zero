plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

dependencies {
    api(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}
