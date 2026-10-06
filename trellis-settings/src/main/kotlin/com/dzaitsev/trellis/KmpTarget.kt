package com.dzaitsev.trellis

import com.dzaitsev.trellis.KmpTarget.Desktop.Companion.application
import com.dzaitsev.trellis.KmpTarget.Desktop.Companion.library
import com.dzaitsev.trellis.KmpTarget.Js.Companion.application
import com.dzaitsev.trellis.KmpTarget.Js.Companion.library
import com.dzaitsev.trellis.KmpTarget.Wasm.Companion.application
import com.dzaitsev.trellis.KmpTarget.Wasm.Companion.library
import com.dzaitsev.trellis.android.AndroidAttrs
import com.dzaitsev.trellis.android.HasAndroidAttrs

/**
 * A Kotlin Multiplatform compilation target attached to a [Module.KmpLibrary].
 *
 * Whether a target produces a library-only (klib) output or an additional runnable
 * application/executable is a per-target Gradle DSL toggle (`binaries.executable()`), not a
 * whole-module plugin choice the way `com.android.application`/`com.android.library` is — see
 * [Js.isApplication]/[Wasm.isApplication]/[Desktop.isApplication]. A module can freely mix
 * library-shaped targets (e.g. [Jvm], [Ios]) with application-shaped ones (e.g. a [Desktop] with
 * `isApplication = true`) — [Module.KmpLibrary]'s name describes what this library always
 * produces by default, not a hard constraint on what any individual target can opt into.
 *
 * This interface is open: a consuming project can add its own target kinds (e.g. a single native
 * target instead of the [Desktop] group) as additional implementations without changing this
 * library. [Jvm], [Ios], [Android], [Js], [Wasm] and [Desktop] are provided as the built-ins this
 * project currently needs.
 */
public interface KmpTarget {
    /** Builds a target set: `jvm + ios()`; the stdlib `Set<T> + T` extends it further. */
    public operator fun plus(that: KmpTarget): Set<KmpTarget> = setOf(this, that)

    public operator fun plus(that: Collection<KmpTarget>): Set<KmpTarget> = setOf(this) + that

    /**
     * The Android target of a KMP module. Constructor-narrowed to [AndroidAttrs.Kmp] — not the
     * broader [AndroidAttrs] interface — so [AndroidAttrs.Native] (with its app/library-only
     * [AndroidAttrs.Native.flavors]) cannot be mistakenly attached to a KMP target and silently
     * dropped by [com.dzaitsev.trellis.configurator.KmpAndroidTargetConfigurator]'s
     * dispatch, the way an unconstrained `AndroidAttrs` parameter would allow.
     */
    public data class Android(override val androidAttrs: AndroidAttrs.Kmp) : KmpTarget, HasAndroidAttrs

    /**
     * The iOS targets listed in [hosts]: `iosArm64` and `iosSimulatorArm64` by default, `iosX64`
     * (Intel simulators) on request.
     *
     * Set [framework] to additionally export an Objective-C/Swift framework binary from these
     * targets — the "umbrella module" pattern for shipping a KMP module graph to an iOS app.
     * Leave it `null` (the default) for a target meant to be consumed as Kotlin source by another
     * KMP module rather than built as a standalone framework.
     */
    public data class Ios(
        public val framework: Framework? = null,
        public val hosts: Set<IosHost> = IosHost.DEFAULT,
    ) : KmpTarget {
        init {
            require(hosts.isNotEmpty()) { "Ios target needs at least one host" }
        }

        /**
         * Framework export config for [Ios].
         *
         * [export] takes Gradle project paths (e.g. `":domain"`) rather than `Project`
         * references, so a [Module] declaration stays plain data with no Gradle API
         * dependency. Each exported path is also added as an `api` dependency of the `iosMain`
         * source set — Kotlin/Native requires a dependency exported from a framework to be an API
         * dependency of that compilation, so the configurator wires both from this one list
         * instead of requiring it to be declared twice.
         *
         * Applies `-Xpartial-linkage-loglevel=WARNING` unconditionally, and
         * `-Xbinary=gc=concurrent_mark_and_sweep` / `-Xallocator=std` / `-opt` for release
         * binaries only — a common recommended baseline for shipping this kind of framework, not
         * something specific to any one project.
         */
        public data class Framework(
            public val baseName: String,
            public val export: List<String> = emptyList(),
            public val isStatic: Boolean = true,
            public val freeCompilerArgs: List<String> = DEFAULT_FREE_COMPILER_ARGS,
            public val releaseFreeCompilerArgs: List<String> = DEFAULT_RELEASE_FREE_COMPILER_ARGS,
        ) {
            public companion object {
                public val DEFAULT_FREE_COMPILER_ARGS: List<String> = listOf("-Xpartial-linkage-loglevel=WARNING")
                public val DEFAULT_RELEASE_FREE_COMPILER_ARGS: List<String> = listOf(
                    "-Xbinary=gc=concurrent_mark_and_sweep",
                    "-Xallocator=std",
                    "-opt",
                )
            }
        }
    }

    /** Kotlin/Native iOS targets an [Ios] target can register. */
    public enum class IosHost {
        IOS_ARM64,
        IOS_SIMULATOR_ARM64,
        IOS_X64,
        ;

        public companion object {
            /** Device and Apple Silicon simulator; `IOS_X64` is opt-in as Kotlin is phasing out x86_64 Apple targets. */
            public val DEFAULT: Set<IosHost> = setOf(IOS_ARM64, IOS_SIMULATOR_ARM64)
        }
    }

    public data object Jvm : KmpTarget

    /** Execution environments of a [Js] or [Wasm] target. */
    public enum class JsEnvironment {
        BROWSER,
        NODE_JS,
    }

    /** Kotlin/JS target: registers the `browser()` and/or `nodejs()` sub-targets listed in [environments].
     *
     * Set [isApplication] to `true` to additionally register an executable binary (`fun main()`
     * in this target's source set becomes a runnable/bundled JS app); leave it `false` (the
     * default) for a target meant to be consumed as a library by another module. Prefer the
     * [library]/[application] factories over the raw constructor for readability at the call site.
     */
    public data class Js(
        public val isApplication: Boolean = false,
        public val environments: Set<JsEnvironment> = JsEnvironment.entries.toSet(),
    ) : KmpTarget {
        init {
            require(environments.isNotEmpty()) { "Js target needs at least one environment" }
        }

        public companion object : WebTargetFactory<Js> {
            override fun of(isApplication: Boolean, environments: Set<JsEnvironment>): Js =
                Js(isApplication, environments)
        }
    }

    /** Kotlin/Wasm (`wasmJs`) target: registers the `browser()` and/or `nodejs()` sub-targets listed in [environments].
     *
     * Set [isApplication] to `true` to additionally register an executable binary, same as [Js].
     * Prefer the [library]/[application] factories over the raw constructor for readability at
     * the call site.
     */
    public data class Wasm(
        public val isApplication: Boolean = false,
        public val environments: Set<JsEnvironment> = JsEnvironment.entries.toSet(),
    ) : KmpTarget {
        init {
            require(environments.isNotEmpty()) { "Wasm target needs at least one environment" }
        }

        public companion object : WebTargetFactory<Wasm> {
            override fun of(isApplication: Boolean, environments: Set<JsEnvironment>): Wasm =
                Wasm(isApplication, environments)
        }
    }

    /** Desktop native targets: `macosArm64`, `linuxX64`, `mingwX64` (Windows), limited to [hosts].
     *
     * Set [isApplication] to `true` to additionally register an executable binary on each of
     * them (`fun main()` in this target's source set becomes a runnable native binary); leave it
     * `false` (the default) for targets meant to be consumed as a library by another module.
     * Prefer the [library]/[application] factories over the raw constructor for readability at
     * the call site.
     */
    public data class Desktop(
        public val isApplication: Boolean = false,
        public val hosts: Set<DesktopHost> = DesktopHost.entries.toSet(),
    ) : KmpTarget {
        init {
            require(hosts.isNotEmpty()) { "Desktop target needs at least one host" }
        }

        public companion object : AppLibraryFactory<Desktop> {
            override fun of(isApplication: Boolean): Desktop = Desktop(isApplication)
        }
    }

    /** Kotlin/Native desktop targets a [Desktop] target can register. */
    public enum class DesktopHost {
        MACOS_ARM64,
        LINUX_X64,
        MINGW_X64,
    }
}

/**
 * Shared factory shape for [KmpTarget] kinds that toggle between library-only and
 * application (executable-producing) output through a single `isApplication` flag — currently
 * [KmpTarget.Js], [KmpTarget.Wasm] and [KmpTarget.Desktop]. Implement [of] once in each target's
 * companion object; [library] and [application] come for free and read the same way
 * `androidApp`/`androidLibrary` do at the call site (e.g. `KmpTarget.Desktop.application()`).
 */
public interface AppLibraryFactory<T : KmpTarget> {
    public fun of(isApplication: Boolean): T
    public fun library(): T = of(isApplication = false)
    public fun application(): T = of(isApplication = true)
}

/** [AppLibraryFactory] for [KmpTarget.Js]/[KmpTarget.Wasm], additionally choosing their [KmpTarget.JsEnvironment]s. */
public interface WebTargetFactory<T : KmpTarget> : AppLibraryFactory<T> {
    public fun of(isApplication: Boolean, environments: Set<KmpTarget.JsEnvironment>): T

    override fun of(isApplication: Boolean): T = of(isApplication, KmpTarget.JsEnvironment.entries.toSet())

    public fun library(vararg environments: KmpTarget.JsEnvironment): T = of(false, environments.toSet())

    public fun application(vararg environments: KmpTarget.JsEnvironment): T = of(true, environments.toSet())
}

// Kept for source compatibility of existing `import com.dzaitsev.trellis.plus`.
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public operator fun KmpTarget.plus(that: KmpTarget): Set<KmpTarget> = setOf(this, that)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public operator fun KmpTarget.plus(that: Collection<KmpTarget>): Set<KmpTarget> = setOf(this) + that
