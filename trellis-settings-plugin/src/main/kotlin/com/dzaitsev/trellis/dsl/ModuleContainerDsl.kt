package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.Module.AndroidApp
import com.dzaitsev.trellis.Module.AndroidLibrary
import com.dzaitsev.trellis.Module.Java
import com.dzaitsev.trellis.Module.KmpLibrary
import com.dzaitsev.trellis.Module.Root
import com.dzaitsev.trellis.android.AndroidAttrs.Native
import com.dzaitsev.trellis.android.Flavor

@TrellisDslMarker
public abstract class ModuleContainerDsl {
    public abstract fun custom(module: Module)

    // Module-level DSL API.

    public fun kmp(
        name: String,
        hasExplicitApi: Boolean = false,
        autoConfigureTargets: Boolean = true,
        usesCompose: Boolean = false,
        targets: KmpTargetsScope.() -> Set<KmpTarget>,
    ) {
        custom(
            KmpLibrary(
                name = name,
                targets = KmpTargetsScope.run(targets),
                hasExplicitApi = hasExplicitApi,
                autoConfigureTargets = autoConfigureTargets,
                usesCompose = usesCompose,
            ),
        )
    }

    public fun root(
        name: String,
        hasExplicitApi: Boolean = false,
    ) {
        custom(
            Root(
                name = name,
                hasExplicitApi = hasExplicitApi,
            ),
        )
    }

    public fun androidApp(
        name: String,
        attrs: Native,
        hasExplicitApi: Boolean = false,
    ) {
        custom(
            AndroidApp(
                name = name,
                androidAttrs = attrs,
                hasExplicitApi = hasExplicitApi,
            ),
        )
    }

    public fun androidApp(
        name: String,
        namespace: String,
        usesCompose: Boolean = false,
        flavors: List<Flavor> = emptyList(),
        hasExplicitApi: Boolean = false,
    ) {
        androidApp(name, Native(namespace, usesCompose, flavors), hasExplicitApi)
    }

    public fun androidLibrary(
        name: String,
        namespace: String,
        usesCompose: Boolean = false,
        flavors: List<Flavor> = emptyList(),
        hasExplicitApi: Boolean = false,
    ) {
        androidLibrary(name, Native(namespace, usesCompose, flavors), hasExplicitApi)
    }

    public fun androidLibrary(
        name: String,
        attrs: Native,
        hasExplicitApi: Boolean = false,
    ) {
        custom(
            AndroidLibrary(
                name = name,
                androidAttrs = attrs,
                hasExplicitApi = hasExplicitApi,
            ),
        )
    }

    public fun java(
        name: String,
        group: String? = null,
        version: String? = null,
        mainClass: String? = null,
        hasExplicitApi: Boolean = false,
        usesCompose: Boolean = false,
    ) {
        custom(
            Java(
                name = name,
                group = group,
                version = version,
                mainClass = mainClass,
                hasExplicitApi = hasExplicitApi,
                usesCompose = usesCompose,
            ),
        )
    }
}
