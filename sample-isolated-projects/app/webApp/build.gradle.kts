// In Isolated Projects mode (com.dzaitsev.trellis.local), each module must apply the local plugin
// to configure only itself. The local plugin reads conventions from settings.gradle.kts and applies
// plugins and configures the module before the rest of this script runs.
plugins {
    alias(libs.plugins.trellisLocal)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.ui)

            implementation(libs.compose.ui)
        }
    }
}