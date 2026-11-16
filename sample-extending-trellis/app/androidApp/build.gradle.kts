// In root mode (com.dzaitsev.trellis.conventions), the convention plugin applies all plugins
// and configures every module from the root build.gradle.kts, so there is no need to declare
// plugins here. This module's build.gradle.kts contains only what is specific to it: dependencies.
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