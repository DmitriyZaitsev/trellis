package com.dzaitsev.trellis.plugin

public class ProjectConventions {
    /** Kotlin language and API version, e.g. `"2.4"`; `null` keeps the compiler's default. */
    public var kotlinVersion: String? = null

    /** JVM bytecode target, e.g. `17`; `null` keeps the Kotlin, Java and Android tooling defaults. */
    public var javaVersion: Int? = null

    /** Required when any Android module or KMP Android target is declared. */
    public var androidMinSdk: Int? = null

    /** Required when any Android module or KMP Android target is declared; sets `compileSdk` and `targetSdk`. */
    public var androidTargetSdk: Int? = null
    public var useJavaToolchain: Boolean = false

    internal fun snapshot(): ProjectConventions = ProjectConventions().also {
        it.kotlinVersion = kotlinVersion
        it.javaVersion = javaVersion
        it.androidMinSdk = androidMinSdk
        it.androidTargetSdk = androidTargetSdk
        it.useJavaToolchain = useJavaToolchain
    }
}
