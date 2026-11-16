package com.robert.courtcompanion.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.swing.SwingUtilities

class MainDispatcherTest {
    /** View models launch on Dispatchers.Main (e.g. when they are created); on desktop that needs kotlinx-coroutines-swing. */
    @Test
    fun mainDispatcherRunsOnTheSwingThread() = runBlocking {
        assertTrue(withContext(Dispatchers.Main) { SwingUtilities.isEventDispatchThread() })
    }
}
