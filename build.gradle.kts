plugins {
    alias(libs.plugins.android.application) apply false
    // Kotlin is provided by AGP 9.0+; no standalone Kotlin plugin needed.
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
