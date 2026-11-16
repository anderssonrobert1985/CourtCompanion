package com.robert.courtcompanion.ui

import com.robert.courtcompanion.data.AppRepository
import com.robert.courtcompanion.model.Player
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

class PracticeViewModelTest {
    private val dir: File = Files.createTempDirectory("courtcompanion-vm").toFile()
    private val repo = AppRepository(dir).apply {
        updateRoster { r -> r.copy(players = List(9) { Player(id = "p$it", name = "P$it", skill = 1 + it % 5) }) }
    }
    private val vm = PracticeViewModel(repo, Random(7))

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun generatingUsesOnlyPresentPlayersAndRecordsRounds() {
        vm.togglePresent("p0")
        assertTrue(vm.generate())
        val ids = vm.result.value!!.groups.flatten().map { it.id }
        assertEquals(8, ids.size)
        assertFalse("p0" in ids)
        assertEquals(1, repo.session.value.rounds.size)

        // Re-roll replaces the round; next round adds one.
        vm.reroll()
        assertEquals(1, repo.session.value.rounds.size)
        vm.generate()
        assertEquals(2, repo.session.value.rounds.size)

        vm.resetToday()
        assertEquals(0, repo.session.value.rounds.size)
        assertEquals(emptySet<String>(), repo.session.value.absent)
    }

    @Test
    fun nobodyHereGeneratesNothing() {
        vm.setAllPresent(false)
        assertFalse(vm.generate())
    }

    @Test
    fun rosterEditsKeepTagsAndPairsConsistent() {
        repo.updateRoster { RosterEdits.addKeepApart(it, "p1", "p2") }
        repo.updateRoster { RosterEdits.savePlayer(it, it.player("p1")!!.copy(tags = setOf("Quick"))) }
        repo.updateRoster { RosterEdits.renameTag(it, "Quick", "Fast") }
        assertEquals(setOf("Fast"), repo.roster.value.player("p1")!!.tags)
        assertTrue("Fast" in repo.roster.value.tags)
        repo.updateRoster { RosterEdits.deletePlayer(it, "p2") }
        assertEquals(0, repo.roster.value.keepApart.size)
    }
}
