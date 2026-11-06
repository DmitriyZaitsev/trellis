plugins {
    listOf(
        libs.plugins.androidApplication,
        libs.plugins.androidMultiplatformLibrary,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.kotlinJvm,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.ktor,
    ).forEach { alias(it) apply false }

    // We apply false here because in Isolated Projects mode, each module applies the local plugin
    // (com.dzaitsev.trellis.local) directly. This puts the plugins on the classpath so that
    // the local plugin can apply them with the versions specified in libs.versions.toml,
    // but avoids loading the plugins in the root project's classloader.
    alias(libs.plugins.trellisLocal) apply false
}
