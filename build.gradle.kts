// Aggregator only; see trellis-settings and trellis-gradle-plugin.
plugins {
    id("com.gradle.plugin-publish") version "2.2.1" apply false
    id("com.diffplug.spotless") version "8.10.3"
}

repositories {
    mavenCentral()
}

tasks.register("ciCheck") {
    group = "verification"
    dependsOn(":trellis-settings-plugin:check", ":trellis-project-plugin:check")
}

subprojects {
    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("org.gradle.kotlin.kotlin-dsl") {
        pluginManager.apply("com.diffplug.spotless")

        extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            kotlin {
                ktlint("1.0.0")
                    .editorConfigOverride(
                        mapOf(
                            "trim_trailing_whitespace" to true,
                            "insert_final_newline" to true,
                        )
                    )
                    .customRuleSets(
                        listOf(
                            "io.nlopez.compose.rules:ktlint:0.4.25"
                        )
                    )
            }
            kotlinGradle {
                target("*.gradle.kts") // default target for kotlinGradle
                ktlint()
            }
        }
    }
}
