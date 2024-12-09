import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
    id("com.google.gms.google-services")
}
val secretsFile = rootProject.file("secret.propert" +
        "ies")
val secrets = Properties().apply {
    if (secretsFile.exists()) {
        load(secretsFile.inputStream())
    }
}

android {
    namespace = "com.ayush.tranxporter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ayush.tranxporter"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPS_API_KEY", "\"${secrets.getProperty("MAPS_API_KEY", "")}\"")
        manifestPlaceholders["MAPS_API_KEY"] = secrets.getProperty("MAPS_API_KEY", "")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.play.services.location)
    implementation(libs.bundles.maps)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // Navigation & Permissions
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.permissions)

    // Network & Data
    implementation(libs.slf4j.simple)
    implementation(libs.grpc.okhttp)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)

    // Hilt
    implementation(libs.bundles.hilt)
    kapt(libs.hilt.compiler)

//    // Room
//    implementation(libs.bundles.room)
//    kapt(libs.room.compiler)

    // Voyager
    implementation(libs.bundles.voyager)

//    // Ktor
//    implementation(libs.bundles.ktor)

    // Serialization
    implementation(libs.bundles.serialization)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Lottie
    implementation(libs.lottie.compose)

    // Accompanist
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)
//    Testing
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
}