// In Isolated Projects mode (com.dzaitsev.trellis.local), each module must apply the local plugin
// to configure only itself. The local plugin reads conventions from settings.gradle.kts and applies
// plugins and configures the module before the rest of this script runs.
plugins {
    alias(libs.plugins.trellisLocal)
}

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

dependencies {
    implementation(projects.shared.ui)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.dzaitsev.trellissample.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.dzaitsev.trellissample"
            packageVersion = "1.0.0"
        }
    }
}