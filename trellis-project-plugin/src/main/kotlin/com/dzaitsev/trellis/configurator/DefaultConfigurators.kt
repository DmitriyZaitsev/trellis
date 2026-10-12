package com.dzaitsev.trellis.configurator

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.Module.AndroidApp
import com.dzaitsev.trellis.Module.AndroidLibrary
import com.dzaitsev.trellis.Module.Java
import com.dzaitsev.trellis.Module.KmpLibrary
import com.dzaitsev.trellis.configurator.android.AndroidAppConfigurator
import com.dzaitsev.trellis.configurator.android.AndroidLibraryConfigurator
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension

/**
 * The built-in [Configurator]s this library ships for a [Module], dispatched by module
 * kind: applies the Android/Kotlin/Java Gradle plugin(s) the declared kind implies
 * ([ApplyPluginConfigurator]), then configures `com.android.application`/`com.android.library`
 * DSL for [Module.AndroidApp]/[Module.AndroidLibrary], plain Java configuration for
 * [Module.Java], the Android target of a KMP module, and the declared
 * Kotlin Multiplatform targets of a [Module.KmpLibrary], then enables `explicitApi()` per
 * [Module.hasExplicitApi] ([ExplicitApiConfigurator]).
 *
 * Call this from your own [com.dzaitsev.trellis.plugin.TrellisProjectPlugin]
 * subclass's `immediateConfigurators` override and append your own project-specific
 * [Configurator]s (static analysis, publishing, shared test dependencies, etc.):
 * ```
 * override fun immediateConfigurators(module: Module, extension: MyExtension) =
 *     defaultProjectModuleConfigurators<MyExtension>(module) + listOf(
 *         MyOwnConfigurator(),
 *     )
 * ```
 */
public fun <E : TrellisProjectExtension> defaultProjectModuleConfigurators(module: Module): List<Configurator<Module, E>> = listOfNotNull(
    ApplyPluginConfigurator(),
    when (module) {
        is AndroidApp -> {
            AndroidAppConfigurator(module.androidAttrs)
        }

        is AndroidLibrary -> {
            AndroidLibraryConfigurator(module.androidAttrs)
        }

        is Java -> {
            JavaConfigurator()
        }

        is KmpLibrary -> {
            module.targets
                .filterIsInstance<KmpTarget.Android>()
                .firstOrNull()
                ?.let { KmpAndroidTargetConfigurator(it.androidAttrs) }
        }

        else -> {
            null
        }
    },
    KotlinMultiplatformTargetsConfigurator(),
    JvmTargetConfigurator(),
    ExplicitApiConfigurator(),
)
