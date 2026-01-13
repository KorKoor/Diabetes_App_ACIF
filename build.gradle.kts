// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Plugins de nivel de proyecto con versiones directas
    id("com.android.application") version "8.8.0" apply false // Android Gradle Plugin
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false // Plugin de Kotlin
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false // Plugin de Google Services
}

buildscript {
    dependencies {
        // Asegúrate de que esta dependencia del classpath coincida con la versión de gms.google-services de arriba
        classpath("com.google.gms:google-services:4.4.2")
    }
}