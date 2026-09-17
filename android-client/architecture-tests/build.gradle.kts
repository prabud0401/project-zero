plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(project(":domain"))
    testImplementation(project(":contracts"))
    testImplementation(project(":local-ai"))
    testImplementation(project(":registry"))
    testImplementation(project(":intent-router"))
    testImplementation(project(":action-resolver"))
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    systemProperty("repoRoot", rootProject.projectDir.parentFile.absolutePath)
    systemProperty("androidClientRoot", rootProject.projectDir.absolutePath)
    systemProperty("domainMainSource", rootProject.file("domain/src/main").absolutePath)
    systemProperty("contractsMainSource", rootProject.file("contracts/src/main").absolutePath)
    systemProperty("appManifest", rootProject.file("app/src/main/AndroidManifest.xml").absolutePath)
    systemProperty("ingestManifest", rootProject.file("notification-ingest/src/main/AndroidManifest.xml").absolutePath)
}