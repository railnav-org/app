plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    add("implementation", versionCatalogs.named("libs").findLibrary("hilt-android").get())
    add("ksp", versionCatalogs.named("libs").findLibrary("hilt-compiler").get())
}
