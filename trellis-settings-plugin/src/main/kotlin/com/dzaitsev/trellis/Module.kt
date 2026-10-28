package com.dzaitsev.trellis

import com.dzaitsev.trellis.android.Flavor
import com.dzaitsev.trellis.android.HasAndroidNamespace
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * A declared Gradle project module and the properties the reusable configurators need to wire
 * it up: whether it is Android (and if so, an application or a library), what Kotlin
 * Multiplatform targets it has, and whether it opts into `explicitApi()`.
 *
 * This interface is open: [Root], [AndroidApp], [AndroidLibrary], [Java] and [KmpLibrary] are the kinds
 * this library ships with, but a consuming project can add further kinds (e.g. a Gradle plugin module) as additional implementations.
 */
public interface Module {
    /** The project name; its Gradle path is built from enclosing groups. */
    public val name: String

    public val hasExplicitApi: Boolean

    /** A module kind that can opt into Compose; see `usesCompose` in the module DSL for what it applies. */
    public interface UsesCompose {
        public val usesCompose: Boolean
    }

    /** An umbrella/aggregator project that has no sources of its own (e.g. the Gradle root project). */
    public data class Root(override val name: String, override val hasExplicitApi: Boolean = false) : Module

    /** An Android application module — expected to apply `com.android.application`. Explicit API mode is off by default. */
    public data class AndroidApp(
        override val name: String,
        override val namespace: String,
        override val usesCompose: Boolean = false,
        public val flavors: List<Flavor> = emptyList(),
        override val hasExplicitApi: Boolean = false,
    ) : Module,
        UsesCompose,
        HasAndroidNamespace

    /** A plain (non-KMP) Android library module — expected to apply `com.android.library`. */
    public data class AndroidLibrary(
        override val name: String,
        override val namespace: String,
        override val usesCompose: Boolean = false,
        public val flavors: List<Flavor> = emptyList(),
        override val hasExplicitApi: Boolean = false,
    ) : Module,
        UsesCompose,
        HasAndroidNamespace

    /**
     * A plain Java module (application or library). Optionally provide a main class to make it an
     * application; explicit API mode defaults to off and Compose is opt-in. [group]/[version] are
     * left to Gradle's defaults when `null`.
     */
    public data class Java(
        override val name: String,
        public val group: String? = null,
        public val version: String? = null,
        public val mainClass: String? = null,
        override val hasExplicitApi: Boolean = false,
        override val usesCompose: Boolean = false,
    ) : Module,
        UsesCompose

    /**
     * A Kotlin Multiplatform module with one or more [KmpTarget]s.
     *
     * Set [autoConfigureTargets] to `false` to opt a module out of this library's built-in KMP
     * target configurator entirely, e.g. for a module whose `build.gradle.kts` hand-configures
     * its targets itself (a native framework export, custom binary settings, etc.) instead of
     * relying on the declarative defaults.
     *
     * Set [usesCompose] to apply the Compose compiler and Multiplatform plugins.
     *
     * Set [xcframework] to bundle the frameworks of all Apple targets into one XCFramework; they
     * must share a base name, which becomes the XCFramework's name.
     */
    public data class KmpLibrary(
        override val name: String,
        public val targets: Set<KmpTarget>,
        override val hasExplicitApi: Boolean = false,
        public val autoConfigureTargets: Boolean = true,
        override val usesCompose: Boolean = false,
        public val xcframework: Boolean = false,
    ) : Module,
        UsesCompose
}

/** The Android namespace this module declares, directly or through its KMP Android target, or `null`. */
public fun Module.androidNamespace(): String? = when (this) {
    is HasAndroidNamespace -> namespace
    is Module.KmpLibrary -> targets.firstNotNullOfOrNull { (it as? HasAndroidNamespace)?.namespace }
    else -> null
}

@OptIn(ExperimentalContracts::class)
public fun Module.isRoot(): Boolean {
    contract { returns(true) implies (this@isRoot is Module.Root) }
    return this is Module.Root
}

@OptIn(ExperimentalContracts::class)
public fun Module.isAndroidApp(): Boolean {
    contract { returns(true) implies (this@isAndroidApp is Module.AndroidApp) }
    return this is Module.AndroidApp
}

@OptIn(ExperimentalContracts::class)
public fun Module.isJava(): Boolean {
    contract { returns(true) implies (this@isJava is Module.Java) }
    return this is Module.Java
}
