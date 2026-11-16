package com.robert.courtcompanion

import androidx.compose.runtime.staticCompositionLocalOf
import com.robert.courtcompanion.data.AppRepository
import java.io.File

/** Everything the shared app needs from the operating system. Implemented once per platform. */
interface Platform {
    val repository: AppRepository

    /** Scratch directory for exported player files; its contents may be replaced on every export. */
    val exportDir: File

    /**
     * Hands an exported player file to the user: the share sheet on Android (e.g. to Drive or a chat), a "Save as"
     * dialog on desktop. Returns a message to show, or null to show nothing (e.g. when the user cancelled).
     */
    fun exportFile(file: File): String?
}

val LocalPlatform = staticCompositionLocalOf<Platform> { error("No Platform provided") }
