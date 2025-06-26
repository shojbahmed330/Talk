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
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose) // Was ("androidx.activity:activity-compose:1.10.1") but this version is in toml
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)

    // Networking and coroutines
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.kotlinx.coroutines.android)

    // Other SDKs
    implementation(libs.coil.compose)
    implementation(libs.agora.rtc.full)
    implementation(libs.squareup.okhttp)

    // WebRTC
    implementation(files("libs/libwebrtc-123.0.0.aar"))

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Google Cloud Services
    implementation(platform(libs.google.cloud.libraries.bom))
    implementation(libs.google.cloud.speech)
    implementation(libs.google.cloud.translate)
    implementation(libs.google.cloud.texttospeech)
    implementation(libs.grpc.okhttp)

    // Tests
    testImplementation(libs.junit) // Was ("junit:junit:4.13.2") but this version is in toml
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}