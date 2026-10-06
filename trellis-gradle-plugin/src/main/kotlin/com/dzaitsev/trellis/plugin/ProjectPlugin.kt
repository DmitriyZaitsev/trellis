package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.ModuleModel
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.reflect.KClass

/**
 * Reusable base for a Gradle plugin that walks every project in the build (via `allprojects`),
 * resolves each project's module of type [M], and runs [immediateConfigurators] once the root
 * project's own `build.gradle.kts` has finished evaluating and [lateConfigurators] inside a
 * per-project `afterEvaluate`, once that project's own `build.gradle.kts` has finished evaluating
 * (needed for anything that touches AGP/Kotlin extensions or dependency configurations those
 * plugins register).
 *
 * [immediateConfigurators] deliberately waits for the *root* project's own script to finish (via
 * `project.afterEvaluate`) rather than running synchronously inside `apply()`: this plugin is
 * applied from the root's own `plugins { }` block, which Gradle processes before the rest of that
 * script runs — so anything in [extensionType] the root script sets afterwards (e.g. `javaVersion`,
 * `androidMinSdk`) would otherwise not be readable yet. Gradle evaluates the root project fully,
 * including firing its own `afterEvaluate` listeners, before it begins evaluating any subproject's
 * own script — so this wait still keeps [immediateConfigurators] running strictly before every
 * subproject's own `build.gradle.kts`, preserving "immediate" semantics for module code, while
 * fixing extension-value ordering for root project code.
 *
 * [rootConfigurators] run exactly once, against the project the plugin was applied to — useful
 * for registering aggregate tasks (e.g. "clean all", "run all lint") that only make sense once per
 * build rather than once per module.
 *
 * Subclasses own the shape of their own extension type [E], their module type [M], and the
 * [Configurator]s they ship; this base class only owns module resolution and ordering.
 */
public abstract class ProjectPlugin<M : ModuleModel, E : ProjectExtension>(
    private val extensionName: String,
    private val extensionType: KClass<E>,
) : Plugin<Project> {

    /**
     * Resolves the module of type [M] declared for [project], e.g. via [findModule]. Return `null`
     * for projects that should be left untouched (implicit intermediate projects such as
     * `:feature` for `:feature:auth`, or projects included without a registry declaration).
     */
    protected abstract fun moduleOf(project: Project): M?

    /** Ordered configurators applied to every project in the build, once the root project's own script has evaluated. */
    protected abstract fun immediateConfigurators(module: M, extension: E): List<Configurator<M, E>>

    /** Ordered configurators applied to every project in the build, inside that project's own `afterEvaluate`. */
    protected open fun lateConfigurators(module: M, extension: E): List<Configurator<M, E>> = emptyList()

    /** Ordered configurators applied exactly once, against the project the plugin was applied to. */
    protected open fun rootConfigurators(extension: E): List<Configurator<M, E>> = emptyList()

    final override fun apply(project: Project) {
        val extension = project.extensions.create(extensionName, extensionType.java)

        project.afterEvaluate {
            project.allprojects {
                val module = moduleOf(this) ?: return@allprojects
                immediateConfigurators(module, extension).forEach { it.configure(this, module, extension) }

                val runLate = { lateConfigurators(module, extension).forEach { it.configure(this, module, extension) } }
                // We are already inside this project's afterEvaluate; registering another one would fail.
                if (this == project) runLate() else afterEvaluate { runLate() }
            }

            val rootConfigurators = rootConfigurators(extension)
            if (rootConfigurators.isNotEmpty()) {
                val rootModule = checkNotNull(moduleOf(project)) {
                    "No module declared for ${project.path}, which root configurators require"
                }
                rootConfigurators.forEach { it.configure(project, rootModule, extension) }
            }
        }
    }
}
