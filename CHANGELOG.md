---
title: Changelog
description: Notable changes to Trellis, by release
---

All notable changes to Trellis are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and versions follow [Semantic Versioning](https://semver.org/).

## [0.1.0] - Unreleased

First public release.

### Added

* Settings plugin `com.dzaitsev.trellis`: declare modules once with `androidApp`, `androidLibrary`, `java`, `kmp` and
  `root`, nested with `group`, with shared options in `defaults { }`. Trellis includes every declared project and can
  create missing directories.
* Kotlin Multiplatform targets `jvm`, `android`, `ios`, `tvos`, `watchos`, `macos`, `linux`, `mingw`, `androidNative`,
  `desktop`, `js` and `wasmJs`, with selectable hosts and environments, executable binaries and Objective-C/Swift
  framework export from every Apple target, optionally bundled into one XCFramework with `xcframework = true`.
* Build-wide validation: duplicate project paths, root names and Android namespaces, repeated target kinds, hierarchy
  markers used as targets, and framework exports of undeclared modules.
* Project plugins `com.dzaitsev.trellis.conventions` (configures every module from the root) and
  `com.dzaitsev.trellis.local` (configures one module, compatible with Isolated Projects).
* Shared conventions `kotlinVersion`, `javaVersion`, `androidMinSdk`, `androidTargetSdk` and `useJavaToolchain`, declared
  in settings `projectConventions { }` or the root `trellis { }` block.
* Extension API: `Configurator`, `TrellisProjectPlugin`, `TrellisLocalProjectPlugin` and
  `defaultProjectModuleConfigurators()`, plus the `Module.UsesCompose` and `HasAndroidNamespace` capabilities.

### Migrating from pre-release snapshots

Only builds that used a locally published snapshot of `0.1.0` need these changes:

* Declare targets as statements instead of combining them with `+`: `jvm()` is a function, `ios { framework("Shared") }`
  replaces `Framework(...)`, and hosts and environments are arguments (`js(browser)`, `ios(iosArm64)`).
* Put module options in the trailing block: `androidApp("app", "com.example.app") { usesCompose = true }`,
  `java("server") { mainClass = "MainKt" }`. Product flavors use `flavors { dimension(...) { flavor(...) } }`.
* `android(namespace)` no longer takes `enableResources` or `hostTests`: host tests are always registered, and Android
  resources follow the module's `usesCompose`.
* Conventions no longer have built-in values. Set `androidMinSdk` and `androidTargetSdk` when the build has Android
  modules; `kotlinVersion` and `javaVersion` are optional.
* Extension API: `ModuleModel`, `AndroidAttrs`, `HasAndroidAttrs` and `getAndroidAttrs()` are removed in favor of
  `Module`, `HasAndroidNamespace` and `androidNamespace()`. `Configurator` takes only the extension type, and plugin
  subclasses override `configurators()` instead of `moduleOf()` and `immediateConfigurators()`.
* `macos()` registers `macosArm64` only by default, and `linux()` now registers `linuxArm64` alongside `linuxX64`.
* `KmpTarget.Ios.Framework` moved to `KmpTarget.Framework`, and `ios`, `macos`, `tvos` and `watchos` all accept
  `{ framework("...") }`.
