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
    dependsOn(
        ":trellis-settings-plugin:check",
        ":trellis-project-plugin:check",
    )
}

subprojects {
    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("org.gradle.kotlin.kotlin-dsl") {
        pluginManager.apply("com.diffplug.spotless")

        extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            kotlin {
                ktlint("1.8.0")
                    .editorConfigOverride(
                        mapOf(
                            "trim_trailing_whitespace" to true,
                            "insert_final_newline" to true,
                            "max_line_length" to 140,
                            "blank_line_between_when_conditions" to false,
                            "ktlint_standard_when-entry-bracing" to false,
//                            "ktlint_standard_trailing-comma-on-declaration-site" to true,
//                            "ktlint_standard_trailing-comma-on-call-site" to true,
//                            "ij_kotlin_allow_trailing_comma" to true,
//                            "ij_kotlin_allow_trailing_comma_on_call_site" to true,
//                            "ij_kotlin_line_break_after_multiline_lambda_argument" to true,
//                            "ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to 2,
//                            "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
//                            "ktlint_function_signature_body_expression_wrapping" to "default",
//                            "ktlint_standard_multiline-expression-wrapping" to false,
//                            "ktlint_standard_no-unit-return" to false,
//                            "ktlint_standard_no-unused-imports" to true,
//                            "ktlint_standard_statement-wrapping" to false,
//                            "ktlint_standard_string-template-indent" to false,
//                            "ktlint_standard_blank-line-between-when-conditions" to false
                        )
                    )
                    .customRuleSets(
                        listOf(
                            "io.nlopez.compose.rules:ktlint:0.6.8"
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
