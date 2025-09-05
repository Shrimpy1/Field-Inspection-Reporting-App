plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tiramisu.groupapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tiramisu.groupapplication"
        minSdk = 26
        targetSdk = 36
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
        debug {
            buildConfigField(
                "String",
                "SUPABASE_URL",
                "\"https://ynwdghcwlazwijeemzgm.supabase.co\""
            )
            buildConfigField(
                "String",
                "SUPABASE_KEY",
                "\"sb_publishable_vwq598uQ0H35qry6rYbetg_ST0QFsO3\""
            )
        }
        release {
            buildConfigField(
                "String",
                "SUPABASE_URL",
                "\"https://ynwdghcwlazwijeemzgm.supabase.co\""
            )
            buildConfigField(
                "String",
                "SUPABASE_KEY",
                "\"sb_publishable_vwq598uQ0H35qry6rYbetg_ST0QFsO3\""
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

kotlin {
    jvmToolchain(11)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode"
        )
    }
}

dependencies {
    // Core & UI (Your existing dependencies)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // ViewModel & Navigation
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room Database (Offline Storage)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.google.firebase.auth)
    ksp(libs.androidx.room.compiler)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.storage.kt)

    // Location & Images
    implementation(libs.play.services.location)
    implementation(libs.coil.compose)

    // Testing (Your existing dependencies)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}