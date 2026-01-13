// Archivo de configuración de Gradle para el módulo de la aplicación.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Plugin de Firebase
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.diabetes_app"
    compileSdk = 35 // <--- MODIFICADO a 33 para compatibilidad con Java 11

    defaultConfig {
        applicationId = "com.example.diabetes_app"
        minSdk = 25
        targetSdk = 35 // <--- MODIFICADO a 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true // Habilita desugaring para Java 8+ APIs
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        // Versión del compilador de Kotlin compatible con Compose BOM 2024.06.00
        kotlinCompilerExtensionVersion = "1.5.13"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // === 1. FIREBASE BILL OF MATERIALS (BOM) ===
    // Importa el BOM de Firebase para gestionar todas las versiones de forma compatible.
    implementation(platform("com.google.firebase:firebase-bom:33.0.0")) // Versión consolidada

    // === 2. COMPOSE BILL OF MATERIALS (BOM) ===
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // === 3. DEPENDENCIAS DE FIREBASE (SIN ESPECIFICAR VERSIÓN) ===
    // Core
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Las dependencias de Firebase App Check/reCAPTCHA/SafetyNet han sido eliminadas.

    // === 4. DEPENDENCIAS DE COMPOSE ===
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // === 5. DEPENDENCIAS DE ANDROIDX CORE / UTILITIES ===
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // === 6. LIBRERÍAS MISCELÁNEAS ===
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // === 7. DEPENDENCIAS PARA XML (Necesario si usas XML y Compose) ===
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Dependencias de libs.versions.toml (Asumimos que estas son correctas)
    implementation(libs.androidx.navigation.common.android)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.androidx.animation.core.android) // Ya no está duplicada

    // === 8. DESUGARING ===
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // === 9. TESTING ===
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}