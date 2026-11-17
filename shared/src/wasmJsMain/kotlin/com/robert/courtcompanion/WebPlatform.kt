@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.robert.courtcompanion

import com.robert.courtcompanion.data.AppRepository
import com.robert.courtcompanion.data.TextStore

/**
 * Browser implementation of [Platform]. Data lives in the browser's local storage for this site; on an iPhone, an
 * app added to the home screen has its own storage, separate from Safari's.
 */
object WebPlatform : Platform {
    override val repository = AppRepository(LocalStorageStore)

    override fun exportFile(name: String, text: String): String? {
        shareOrDownload(name, text)
        return null
    }
}

/** A [TextStore] in `localStorage`, every document under the key `courtcompanion/<name>`. */
private object LocalStorageStore : TextStore {
    override fun read(name: String): String? = localStorageGet("courtcompanion/$name")

    override fun write(name: String, text: String) = localStorageSet("courtcompanion/$name", text)
}

private fun localStorageGet(key: String): String? = js("localStorage.getItem(key)")

private fun localStorageSet(key: String, value: String): Unit = js("localStorage.setItem(key, value)")

/** Opens the share sheet with the file where the browser can share files (iOS, Android), else downloads it. */
private fun shareOrDownload(name: String, text: String): Unit = js(
    """{
    const file = new File([text], name, { type: 'application/json' });
    if (navigator.canShare && navigator.canShare({ files: [file] })) {
        navigator.share({ files: [file] }).catch(() => {});
    } else {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(file);
        a.download = name;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(a.href), 10000);
    }
}""",
)
