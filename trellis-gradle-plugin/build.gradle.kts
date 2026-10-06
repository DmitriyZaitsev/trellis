plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
}

gradlePlugin {
    website.set(providers.gradleProperty("trellis.website"))
    vcsUrl.set(providers.gradleProperty("trellis.vcsUrl"))
    plugins {
        create("trellisLocal") {
            id = "com.dzaitsev.trellis.local"
            implementationClass = "com.dzaitsev.trellis.plugin.TrellisLocalPlugin"
            displayName = "Trellis Local Conventions"
            description = "Configures only the current project using settings-provided Trellis conventions"
            tags.set(listOf("kotlin", "multiplatform", "android", "modules"))
        }
        create("trellisConventions") {
            id = "com.dzaitsev.trellis.conventions"
            implementationClass = "com.dzaitsev.trellis.plugin.TrellisConventionsPlugin"

            displayName = "Trellis Conventions"
            description = "Applies and configures Android, Kotlin Multiplatform and JVM plugins for modules declared with Trellis"
            tags.set(listOf("kotlin", "multiplatform", "android", "modules"))
        }
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    explicitApi()
    jvmToolchain(17)
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }
}

dependencies {
    api(project(":trellis-settings"))

    // compileOnly: types only, at runtime the real Kotlin Gradle plugin comes from whatever
    // consumer applies `kotlin("multiplatform")`/`kotlin("jvm")`. Keep this version in sync
    // with the Kotlin version your consuming build uses (see its `libs.versions.toml`).
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")

    // compileOnly: types only (`gradle-api` is the AGP artifact with no plugin implementation
    // classes), at runtime the real AGP comes from whatever consumer applies
    // `com.android.application`/`com.android.library`/`com.android.kotlin.multiplatform.library`.
    // Keep this version in sync with the AGP version your consuming build uses.
    compileOnly("com.android.tools.build:gradle-api:9.4.1")

    testImplementation(kotlin("test"))
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

// Functional tests resolve both artifacts from a Maven repo, like real consumers do, so the
// settings/project class loader split is exercised (TestKit's plugin classpath injection would hide it).
tasks.test {
    useJUnitPlatform()
    dependsOn(
        ":trellis-settings:publishAllPublicationsToTestRepoRepository",
        "publishAllPublicationsToTestRepoRepository",
    )
    systemProperty("trellis.testRepo", rootDir.resolve("build/test-repo").absolutePath)
    systemProperty("trellis.version", version.toString())
    systemProperty("trellis.testKitDir", rootDir.resolve(".gradle/testkit").absolutePath)
    systemProperty("trellis.localProperties", rootDir.resolve("local.properties").absolutePath)
    systemProperty("trellis.requireAndroidSdk", providers.gradleProperty("trellis.requireAndroidSdk").getOrElse("false"))
    maxHeapSize = "1g"
}

tasks.withType<Jar>().configureEach {
    from(listOf(rootDir.resolve("LICENSE"), rootDir.resolve("NOTICE"))) {
        into("META-INF")
    }
}

tasks.named("publishPlugins") {
    doFirst {
        check(providers.gradleProperty("trellis.allowPublicPublish").orNull == "true" &&
            providers.gradleProperty("trellis.website").isPresent &&
            providers.gradleProperty("trellis.vcsUrl").isPresent) {
            "Public publishing requires verified trellis.website / trellis.vcsUrl and explicit trellis.allowPublicPublish=true"
        }
    }
}

tasks.named("check") {
    dependsOn("checkLegacyAbi")
}
