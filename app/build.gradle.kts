plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.malaram.calculator" // Aapka package name
    compileSdk = 34

    defaultConfig {
        applicationId = "com.malaram.calculator"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // --- YE WAHAN HONA CHAHIYE (Inside Android Block) ---
    signingConfigs {
        create("release") {
            storeFile = file("../release.jks") // File name check karein
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release") // Isse connect hoga
        }
    }
    // ---------------------------------------------------
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy("copyApkToRoot")
}
