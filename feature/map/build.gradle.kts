plugins {
    id("railnav.android.library")
    id("railnav.android.compose")
    id("railnav.android.hilt")
}

android {
    namespace = "com.app.railnav.feature.map"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:ui"))

    // Map Specific dependencies
    implementation(libs.osmdroid.android)
    implementation("com.github.MKergall:osmbonuspack:6.9.0")
    implementation(libs.play.services.location)
    
    // UI/Compose
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
}
