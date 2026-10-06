package com.dzaitsev.trellis.configurator

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.KmpTarget.Desktop.DesktopHost
import com.dzaitsev.trellis.KmpTarget.Ios.IosHost
import com.dzaitsev.trellis.KmpTarget.JsEnv
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.PluginIds
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

/**
 * Applies the default source-set hierarchy template and configures the declared [KmpTarget]s of
 * a [Module.KmpLibrary] — everything that's purely mechanical translation from the
 * declared targets to Kotlin Multiplatform Gradle DSL calls.
 *
 * The [KmpTarget.Android] case is deliberately a no-op here: that target is registered by the
 * `com.android.kotlin.multiplatform.library` plugin itself and configured by
 * [KmpAndroidTargetConfigurator].
 *
 * [KmpTarget.Js], [KmpTarget.Wasm] and [KmpTarget.Desktop] additionally register an executable
 * binary when declared with `isApplication = true` — Kotlin/JS, Kotlin/Wasm and Kotlin/Native
 * auto-detect the entry point from a top-level `fun main()` in that target's source set, so no
 * separate `mainClass` needs declaring here.
 *
 * A [KmpTarget.Ios] declared with a non-null [KmpTarget.Ios.framework] additionally registers and
 * configures an exported framework binary — see its KDoc for the export/`api` wiring this
 * configurator performs on its behalf.
 *
 * Skips modules with [Module.KmpLibrary.autoConfigureTargets] set to `false` entirely —
 * e.g. a module that hand-configures its own targets (a native framework export, custom binary
 * settings, etc.) in its own `build.gradle.kts`.
 *
 * Project-specific extras with no generic default (e.g. a shared test-module dependency added to
 * `commonTest`) are not this library's concern; add them via your own additional [Configurator].
 */
public class KotlinMultiplatformTargetsConfigurator : Configurator<Module, TrellisProjectExtension> {
    @OptIn(ExperimentalWasmDsl::class)
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        if (module !is Module.KmpLibrary || !module.autoConfigureTargets) return

        project.pluginManager.withPlugin(PluginIds.KOTLIN_MULTIPLATFORM) {
            project.extensions.configure<KotlinMultiplatformExtension> {
                applyDefaultHierarchyTemplate()

                module.targets.forEach { target ->
                    when (target) {
                        is KmpTarget.Jvm -> {
                            jvm()
                        }

                        is KmpTarget.Android -> {
                            // Registered by com.android.kotlin.multiplatform.library plugin,
                            // configured by KmpAndroidTargetConfigurator.
                        }

                        is KmpTarget.Ios -> {
                            val iosTargets = target.hosts.map { host ->
                                when (host) {
                                    IosHost.IOS_ARM64 -> iosArm64()
                                    IosHost.IOS_SIMULATOR_ARM64 -> iosSimulatorArm64()
                                    IosHost.IOS_X64 -> iosX64()
                                }
                            }

                            target.framework?.let { framework ->
                                iosTargets.forEach { iosTarget ->
                                    iosTarget.binaries.framework {
                                        baseName = framework.baseName
                                        isStatic = framework.isStatic
                                        framework.export.forEach { path -> export(project.dependencies.project(path)) }

                                        freeCompilerArgs += framework.freeCompilerArgs

                                        if (buildType == NativeBuildType.RELEASE) {
                                            freeCompilerArgs += framework.releaseFreeCompilerArgs
                                        }
                                    }
                                }

                                // A dependency exported from a Kotlin/Native framework must also
                                // be an `api` dependency of the exporting source set.
                                if (framework.export.isNotEmpty()) {
                                    sourceSets.getByName("iosMain").dependencies {
                                        framework.export.forEach { path -> api(project.dependencies.project(path)) }
                                    }
                                }
                            }
                        }

                        is KmpTarget.Js -> {
                            js {
                                if (JsEnv.BROWSER in target.environments) browser()
                                if (JsEnv.NODE_JS in target.environments) nodejs()
                                if (target.isApplication) binaries.executable()
                            }
                        }

                        is KmpTarget.Wasm -> {
                            wasmJs {
                                if (JsEnv.BROWSER in target.environments) browser()
                                if (JsEnv.NODE_JS in target.environments) nodejs()
                                if (target.isApplication) binaries.executable()
                            }
                        }

                        is KmpTarget.Desktop -> {
                            target.hosts.map { host ->
                                when (host) {
                                    DesktopHost.MACOS_ARM64 -> macosArm64()
                                    DesktopHost.LINUX_X64 -> linuxX64()
                                    DesktopHost.MINGW_X64 -> mingwX64()
                                }
                            }.forEach {
                                if (target.isApplication) it.binaries.executable()
                            }
                        }

                        else -> Unit // consumer-added KmpTarget kinds: no built-in default
                    }
                }
            }
        }
    }
}
