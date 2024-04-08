plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "barter.swapify"
    compileSdk = 34

    defaultConfig {
        applicationId = "barter.swapify"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isZipAlignEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Core Android libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase libraries
    implementation(libs.transport.runtime)
    implementation(libs.firebase.auth)
    implementation ("com.google.firebase:firebase-database:20.3.1")
    implementation ("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation ("com.google.firebase:firebase-storage:20.3.0")
    implementation ("com.google.firebase:firebase-firestore:24.11.0")

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Google Play services for authentication
    implementation ("com.google.android.gms:play-services-auth:21.0.0")

    // RxJava for reactive programming
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // Dagger2 core
    implementation("com.google.dagger:dagger:2.51.1")
    annotationProcessor("com.google.dagger:dagger-compiler:2.51.1")

    // Dagger Android
    implementation("com.google.dagger:dagger-android:2.51.1")
    implementation("com.google.dagger:dagger-android-support:2.51.1")
    annotationProcessor("com.google.dagger:dagger-android-processor:2.51.1")

    // Facebook Shimmer library for shimmer effect
    implementation ("com.facebook.shimmer:shimmer:0.5.0")
}