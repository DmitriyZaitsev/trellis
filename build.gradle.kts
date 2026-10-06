// Aggregator only; see trellis-settings and trellis-gradle-plugin.
plugins {
	id("com.gradle.plugin-publish") version "2.2.1" apply false
}

tasks.register("ciCheck") {
	group = "verification"
	dependsOn(":trellis-settings-plugin:check", ":trellis-project-plugin:check")
}
