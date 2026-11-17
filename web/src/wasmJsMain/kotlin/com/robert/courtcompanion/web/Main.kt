package com.robert.courtcompanion.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.robert.courtcompanion.App
import com.robert.courtcompanion.WebPlatform

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("app") { App(WebPlatform) }
    removeLoadingMessage()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun removeLoadingMessage(): Unit = js("document.getElementById('message')?.remove()")
