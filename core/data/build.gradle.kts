plugins {
    id("railnav.android.library")
    id("railnav.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.app.railnav.core.data"
}

dependencies {
    implementation(project(":core:network"))

    // ROOM IMPLEMENTATION
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // OSMDroid Core (needed for GeoPoint)
    implementation(libs.osmdroid.android)
}
