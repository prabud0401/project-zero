plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.protobuf) apply false
}

subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension>("java") {
            toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    pluginManager.withPlugin("java") {
        dependencyLocking {
            lockAllConfigurations()
            lockMode.set(LockMode.STRICT)
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
    pluginManager.withPlugin("com.android.application") {
        dependencyLocking {
            lockAllConfigurations()
            lockMode.set(LockMode.STRICT)
        }
    }
}
