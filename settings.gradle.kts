pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.1.0" apply false
        id("org.jetbrains.kotlin.android") version "2.0.20" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://raw.githubusercontent.com/AgoraIO/Agora-RTC-SDK/master/m2") }
        maven { url = uri("https://maven.webrtc.org") }
    }
}

rootProject.name = "RealTimeCallTranslation"
include(":app")
