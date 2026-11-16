package com.robert.courtcompanion.ui

import androidx.compose.runtime.Composable

/** Calls [onBack] when the system back button or gesture is used. Does nothing where there is no such thing. */
@Composable
expect fun SystemBackHandler(onBack: () -> Unit)

/**
 * Returns a function that lets the user pick a JSON file (Android document picker, desktop file dialog) and passes
 * its text to [onPicked]. Nothing happens if the user cancels.
 */
@Composable
expect fun rememberJsonFilePicker(onPicked: (String) -> Unit): () -> Unit
