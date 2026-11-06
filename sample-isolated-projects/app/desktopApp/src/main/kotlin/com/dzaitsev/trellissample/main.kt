package com.dzaitsev.trellissample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Trellis_Sample",
    ) {
        App()
    }
}