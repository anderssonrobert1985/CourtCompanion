package com.robert.courtcompanion.cli

import com.robert.courtcompanion.data.AppRepository
import com.robert.courtcompanion.data.RosterJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.Base64

/** Talks to the server the way an MCP client does: JSON-RPC lines in, JSON-RPC lines out. */
class McpServerTest {
    private val dir: File = Files.createTempDirectory("courtcompanion-mcp").toFile()
    private val service = GroupService(AppRepository(File(dir, "data")), File(dir, "exports"))

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun output(vararg requests: String): List<String> {
        val out = ByteArrayOutputStream()
        McpServer(service, ByteArrayInputStream(requests.joinToString("\n").toByteArray()), out).run()
        return out.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
    }

    private fun exchange(vararg requests: String): List<JsonObject> = output(*requests).map { Json.parseToJsonElement(it).jsonObject }

    private fun call(tool: String, arguments: String) =
        """{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"$tool","arguments":$arguments}}"""

    private fun callOne(tool: String, arguments: String) = exchange(call(tool, arguments)).single()

    private fun JsonObject.result() = getValue("result").jsonObject
    private fun JsonObject.content() = result().getValue("content").jsonArray.map { it.jsonObject }
    private fun JsonObject.text() = content().first().getValue("text").jsonPrimitive.content
    private fun JsonObject.isError() = result().getValue("isError").jsonPrimitive.boolean

    private val team = """{"players":[
        {"name":"Anna","skill":5,"tags":["Quick"]},{"name":"Bea","skill":5,"tags":["Tall"]},
        {"name":"Cleo","skill":4,"tags":["Physical"]},{"name":"Dina","skill":4},
        {"name":"Ella","skill":3},{"name":"Frida","skill":3},{"name":"Greta","skill":2},
        {"name":"Hanna","skill":2},{"name":"Ida","skill":1}]}""".replace(Regex("\\s+"), " ")

    @Test
    fun handshakeAndToolList() {
        val replies = exchange(
            """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}""",
            """{"jsonrpc":"2.0","method":"notifications/initialized"}""",
            """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""",
            """{"jsonrpc":"2.0","id":3,"method":"ping"}""",
            """{"jsonrpc":"2.0","id":4,"method":"resources/list"}""",
        )
        assertEquals("Notifications get no reply", listOf(1, 2, 3, 4), replies.map { it.getValue("id").jsonPrimitive.int })
        val init = replies[0].result()
        assertEquals("2025-03-26", init.getValue("protocolVersion").jsonPrimitive.content)
        assertEquals("courtcompanion", init.getValue("serverInfo").jsonObject.getValue("name").jsonPrimitive.content)
        assertTrue(init.getValue("instructions").jsonPrimitive.content.contains("export_roster"))
        val tools = replies[1].result().getValue("tools").jsonArray.map { it.jsonObject.getValue("name").jsonPrimitive.content }
        assertEquals(listOf("list_players", "upsert_players", "remove_players", "set_keep_apart", "generate_groups", "export_roster"), tools)
        assertEquals(-32601, replies[3].getValue("error").jsonObject.getValue("code").jsonPrimitive.int)
    }

    @Test
    fun unknownProtocolVersionGetsTheNewest() {
        val reply = exchange("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"1999-01-01"}}""").single()
        assertEquals(McpServer.PROTOCOL_VERSIONS.first(), reply.result().getValue("protocolVersion").jsonPrimitive.content)
    }

    @Test
    fun buildTheRosterThenExportIt() {
        val added = callOne("upsert_players", team)
        assertEquals(added.text(), false, added.isError())
        assertTrue(added.text(), added.text().contains("Added 9"))

        val updated = callOne("upsert_players", """{"players":[{"name":"anna","skill":4,"tags":["Quick","Tall"]},{"name":"Ida","rename":"Ida B"}]}""")
        assertTrue(updated.text(), updated.text().contains("Updated 2: Anna, Ida B"))
        assertEquals(4, service.roster().playerNamed("Anna")!!.skill)

        val pairs = callOne("set_keep_apart", """{"add":[["Anna","Bea"]]}""")
        assertTrue(pairs.text(), pairs.text().contains("Anna ↔ Bea"))

        val list = callOne("list_players", "{}").text()
        assertTrue(list, list.contains("- Anna: skill 4, Quick, Tall"))
        assertTrue(list, list.contains("Keep apart: Anna ↔ Bea"))

        val export = callOne("export_roster", "{}")
        val file = File(dir, "exports/courtcompanion-players.json")
        assertTrue(export.text(), export.text().contains(file.absolutePath))
        val roster = RosterJson.parse(file.readText())
        assertEquals(9, roster.players.size)
        assertEquals(1, roster.keepApart.size)

        val removed = callOne("remove_players", """{"names":["Bea"]}""")
        assertTrue(removed.text(), removed.text().contains("8 players"))
        assertEquals(0, service.roster().keepApart.size)
    }

    @Test
    fun generateReturnsGroupsAndAPng() {
        callOne("upsert_players", team)
        val reply = callOne("generate_groups", """{"group_size":2,"absent":["Ida"],"goal":"similar","seed":3}""")
        assertEquals(reply.text(), false, reply.isError())
        assertTrue(reply.text(), reply.text().startsWith("8 players present."))
        // Pairs of equal skill: 5+5, 4+4, 3+3, 2+2.
        assertTrue(reply.text(), reply.text().contains("Group 1 (2 players, avg skill 5.0): Anna (5, Quick), Bea (5, Tall)"))
        assertTrue(!reply.text().contains("Ida"))
        val image = reply.content()[1]
        assertEquals("image/png", image.getValue("mimeType").jsonPrimitive.content)
        val png = Base64.getDecoder().decode(image.getValue("data").jsonPrimitive.content)
        assertEquals("PNG", String(png, 1, 3))

        val teams = callOne("generate_groups", """{"group_count":2,"goal":"balanced","keep_apart":[["Anna","Bea"]]}""")
        assertTrue(teams.text(), teams.text().contains("Team 2"))
        // Nothing in the app's history changes.
        assertEquals(0, service.repository.session.value.rounds.size)
    }

    @Test
    fun problemsComeBackAsToolErrors() {
        val empty = callOne("generate_groups", "{}")
        assertEquals(true, empty.isError())
        assertTrue(empty.text(), empty.text().contains("empty"))

        callOne("upsert_players", team)
        val unknown = callOne("generate_groups", """{"absent":["Zelda"]}""")
        assertEquals(true, unknown.isError())
        assertTrue(unknown.text(), unknown.text().contains("No player called \"Zelda\""))

        val badSkill = callOne("upsert_players", """{"players":[{"name":"X","skill":7}]}""")
        assertEquals(true, badSkill.isError())

        val both = callOne("generate_groups", """{"group_size":2,"group_count":2}""")
        assertEquals(true, both.isError())
    }

    @Test
    fun argumentsSentAsJsonTextAreAccepted() {
        val asText = Json.encodeToString(kotlinx.serialization.json.JsonPrimitive.serializer(), kotlinx.serialization.json.JsonPrimitive("""[{"name":"Anna"}]"""))
        val reply = callOne("upsert_players", """{"players":$asText}""")
        assertEquals(reply.text(), false, reply.isError())
    }

    @Test
    fun malformedLinesGetAParseErrorAndTheServerKeepsGoing() {
        val replies = exchange("not json", """{"jsonrpc":"2.0","id":5,"method":"ping"}""")
        assertEquals(-32700, replies[0].getValue("error").jsonObject.getValue("code").jsonPrimitive.int)
        assertEquals(5, replies[1].getValue("id").jsonPrimitive.int)
    }

    @Test
    fun batchesGetABatchReply() {
        val reply = output("""[{"jsonrpc":"2.0","id":1,"method":"ping"},{"jsonrpc":"2.0","method":"notifications/initialized"},{"jsonrpc":"2.0","id":2,"method":"ping"}]""")
        val batch = Json.parseToJsonElement(reply.single()).jsonArray
        assertEquals(listOf(1, 2), batch.map { it.jsonObject.getValue("id").jsonPrimitive.int })
    }
}
