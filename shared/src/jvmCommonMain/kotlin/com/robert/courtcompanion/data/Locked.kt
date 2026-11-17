package com.robert.courtcompanion.data

internal actual inline fun <T> locked(lock: Any, block: () -> T): T = synchronized(lock, block)
