package com.dzaitsev.trellis.dsl

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
public annotation class TrellisDslMarker
