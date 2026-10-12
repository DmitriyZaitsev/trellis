package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.Module

@TrellisDslMarker
public class ModuleGroupBuilder internal constructor(private val prefix: ProjectPath) : ModuleContainerDsl() {
    private val modules = mutableListOf<ModuleDeclaration>()

    override fun custom(module: Module) {
        require(module !is Module.Root) {
            "Root module '${module.name}' cannot be declared inside group '$prefix'"
        }
        modules +=
            ModuleDeclaration(
                path = prefix.child(module.name),
                module = module,
            )
    }

    public fun group(name: String, block: ModuleGroupBuilder.() -> Unit) {
        modules += ModuleGroupBuilder(prefix.child(name)).apply(block).build()
    }

    internal fun build(): List<ModuleDeclaration> = modules.toList()
}
