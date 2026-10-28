package com.dzaitsev.trellis.configurator

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.PluginIds
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import com.dzaitsev.trellis.plugin.requireAndroidSdkLevels
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configures the Android target of a KMP module — AGP 9's `com.android.kotlin.multiplatform.library`
 * registers it directly on [KotlinMultiplatformExtension] as [KotlinMultiplatformAndroidLibraryTarget],
 * rather than via a separate `com.android.library`/`com.android.application` extension.
 *
 * Host tests are always registered (they cost nothing without sources). Android resources, off by
 * default in AGP, are enabled for Compose modules, which need them for Compose resources.
 */
public class KmpAndroidTargetConfigurator : Configurator<TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        if (module !is Module.KmpLibrary) return
        val target = module.targets.filterIsInstance<KmpTarget.Android>().firstOrNull() ?: return
        val sdk = extension.requireAndroidSdkLevels(project.path)

        project.pluginManager.withPlugin(PluginIds.ANDROID_KMP_LIBRARY) {
            project.pluginManager.withPlugin(PluginIds.KOTLIN_MULTIPLATFORM) {
                val kmpExt = project.extensions.getByType<KotlinMultiplatformExtension>()
                (kmpExt as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                    namespace = target.namespace
                    compileSdk = sdk.targetSdk
                    minSdk = sdk.minSdk
                    if (module.usesCompose) {
                        androidResources.enable = true
                    }
                    withHostTest {
                        isIncludeAndroidResources = module.usesCompose
                    }
                }
            }
        }
    }
}
