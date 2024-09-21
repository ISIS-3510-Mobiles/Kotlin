plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.ecostyle"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ecostyle"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    viewBinding {
        enable = true
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // FirebaseUI for Firebase Auth
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Jetpack Compose dependencies
    implementation("androidx.activity:activity-compose:1.9.2")      // Compose activity integration
    implementation("androidx.compose.material:material:1.7.2")     // Material Design components for Compose
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.2") // Previewing composables in Android Studio
    implementation("androidx.compose.ui:ui:1.7.2") // Core Compose UI library (contains Modifier, etc.)
    implementation("androidx.compose.runtime:runtime:1.7.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.2")  // For Compose Preview tools
    implementation("androidx.compose.material:material-icons-core:1.7.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.2")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.1")
}