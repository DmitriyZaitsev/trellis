---
title: Trellis
description: Settings module declarations and optional project conventions for Gradle
---

## Artifacts

Trellis declares modules once in settings. `trellis-settings` contains the
model, DSL, registry and settings plugin, without Kotlin or Android plugin
dependencies. `trellis-gradle-plugin` contains project configurators.
Both use group `com.dzaitsev.trellis` and version `0.1.0`.

The artifacts are currently available through local publishing, not a verified
public repository. Add `mavenLocal()` to `pluginManagement.repositories` after
running `./gradlew build publishToMavenLocal` here.

## Root conventions

In your settings script:

```kotlin
pluginManagement {
    repositories {
        mavenLocal(); google(); mavenCentral(); gradlePluginPortal()
    }
}
plugins { id("com.dzaitsev.trellis") version "0.1.0" }

trellis {
    createMissingDirectories()
    java("service", mainClass = "example.MainKt")
    kmp("shared", jvm + android("example.shared"))
    androidApp("app", namespace = "example.app", usesCompose = true)
}
```

In the root build script:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.20" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.4.20" apply false
    id("com.android.application") version "9.3.3" apply false
    id("com.android.kotlin.multiplatform.library") version "9.3.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("com.dzaitsev.trellis.conventions") version "0.1.0"
}

trellis {
    kotlinVersion = "2.4.20"
    javaVersion = 17
    androidMinSdk = 24
    androidTargetSdk = 37
}
```

The existing `ProjectPlugin`, `ProjectExtension` and root `trellis {}` API remain
available for custom conventions. Root mode uses `allprojects` and is not
compatible with Isolated Projects. Immediate conventions run before subproject
script bodies; module scripts can override scalar Kotlin, Java and Android values.
Declared targets and host tests are additive; opt out in the settings declaration.

`usesCompose = true` on JVM or KMP modules applies both the Compose compiler and
Compose Multiplatform plugins. Supply `org.jetbrains.compose` version `1.12.1`
on the project classpath with `apply false` for those modules. Native Android
modules apply only the compiler plugin. Runtime/UI dependencies remain yours.

## Local conventions and Isolated Projects

Keep the settings plugin and module declarations, and add:

```kotlin
trellis {
    projectConventions {
        kotlinVersion = "2.4.20"
        javaVersion = 17
        androidMinSdk = 24
        androidTargetSdk = 37
        useJavaToolchain = false
    }
}
```

Replace the root conventions plugin with:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.20" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.4.20" apply false
    id("com.dzaitsev.trellis.local") version "0.1.0" apply false
}
```

Apply this in each declared project's build script:

```kotlin
plugins { id("com.dzaitsev.trellis.local") }
```

Keep any versioned Android/Compose plugins needed by those modules on the
project classpath too. The settings plugin transports plain data through
`gradle.lifecycle.beforeProject`; the local plugin configures only its own project
before the script body. Do not combine local and root conventions for one module.

```sh
./gradlew :service:compileKotlin --configuration-cache \
  -Dorg.gradle.unsafe.isolated-projects=true
```

JVM and JVM-only KMP compilation plus configuration-cache reuse are tested with
Isolated Projects. JS/Node infrastructure in KGP 2.4.20 still accesses other
projects, so JS-containing builds are not covered by this isolation guarantee.
Android and Compose are tested in root mode, not certified for isolation.
Use an init-script-free Gradle user home for isolation checks.

`Project.findModule()` and `Project.moduleRegistry()` provide local registry
access and path-based lookup, including nested groups. Undeclared projects return
`null` from `findModule()`. The legacy `Gradle.moduleRegistry()` accessor remains
available, but is not the isolated-project access path. Host settings may remap
included project directories within the settings directory using `projectDir`.

## Versions and test gates

| Component | Compile/build version | Tested consumer version |
| --- | --- | --- |
| Gradle | 9.3.0 wrapper | 9.3.0 JVM/KMP; 9.8.0 Android |
| Kotlin Gradle plugin | 2.4.20 compile-only API | 2.4.20 |
| Android Gradle plugin | 9.4.1 compile-only API | 9.3.3 |
| Compose Multiplatform | Consumer supplied | 1.12.1 |
| JVM bytecode/toolchain | 17 | 17; module override to 11 |

These are tested combinations, not a claim that every intervening version is
supported. Kotlin DSL compilation uses Gradle's embedded Kotlin plugin. Android
tests resolve SDK paths from local `local.properties` or `ANDROID_HOME` and need
SDK platform 37, including its `android-37.0` directory layout. Without it they
report an explicit skip. CI should require it:

```sh
./gradlew ciCheck -Ptrellis.requireAndroidSdk=true --console=plain
./gradlew build publishToMavenLocal --console=plain
```

`ciCheck` includes unit tests, Maven-backed TestKit tests, plugin validation and
ABI checks for both artifacts. TestKit resolves settings and project artifacts
from `build/test-repo` to exercise their real classloader boundary. Isolation
fixtures use a separate Gradle user home rather than changing global init scripts.
No CI provider or remote pipeline is assumed; invoke this gate from your runner.

## Binary API and publishing preparation

Kotlin's built-in ABI validator checks the committed `api/*.api` baselines through
`checkLegacyAbi`, wired to `check` and `ciCheck`. Update baselines only after
reviewing intentional public API changes, in a separate invocation:

```sh
./gradlew updateLegacyAbi
./gradlew checkLegacyAbi
```

The initial baselines protect subsequent changes; they do not prove compatibility
with previously released artifacts. `com.gradle.plugin-publish` is pinned to
`2.2.1`. Local publications include plugin markers, sources and Javadoc artifacts.
The confirmed website is <https://dzaitsev.com> and the public repository is
<https://github.com/DmitriyZaitsev/trellis>. Maven developer metadata uses the
repository owner handle `DmitriyZaitsev`; personal release authorization is confirmed.
The Plugin Portal publisher account and namespace approval must be verified before
the first public release. No private remote URLs are embedded in publication metadata.
`publishPlugins` requires the configured `trellis.website`, `trellis.vcsUrl` and
an explicit `trellis.allowPublicPublish=true`. No public publish is part of the
local or CI gates.

## License

Trellis is licensed under the [Apache License, Version 2.0](LICENSE)
(SPDX: `Apache-2.0`). Copyright 2026 Dmytro Zaitsev.
Published binary, source and Javadoc JARs include [LICENSE](LICENSE) and
[NOTICE](NOTICE) under `META-INF`. Maven publications include matching license
and developer metadata.
