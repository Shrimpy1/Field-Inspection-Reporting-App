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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

kotlin {
    jvmToolchain(17) // Updated from 11 to 17
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode"
        )
    }
}


dependencies {
    // Core & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Google map
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.accompanist.permissions)

    // ViewModel & Navigation
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)


    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.storage.kt)

    // Location & Images
    implementation(libs.play.services.location)
    implementation(libs.coil.compose)

    // Accompanist & Maps
    implementation(libs.accompanist.permissions)
    implementation(libs.maps.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

