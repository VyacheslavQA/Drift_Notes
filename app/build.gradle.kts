plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.driftnotes"
    compileSdk = 34

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "com.example.driftnotes"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "YANDEX_WEATHER_API_KEY", "@string/yandex_weather_api_key")
        }

        debug {
            buildConfigField("String", "YANDEX_WEATHER_API_KEY", "@string/yandex_weather_api_key")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Остальные зависимости остаются без изменений
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    // ... все остальные зависимости
}