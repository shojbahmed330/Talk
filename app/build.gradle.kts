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
}

dependencies {
    // Jetpack Compose and AndroidX
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Networking, JSON, Coroutines, etc.
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Third party SDKs
    implementation("io.agora.rtc:full-sdk:4.2.6")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // AWS SDK AARs (from local libs folder)
    implementation(files("libs/aws-android-sdk-core-2.61.0.aar"))
    implementation(files("libs/aws-android-sdk-polly-2.61.0.aar"))
    implementation(files("libs/aws-android-sdk-s3-2.61.0.aar"))
    implementation(files("libs/aws-android-sdk-transcribe-2.61.0.aar"))
    implementation ("androidx.compose.material:material-icons-extended:1.6.7")
    implementation ("io.coil-kt:coil-compose:2.4.0")
    implementation ("androidx.compose.ui:ui:1.6.1")
    implementation ("androidx.compose.material:material:1.6.1")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.6.1")
    implementation ("androidx.activity:activity-compose:1.8.2")
    implementation ("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation ("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("androidx.core:core-ktx:1.12.0")}