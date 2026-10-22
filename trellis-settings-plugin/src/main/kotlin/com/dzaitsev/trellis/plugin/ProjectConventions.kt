package com.dzaitsev.trellis.plugin

public class ProjectConventions {
    /** Kotlin language and API version, e.g. `"2.4"`; `null` keeps the compiler's default. */
    public var kotlinVersion: String? = null
    public var javaVersion: Int = 17
    public var androidMinSdk: Int = 24
    public var androidTargetSdk: Int = 36
    public var useJavaToolchain: Boolean = false

    internal fun snapshot(): ProjectConventions = ProjectConventions().also {
        it.kotlinVersion = kotlinVersion
        it.javaVersion = javaVersion
        it.androidMinSdk = androidMinSdk
        it.androidTargetSdk = androidTargetSdk
        it.useJavaToolchain = useJavaToolchain
    }
}
