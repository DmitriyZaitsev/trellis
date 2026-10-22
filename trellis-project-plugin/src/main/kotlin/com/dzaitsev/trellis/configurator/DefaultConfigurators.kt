package com.dzaitsev.trellis.configurator

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.configurator.android.AndroidAppConfigurator
import com.dzaitsev.trellis.configurator.android.AndroidLibraryConfigurator
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension

/**
 * The built-in [Configurator]s this library ships. Each one decides from the module whether it
 * applies: [ApplyPluginConfigurator] applies the plugins the declared kind implies, then the Android,
 * Java and Kotlin Multiplatform configurators set up their module kinds, and [ExplicitApiConfigurator]
 * enables `explicitApi()` per [Module.hasExplicitApi].
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
@Suppress("UNUSED_PARAMETER") // kept for source compatibility; the list no longer depends on the module
public fun <E : TrellisProjectExtension> defaultProjectModuleConfigurators(module: Module): List<Configurator<Module, E>> = listOf(
    ApplyPluginConfigurator(),
    AndroidAppConfigurator(),
    AndroidLibraryConfigurator(),
    JavaConfigurator(),
    KmpAndroidTargetConfigurator(),
    KotlinMultiplatformTargetsConfigurator(),
    KotlinVersionConfigurator(),
    JvmTargetConfigurator(),
    ExplicitApiConfigurator(),
)
