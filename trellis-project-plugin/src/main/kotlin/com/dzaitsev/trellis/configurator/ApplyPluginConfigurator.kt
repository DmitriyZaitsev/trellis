package com.dzaitsev.trellis.configurator

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.Module.AndroidApp
import com.dzaitsev.trellis.Module.AndroidLibrary
import com.dzaitsev.trellis.Module.Java
import com.dzaitsev.trellis.Module.KmpLibrary
import com.dzaitsev.trellis.Module.Root
import com.dzaitsev.trellis.PluginIds
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import org.gradle.api.Project

/**
 * Applies the Gradle plugin(s) a declared [Module] kind implies, so a module's own
 * `build.gradle.kts` only needs a `dependencies { }` block — not a `plugins { }` block repeating
 * information the module registry already states:
 *
 * | Declared kind | Applied plugin(s) |
 * |---|---|
 * | [Module.AndroidApp] | `com.android.application`, plus `org.jetbrains.kotlin.android` when AGP built-in Kotlin is disabled |
 * | [Module.AndroidLibrary] | `com.android.library`, plus `org.jetbrains.kotlin.android` when AGP built-in Kotlin is disabled |
 * | [Module.Java] | `org.jetbrains.kotlin.jvm`, plus `application` when [Module.Java.mainClass] is set |
 * | [Module.KmpLibrary] | `org.jetbrains.kotlin.multiplatform`, plus `com.android.kotlin.multiplatform.library` when it declares a [KmpTarget.Android] target |
 * | [Module.Root] | none |
 *
 * [Module.Java] and [Module.KmpLibrary] declared with `usesCompose = true` also get
 * `org.jetbrains.kotlin.plugin.compose` and `org.jetbrains.compose` for resources and desktop packaging.
 *
 * A module's own `build.gradle.kts` may still declare these plugins explicitly (e.g. mid-migration):
 * applying an already-applied plugin is a documented Gradle no-op, so this is redundant but not
 * broken. Plugins genuinely specific to one module (serialization, parcelize,
 * feature-flag plugins, etc.) are not this library's concern and stay in that module's own
 * `plugins { }` block.
 *
 * Runs first among [defaultProjectModuleConfigurators] so every other built-in [Configurator]'s
 * `pluginManager.withPlugin(...)` guard fires immediately rather than waiting on the module's own
 * script to (redundantly) re-declare the plugin.
 */
public class ApplyPluginConfigurator : Configurator<Module, TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        val pluginManager = project.pluginManager
        when (module) {
            is AndroidApp -> {
                pluginManager.apply(PluginIds.ANDROID_APPLICATION)
                applyKotlinAndroidUnlessBuiltIn(project)
            }

            is AndroidLibrary -> {
                pluginManager.apply(PluginIds.ANDROID_LIBRARY)
                applyKotlinAndroidUnlessBuiltIn(project)
            }

            is Java -> {
                pluginManager.apply(PluginIds.KOTLIN_JVM)
                if (module.usesCompose) {
                    pluginManager.apply(PluginIds.KOTLIN_COMPOSE)
                    pluginManager.apply(PluginIds.COMPOSE_MULTIPLATFORM)
                }
            }

            is KmpLibrary -> {
                pluginManager.apply(PluginIds.KOTLIN_MULTIPLATFORM)
                if (module.targets.any { it is KmpTarget.Android }) {
                    pluginManager.apply(PluginIds.ANDROID_KMP_LIBRARY)
                }
                if (module.usesCompose) {
                    pluginManager.apply(PluginIds.KOTLIN_COMPOSE)
                    pluginManager.apply(PluginIds.COMPOSE_MULTIPLATFORM)
                }
            }

            is Root -> Unit

            else -> Unit // consumer-added Module kinds: no built-in default
        }
    }

    // AGP 9 built-in Kotlin registers the `kotlin` extension itself and rejects kotlin-android;
    // with `android.builtInKotlin=false` the module still needs kotlin-android.
    private fun applyKotlinAndroidUnlessBuiltIn(project: Project) {
        if (project.extensions.findByName("kotlin") == null) {
            project.pluginManager.apply(PluginIds.KOTLIN_ANDROID)
        }
    }
}
