package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.configurator.defaultProjectModuleConfigurators
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import kotlin.reflect.KClass

/**
 * Base for a plugin applied to each declared module (`plugins { id("...") }` in its own build
 * script) that configures only that project, which keeps the build compatible with Isolated
 * Projects. Conventions come from settings: `trellis { projectConventions { } }`.
 *
 * Configurators run while the plugin is applied, before the rest of the module script, so read
 * extra [E] properties lazily (through providers) rather than with `get()`.
 *
 * ```
 * class AcmeLocalPlugin : TrellisLocalProjectPlugin<AcmeExtension>(AcmeExtension::class) {
 *     override fun configurators() = super.configurators() + DetektConfigurator()
 * }
 * ```
 */
public abstract class TrellisLocalProjectPlugin<E : TrellisProjectExtension>(private val extensionType: KClass<E>) : Plugin<Project> {
    /** Applied in order to this project; defaults to the built-in configurators. */
    protected open fun configurators(): List<Configurator<E>> = defaultProjectModuleConfigurators()

    final override fun apply(project: Project) {
        val conventions =
            requireNotNull(project.extensions.getByType<ModuleRegistryHolder>().projectConventions) {
                "Declare trellis { projectConventions { ... } } in settings before applying a local Trellis plugin"
            }
        val module = project.findModule() ?: return
        val extension = project.extensions.create(TRELLIS_EXTENSION_NAME, extensionType.java)
        extension.useSettingsConventions(conventions)
        configurators().forEach { it.configure(project, module, extension) }
    }
}
