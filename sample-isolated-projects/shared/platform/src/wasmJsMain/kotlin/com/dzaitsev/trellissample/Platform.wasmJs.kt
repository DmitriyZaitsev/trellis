package com.dzaitsev.trellissample

public class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

public actual fun getPlatform(): Platform = WasmPlatform()