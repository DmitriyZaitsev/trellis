package com.dzaitsev.trellis

import com.dzaitsev.trellis.android.AndroidAttrs
import com.dzaitsev.trellis.android.HasAndroidAttrs
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
public interface Module : ModuleModel {
    public val hasExplicitApi: Boolean

    /** An umbrella/aggregator project that has no sources of its own (e.g. the Gradle root project). */
    public data class Root(
        override val name: String,
        override val hasExplicitApi: Boolean = false,
    ) : Module

    /** An Android application module — expected to apply `com.android.application`. Explicit API mode is off by default. */
    public data class AndroidApp(
        override val name: String,
        override val androidAttrs: AndroidAttrs.Native,
        override val hasExplicitApi: Boolean = false,
    ) : Module, HasAndroidAttrs

    /** A plain (non-KMP) Android library module — expected to apply `com.android.library`. */
    public data class AndroidLibrary(
        override val name: String,
        override val androidAttrs: AndroidAttrs.Native,
        override val hasExplicitApi: Boolean = false,
    ) : Module, HasAndroidAttrs

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
        public val usesCompose: Boolean = false,
    ) : Module

    /**
     * A Kotlin Multiplatform module with one or more [KmpTarget]s.
     *
     * Set [autoConfigureTargets] to `false` to opt a module out of this library's built-in KMP
     * target configurator entirely, e.g. for a module whose `build.gradle.kts` hand-configures
     * its targets itself (a native framework export, custom binary settings, etc.) instead of
     * relying on the declarative defaults.
     *
     * Set [usesCompose] to apply the Compose compiler and Multiplatform plugins.
     */
    public data class KmpLibrary(
        override val name: String,
        public val targets: Set<KmpTarget>,
        override val hasExplicitApi: Boolean = false,
        public val autoConfigureTargets: Boolean = true,
        public val usesCompose: Boolean = false,
    ) : Module
}

/**
 * The [AndroidAttrs] for this module, whether it is a plain Android module or a KMP module with
 * an Android target — or `null` when the module has no Android attributes at all. Works for any
 * [Module] kind that implements [HasAndroidAttrs] directly, or exposes one through a
 * [KmpTarget] that implements it.
 */
public fun Module.getAndroidAttrs(): AndroidAttrs? = when (this) {
    is HasAndroidAttrs -> androidAttrs
    is Module.KmpLibrary -> targets.firstNotNullOfOrNull { (it as? HasAndroidAttrs)?.androidAttrs }
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
