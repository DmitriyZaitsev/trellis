package com.dzaitsev.trellis

import com.dzaitsev.trellis.android.HasAndroidNamespace

/**
 * A Kotlin Multiplatform compilation target attached to a [Module.KmpLibrary].
 *
 * Targets form a hierarchy that mirrors Kotlin's default source-set hierarchy; [name] is the
 * Kotlin source-set / target-group name (`"apple"`, `"ios"`, `"wasmJs"`, ...), and is unique per
 * kind:
 *
 * ```
 * KmpTarget
 * └── Common
 *     ├── Jvm, Android          (not part of Native/Web, like in Kotlin's own template)
 *     ├── Native
 *     │   ├── Apple ── Macos, Ios, Tvos, Watchos
 *     │   ├── Linux
 *     │   ├── Mingw
 *     │   └── AndroidNative
 *     └── Web ── Js, WasmJs
 * ```
 *
 * Every kind with options is a data class; only [Jvm] has none and is an object. Kinds with
 * several platform variants carry them as a non-empty set that defaults to the kind's `DEFAULT`:
 * `hosts` for [Ios], [Tvos], [Watchos] and [AndroidNative], `environments` for [Js] and [WasmJs].
 * Consumers can dispatch on any level of the hierarchy, e.g. `target is KmpTarget.Apple`.
 *
 * The group singletons ([com.dzaitsev.trellis.Common], [com.dzaitsev.trellis.Native],
 * [com.dzaitsev.trellis.Web], [com.dzaitsev.trellis.Apple]) only name a level of the hierarchy and
 * are rejected as module targets: declare the concrete targets instead.
 *
 * Whether a target produces a library-only (klib) output or an additional runnable
 * application/executable is a per-target Gradle DSL toggle (`binaries.executable()`), not a
 * whole-module plugin choice the way `com.android.application`/`com.android.library` is — see
 * `isApplication` on [Js], [WasmJs], [Macos], [Linux] and [Mingw]. A module can freely mix
 * library-shaped targets (e.g. [Jvm], [Ios]) with application-shaped ones.
 */
public sealed class KmpTarget(public val name: String) {
    public sealed class Common(name: String) : KmpTarget(name)

    /** The JVM target. */
    public data object Jvm : Common("jvm")

    /**
     * The Android target of a KMP module. Android resources follow the module's `usesCompose` and host
     * tests are always registered; a module script can still change either.
     */
    public data class Android(override val namespace: String) :
        Common("android"),
        HasAndroidNamespace

    public sealed class Native(name: String) : Common(name)

    public sealed class Web(name: String) : Common(name)

    /** An Apple target, which can additionally export an Objective-C/Swift [Framework]. */
    public sealed class Apple(name: String) : Native(name) {
        /** Framework built for each of this target's hosts, or `null` to build none. */
        public abstract val framework: Framework?
    }

    /**
     * Objective-C/Swift framework export of an [Apple] target — the "umbrella module" pattern for
     * shipping a KMP module graph to an Apple app.
     *
     * [export] takes Gradle project paths (e.g. `":domain"`) rather than `Project` references, so a
     * [Module] declaration stays plain data. Each exported path is also added as an `api` dependency
     * of each host's main source set, which Kotlin/Native requires for exported dependencies.
     *
     * Applies `-Xpartial-linkage-loglevel=WARNING` unconditionally, and
     * `-Xbinary=gc=concurrent_mark_and_sweep` / `-Xallocator=std` / `-opt` for release binaries only.
     */
    public data class Framework(
        public val baseName: String,
        public val export: List<String> = emptyList(),
        public val isStatic: Boolean = true,
        public val freeCompilerArgs: List<String> = DEFAULT_FREE_COMPILER_ARGS,
        public val releaseFreeCompilerArgs: List<String> = DEFAULT_RELEASE_FREE_COMPILER_ARGS,
    ) {
        public companion object {
            public val DEFAULT_FREE_COMPILER_ARGS: List<String> =
                listOf("-Xpartial-linkage-loglevel=WARNING")
            public val DEFAULT_RELEASE_FREE_COMPILER_ARGS: List<String> =
                listOf(
                    "-Xbinary=gc=concurrent_mark_and_sweep",
                    "-Xallocator=std",
                    "-opt",
                )
        }
    }

    /**
     * Kotlin/Native Linux: `linuxX64` and `linuxArm64` by default. Set [isApplication] to register an
     * executable binary on each host.
     */
    public data class Linux(public val isApplication: Boolean = false, public val hosts: Set<LinuxHost> = LinuxHost.DEFAULT) :
        Native("linux") {
        init {
            require(hosts.isNotEmpty()) { "Linux target needs at least one host" }
        }

        public companion object : AppLibraryFactory<Linux> {
            override fun of(isApplication: Boolean): Linux = Linux(isApplication)
        }

        public enum class LinuxHost {
            LINUX_X64,
            LINUX_ARM64,
            ;

            public companion object {
                public val DEFAULT: Set<LinuxHost> = entries.toSet()
            }
        }
    }

    /** Kotlin/Native Windows (`mingwX64`, its only host). Set [isApplication] to register an executable binary. */
    public data class Mingw(public val isApplication: Boolean = false) : Native("mingw") {
        public companion object : AppLibraryFactory<Mingw> {
            override fun of(isApplication: Boolean): Mingw = Mingw(isApplication)
        }
    }

    /**
     * Kotlin/Native Android (`androidNativeArm64`, `androidNativeX64`) — not to be confused with
     * [Android], the JVM-based Android target.
     */
    public data class AndroidNative(public val hosts: Set<AndroidNativeHost> = AndroidNativeHost.DEFAULT) : Native("androidNative") {
        init {
            require(hosts.isNotEmpty()) { "AndroidNative target needs at least one host" }
        }

        public enum class AndroidNativeHost {
            ANDROID_NATIVE_ARM64,
            ANDROID_NATIVE_X64,
            ;

            public companion object {
                public val DEFAULT: Set<AndroidNativeHost> = entries.toSet()
            }
        }
    }

    /**
     * Kotlin/JS target: registers the `browser()` and/or `nodejs()` sub-targets listed in [environments].
     *
     * Set [isApplication] to `true` to additionally register an executable binary (`fun main()`
     * in this target's source set becomes a runnable/bundled JS app); leave it `false` (the
     * default) for a target meant to be consumed as a library by another module.
     */
    public data class Js(public val isApplication: Boolean = false, public val environments: Set<JsEnv> = JsEnv.DEFAULT) : Web("js") {
        init {
            require(environments.isNotEmpty()) { "Js target needs at least one environment" }
        }

        public companion object : WebTargetFactory<Js> {
            override fun of(isApplication: Boolean, environments: Set<JsEnv>): Js = Js(isApplication, environments)
        }
    }

    /**
     * Kotlin/Wasm (`wasmJs`) target: registers the `browser()` and/or `nodejs()` sub-targets listed
     * in [environments]. Set [isApplication] to `true` to additionally register an executable
     * binary, same as [Js].
     */
    public data class WasmJs(public val isApplication: Boolean = false, public val environments: Set<JsEnv> = JsEnv.DEFAULT) :
        Web("wasmJs") {
        init {
            require(environments.isNotEmpty()) { "WasmJs target needs at least one environment" }
        }

        public companion object : WebTargetFactory<WasmJs> {
            override fun of(isApplication: Boolean, environments: Set<JsEnv>): WasmJs = WasmJs(isApplication, environments)
        }
    }

    /** Execution environments of a [Js] or [WasmJs] target. */
    public enum class JsEnv {
        BROWSER,
        NODE_JS,
        ;

        public companion object {
            public val DEFAULT: Set<JsEnv> = entries.toSet()
        }
    }

    /**
     * Kotlin/Native macOS: `macosArm64` by default, `macosX64` (Intel) on request. Set [isApplication]
     * to register an executable binary on each host.
     */
    public data class Macos(
        public val isApplication: Boolean = false,
        public val hosts: Set<MacosHost> = MacosHost.DEFAULT,
        override val framework: Framework? = null,
    ) : Apple("macos") {
        init {
            require(hosts.isNotEmpty()) { "Macos target needs at least one host" }
        }

        public companion object : AppLibraryFactory<Macos> {
            override fun of(isApplication: Boolean): Macos = Macos(isApplication)
        }

        public enum class MacosHost {
            MACOS_ARM64,
            MACOS_X64,
            ;

            public companion object {
                /** Apple Silicon only; `MACOS_X64` is opt-in as Kotlin is phasing out x86_64 Apple targets. */
                public val DEFAULT: Set<MacosHost> = setOf(MACOS_ARM64)
            }
        }
    }

    /**
     * The iOS targets listed in [hosts]: `iosArm64` and `iosSimulatorArm64` by default, `iosX64`
     * (Intel simulators) on request.
     */
    public data class Ios(override val framework: Framework? = null, public val hosts: Set<IosHost> = IosHost.DEFAULT) : Apple("ios") {
        init {
            require(hosts.isNotEmpty()) { "Ios target needs at least one host" }
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
    }

    /** Kotlin/Native tvOS: device and Apple Silicon simulator. */
    public data class Tvos(public val hosts: Set<TvosHost> = TvosHost.DEFAULT, override val framework: Framework? = null) :
        Apple("tvos") {
        init {
            require(hosts.isNotEmpty()) { "Tvos target needs at least one host" }
        }

        public enum class TvosHost {
            TVOS_ARM64,
            TVOS_SIMULATOR_ARM64,
            ;

            public companion object {
                public val DEFAULT: Set<TvosHost> = entries.toSet()
            }
        }
    }

    /** Kotlin/Native watchOS: device and Apple Silicon simulator. */
    public data class Watchos(public val hosts: Set<WatchosHost> = WatchosHost.DEFAULT, override val framework: Framework? = null) :
        Apple("watchos") {
        init {
            require(hosts.isNotEmpty()) { "Watchos target needs at least one host" }
        }

        public enum class WatchosHost {
            WATCHOS_ARM64,
            WATCHOS_SIMULATOR_ARM64,
            ;

            public companion object {
                public val DEFAULT: Set<WatchosHost> = entries.toSet()
            }
        }
    }
}

// Group singletons: name a level of the hierarchy; not declarable as module targets.
public data object Common : KmpTarget.Common("common")

public data object Native : KmpTarget.Native("native")

public data object Web : KmpTarget.Web("web")

public data object Apple : KmpTarget.Apple("apple") {
    override val framework: Framework? = null
}

/** True for the group singletons that only name a hierarchy level rather than a concrete target. */
internal val KmpTarget.isGroup: Boolean
    get() = this === Common || this === Native || this === Web || this === Apple

/**
 * Shared factory shape for [KmpTarget] kinds that toggle between library-only and
 * application (executable-producing) output through a single `isApplication` flag — currently
 * [KmpTarget.Js], [KmpTarget.WasmJs], [KmpTarget.Macos], [KmpTarget.Linux] and [KmpTarget.Mingw].
 * Implement [of] once in each kind's companion object; [library] and [application] come for free
 * and read the same way `androidApp`/`androidLibrary` do at the call site (e.g.
 * `KmpTarget.Linux.application()`).
 */
public interface AppLibraryFactory<T : KmpTarget> {
    public fun of(isApplication: Boolean): T

    public fun library(): T = of(isApplication = false)

    public fun application(): T = of(isApplication = true)
}

/** [AppLibraryFactory] for [KmpTarget.Js]/[KmpTarget.WasmJs], additionally choosing their [KmpTarget.JsEnv]s. */
public interface WebTargetFactory<T : KmpTarget> : AppLibraryFactory<T> {
    public fun of(isApplication: Boolean, environments: Set<KmpTarget.JsEnv>): T

    override fun of(isApplication: Boolean): T = of(isApplication, KmpTarget.JsEnv.DEFAULT)

    public fun library(vararg environments: KmpTarget.JsEnv): T = of(false, environments.toSet().ifEmpty { KmpTarget.JsEnv.DEFAULT })

    public fun application(vararg environments: KmpTarget.JsEnv): T = of(true, environments.toSet().ifEmpty { KmpTarget.JsEnv.DEFAULT })
}
