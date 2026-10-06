package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.Module

@ModuleDslMarker
public class ModuleGroupScope internal constructor(
    private val builder: ModuleGroupBuilder,
) : ModuleContainerDsl() {
    override fun custom(module: Module) {
        builder.custom(module)
    }

    public fun group(
        name: String,
        block: ModuleGroupScope.() -> Unit,
    ) {
        builder.group(name) {
            ModuleGroupScope(this).block()
        }
    }
}
