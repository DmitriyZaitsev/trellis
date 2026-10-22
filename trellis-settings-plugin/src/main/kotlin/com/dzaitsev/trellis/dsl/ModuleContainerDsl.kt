package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.Module.AndroidApp
import com.dzaitsev.trellis.Module.AndroidLibrary
import com.dzaitsev.trellis.Module.Java
import com.dzaitsev.trellis.Module.KmpLibrary
import com.dzaitsev.trellis.Module.Root

/**
 * Module declarations. Parameters carry a module's identity (its name, and the namespace of
 * Android modules); every optional setting lives in the trailing block, which may be omitted.
 */
@TrellisDslMarker
public abstract class ModuleContainerDsl {
    public abstract fun custom(module: Module)

    private var moduleDeclared = false
    internal var defaults: List<ModuleOptionsScope.() -> Unit> = emptyList()

    /**
     * Options applied to every module declared after this call in this container and its groups,
     * before the module's own block, which can override them.
     */
    public fun defaults(block: ModuleOptionsScope.() -> Unit) {
        check(!moduleDeclared) { "Declare defaults { } before the modules and groups it applies to" }
        defaults = defaults + block
    }

    internal fun markModuleDeclared() {
        moduleDeclared = true
    }

    private fun <S : ModuleOptionsScope> S.configure(block: S.() -> Unit): S {
        markModuleDeclared()
        defaults.forEach { it() }
        return apply(block)
    }

    /** A Kotlin Multiplatform module; declare its targets and options in [block]. */
    public fun kmp(name: String, block: KmpModuleScope.() -> Unit) {
        val scope = KmpModuleScope().configure(block)
        custom(
            KmpLibrary(
                name = name,
                targets = scope.build(),
                hasExplicitApi = scope.hasExplicitApi,
                autoConfigureTargets = scope.autoConfigureTargets,
                usesCompose = scope.usesCompose,
            ),
        )
    }

    public fun root(name: String) {
        markModuleDeclared()
        custom(Root(name))
    }

    public fun androidApp(name: String, namespace: String, block: AndroidModuleScope.() -> Unit = {}) {
        val scope = AndroidModuleScope().configure(block)
        custom(
            AndroidApp(
                name = name,
                namespace = namespace,
                usesCompose = scope.usesCompose,
                flavors = scope.buildFlavors(),
                hasExplicitApi = scope.hasExplicitApi,
            ),
        )
    }

    public fun androidLibrary(name: String, namespace: String, block: AndroidModuleScope.() -> Unit = {}) {
        val scope = AndroidModuleScope().configure(block)
        custom(
            AndroidLibrary(
                name = name,
                namespace = namespace,
                usesCompose = scope.usesCompose,
                flavors = scope.buildFlavors(),
                hasExplicitApi = scope.hasExplicitApi,
            ),
        )
    }

    public fun java(name: String, block: JavaModuleScope.() -> Unit = {}) {
        val scope = JavaModuleScope().configure(block)
        custom(
            Java(
                name = name,
                group = scope.group,
                version = scope.version,
                mainClass = scope.mainClass,
                hasExplicitApi = scope.hasExplicitApi,
                usesCompose = scope.usesCompose,
            ),
        )
    }
}
