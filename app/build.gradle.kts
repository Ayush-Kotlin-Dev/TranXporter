import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}
val secretsFile = rootProject.file("secret.properties")
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
    val firebaseAuthV = "23.0.0"
    val firebaseGmsPlayServices = "21.2.0"

//Firebase
    val firebaseAuth = "com.google.firebase:firebase-auth-ktx:${firebaseAuthV}"
    val gmsPlayServices =
        "com.google.android.gms:play-services-auth:${firebaseGmsPlayServices}"
    val firebaseAuthKtx = "com.google.firebase:firebase-auth-ktx:23.0.0"
    val firebaseBom = "com.google.firebase:firebase-bom:33.4.0"

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:4.3.0")

    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    implementation(firebaseAuth)
    implementation(gmsPlayServices)
    implementation(firebaseAuthKtx)
    implementation(firebaseBom)
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("io.grpc:grpc-okhttp:1.53.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.android.libraries.places:places:4.1.0")


}