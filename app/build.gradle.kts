plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mohammad.instalite"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mohammad.instalite"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures { buildConfig = true }

    // Two versions from one codebase:
    //  full -> the original InstaLite (feed, DMs, stories, profiles; no Reels feed)
    //  dms  -> messages only: locked to Instagram DMs (/direct/), everything else blocked
    flavorDimensions += "mode"
    productFlavors {
        create("full") {
            dimension = "mode"
            resValue("string", "app_name", "InstaLite")
            buildConfigField("boolean", "DMS_ONLY", "false")
            buildConfigField("String", "HOME_URL", "\"https://www.instagram.com/\"")
        }
        create("dms") {
            dimension = "mode"
            applicationIdSuffix = ".dms"
            versionNameSuffix = "-dms"
            resValue("string", "app_name", "InstaLite DMs")
            buildConfigField("boolean", "DMS_ONLY", "true")
            buildConfigField("String", "HOME_URL", "\"https://www.instagram.com/direct/inbox/\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the release APK installs directly.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
}
