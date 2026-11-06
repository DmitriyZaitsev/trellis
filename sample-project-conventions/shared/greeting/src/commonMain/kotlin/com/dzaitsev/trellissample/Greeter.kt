package com.dzaitsev.trellissample

public object Greeter {
    private val platform = getPlatform()

    public fun greet(): String = sayHello(platform.name)
}