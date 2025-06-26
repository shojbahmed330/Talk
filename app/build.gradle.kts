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
    implementation("androidx.compose.ui:ui:1.8.3")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.3")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.core.ktx)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    // Networking and coroutines
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Other SDKs
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.agora.rtc:full-sdk:4.5.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // WebRTC
    implementation(files("libs/libwebrtc-123.0.0.aar"))

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Google Cloud Services
    implementation(platform("com.google.cloud:libraries-bom:26.32.0"))
    implementation("com.google.cloud:google-cloud-speech")
    implementation("com.google.cloud:google-cloud-translate")
    implementation("com.google.cloud:google-cloud-texttospeech")
    implementation("io.grpc:grpc-okhttp:1.59.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}