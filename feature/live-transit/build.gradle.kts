plugins {
    id("railnav.android.library")
    id("railnav.android.compose")
}

android {
    namespace = "com.app.railnav.feature.livetransit"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:ui"))

    // UI/Compose
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
