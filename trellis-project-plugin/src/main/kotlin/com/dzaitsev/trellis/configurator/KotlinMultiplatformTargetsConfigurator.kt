package com.dzaitsev.trellis.configurator

import com.dzaitsev.trellis.Apple
import com.dzaitsev.trellis.Common
import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.KmpTarget.AndroidNative.AndroidNativeHost
import com.dzaitsev.trellis.KmpTarget.Ios.IosHost
import com.dzaitsev.trellis.KmpTarget.JsEnv
import com.dzaitsev.trellis.KmpTarget.Linux.LinuxHost
import com.dzaitsev.trellis.KmpTarget.Macos.MacosHost
import com.dzaitsev.trellis.KmpTarget.Tvos.TvosHost
import com.dzaitsev.trellis.KmpTarget.Watchos.WatchosHost
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.Native
import com.dzaitsev.trellis.PluginIds
import com.dzaitsev.trellis.Web
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkConfig
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

/**
 * Applies the default source-set hierarchy template and configures the declared [KmpTarget]s of
 * a [Module.KmpLibrary] — everything that's purely mechanical translation from the
 * declared targets to Kotlin Multiplatform Gradle DSL calls.
 *
 * The [KmpTarget.Android] case is deliberately a no-op here: that target is registered by the
 * `com.android.kotlin.multiplatform.library` plugin itself and configured by
 * [KmpAndroidTargetConfigurator].
 *
 * [KmpTarget.Js], [KmpTarget.WasmJs], [KmpTarget.Macos], [KmpTarget.Linux] and [KmpTarget.Mingw]
 * additionally register an executable
 * binary when declared with `isApplication = true` — Kotlin/JS, Kotlin/Wasm and Kotlin/Native
 * auto-detect the entry point from a top-level `fun main()` in that target's source set, so no
 * separate `mainClass` needs declaring here.
 *
 * [KmpTarget.Ios], [KmpTarget.Tvos], [KmpTarget.Watchos] and [KmpTarget.AndroidNative] register exactly
 * the `hosts` they declare; [KmpTarget.Js] and [KmpTarget.WasmJs] register exactly their `environments`.
 *
 * A [KmpTarget.Apple] target declared with a [KmpTarget.Apple.framework] additionally registers and
 * configures an exported framework binary on each host — see [KmpTarget.Framework] for the export/`api`
 * wiring this configurator performs on its behalf.
 *
 * Skips target registration for modules with [Module.KmpLibrary.autoConfigureTargets] set to `false` —
 * e.g. a module that hand-configures its own targets (a native framework export, custom binary
 * settings, etc.) in its own `build.gradle.kts`.
 *
 * Project-specific extras with no generic default (e.g. a shared test-module dependency added to
 * `commonTest`) are not this library's concern; add them via your own additional [Configurator].
 */
public class KotlinMultiplatformTargetsConfigurator : Configurator<TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        if (module !is Module.KmpLibrary) return

        project.pluginManager.withPlugin(PluginIds.KOTLIN_MULTIPLATFORM) {
            project.extensions.configure<KotlinMultiplatformExtension> {
                if (module.autoConfigureTargets) registerTargets(module, project)
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    private fun KotlinMultiplatformExtension.registerTargets(module: Module.KmpLibrary, project: Project) {
        applyDefaultHierarchyTemplate()

        val xcFramework =
            if (module.xcframework) {
                val name = module.targets.filterIsInstance<KmpTarget.Apple>().firstNotNullOf { it.framework }.baseName
                XCFrameworkConfig(project, name)
            } else {
                null
            }

        module.targets.forEach { target ->
            when (target) {
                is KmpTarget.Jvm -> jvm()

                is KmpTarget.Android -> {
                    // Registered by com.android.kotlin.multiplatform.library plugin,
                    // configured by KmpAndroidTargetConfigurator.
                }

                is KmpTarget.Ios -> target.hosts.map { host ->
                    when (host) {
                        IosHost.IOS_ARM64 -> iosArm64()
                        IosHost.IOS_SIMULATOR_ARM64 -> iosSimulatorArm64()
                        IosHost.IOS_X64 -> iosX64()
                    }
                }.withFramework(target.framework, project, xcFramework)

                is KmpTarget.Tvos -> target.hosts.map { host ->
                    when (host) {
                        TvosHost.TVOS_ARM64 -> tvosArm64()
                        TvosHost.TVOS_SIMULATOR_ARM64 -> tvosSimulatorArm64()
                    }
                }.withFramework(target.framework, project, xcFramework)

                is KmpTarget.Watchos -> target.hosts.map { host ->
                    when (host) {
                        WatchosHost.WATCHOS_ARM64 -> watchosArm64()
                        WatchosHost.WATCHOS_SIMULATOR_ARM64 -> watchosSimulatorArm64()
                    }
                }.withFramework(target.framework, project, xcFramework)

                is KmpTarget.Macos -> target.hosts.map { host ->
                    when (host) {
                        MacosHost.MACOS_ARM64 -> macosArm64()
                        MacosHost.MACOS_X64 -> macosX64()
                    }.apply { configureExecutable(target.isApplication) }
                }.withFramework(target.framework, project, xcFramework)

                is KmpTarget.Linux -> target.hosts.forEach { host ->
                    when (host) {
                        LinuxHost.LINUX_X64 -> linuxX64()
                        LinuxHost.LINUX_ARM64 -> linuxArm64()
                    }.configureExecutable(target.isApplication)
                }

                is KmpTarget.Mingw -> mingwX64().configureExecutable(target.isApplication)

                is KmpTarget.AndroidNative -> target.hosts.forEach { host ->
                    when (host) {
                        AndroidNativeHost.ANDROID_NATIVE_ARM64 -> androidNativeArm64()
                        AndroidNativeHost.ANDROID_NATIVE_X64 -> androidNativeX64()
                    }
                }

                is KmpTarget.Js -> js().configureWeb(target.environments, target.isApplication)

                is KmpTarget.WasmJs -> wasmJs().configureWeb(target.environments, target.isApplication)

                Common, Native, Web, Apple -> error(
                    "Hierarchy marker '${target.name}' is not a concrete KMP target",
                )
            }
        }
    }

    private fun KotlinNativeTarget.configureExecutable(isApplication: Boolean) {
        if (isApplication) binaries.executable()
    }

    private fun List<KotlinNativeTarget>.withFramework(
        framework: KmpTarget.Framework?,
        project: Project,
        xcFramework: XCFrameworkConfig?,
    ) {
        if (framework == null) return
        val exports = framework.export.map { project.dependencies.project(it) }
        forEach { target ->
            if (exports.isNotEmpty()) {
                target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).defaultSourceSet.dependencies {
                    exports.forEach { api(it) }
                }
            }
            target.binaries.framework {
                baseName = framework.baseName
                isStatic = framework.isStatic
                exports.forEach { export(it) }
                freeCompilerArgs += framework.freeCompilerArgs
                if (buildType == NativeBuildType.RELEASE) {
                    freeCompilerArgs += framework.releaseFreeCompilerArgs
                }
                xcFramework?.add(this)
            }
        }
    }

    private fun KotlinJsTargetDsl.configureWeb(environments: Set<JsEnv>, isApplication: Boolean) {
        if (JsEnv.BROWSER in environments) browser()
        if (JsEnv.NODE_JS in environments) nodejs()
        if (isApplication) binaries.executable()
    }
}
