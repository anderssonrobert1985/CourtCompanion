package com.robert.courtcompanion.data

/** The browser runs everything on one thread. */
internal actual inline fun <T> locked(lock: Any, block: () -> T): T = block()
