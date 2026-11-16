package com.robert.courtcompanion

import com.robert.courtcompanion.data.AppRepository
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Desktop implementation of [Platform]. Data lives in the XDG data directory (~/.local/share/courtcompanion on
 * Linux), shared with the command line tool and the MCP server. [window] is the parent for file dialogs.
 */
class DesktopPlatform(private val window: Frame?) : Platform {
    override val repository = AppRepository(dataDir())

    override val exportDir = File(xdgDir("XDG_CACHE_HOME", ".cache"), "courtcompanion/exports")

    override fun exportFile(file: File): String? {
        val dialog = FileDialog(window, "Export players", FileDialog.SAVE).apply {
            directory = (File(home, "Documents").takeIf { it.isDirectory } ?: home).path
            this.file = file.name
        }
        dialog.isVisible = true // Blocks until closed.
        val name = dialog.file ?: return null
        val target = File(dialog.directory, if (name.endsWith(".json", ignoreCase = true)) name else "$name.json")
        return runCatching {
            file.copyTo(target, overwrite = true)
            target.path.replaceFirst(home.path, "~")
        }.getOrElse { "Error: ${it.message}" }
    }

    companion object {
        private val home = File(System.getProperty("user.home"))

        private fun xdgDir(variable: String, fallback: String): File =
            System.getenv(variable)?.takeIf { it.isNotBlank() }?.let(::File) ?: File(home, fallback)

        /** Where the desktop app, the command line tool and the MCP server keep the roster. */
        fun dataDir(): File = File(xdgDir("XDG_DATA_HOME", ".local/share"), "courtcompanion")
    }
}
