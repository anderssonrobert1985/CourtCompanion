package com.robert.courtcompanion.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.robert.courtcompanion.App
import com.robert.courtcompanion.DesktopPlatform
import org.jetbrains.skia.Image

fun main(args: Array<String>) {
    // "courtcompanion mcp", "courtcompanion list", ...: the installed launcher doubles as the command line tool.
    if (args.isNotEmpty()) return com.robert.courtcompanion.cli.main(args)
    runWindow()
}

private fun runWindow() = application {
    val icon = remember {
        Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
            ?.use { BitmapPainter(Image.makeFromEncoded(it.readBytes()).toComposeImageBitmap()) }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "CourtCompanion",
        icon = icon,
        state = rememberWindowState(width = 820.dp, height = 960.dp),
    ) {
        val platform = remember { DesktopPlatform(window) }
        App(platform)
    }
}
