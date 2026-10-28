package com.dzaitsev.trellis.configurator.android

import com.android.build.api.dsl.ApplicationExtension
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.PluginIds
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import com.dzaitsev.trellis.plugin.requireAndroidSdkLevels
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Configures the `com.android.application` extension for a [Module.AndroidApp].
 *
 * Runs before the module's own script, so values set here (e.g. release `isMinifyEnabled = true`,
 * `buildConfig = false`) are defaults that the module's `android { }` block can override.
 */
public class AndroidAppConfigurator : Configurator<TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        if (module !is Module.AndroidApp) return
        val sdk = extension.requireAndroidSdkLevels(project.path)

        project.pluginManager.withPlugin(PluginIds.ANDROID_APPLICATION) {
            project.extensions.configure<ApplicationExtension> {
                configureCommonAndroid(
                    module.namespace,
                    module.usesCompose,
                    module.flavors,
                    sdk.minSdk,
                    sdk.targetSdk,
                    extension.javaVersion.orNull,
                )

                defaultConfig.targetSdk = sdk.targetSdk

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
                    module.flavors.forEach { flavor ->
                        create(flavor.name) {
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
