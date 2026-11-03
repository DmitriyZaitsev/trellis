import org.gradle.plugin.compatibility.compatibility

plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
}

// No KGP/AGP references allowed here: this artifact is loaded into the settings class loader,
// which cannot see the Kotlin/Android Gradle plugins applied later by the projects.
gradlePlugin {
    website.set(providers.gradleProperty("trellis.website"))
    vcsUrl.set(providers.gradleProperty("trellis.vcsUrl"))
    plugins {
        create("trellis") {
            id = "com.dzaitsev.trellis"
            implementationClass = "com.dzaitsev.trellis.plugin.TrellisSettingsPlugin"

            displayName = "Trellis"
            description = "Declare Android, Kotlin Multiplatform and JVM modules once in settings; Trellis includes and configures them"
            tags.set(listOf("kotlin", "multiplatform", "android", "modules"))

            // Verified by TrellisProjectPluginFunctionalTest (configuration cache reuse + isolated projects).
            // Once declared, later versions must keep declaring these features.
            compatibility {
                features {
                    configurationCache = true
                    isolatedProjects = true
                }
            }
        }
    }
}

repositories {
    mavenCentral()
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            url.set(providers.gradleProperty("trellis.website"))
            scm {
                url.set(providers.gradleProperty("trellis.vcsUrl"))
                connection.set(providers.gradleProperty("trellis.vcsUrl").map { "scm:git:$it.git" })
            }
            developers {
                developer {
                    id.set("DmitriyZaitsev")
                    name.set("Dmytro Zaitsev")
                    url.set(providers.gradleProperty("trellis.website"))
                }
            }
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    distribution.set("repo")
                }
            }
        }
    }
    repositories {
        maven {
            name = "testRepo"
            url = uri(rootDir.resolve("build/test-repo"))
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar>().configureEach {
    from(listOf(rootDir.resolve("LICENSE"), rootDir.resolve("NOTICE"))) {
        into("META-INF")
    }
}

tasks.named("check") {
    dependsOn("checkLegacyAbi")
}

tasks.named("publishPlugins") {
    doFirst {
        check(
            providers.gradleProperty("trellis.allowPublicPublish").orNull == "true" &&
                providers.gradleProperty("trellis.website").isPresent &&
                providers.gradleProperty("trellis.vcsUrl").isPresent,
        ) {
            "Public publishing requires verified trellis.website / trellis.vcsUrl and explicit trellis.allowPublicPublish=true"
        }
    }
}

kotlin {
    explicitApi()
    jvmToolchain(17)
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }
}
