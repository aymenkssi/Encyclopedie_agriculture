import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Publication settings (see play-store/README.md). Values come from gradle.properties,
// ~/.gradle/gradle.properties or environment variables (GitHub Actions secrets); nothing secret is committed.
fun setting(name: String): String? =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name.uppercase().replace('.', '_'))?.takeIf { it.isNotBlank() }

// AdMob: Google's official test IDs are used until the real ones are set (and always in debug builds).
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestBannerId = "ca-app-pub-3940256099942544/9214589741"
val admobAppId = setting("agripedia.admob.appId") ?: admobTestAppId
val admobBannerId = setting("agripedia.admob.bannerId") ?: admobTestBannerId

android {
    namespace = "org.agripedia.app"
    compileSdk = 35

    defaultConfig {
        // Play Store package name: cannot change once published.
        applicationId = "com.aymen.agripedia"
        minSdk = 26
        targetSdk = 35
        // Increase versionCode by 1 before every build sent to Google Play (even internal tests).
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["admobAppId"] = admobAppId
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            // Upload key, provided by CI secrets or ~/.gradle/gradle.properties (never committed).
            val storePath = setting("agripedia.signing.storeFile")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = setting("agripedia.signing.storePassword")
                keyAlias = setting("agripedia.signing.keyAlias")
                keyPassword = setting("agripedia.signing.keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobTestBannerId\"")
        }
        release {
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // The seed dataset is shared with the :core module tests: /data/seed -> assets/seed
    sourceSets["main"].assets.srcDir(rootProject.file("data"))

    androidResources {
        generateLocaleConfig = false
    }

    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
