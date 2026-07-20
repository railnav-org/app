plugins {
    id("railnav.android.library")
    id("railnav.android.compose")
}

android {
    namespace = "com.app.railnav.core.ui"
}

dependencies {
    implementation(project(":core:data"))
    
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // OSMDroid Core
    implementation(libs.osmdroid.android)
}
