pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "android-client"
include(":app")
include(":domain")
include(":contracts")
include(":architecture-tests")
include(":local-ai")
include(":data-local")
include(":notification-ingest")
include(":registry")
include(":intent-router")
include(":action-resolver")
include(":android-executor")
