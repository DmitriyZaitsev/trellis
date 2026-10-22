package com.dzaitsev.trellis.configurator.android

import com.android.build.api.dsl.LibraryExtension
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.PluginIds
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import com.dzaitsev.trellis.plugin.requireAndroidSdkLevels
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Configures the `com.android.library` extension for a [Module.AndroidLibrary].
 *
 * Runs before the module's own script, so values set here are defaults that the module's
 * `android { }` block can override.
 */
public class AndroidLibraryConfigurator : Configurator<Module, TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        if (module !is Module.AndroidLibrary) return
        val sdk = extension.requireAndroidSdkLevels(project.path)

        project.pluginManager.withPlugin(PluginIds.ANDROID_LIBRARY) {
            project.extensions.configure<LibraryExtension> {
                configureCommonAndroid(
                    module.namespace,
                    module.usesCompose,
                    module.flavors,
                    sdk.minSdk,
                    sdk.targetSdk,
                    extension.javaVersion.orNull,
                )

                val consumerRules = project.file("consumer-rules.pro")
                if (consumerRules.exists()) {
                    defaultConfig.consumerProguardFiles(consumerRules)
                }

                productFlavors {
                    module.flavors.forEach { flavor ->
                        create(flavor.name) {
                            dimension = flavor.dimension
                            isDefault = flavor.isDefault
                        }
                    }
                }
            }
        }
    }
}
