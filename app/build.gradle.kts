plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.aistudio.calculator.ywrbt"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aistudio.calculator.ywrbt"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"dsnetrfzy\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"222321832832871\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"oHqH2-oyDM7t7htVQkGrf0yVi7c\"")
    }

    // --- SIGNING CONFIGURATION ADDED HERE ---
    signingConfigs {
        create("release") {
            // GitHub Action step "Decode Keystore" isse 'app/release.keystore' pe banayega
            storeFile = file("release.keystore")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Isse signed APK banegi
            signingConfig = signingConfigs.getByName("release")
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Network & Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.cloudinary)
    implementation(libs.google.play.services.auth)
    implementation(libs.google.play.services.ads)
    implementation(libs.coil.compose)
}

tasks.register("copyApkToRoot") {
    doLast {
        val apkFile = file("build/outputs/apk/release/app-release.apk")
        if (apkFile.exists()) {
            val destDir = file("../malaramofficial")
            destDir.mkdirs()
            apkFile.copyTo(file("../malaramofficial/app-release.apk"), overwrite = true)
            apkFile.copyTo(file("../app-release.apk"), overwrite = true)
            println("Signed APK copied successfully!")
        }
    }
}

afterEvaluate {
    tasks.findByName("assembleRelease")?.finalizedBy("copyApkToRoot")
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy("copyApkToRoot")
}
