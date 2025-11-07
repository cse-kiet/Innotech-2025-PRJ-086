pluginManagement {
    repositories {
        google()         // ✅ No filter
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()         // ✅ Needed to resolve Firebase artifacts
        mavenCentral()
    }
}

rootProject.name = "Face_recognition"
include(":app")

include(":myapplication")
