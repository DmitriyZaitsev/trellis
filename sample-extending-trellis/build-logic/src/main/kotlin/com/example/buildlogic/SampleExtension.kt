package com.example.buildlogic

import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import org.gradle.api.file.RegularFileProperty

// Inherits Trellis's SDK, Kotlin and JVM settings, then adds our own setting.
interface SampleExtension : TrellisProjectExtension {
    val detektConfig: RegularFileProperty
}
