// Root build file used when no Android SDK is configured (see settings.gradle.kts):
// only the plugins of the pure Kotlin :core module are resolved.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
