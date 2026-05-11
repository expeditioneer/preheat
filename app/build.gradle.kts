plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    // Kotlin support is built into AGP 9.0+; the standalone
    // org.jetbrains.kotlin.android plugin must NOT be applied — it
    // throws a hard error against AGP 9.0+ built-in Kotlin.
    // See https://kotl.in/gradle/agp-built-in-kotlin
}

android {
    namespace = "dev.expeditioneer.preheat"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.expeditioneer.preheat"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 22
        versionName = "2.2"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable += "GoogleAppIndexingWarning"
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.datastore.preferences)
}
