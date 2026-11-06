plugins {
    // In Isolated Projects mode (com.dzaitsev.trellis.local), each module must apply the local plugin
// to configure only itself. The local plugin reads conventions from settings.gradle.kts and applies
// plugins and configures the module before the rest of this script runs.
alias(libs.plugins.trellisLocal)
}

dependencies {
    implementation(projects.shared.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    defaultConfig {
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}