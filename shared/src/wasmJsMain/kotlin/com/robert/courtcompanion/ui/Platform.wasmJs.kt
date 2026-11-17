@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.robert.courtcompanion.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState

/** The browser's back button would leave the app, so it isn't used; screens have their own back arrow. */
@Composable
actual fun SystemBackHandler(onBack: () -> Unit) = Unit

@Composable
actual fun rememberJsonFilePicker(onPicked: (String) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPicked)
    return { pickTextFile { callback.value(it) } }
}

// No accept filter: iOS greys out files it doesn't recognise as JSON, e.g. ones saved from a chat.
private fun pickTextFile(onText: (String) -> Unit): Unit = js(
    """{
    const input = document.createElement('input');
    input.type = 'file';
    input.style.display = 'none';
    input.addEventListener('change', () => {
        const file = input.files && input.files[0];
        input.remove();
        if (file) file.text().then(onText, () => onText(''));
    });
    document.body.appendChild(input);
    input.click();
}""",
)
