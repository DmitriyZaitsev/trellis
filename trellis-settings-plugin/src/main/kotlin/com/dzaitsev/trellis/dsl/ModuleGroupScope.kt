package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.Module

@TrellisDslMarker
public class ModuleGroupScope internal constructor(private val builder: ModuleGroupBuilder) : ModuleContainerDsl() {
    override fun custom(module: Module) {
        builder.custom(module)
    }

    public fun group(name: String, block: ModuleGroupScope.() -> Unit) {
        markModuleDeclared()
        builder.group(name) {
            ModuleGroupScope(this).also { it.defaults = defaults }.block()
        }
    }
}
