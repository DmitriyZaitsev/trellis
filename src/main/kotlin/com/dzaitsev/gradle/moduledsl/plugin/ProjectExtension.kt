package com.dzaitsev.gradle.moduledsl.plugin

import org.gradle.api.JavaVersion
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/**
 * Base interface for a Gradle project extension that configures a [ProjectPlugin].
 *
 * Implement this on your own extension interface (with further `Property<T>` getters, as usual
 * for Gradle-managed extensions) to plug it into [ProjectPlugin] and [Configurator].
 *
 * [kotlinVersion], [javaVersion], [androidMinSdk] and [androidTargetSdk] live here rather than on
 * each consumer's own extension type because every built-in [Configurator] this library ships
 * (Kotlin/Java toolchain setup, Android app/library/KMP-target configuration) needs them.
 */
public interface ProjectExtension {
    /** The Kotlin language/API version to compile against, e.g. `"2.2"`. */
    @get:Input
    public val kotlinVersion: Property<String>

    /** The JVM target/toolchain version, e.g. `17`. */
    @get:Input
    public val javaVersion: Property<Int>

    /** The Android `minSdk` applied to every Android app/library/KMP-Android-target module. */
    @get:Input
    public val androidMinSdk: Property<Int>

    /** The Android `compileSdk`/`targetSdk` applied to every Android app/library/KMP-Android-target module. */
    @get:Input
    public val androidTargetSdk: Property<Int>
}

public fun ProjectExtension.getKotlinVersion(): KotlinVersion = kotlinVersion.get()
    .split(".")
    .let { KotlinVersion.fromVersion("${it[0]}.${it[1]}") }

public fun ProjectExtension.getJvmTarget(): JvmTarget =
    JvmTarget.fromTarget(JavaVersion.toVersion(javaVersion.get()).toString())
