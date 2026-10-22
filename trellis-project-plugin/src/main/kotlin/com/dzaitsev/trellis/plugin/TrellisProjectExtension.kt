package com.dzaitsev.trellis.plugin

import org.gradle.api.JavaVersion
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/**
 * Base interface for a Gradle project extension that configures a [TrellisProjectPlugin].
 *
 * Implement this on your own extension interface (with further `Property<T>` getters, as usual
 * for Gradle-managed extensions) to plug it into [TrellisProjectPlugin] and [Configurator].
 *
 * [kotlinVersion], [javaVersion], [androidMinSdk] and [androidTargetSdk] live here rather than on
 * each consumer's own extension type because every built-in [Configurator] this library ships
 * (Kotlin/Java toolchain setup, Android app/library/KMP-target configuration) needs them.
 */
public interface TrellisProjectExtension {
    /** The Kotlin language/API version to compile against, e.g. `"2.4"`; unset keeps the compiler's default. */
    @get:Input
    @get:Optional
    public val kotlinVersion: Property<String>

    /** The JVM target/toolchain version, e.g. `17`; unset keeps the tooling defaults. */
    @get:Input
    @get:Optional
    public val javaVersion: Property<Int>

    /** The Android `minSdk`; required when an Android module or KMP Android target is declared. */
    @get:Input
    @get:Optional
    public val androidMinSdk: Property<Int>

    /** The Android `compileSdk`/`targetSdk`; required when an Android module or KMP Android target is declared. */
    @get:Input
    @get:Optional
    public val androidTargetSdk: Property<Int>

    /** Compile with a [javaVersion] JDK toolchain instead of the JDK running Gradle. Defaults to `false`. */
    @get:Input
    @get:Optional
    public val useJavaToolchain: Property<Boolean>
}

/** The configured Kotlin language/API version, or `null` when the compiler's default applies. */
public fun TrellisProjectExtension.getKotlinVersion(): KotlinVersion? = kotlinVersion.orNull?.toKotlinVersion()

/** Parses `"2.4"` or a full release such as `"2.4.20"`; only major and minor matter for the language version. */
internal fun String.toKotlinVersion(): KotlinVersion = KotlinVersion.fromVersion(split('.').take(2).joinToString("."))

/** The configured JVM target, or `null` when the tooling defaults apply. */
public fun TrellisProjectExtension.getJvmTarget(): JvmTarget? =
    javaVersion.orNull?.let { JvmTarget.fromTarget(JavaVersion.toVersion(it).toString()) }

internal class AndroidSdkLevels(val minSdk: Int, val targetSdk: Int)

/** The Android SDK levels for [projectPath], failing with what to set when either is missing. */
internal fun TrellisProjectExtension.requireAndroidSdkLevels(projectPath: String): AndroidSdkLevels {
    val minSdk = androidMinSdk.orNull
    val targetSdk = androidTargetSdk.orNull
    if (minSdk != null && targetSdk != null) return AndroidSdkLevels(minSdk, targetSdk)

    val missing = listOfNotNull("androidMinSdk".takeIf { minSdk == null }, "androidTargetSdk".takeIf { targetSdk == null })
    error(
        "$projectPath declares an Android module or target, but ${missing.joinToString(" and ")} " +
            "${if (missing.size == 1) "is" else "are"} not set. Set them in settings " +
            "`trellis { projectConventions { } }` or in the root build script's `trellis { }` block.",
    )
}

/**
 * Uses [conventions] declared in settings (`trellis { projectConventions { } }`) as this extension's
 * defaults; values assigned in a build script still win.
 */
public fun TrellisProjectExtension.useSettingsConventions(conventions: ProjectConventions) {
    kotlinVersion.convention(conventions.kotlinVersion)
    javaVersion.convention(conventions.javaVersion)
    androidMinSdk.convention(conventions.androidMinSdk)
    androidTargetSdk.convention(conventions.androidTargetSdk)
    useJavaToolchain.convention(conventions.useJavaToolchain)
}
