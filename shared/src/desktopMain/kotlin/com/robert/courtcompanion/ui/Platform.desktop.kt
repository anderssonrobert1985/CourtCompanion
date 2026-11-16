package com.robert.courtcompanion.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Desktop windows have their own back arrow in the top bar; there is no system back gesture. */
@Composable
actual fun SystemBackHandler(onBack: () -> Unit) = Unit

@Composable
actual fun rememberJsonFilePicker(onPicked: (String) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPicked)
    return {
        val dialog = FileDialog(null as Frame?, "Import players", FileDialog.LOAD).apply {
            setFilenameFilter { _, name -> name.endsWith(".json", ignoreCase = true) }
        }
        dialog.isVisible = true // Blocks until closed.
        val name = dialog.file
        if (name != null) callback.value(runCatching { File(dialog.directory, name).readText() }.getOrDefault(""))
    }
}
