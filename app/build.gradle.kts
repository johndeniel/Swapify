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
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ui.firestore)
    implementation(libs.firebase.storage)

    // Google Play services for authentication
    implementation (libs.play.services.auth)

    // RxJava for reactive programming
    implementation(libs.rxandroid)

    // Glide library for efficient image loading and caching.
    implementation (libs.glide)

    // Shimmer library for shimmer effect
    implementation (libs.shimmer)

    // Dagger2 core
    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)

    // Dagger Android
    implementation(libs.dagger.android)
    implementation(libs.dagger.android.support)
    annotationProcessor(libs.dagger.android.processor)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}