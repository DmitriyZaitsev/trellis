package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.dsl.ModuleContainerDsl
import com.dzaitsev.trellis.dsl.ModuleGroupScope
import com.dzaitsev.trellis.dsl.ModuleRegistry
import com.dzaitsev.trellis.dsl.ModuleRegistryBuilder
import com.dzaitsev.trellis.dsl.ModuleRegistryOptions.MissingDirectoryPolicy
import com.dzaitsev.trellis.dsl.TrellisDslMarker

@TrellisDslMarker
public abstract class TrellisSettingsExtension : ModuleContainerDsl() {
    private val builder = ModuleRegistryBuilder()
    internal var conventions: ProjectConventions? = null

    public fun projectConventions(block: ProjectConventions.() -> Unit) {
        conventions = ProjectConventions().apply(block)
    }

    public var missingDirectoryPolicy: MissingDirectoryPolicy
        get() = builder.missingDirectoryPolicy
        set(value) {
            builder.missingDirectoryPolicy = value
        }

    /** Creates declared project directories that do not exist yet, instead of failing. */
    public fun createMissingDirectories() {
        builder.createMissingDirectories()
    }

    public fun group(name: String, block: ModuleGroupScope.() -> Unit) {
        markModuleDeclared()
        builder.group(name = name) {
            ModuleGroupScope(this).also { it.defaults = defaults }.block()
        }
    }

    override fun custom(module: Module) {
        builder.custom(module)
    }

    internal fun buildRegistry(): ModuleRegistry = builder.build()
}
