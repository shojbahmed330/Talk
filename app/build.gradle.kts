plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.realtimecalltranslation"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.realtimecalltranslation"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

repositories {
    // For local AARs in /libs
    flatDir {
        dirs("libs")
    }
    google()
    mavenCentral()
    // Add Agora Maven repository
    maven { url = uri("https://raw.githubusercontent.com/AgoraIO/Agora-RTC-SDK/master/m2") }
}

dependencies {
    // Jetpack Compose and AndroidX (Updated to latest stable versions)
    implementation("androidx.compose.ui:ui:1.6.7") // Latest as of June 2025
    implementation("androidx.compose.material3:material3:1.2.0") // Latest stable
    implementation("androidx.activity:activity-compose:1.9.0") // Latest stable
    implementation("androidx.navigation:navigation-compose:2.8.0") // Latest stable
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    implementation("androidx.appcompat:appcompat:1.7.0") // Latest stable
    implementation("androidx.core:core-ktx:1.13.1") // Latest stable

    // Networking, JSON, Coroutines, etc. (Updated to latest)
    implementation("com.squareup.retrofit2:retrofit:2.11.0") // Latest stable
    implementation("com.squareup.retrofit2:converter-gson:2.11.0") // Latest stable
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1") // Latest stable

    // Third party SDKs (Updated to v4.5.2 with local AAR)
    // Assuming you have agora-rtc-sdk-4.5.2.aar; if not, use .jar as fallback
    //implementation(files("libs/agora-rtc-sdk-4.5.2.aar")) // Replace with .jar if AAR not found
    implementation("io.coil-kt:coil-compose:2.6.0") // Latest consistent version

    // AWS SDK AARs (from local libs folder)
    implementation(files("libs/aws-android-sdk-core-2.61.0.aar"))
    implementation(files("libs/aws-android-sdk-polly-2.61.0.aar"))
    implementation(files("libs/aws-android-sdk-s3-2.61.0.aar"))
    implementation(files("libs/aws-android-sdk-transcribe-2.61.0.aar"))

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.34.0") // Latest stable
    implementation("io.agora.rtc:full-sdk:4.5.2") // ✅ সঠিক: double quote ব্যবহার

}