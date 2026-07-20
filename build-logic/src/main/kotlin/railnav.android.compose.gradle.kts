import com.android.build.api.dsl.CommonExtension

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

// This plugin must be applied alongside railnav.android.application or railnav.android.library.
// Access the android extension by type since the android {} accessor isn't generated here.
val androidExtension = extensions.getByType(CommonExtension::class.java)
androidExtension.buildFeatures.compose = true

dependencies {
    add("implementation", platform(versionCatalogs.named("libs").findLibrary("androidx-compose-bom").get()))
    add("implementation", versionCatalogs.named("libs").findLibrary("androidx-compose-material3").get())
    add("implementation", versionCatalogs.named("libs").findLibrary("androidx-compose-ui").get())
    add("implementation", versionCatalogs.named("libs").findLibrary("androidx-compose-ui-tooling-preview").get())
    add("implementation", versionCatalogs.named("libs").findLibrary("androidx-compose-runtime").get())
    add("debugImplementation", versionCatalogs.named("libs").findLibrary("androidx-compose-ui-tooling").get())
}
