import java.util.Properties
import java.io.FileInputStream

plugins {
    id("railnav.android.library")
    id("railnav.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}
val rapidApiKey = localProperties.getProperty("RAPIDAPI_KEY", "")

android {
    namespace = "com.app.railnav.core.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "RAPIDAPI_KEY", "\"$rapidApiKey\"")
    }
}

dependencies {
    // NETWORKING (Retrofit + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
}
