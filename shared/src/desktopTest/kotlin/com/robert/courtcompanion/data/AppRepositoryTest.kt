package com.robert.courtcompanion.data

import com.robert.courtcompanion.model.KeepApart
import com.robert.courtcompanion.model.Player
import com.robert.courtcompanion.model.Roster
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

class AppRepositoryTest {
    private val dir: File = Files.createTempDirectory("courtcompanion-repo").toFile()

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun rosterAndSessionSurviveARestart() {
        val day = LocalDate.of(2026, 11, 16)
        val repo = AppRepository(dir) { day }
        repo.updateRoster { it.copy(players = listOf(Player(id = "a", name = "Anna", skill = 4))) }
        repo.updateSession { it.copy(absent = setOf("a")) }

        val again = AppRepository(dir) { day }
        assertEquals("Anna", again.roster.value.players.single().name)
        assertEquals(setOf("a"), again.session.value.absent)

        // The next day starts a fresh session but keeps the roster.
        val tomorrow = AppRepository(dir) { day.plusDays(1) }
        assertEquals(emptySet<String>(), tomorrow.session.value.absent)
        assertEquals(1, tomorrow.roster.value.players.size)
    }

    @Test
    fun normalizeCleansUpImportedFiles() {
        val roster = RosterJson.parse(
            """{"players":[
                {"id":"x","name":" Anna ","skill":9,"tags":["quick","Goalie"]},
                {"id":"x","name":"Bea","skill":0},
                {"name":"   "}
            ],"tags":["Quick"],"keepApart":[{"a":"x","b":"missing"}],"unknownField":1}""",
        )
        assertEquals(listOf("Anna", "Bea"), roster.players.map { it.name })
        assertEquals(listOf(5, 1), roster.players.map { it.skill })
        assertEquals(setOf("Quick", "Goalie"), roster.players[0].tags)
        assertEquals(listOf("Quick", "Goalie"), roster.tags)
        assertTrue("Ids made unique", roster.players[0].id != roster.players[1].id)
        assertEquals(emptyList<KeepApart>(), roster.keepApart)
    }

    @Test
    fun exportedFilesImportUnchanged() {
        val roster = Roster(
            players = listOf(Player(id = "a", name = "Anna", skill = 4, tags = setOf("Tall")), Player(id = "b", name = "Bea")),
            keepApart = listOf(KeepApart("a", "b")),
        )
        assertEquals(RosterJson.normalize(roster), RosterJson.parse(RosterJson.write(roster)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun garbageIsRejected() {
        RosterJson.parse("not json")
    }
}
