package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.Module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModuleRegistryTest {
    @Test
    fun `root path parses to ROOT`() {
        assertEquals(ProjectPath.ROOT, ProjectPath.parse(":"))
        assertEquals(":", ProjectPath.ROOT.gradlePath)
        assertEquals(":a:b", ProjectPath.parse(":a:b").gradlePath)
        assertEquals("a/b", ProjectPath.parse("a:b").relativeDirectory)
    }

    @Test
    fun `invalid path segments are rejected`() {
        assertFailsWith<IllegalArgumentException> { ProjectPath.parse(":a/b") }
        assertFailsWith<IllegalArgumentException> { ProjectPath.parse(":a::b") }
        assertFailsWith<IllegalArgumentException> { ProjectPath.parse(" ") }
    }

    @Test
    fun `root module maps to the root path, not a subproject`() {
        val registry = moduleRegistry {
            root("app-root")
            java("server")
        }

        assertEquals(listOf(ProjectPath.ROOT, ProjectPath.parse(":server")), registry.modules.map { it.path })
        assertEquals(Module.Root("app-root"), registry.findByPath(ProjectPath.ROOT))
        assertEquals("app-root", registry[":"].name)
    }

    @Test
    fun `several roots resolve by root project name`() {
        val registry = moduleRegistry {
            root("android")
            root("ios")
        }

        assertEquals("ios", registry.findRoot("ios")?.name)
        assertNull(registry.findRoot("other"))
        assertNull(registry.findByPath(ProjectPath.ROOT))
    }

    @Test
    fun `nested groups build nested paths`() {
        val registry = moduleRegistry {
            group("feature") {
                group("auth") {
                    kmp("api", jvm)
                }
                kmp("profile", jvm)
            }
        }

        assertEquals(
            listOf(":feature:auth:api", ":feature:profile"),
            registry.modules.map { it.path.gradlePath },
        )
        assertEquals("api", registry[":feature:auth:api"].name)
    }

    @Test
    fun `undeclared paths resolve to null`() {
        val registry = moduleRegistry { group("feature") { kmp("auth", jvm) } }

        assertNull(registry.findByPath(ProjectPath.parse(":feature")))
        assertNull(registry.findByPath(ProjectPath.ROOT))
    }

    @Test
    fun `root inside a group is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            moduleRegistry { group("g") { root("r") } }
        }
    }

    @Test
    fun `duplicate project paths are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            moduleRegistry {
                java("a")
                kmp("a", jvm)
            }
        }
    }

    @Test
    fun `kmp module without targets is rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            moduleRegistry { kmp("a", emptySet()) }
        }
        assertTrue("declares no targets" in error.message.orEmpty())
    }

    @Test
    fun `repeated target kinds are rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            moduleRegistry { kmp("a", js(browser) + js(nodeJs)) }
        }
        assertTrue("Js" in error.message.orEmpty())
    }

    @Test
    fun `framework exports must be declared`() {
        val error = assertFailsWith<IllegalArgumentException> {
            moduleRegistry { kmp("umbrella", ios(framework("Shared", export = listOf(":missing")))) }
        }
        assertTrue(":missing" in error.message.orEmpty())

        moduleRegistry {
            kmp("domain", ios())
            kmp("umbrella", ios(framework("Shared", export = listOf(":domain"))))
        }
    }

    @Test
    fun `duplicate android namespaces are rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            moduleRegistry {
                androidLibrary("a", namespace = "com.example")
                kmp("b", android("com.example"))
            }
        }
        assertTrue("com.example" in error.message.orEmpty())
    }

    @Test
    fun `registries compare structurally`() {
        val block: ModuleRegistryBuilder.() -> Unit = { kmp("a", jvm + ios()) }

        assertEquals(moduleRegistry(block), moduleRegistry(block))
    }

    @Test
    fun `target builders default to all environments and hosts`() {
        lateinit var targets: List<KmpTarget>
        moduleRegistry {
            targets = listOf(js(), js(browser, isApplication = true), desktop(), desktop(macosArm64))
        }

        assertEquals(KmpTarget.JsEnvironment.entries.toSet(), (targets[0] as KmpTarget.Js).environments)
        assertEquals(KmpTarget.Js(isApplication = true, environments = setOf(KmpTarget.JsEnvironment.BROWSER)), targets[1])
        assertEquals(KmpTarget.DesktopHost.entries.toSet(), (targets[2] as KmpTarget.Desktop).hosts)
        assertEquals(setOf(KmpTarget.DesktopHost.MACOS_ARM64), (targets[3] as KmpTarget.Desktop).hosts)
    }

    @Test
    fun `ios builder defaults to device and arm64 simulator`() {
        lateinit var targets: List<KmpTarget.Ios>
        moduleRegistry {
            targets = listOf(
                ios(),
                ios(framework("Shared")),
                ios(iosSimulatorArm64, iosX64, framework = framework("Shared")),
            )
        }

        assertEquals(KmpTarget.IosHost.DEFAULT, targets[0].hosts)
        assertEquals(KmpTarget.IosHost.DEFAULT, targets[1].hosts)
        assertEquals("Shared", targets[1].framework?.baseName)
        assertEquals(setOf(KmpTarget.IosHost.IOS_SIMULATOR_ARM64, KmpTarget.IosHost.IOS_X64), targets[2].hosts)
        assertEquals("Shared", targets[2].framework?.baseName)
        assertFailsWith<IllegalArgumentException> { KmpTarget.Ios(hosts = emptySet()) }
    }

    @Test
    fun `application modules default to no explicit api`() {
        val registry = moduleRegistry {
            androidApp("app", namespace = "com.example.app")
            java("server", mainClass = "MainKt")
            java("lib")
        }

        assertEquals(false, registry["app"].hasExplicitApi)
        assertEquals(false, registry["server"].hasExplicitApi)
        assertEquals(true, registry["lib"].hasExplicitApi)
    }
}
