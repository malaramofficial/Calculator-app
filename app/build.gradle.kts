plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android { // <--- 1. Android Block Shuru
    namespace = "com.malaram.calculator"
    compileSdk = 34

    defaultConfig { // <--- 2. DefaultConfig Shuru
        applicationId = "com.malaram.calculator"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    } // <--- 2. DefaultConfig Khatam

    // --- YE SAHI JAGAH HAI ---
    signingConfigs {
        create("release") {
            storeFile = file("../release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
} // <--- 1. Android Block Khatam (ISKE BAHAR KUCH NAHI HONA CHAHIYE)
