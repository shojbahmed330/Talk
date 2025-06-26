plugins {
    id("com.android.application") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
        classpath("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:2.0.20")
        classpath("com.google.gms:google-services:4.4.2")
    }
}

// ❌ Do NOT include repositories{} block here — it causes conflicts when RepositoriesMode.PREFER_SETTINGS is set.

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.webtrc.org/repo") } // Agora এর জন্য
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}