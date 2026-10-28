package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.configurator.defaultProjectModuleConfigurators
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import kotlin.reflect.KClass

/**
 * Base for a plugin applied to the root project that configures every declared module in the
 * build (via `allprojects`), with its settings in the `trellis { }` extension of type [E].
 *
 * [configurators] run once the root project's own script has evaluated (so values it assigns to the
 * extension are readable) and before any subproject's script. [lateConfigurators] run in each
 * project's `afterEvaluate`, after its own script. [rootConfigurators] run once, on the root project.
 * Projects without a module declaration are left untouched.
 *
 * ```
 * class AcmeConventionsPlugin : TrellisProjectPlugin<AcmeExtension>(AcmeExtension::class) {
 *     override fun configurators() = super.configurators() + DetektConfigurator()
 * }
 * ```
 */
public abstract class TrellisProjectPlugin<E : TrellisProjectExtension>(private val extensionType: KClass<E>) : Plugin<Project> {
    /** Applied in order to every declared module; defaults to the built-in configurators. */
    protected open fun configurators(): List<Configurator<E>> = defaultProjectModuleConfigurators()

    /** Applied in order to every declared module, inside its own `afterEvaluate`. */
    protected open fun lateConfigurators(): List<Configurator<E>> = emptyList()

    /** Applied once, to the root project, which must then be declared with `root(name)`. */
    protected open fun rootConfigurators(): List<Configurator<E>> = emptyList()

    final override fun apply(project: Project) {
        val extension = project.extensions.create(TRELLIS_EXTENSION_NAME, extensionType.java)
        project.extensions
            .findByType<ModuleRegistryHolder>()
            ?.projectConventions
            ?.let(extension::useSettingsConventions)

        project.afterEvaluate {
            project.allprojects {
                val module = findModule() ?: return@allprojects
                configurators().forEach { it.configure(this, module, extension) }

                val runLate = { lateConfigurators().forEach { it.configure(this, module, extension) } }
                // We are already inside this project's afterEvaluate; registering another one would fail.
                if (this == project) runLate() else afterEvaluate { runLate() }
            }

            val rootConfigurators = rootConfigurators()
            if (rootConfigurators.isNotEmpty()) {
                val rootModule =
                    checkNotNull(project.findModule()) {
                        "No module declared for ${project.path}, which root configurators require"
                    }
                rootConfigurators.forEach { it.configure(project, rootModule, extension) }
            }
        }
    }
}
