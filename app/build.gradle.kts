import java.util.Properties
import java.io.FileInputStream

plugins {
    id("railnav.android.application")
    id("railnav.android.compose")
    id("railnav.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

// Read local.properties at build time
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}
val rapidApiKey = localProperties.getProperty("RAPIDAPI_KEY", "")

android {
    namespace = "com.app.railnav"

    defaultConfig {
        applicationId = "com.app.railnav"
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // Inject the API key into BuildConfig
        buildConfigField("String", "RAPIDAPI_KEY", "\"$rapidApiKey\"")

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
}

dependencies {

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.text)
    testImplementation(libs.junit)
    implementation(libs.play.services.location)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.osmdroid.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ROOM IMPLEMENTATION
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // KSP (Annotation Processing)
    ksp(libs.androidx.room.compiler)

    // NETWORKING (Retrofit + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // OSMDroid Bonus Pack for Clustering
    implementation("com.github.MKergall:osmbonuspack:6.9.0")

    implementation(libs.hilt.navigation.compose)
}