plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
    `java-library`
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
}

configurations.matching {
    val name = it.name.lowercase()
    name.contains("protobuf") || name.contains("protoc")
}.configureEach {
    // Native protoc binaries are OS-specific; locking them breaks Ubuntu CI vs Windows hosts.
    resolutionStrategy.deactivateDependencyLocking()
}

dependencies {
    api(project(":domain"))
    api(libs.protobuf.java)
    implementation(libs.jackson.databind)
    implementation(libs.json.schema.validator)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}