package com.dzaitsev.trellis.configurator.android

import com.android.build.api.dsl.ApplicationExtension
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.PluginIds
import com.dzaitsev.trellis.android.AndroidAttrs
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Configures the `com.android.application` extension for a [Module.AndroidApp].
 *
 * Runs before the module's own script, so values set here (e.g. release `isMinifyEnabled = true`,
 * `buildConfig = false`) are defaults that the module's `android { }` block can override.
 */
public class AndroidAppConfigurator(private val androidAttrs: AndroidAttrs.Native) : Configurator<Module, TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        project.pluginManager.withPlugin(PluginIds.ANDROID_APPLICATION) {
            project.extensions.configure<ApplicationExtension> {
                configureCommonAndroid(project, androidAttrs, extension)

                defaultConfig.targetSdk = extension.androidTargetSdk.get()

                buildTypes.apply {
                    getByName("release") {
                        isMinifyEnabled = true
                        isDebuggable = false
                        isJniDebuggable = false
                    }
                    getByName("debug") {
                        isMinifyEnabled = false
                        isDebuggable = true
                        isJniDebuggable = true
                    }
                }

                productFlavors {
                    androidAttrs.flavors.forEach { flavor ->
                        create(flavor.flavorName) {
                            dimension = flavor.dimension
                            isDefault = flavor.isDefault
                            applicationIdSuffix = flavor.applicationIdSuffix
                        }
                    }
                }
            }
        }
    }
}
