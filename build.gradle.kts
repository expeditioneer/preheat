plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.cyclonedx.bom) apply false
    // Kotlin is provided by AGP 9.0+; no standalone Kotlin plugin needed.
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
