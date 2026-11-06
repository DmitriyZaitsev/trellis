// In root mode (com.dzaitsev.trellis.conventions), the convention plugin applies all necessary
// plugins and configures this Java module based on its declaration in settings.gradle.kts.
// The build.gradle.kts contains only what is specific to this module: its dependencies and
// configuration.

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

dependencies {
    implementation(project(":shared:ui"))

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