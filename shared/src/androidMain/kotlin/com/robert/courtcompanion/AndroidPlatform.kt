package com.robert.courtcompanion

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.robert.courtcompanion.data.AppRepository
import java.io.File

/** Android implementation of [Platform]. [context] should be the hosting activity, so the share sheet opens over it. */
class AndroidPlatform(private val context: Context) : Platform {
    override val repository: AppRepository = repository(context)

    // Must match the cache-path in the app's res/xml/file_paths.xml, which FileProvider shares from.
    private val exportDir = File(context.cacheDir, "exports")

    override fun exportFile(name: String, text: String): String? {
        val file = File(exportDir, name)
        exportDir.mkdirs()
        file.writeText(text)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, file.name))
        return null
    }

    private companion object {
        @Volatile
        private var sharedRepository: AppRepository? = null

        /** One repository per process, so it outlives activity recreation along with the view models using it. */
        fun repository(context: Context): AppRepository = sharedRepository ?: synchronized(this) {
            sharedRepository ?: AppRepository(File(context.applicationContext.filesDir, "data"))
                .also { sharedRepository = it }
        }
    }
}
