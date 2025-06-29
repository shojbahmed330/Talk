plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.realtimecalltranslation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.realtimecalltranslation"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.20" // Matches Kotlin version in settings.gradle.kts
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += listOf("META-INF/DEPENDENCIES", "META-INF/INDEX.LIST")
        }
    }
}

dependencies {
    // Jetpack Compose and AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Firebase (using BOM for version management)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx) // Firebase Storage dependency

    // Networking and coroutines
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.squareup.okhttp)

    // Other SDKs
    implementation(libs.coil.compose)
    implementation(libs.agora.rtc.full)

    // WebRTC
    implementation(files("libs/libwebrtc-123.0.0.aar"))

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Google Cloud Services (using BOM from libs.versions.toml)
    implementation(platform(libs.google.cloud.libraries.bom))
    implementation(libs.google.cloud.speech)
    implementation(libs.google.cloud.translate)
    implementation(libs.google.cloud.texttospeech)
    implementation(libs.grpc.okhttp)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}