# Trellis sample: custom configurators

This sample extends [sample-project-conventions](../sample-project-conventions) with shared Detekt configuration. It
keeps the same Android, iOS, web, desktop and server applications, and replaces the built-in root conventions plugin
with a small custom plugin. Trellis still handles module inclusion, plugin application, targets and SDK settings.


## Shared sources

The module `src` directories and the entire `app/iosApp` directory are relative symlinks to
`sample-project-conventions`, the canonical copy. Edit shared sources there to update all three samples.
Gradle files, build scripts, Gradle build outputs and Gradle local settings remain local to each sample.
The iOS project, configuration and Xcode user settings are shared. Xcode may resolve the linked project to its
canonical location; its Gradle build phase uses `SRCROOT` to locate the sample build.

Clone the whole repository with symlink support. On Windows, enable Developer Mode and use Git's
`core.symlinks=true` option. For a standalone copy, dereference the links with
`cp -RL sample-extending-trellis /path/to/new-project` from the repository root.

## Start here

| File | What it demonstrates |
| --- | --- |
| [settings.gradle.kts](./settings.gradle.kts) | Includes `build-logic`, declares the mobile and multiplatform modules, and declares the root for aggregate task registration. |
| [build.gradle.kts](./build.gradle.kts) | Applies `com.example.trellis.conventions`, supplies shared plugin versions with `apply false`, and sets the Detekt config file. |
| [build-logic/build.gradle.kts](./build-logic/build.gradle.kts) | Registers the custom plugin and declares its Trellis and Detekt implementation dependencies. |
| [SampleExtension.kt](./build-logic/src/main/kotlin/com/example/buildlogic/SampleExtension.kt) | Adds a managed `detektConfig` property to Trellis's built-in conventions. |
| [SampleConventionsPlugin.kt](./build-logic/src/main/kotlin/com/example/buildlogic/SampleConventionsPlugin.kt) | Adds module and root configurators through separate hooks. |
| [DetektConfigurator.kt](./build-logic/src/main/kotlin/com/example/buildlogic/DetektConfigurator.kt) | Configures Detekt for source modules. |
| [RootDetektConfigurator.kt](./build-logic/src/main/kotlin/com/example/buildlogic/RootDetektConfigurator.kt) | Creates `runAllDetekt` once on the root project. |
| [config/detekt/detekt.yml](./config/detekt/detekt.yml) | Shares Detekt rule settings, including Compose naming exceptions. |

## How the extension works

The settings plugin is still `com.dzaitsev.trellis`. In the root build script, the custom
`com.example.trellis.conventions` plugin **replaces** `com.dzaitsev.trellis.conventions`; applying both would create two
`trellis` extensions. Keep the Kotlin, Android, Compose and Ktor plugin aliases in the root with `apply false`, just as
in the original sample.

The custom plugin extends `TrellisProjectPlugin<SampleExtension>`:

```kotlin
class SampleConventionsPlugin : TrellisProjectPlugin<SampleExtension>(SampleExtension::class) {
    override fun configurators() = super.configurators() + DetektConfigurator()

    override fun rootConfigurators() = super.rootConfigurators() + RootDetektConfigurator()
}
```

Calling `super.configurators()` preserves all built-in behavior. `DetektConfigurator` runs for each declared module
before its build script, skips `Module.Root`, and applies Detekt to source modules. It configures Detekt through
`withPlugin`, with lazy task configuration via `configureEach`. No module needs a Detekt plugin block.
`RootDetektConfigurator` runs once on the root project through `rootConfigurators()` and registers the aggregate task.
These hooks follow the [extension guide](../README.md#choosing-when-a-configurator-runs).

`SampleExtension` inherits Trellis's SDK and compiler settings and adds a `RegularFileProperty`. The root build script
sets it alongside those conventions:

```kotlin
trellis {
    // Existing compiler and SDK settings come from the version catalog.
    detektConfig = layout.projectDirectory.file("config/detekt/detekt.yml")
}
```

The configurator passes that property to Detekt's file collection without calling `get()` early. It enables all rules,
merges the shared YAML with Detekt defaults, enables parallel analysis, and requests XML, HTML, TXT and SARIF reports.

## Source selection and aggregation

The generic `detekt` task normally targets JVM-style source directories. This sample sets its source root to each
module's `src` directory so it also sees KMP `commonMain`, `androidMain`, `iosMain`, `webMain` and other platform sources.
It includes Kotlin files and excludes generated/build directories, test source sets and release-only source directories.
The YAML contains a few exceptions appropriate for the copied sample; findings still fail the analysis.

`RootDetektConfigurator` creates `runAllDetekt` through `rootConfigurators()`. Declare the root in settings as
required by that hook:

```kotlin
trellis {
    root(rootProject.name)
    // Source module declarations follow.
}
```

`runAllDetekt` depends on each module's generic `detekt` task. This deliberately uses source analysis without type
resolution, avoiding duplicate variant analysis and compilation of every platform. Android variant-specific Detekt tasks
are disabled; the generic task still analyzes Android sources. The aggregate task does not depend on test, release or
platform compilation tasks. It does not merge reports: each module writes its own reports.

Detekt is pinned to **1.23.8**, matching the [extension guide](../README.md#extending-trellis) and its
[Gradle API](https://detekt.dev/docs/1.23.8/gettingstarted/gradle/). Its embedded Kotlin analyzer has its own version;
source analysis here does not establish type-resolution compatibility with every Kotlin/AGP version in the sample.

## Run the example

Use the included wrapper with JDK 17. Full project configuration also requires the Android SDK described in the
[version catalog](./gradle/libs.versions.toml); Apple builds require macOS and Xcode. The first run resolves published
Trellis artifacts and the included build's dependencies.

From this directory:

```shell
# Compile the custom plugin separately.
./gradlew -p build-logic build

# Analyze production Kotlin sources across the declared modules.
./gradlew runAllDetekt

# Analyze just one shared mobile library.
./gradlew :shared:greeting:detekt

# Inspect aggregation without executing analysis.
./gradlew runAllDetekt --dry-run
```

Reports are written under each module's `build/reports/detekt/`. For example, open
`shared/greeting/build/reports/detekt/detekt.html` after running that module's analysis. Regular `check` tasks also retain
Detekt's normal verification wiring; `runAllDetekt` provides a focused analysis entry point.

The applications retain the original sample's commands, for example:

```shell
./gradlew :app:androidApp:assembleDebug
./gradlew :app:desktopApp:run
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun
./gradlew :server:run
```

See the [base sample's app layout and iOS instructions](../sample-project-conventions/README.md#project-layout) for the
unchanged source structure and Xcode workflow.

## Add another configurator

Implement `Configurator<SampleExtension>`, then append an instance in `configurators()` beside `DetektConfigurator()`.
Use `module` to select Android apps, KMP libraries or modules with a particular capability. Add managed properties to
`SampleExtension` when the behavior needs a project-wide setting. Use `rootConfigurators()` for aggregate tasks, as `RootDetektConfigurator` demonstrates.
Use `lateConfigurators()` only when configuration must read values assigned in a module's own build script.

This sample uses Trellis's root mode and accesses subprojects for aggregation. It is not an Isolated Projects example.
