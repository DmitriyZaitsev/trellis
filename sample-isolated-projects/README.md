# Trellis sample: isolated projects

This Kotlin Multiplatform project showcases the Trellis Gradle plugin's **local conventions mode**, intended for Gradle Isolated Projects, with Android, iOS, web (JS and Wasm), desktop (JVM), and Ktor server applications.


## Shared sources

The module `src` directories and the entire `app/iosApp` directory are relative symlinks to
`sample-project-conventions`, the canonical copy. Edit shared sources there to update all three samples.
Gradle files, build scripts, Gradle build outputs and Gradle local settings remain local to each sample.
The iOS project, configuration and Xcode user settings are shared. Xcode may resolve the linked project to its
canonical location; its Gradle build phase uses `SRCROOT` to locate the sample build.

Clone the whole repository with symlink support. On Windows, enable Developer Mode and use Git's
`core.symlinks=true` option. For a standalone copy, dereference the links with
`cp -RL sample-isolated-projects /path/to/new-project` from the repository root.

## How this sample uses Trellis

Start with these files:

1. [settings.gradle.kts](./settings.gradle.kts) applies `com.dzaitsev.trellis` and declares the module structure in `trellis { }`. The `androidApp`, `kmp`, and `java` declarations describe each module and its targets. `group` and `defaults` share options across modules, and `createMissingDirectories()` creates missing module directories.
2. The same settings script defines `projectConventions { }`: Kotlin 2.4.21, Java 17, Android minimum SDK 24, and target SDK 37. These values are explicit here because the project's `libs` version catalog accessor is unavailable in settings. Keep the Kotlin convention aligned with the Kotlin plugin version in the [version catalog](./gradle/libs.versions.toml).
3. [build.gradle.kts](./build.gradle.kts) declares shared plugin aliases and `trellisLocal` with `apply false`, making plugin versions available without applying the local plugin to the root project.
4. Each module that has a build script applies `alias(libs.plugins.trellisLocal)` (`com.dzaitsev.trellis.local`). It reads the declarations and conventions from settings and configures only that module before the rest of its script runs. The [Android build script](./app/androidApp/build.gradle.kts) then adds dependencies and packaging options; the [server build script](./server/build.gradle.kts) also applies the Ktor plugin.

This per-module configuration is the key pattern demonstrated here. For the alternative that configures modules centrally from the root build script, see the [project conventions sample](../sample-project-conventions/README.md).

The settings DSL demonstrates several reusable conventions:

- All modules in the `app` group enable Compose through `defaults { usesCompose = true }`.
- The `shared` group enables explicit API mode for its library modules.
- The local `sharedTargets` helper gives `shared:ui`, `shared:platform`, and `shared:greeting` the same Android, iOS, JVM, JS, and Wasm targets, with a distinct Android namespace for each.
- The `:shared` module builds the iOS `Shared` framework and exports the three shared libraries to Swift.
- The server declaration sets its group, version, and main class in settings.

## Enabling Gradle Isolated Projects

The [Gradle properties](./gradle.properties) enable configuration caching and build caching, but currently do not enable Isolated Projects. To opt in for an invocation, run a task with the isolation property, for example:

```shell
./gradlew :server:test -Dorg.gradle.unsafe.isolated-projects=true
```

To enable it persistently, add `org.gradle.unsafe.isolated-projects=true` to `gradle.properties`. The commands below use the default configuration unless you also pass that property.

## Project layout

| Path | Purpose |
| --- | --- |
| [app/androidApp](./app/androidApp) | Android application using the shared Compose UI. |
| [app/desktopApp](./app/desktopApp) | JVM desktop application, including Compose desktop packaging configuration. |
| [app/webApp](./app/webApp) | JS and Wasm browser applications, with their common entry point in [webMain](./app/webApp/src/webMain). |
| [app/iosApp](./app/iosApp) | SwiftUI host application and [Xcode project](./app/iosApp/iosApp.xcodeproj). Its build phase invokes Gradle to embed the `Shared` framework. |
| [shared](./shared) (`:shared`) | iOS framework umbrella module, with a minimal [common source file](./shared/src/commonMain/kotlin/Shared.kt). |
| [shared/ui](./shared/ui) | Shared Compose UI in [commonMain](./shared/ui/src/commonMain), plus an [iOS view controller](./shared/ui/src/iosMain/kotlin/com/dzaitsev/trellissample/MainViewController.kt). |
| [shared/platform](./shared/platform) | Common platform API and Android, iOS, JVM, JS, and Wasm implementations under [src](./shared/platform/src). |
| [shared/greeting](./shared/greeting) | Shared greeting logic in [commonMain](./shared/greeting/src/commonMain/kotlin), depending on the platform library. |
| [server](./server) | Ktor application with [server sources](./server/src/main/kotlin) and [tests](./server/src/test/kotlin). |

## Running the apps

Use the included Gradle wrapper (`gradlew.bat` on Windows). The configured Java version is 17; Android builds also require an Android SDK, and iOS builds require macOS and Xcode. Java and Android SDK conventions are in [settings.gradle.kts](./settings.gradle.kts); dependency and plugin versions are in the [version catalog](./gradle/libs.versions.toml).

| Application | Command |
| --- | --- |
| Android: build a debug APK | `./gradlew :app:androidApp:assembleDebug` |
| Desktop | `./gradlew :app:desktopApp:run` |
| Server (port 8080) | `./gradlew :server:run` |
| Web: Wasm | `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun` |
| Web: JS | `./gradlew :app:webApp:jsBrowserDevelopmentRun` |

For iOS, open [iosApp.xcodeproj](./app/iosApp/iosApp.xcodeproj) in Xcode, select a simulator or device, and run the app. The Xcode build phase calls `:shared:embedAndSignAppleFrameworkForXcode`.

## Running tests

Run the existing [Ktor server tests](./server/src/test/kotlin/com/dzaitsev/trellissample/ApplicationTest.kt) with:

```shell
./gradlew :server:test
```

The shared libraries configure `kotlin.test` for `commonTest`, but currently contain no test sources. When adding shared tests, place them under the relevant module's `src/commonTest/kotlin` directory. For example, JVM tests in `shared:greeting` run with `./gradlew :shared:greeting:jvmTest`. Use `./gradlew :shared:greeting:tasks --all` to inspect the available tasks for its other targets.
