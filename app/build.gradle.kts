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
        // vectorDrawables {
        //     useSupportLibrary = true
        // }

        buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"dsnetrfzy\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"222321832832871\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"oHqH2-oyDM7t7htVQkGrf0yVi7c\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        val apkFile = file("build/outputs/apk/debug/app-debug.apk")
        if (apkFile.exists()) {
            val destDir = file("../malaramofficial")
            destDir.mkdirs()
            apkFile.copyTo(file("../malaramofficial/app-debug.apk"), overwrite = true)
            apkFile.copyTo(file("../app-debug.apk"), overwrite = true)
            println("APK copied successfully to malaramofficial/app-debug.apk and root of the project!")
        } else {
            println("Warning: APK not found at build/outputs/apk/debug/app-debug.apk")
        }
    }
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy("copyApkToRoot")
}
