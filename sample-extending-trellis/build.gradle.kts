plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    listOf(
        libs.plugins.androidApplication,
        libs.plugins.androidMultiplatformLibrary,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.kotlinJvm,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.ktor,
    ).forEach { alias(it) apply false }

    // Custom plugin defined in build-logic, extending the original Trellis
    // conventions plugin (com.dzaitsev.trellis.conventions) with sample-specific behavior.
    id("com.example.trellis.conventions")
}

// In root mode (com.dzaitsev.trellis.conventions), conventions can be set in the root build.gradle.kts
// using trellis { }, which allows access to the version catalog (libs.versions.toml).
// This approach applies conventions to all modules from one place, configured before any subproject script runs.
// Alternatively, use projectConventions { } in settings.gradle.kts (see sample-isolated-projects for that pattern).
trellis {
    kotlinVersion = libs.versions.kotlin.asProvider()
    javaVersion = libs.versions.javaVersion.get().toInt()
    androidMinSdk = libs.versions.androidMinSdk.get().toInt()
    androidTargetSdk = libs.versions.androidTargetSdk.get().toInt()

    // detektConfig is added by our custom SampleExtension (which extends TrellisProjectExtension),
    // and used by DetektConfigurator to share this config file across all declared source modules.
    detektConfig = layout.projectDirectory.file("config/detekt/detekt.yml")
}
