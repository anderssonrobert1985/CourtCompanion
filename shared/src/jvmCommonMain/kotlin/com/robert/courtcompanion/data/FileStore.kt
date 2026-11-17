package com.robert.courtcompanion.data

import kotlinx.datetime.LocalDate
import java.io.File

/** A [TextStore] keeping every document as a file in [dir], which is created if missing. */
class FileStore(private val dir: File) : TextStore {
    init {
        dir.mkdirs()
    }

    override fun read(name: String): String? = File(dir, name).takeIf { it.isFile }?.readText()

    override fun write(name: String, text: String) {
        val file = File(dir, name)
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}

/** An [AppRepository] storing its JSON files in [dir]. */
fun AppRepository(dir: File, today: () -> LocalDate = ::today): AppRepository = AppRepository(FileStore(dir), today)
