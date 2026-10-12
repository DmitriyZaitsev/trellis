package com.dzaitsev.trellis.configurator

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.PluginIds
import com.dzaitsev.trellis.android.AndroidAttrs
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configures the Android target of a KMP module — AGP 9's `com.android.kotlin.multiplatform.library`
 * registers it directly on [KotlinMultiplatformExtension] as [KotlinMultiplatformAndroidLibraryTarget],
 * rather than via a separate `com.android.library`/`com.android.application` extension.
 */
public class KmpAndroidTargetConfigurator(private val androidAttrs: AndroidAttrs.Kmp) : Configurator<Module, TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        project.pluginManager.withPlugin(PluginIds.ANDROID_KMP_LIBRARY) {
            project.pluginManager.withPlugin(PluginIds.KOTLIN_MULTIPLATFORM) {
                val kmpExt = project.extensions.getByType<KotlinMultiplatformExtension>()
                (kmpExt as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                    namespace = androidAttrs.namespace
                    compileSdk = extension.androidTargetSdk.get()
                    minSdk = extension.androidMinSdk.get()
                    if (androidAttrs.enableResources) {
                        androidResources.enable = true
                    }
                    if (androidAttrs.hostTests) {
                        withHostTest {
                            isIncludeAndroidResources = androidAttrs.enableResources
                        }
                    }
                }
            }
        }
    }
}
