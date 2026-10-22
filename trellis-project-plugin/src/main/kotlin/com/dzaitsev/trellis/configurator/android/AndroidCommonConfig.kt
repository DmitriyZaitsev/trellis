package com.dzaitsev.trellis.configurator.android

import com.android.build.api.dsl.CommonExtension
import com.dzaitsev.trellis.android.Flavor
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.testing.logging.TestLogEvent

/**
 * Configures the parts of [CommonExtension] that are identical for `com.android.application` and
 * `com.android.library` modules, reachable through AGP's shared `CommonExtension`,
 * `DefaultConfig`/`BaseFlavor` and `BuildType` supertypes.
 *
 * Deliberately NOT shared here: product flavors. AGP declares `isDefault`/`applicationIdSuffix`
 * only on the per-kind `ApplicationProductFlavor`/`LibraryProductFlavor` subtypes, but its
 * internal flavor objects don't reliably match those subtypes via `is` checks at runtime (AGP
 * validates by the *statically declared* extension type, not the object's actual class) — an `is
 * ApplicationProductFlavor` check can wrongly match a library's flavor and trip AGP's "library
 * projects cannot set applicationIdSuffix" validation. So [AndroidAppConfigurator] and
 * [AndroidLibraryConfigurator] each create their own flavors against their own statically-typed
 * `ApplicationExtension`/`LibraryExtension`.
 *
 * Callers also configure their own `defaultConfig`/`buildTypes` extras (e.g. `targetSdk`,
 * `consumerProguardFiles`, the `debug` build type, `isMinifyEnabled`) that genuinely differ
 * between app and library modules and have no shared AGP supertype to hang off, plus any
 * project-specific extras (e.g. a shared test-module dependency) this library has no opinion on.
 */
public fun CommonExtension.configureCommonAndroid(
    namespace: String,
    usesCompose: Boolean,
    flavors: List<Flavor>,
    extension: TrellisProjectExtension,
) {
    this.namespace = namespace
    compileSdk = extension.androidTargetSdk.get()

    // CommonExtension only exposes property getters for these (the lambda-block DSL methods are
    // redeclared per-subtype by AGP), so configure the returned objects directly instead of using
    // the `defaultConfig { }`/`compileOptions { }`/`testOptions { }`/`lint { }` block syntax.
    defaultConfig.minSdk = extension.androidMinSdk.get()

    compileOptions.apply {
        val javaVersion = JavaVersion.toVersion(extension.javaVersion.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    testOptions.apply {
        unitTests.all {
            it.testLogging.events = TestLogEvent.entries.toSet()
        }
        unitTests.isReturnDefaultValues = true
    }

    sourceSets.configureEach {
        java.directories.add("src/$name/kotlin")
    }

    buildFeatures.apply {
        viewBinding = false
        if (usesCompose) compose = true // don't set `false` explicitly
        resValues = true
        buildConfig = false
        aidl = false
        shaders = false
    }

    buildTypes.getByName("release") {
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }

    if (flavors.isNotEmpty()) {
        flavorDimensions += flavors.map { it.dimension }.distinct()
    }
}
