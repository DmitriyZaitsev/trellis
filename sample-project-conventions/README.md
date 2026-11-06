# Trellis sample: project conventions

This Kotlin Multiplatform project showcases the Trellis Gradle plugin's **root conventions mode** with Android, iOS, web (JS and Wasm), desktop (JVM), and Ktor server applications.

## How this sample uses Trellis

Start with these three files:

1. [settings.gradle.kts](./settings.gradle.kts) applies `com.dzaitsev.trellis` and declares the module structure in `trellis { }`. The `androidApp`, `kmp`, and `java` declarations describe each module and its targets. `group` and `defaults` share options across modules, and `createMissingDirectories()` creates missing module directories.
2. [build.gradle.kts](./build.gradle.kts) applies `com.dzaitsev.trellis.conventions`. In this mode, Trellis applies the required plugins and configures the declared modules centrally, before their build scripts run. Its `trellis { }` block sets the Kotlin version, Java version, and Android SDK levels from the [version catalog](./gradle/libs.versions.toml). The root also declares shared plugin aliases with `apply false` to avoid loading them separately in each subproject's classloader.
3. Module build scripts add dependencies and module-specific configuration. For example, the [Android build script](./app/androidApp/build.gradle.kts) adds UI dependencies and Android packaging options without a `plugins` block. The [server build script](./server/build.gradle.kts) adds the Ktor plugin explicitly alongside the base configuration supplied by Trellis.

The settings DSL demonstrates several reusable conventions:

- All modules in the `app` group enable Compose through `defaults { usesCompose = true }`.
- The `shared` group enables explicit API mode for its library modules.
- The local `sharedTargets` helper gives `shared:ui`, `shared:platform`, and `shared:greeting` the same Android, iOS, JVM, JS, and Wasm targets, with a distinct Android namespace for each.
- The `:shared` module builds the iOS `Shared` framework and exports the three shared libraries to Swift.
- The server declaration sets its group, version, and main class in settings.

Project conventions can alternatively be set with `projectConventions { }` inside the settings `trellis` block; the commented example there shows the syntax. This sample uses the root build script so it can read the version catalog. Values set in the root build script take precedence over settings-based conventions.

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

Use the included Gradle wrapper (`gradlew.bat` on Windows). The configured Java version is 17; Android builds also require an Android SDK, and iOS builds require macOS and Xcode. Version and SDK settings are in the [version catalog](./gradle/libs.versions.toml).

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
