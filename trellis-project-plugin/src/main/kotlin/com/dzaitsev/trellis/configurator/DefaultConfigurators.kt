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
 * Call this from your own [com.dzaitsev.trellis.plugin.TrellisProjectPlugin] or
 * [com.dzaitsev.trellis.plugin.TrellisLocalProjectPlugin] subclass when you replace its
 * `configurators()` outright; the default implementation already returns this list.
 */
public fun defaultProjectModuleConfigurators(): List<Configurator<TrellisProjectExtension>> = listOf(
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
