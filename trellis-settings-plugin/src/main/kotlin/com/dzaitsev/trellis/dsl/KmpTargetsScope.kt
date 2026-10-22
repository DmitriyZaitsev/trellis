package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.KmpTarget.AndroidNative
import com.dzaitsev.trellis.KmpTarget.AndroidNative.AndroidNativeHost
import com.dzaitsev.trellis.KmpTarget.Ios
import com.dzaitsev.trellis.KmpTarget.Ios.Framework
import com.dzaitsev.trellis.KmpTarget.Ios.IosHost
import com.dzaitsev.trellis.KmpTarget.Js
import com.dzaitsev.trellis.KmpTarget.JsEnv
import com.dzaitsev.trellis.KmpTarget.Linux
import com.dzaitsev.trellis.KmpTarget.Macos
import com.dzaitsev.trellis.KmpTarget.Mingw
import com.dzaitsev.trellis.KmpTarget.Tvos
import com.dzaitsev.trellis.KmpTarget.Tvos.TvosHost
import com.dzaitsev.trellis.KmpTarget.WasmJs
import com.dzaitsev.trellis.KmpTarget.Watchos
import com.dzaitsev.trellis.KmpTarget.Watchos.WatchosHost

/**
 * Target declarations for [ModuleContainerDsl.kmp]; every call registers one target kind:
 *
 * ```
 * kmp("shared") {
 *     android("example.shared")
 *     ios { framework("Shared") { export(":core") } }
 *     jvm()
 *     js(browser, isApplication = true)
 * }
 * ```
 *
 * Kinds with platform variants take them as leading varargs (none selects the kind's `DEFAULT`),
 * options are named arguments, and the variant constants (`iosArm64`, `browser`, ...) are members
 * of this scope. Declaring the same kind twice is rejected.
 */
@TrellisDslMarker
public open class KmpTargetsScope internal constructor() {
    private val targets = LinkedHashMap<String, KmpTarget>()

    internal fun build(): Set<KmpTarget> = targets.values.toSet()

    private fun register(target: KmpTarget) {
        require(targets.putIfAbsent(target.name, target) == null) {
            "KMP target of kind '${target.name}' is declared more than once"
        }
    }

    // Common
    public fun jvm(): Unit = register(KmpTarget.Jvm)

    /** The JVM-based Android target (AGP KMP library, source set `androidMain`). For the NDK, see [androidNative]. */
    public fun android(namespace: String): Unit = register(KmpTarget.Android(namespace))

    // Apple
    public fun macos(isApplication: Boolean = false): Unit = register(Macos(isApplication))

    public fun ios(vararg hosts: IosHost, block: IosScope.() -> Unit = {}) {
        register(Ios(IosScope().apply(block).build(), hosts.toSet().ifEmpty { IosHost.DEFAULT }))
    }

    public fun tvos(vararg hosts: TvosHost): Unit = register(Tvos(hosts.toSet().ifEmpty { TvosHost.DEFAULT }))

    public fun watchos(vararg hosts: WatchosHost): Unit = register(Watchos(hosts.toSet().ifEmpty { WatchosHost.DEFAULT }))

    // Native, non-Apple
    public fun linux(isApplication: Boolean = false): Unit = register(Linux(isApplication))

    public fun mingw(isApplication: Boolean = false): Unit = register(Mingw(isApplication))

    /**
     * Kotlin/Native for the Android NDK (`androidNativeArm64`, `androidNativeX64`; source set
     * `androidNativeMain`). Not the Android app/library target; for that, see [android].
     */
    public fun androidNative(vararg hosts: AndroidNativeHost): Unit =
        register(AndroidNative(hosts.toSet().ifEmpty { AndroidNativeHost.DEFAULT }))

    /** Declares [macos], [linux] and [mingw]. */
    public fun desktop(isApplication: Boolean = false) {
        macos(isApplication)
        linux(isApplication)
        mingw(isApplication)
    }

    // Web
    public fun js(vararg environments: JsEnv, isApplication: Boolean = false): Unit =
        register(Js(isApplication, environments.toSet().ifEmpty { JsEnv.DEFAULT }))

    public fun wasmJs(vararg environments: JsEnv, isApplication: Boolean = false): Unit =
        register(WasmJs(isApplication, environments.toSet().ifEmpty { JsEnv.DEFAULT }))

    // Variants
    public val browser: JsEnv = JsEnv.BROWSER
    public val nodeJs: JsEnv = JsEnv.NODE_JS

    public val iosArm64: IosHost = IosHost.IOS_ARM64
    public val iosSimulatorArm64: IosHost = IosHost.IOS_SIMULATOR_ARM64
    public val iosX64: IosHost = IosHost.IOS_X64

    public val tvosArm64: TvosHost = TvosHost.TVOS_ARM64
    public val tvosSimulatorArm64: TvosHost = TvosHost.TVOS_SIMULATOR_ARM64

    public val watchosArm64: WatchosHost = WatchosHost.WATCHOS_ARM64
    public val watchosSimulatorArm64: WatchosHost = WatchosHost.WATCHOS_SIMULATOR_ARM64

    public val androidNativeArm64: AndroidNativeHost = AndroidNativeHost.ANDROID_NATIVE_ARM64
    public val androidNativeX64: AndroidNativeHost = AndroidNativeHost.ANDROID_NATIVE_X64

    /** Options of an [ios] target. */
    @TrellisDslMarker
    public class IosScope internal constructor() {
        private var declared: Framework? = null

        /** Exports an Objective-C/Swift framework binary from the iOS targets. */
        public fun framework(baseName: String, block: FrameworkScope.() -> Unit = {}) {
            check(declared == null) { "iOS framework is declared more than once" }
            declared = FrameworkScope(baseName).apply(block).build()
        }

        internal fun build(): Framework? = declared
    }

    /** Options of an iOS [Framework]; see its documentation for the export wiring. */
    @TrellisDslMarker
    public class FrameworkScope internal constructor(private val baseName: String) {
        private val exports = mutableListOf<String>()

        public var isStatic: Boolean = true
        public var freeCompilerArgs: List<String> = Framework.DEFAULT_FREE_COMPILER_ARGS
        public var releaseFreeCompilerArgs: List<String> = Framework.DEFAULT_RELEASE_FREE_COMPILER_ARGS

        /** Exports the modules at these Gradle project paths (e.g. `":core"`); they must be declared modules. */
        public fun export(vararg projectPaths: String) {
            exports += projectPaths
        }

        internal fun build(): Framework = Framework(baseName, exports.toList(), isStatic, freeCompilerArgs, releaseFreeCompilerArgs)
    }
}
