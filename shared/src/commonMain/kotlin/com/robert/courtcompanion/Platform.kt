package com.robert.courtcompanion

import androidx.compose.runtime.staticCompositionLocalOf
import com.robert.courtcompanion.data.AppRepository

/** Everything the shared app needs from the operating system. Implemented once per platform. */
interface Platform {
    val repository: AppRepository

    /**
     * Hands an exported player file named [name] with contents [text] to the user: the share sheet on Android and
     * in the browser where it has one (e.g. to Drive or a chat), a "Save as" dialog on desktop, a download
     * elsewhere. Returns a message to show, or null to show nothing (e.g. when the user cancelled).
     */
    fun exportFile(name: String, text: String): String?
}

val LocalPlatform = staticCompositionLocalOf<Platform> { error("No Platform provided") }
