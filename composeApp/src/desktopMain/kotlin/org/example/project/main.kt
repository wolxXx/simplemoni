package org.example.project

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SimpleMoni " + VersionInfo.PACKAGE_VERSION,
        state = WindowState(
            isMinimized = false,
            width = 500.dp,
            placement = WindowPlacement.Maximized
        )
    ) {
        App()
    }
}