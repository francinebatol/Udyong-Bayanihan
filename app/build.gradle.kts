plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.udyongbayanihan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.udyongbayanihan"
        minSdk = 29
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Android and UI libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.flexbox)
    implementation (libs.github.glide)
    implementation(libs.swiperefreshlayout)
    annotationProcessor (libs.glide.compiler)

    // Firebase libraries
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation (libs.play.services.auth)
    implementation(libs.firebase.storage)

    implementation (libs.work.runtime.ktx)
    implementation(libs.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.material.calendarview)
    implementation(libs.threetenabp)

    // PhotoView for zooming functionality
    implementation(libs.photoview)

    implementation(libs.circleimageview)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
