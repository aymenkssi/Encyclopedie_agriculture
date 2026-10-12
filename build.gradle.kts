// Root build file. Plugins are declared per module so that the pure Kotlin
// `:core` module can be built without resolving the Android Gradle Plugin.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
