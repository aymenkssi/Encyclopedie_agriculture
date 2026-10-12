import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "AgriPedia"

// Pure Kotlin domain module: always built (no Android SDK required).
include(":core")

// The Android application needs an Android SDK. When none is configured
// (e.g. a sandbox without access to Google's repositories) the module is
// skipped so that `./gradlew :core:test` still works. Set -PforceApp=true
// to include it anyway and get the real configuration error.
val localProps = Properties().apply {
    val f = file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasAndroidSdk = localProps.getProperty("sdk.dir") != null ||
    System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null
if (hasAndroidSdk || providers.gradleProperty("forceApp").orNull == "true") {
    include(":app")
} else {
    logger.warn("AgriPedia: no Android SDK found (sdk.dir / ANDROID_HOME) — ':app' is not included in this build.")
}
