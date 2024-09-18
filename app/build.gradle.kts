plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.foodrecipesapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.foodrecipesapplication"
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

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Drawer Dependency.
    implementation(libs.androidx.drawerlayout)

    //Live Data Dependency.
    implementation(libs.androidx.lifecycle.livedata.ktx)

    //Data Store Dependency.
    implementation(libs.androidx.datastore.preferences)

    //Dagger-Hilt.
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Navigation Components.
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Room Dependency.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    //Coil Dependency.
    implementation(libs.coil.compose)

    // Lottie Animation Dependency.
    implementation(libs.lottie)

    //Shimmmer Effect For Recycler view.
    implementation(libs.shimmer)

    implementation(libs.jsoup)

    //Bubble Tabbar Navigation Dependency.
    implementation(libs.bubbletabbar)

    //View Pager Dependencies.
    implementation(libs.androidx.viewpager2)

    //Firebase Authentication Dependencies.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    //Google Authentication Dependencies.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.facebook.login)
}