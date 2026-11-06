package com.dzaitsev.trellissample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@Suppress("FunctionName")
public fun MainViewController(): UIViewController =
    ComposeUIViewController {
        App()
    }