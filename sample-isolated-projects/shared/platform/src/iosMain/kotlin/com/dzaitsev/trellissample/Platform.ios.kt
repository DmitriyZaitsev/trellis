package com.dzaitsev.trellissample

import platform.UIKit.UIDevice

public class IosPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

public actual fun getPlatform(): Platform = IosPlatform()