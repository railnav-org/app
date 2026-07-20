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
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:map"))
    implementation(project(":feature:live-transit"))
    
    // Needed for RailNavApplication OSMDroid init
    implementation(libs.osmdroid.android)
}