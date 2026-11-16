package com.robert.courtcompanion.cli

import com.robert.courtcompanion.grouping.Goal
import com.robert.courtcompanion.grouping.GroupRequest
import com.robert.courtcompanion.grouping.Leftover
import com.robert.courtcompanion.grouping.Sizing
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintStream
import java.util.Base64

/**
 * A Model Context Protocol server over stdio (newline-delimited JSON-RPC 2.0), giving MCP clients such as the
 * Claude apps tools to build the CourtCompanion player database and divide players into groups. Only the small part
 * of MCP that tool servers need is implemented: initialize, ping, tools/list and tools/call.
 */
class McpServer(
    private val service: GroupService,
    private val input: InputStream = System.`in`,
    output: OutputStream? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val out: PrintStream

    init {
        // Stdout carries the protocol. Send anything else that prints (libraries, stray println) to stderr.
        val stdout = output ?: FileOutputStream(FileDescriptor.out)
        out = PrintStream(stdout, false, Charsets.UTF_8)
        if (output == null) System.setOut(System.err)
    }

    fun run() {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        while (true) {
            val line = reader.readLine() ?: return
            if (line.isBlank()) continue
            val message = try {
                json.parseToJsonElement(line)
            } catch (e: SerializationException) {
                send(error(JsonNull, -32700, "Parse error: ${e.message}"))
                continue
            }
            // Batches were allowed by older protocol versions.
            val replies = if (message is JsonArray) message.mapNotNull { handle(it) } else listOfNotNull(handle(message))
            when {
                replies.isEmpty() -> Unit
                message is JsonArray -> send(JsonArray(replies))
                else -> send(replies.single())
            }
        }
    }

    private fun send(message: JsonElement) {
        synchronized(out) {
            out.println(json.encodeToString(JsonElement.serializer(), message))
            out.flush()
        }
    }

    /** The reply to [message], or null for notifications and responses. */
    private fun handle(message: JsonElement): JsonElement? {
        val obj = message as? JsonObject ?: return error(JsonNull, -32600, "Invalid request")
        val id = obj["id"]
        val method = (obj["method"] as? JsonPrimitive)?.contentOrNull ?: return null
        if (id == null) return null // A notification, e.g. notifications/initialized.
        val params = obj["params"] as? JsonObject ?: JsonObject(emptyMap())
        return try {
            when (method) {
                "initialize" -> result(id, initialize(params))
                "ping" -> result(id, JsonObject(emptyMap()))
                "tools/list" -> result(id, buildJsonObject { put("tools", JsonArray(tools.map { it.definition() })) })
                "tools/call" -> {
                    val name = (params["name"] as? JsonPrimitive)?.contentOrNull
                    val tool = tools.find { it.name == name } ?: return error(id, -32602, "Unknown tool: $name")
                    val args = params["arguments"] as? JsonObject ?: JsonObject(emptyMap())
                    result(id, call(tool, args))
                }
                else -> error(id, -32601, "Method not found: $method")
            }
        } catch (e: Exception) {
            System.err.println("courtcompanion mcp: $method failed: $e")
            error(id, -32603, "Internal error: ${e.message}")
        }
    }

    private fun call(tool: Tool, args: JsonObject): JsonObject = try {
        tool.call(args)
    } catch (e: InputError) {
        errorResult(e.message ?: "Invalid input.")
    }

    private fun initialize(params: JsonObject): JsonObject {
        val requested = (params["protocolVersion"] as? JsonPrimitive)?.contentOrNull
        return buildJsonObject {
            put("protocolVersion", if (requested in PROTOCOL_VERSIONS) requested else PROTOCOL_VERSIONS.first())
            putJsonObject("capabilities") { putJsonObject("tools") { put("listChanged", false) } }
            putJsonObject("serverInfo") {
                put("name", "courtcompanion")
                put("title", "CourtCompanion")
                put("version", VERSION)
            }
            put("instructions", INSTRUCTIONS)
        }
    }

    private fun result(id: JsonElement, result: JsonElement) = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        put("result", result)
    }

    private fun error(id: JsonElement, code: Int, message: String) = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        putJsonObject("error") {
            put("code", code)
            put("message", message)
        }
    }

    // region Tools

    private class Tool(
        val name: String,
        val title: String,
        val description: String,
        val inputSchema: JsonObject,
        val readOnly: Boolean,
        val destructive: Boolean = false,
        val call: (JsonObject) -> JsonObject,
    ) {
        fun definition() = buildJsonObject {
            put("name", name)
            put("title", title)
            put("description", description)
            put("inputSchema", inputSchema)
            putJsonObject("annotations") {
                put("title", title)
                put("readOnlyHint", readOnly)
                put("destructiveHint", destructive)
                put("openWorldHint", false)
            }
        }
    }

    private fun schema(required: List<String> = emptyList(), properties: JsonObjectBuilder.() -> Unit) = buildJsonObject {
        put("type", "object")
        if (required.isNotEmpty()) putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
        putJsonObject("properties", properties)
    }

    private fun JsonObjectBuilder.prop(name: String, type: String, description: String, extra: JsonObjectBuilder.() -> Unit = {}) =
        putJsonObject(name) {
            put("type", type)
            put("description", description)
            extra()
        }

    private fun JsonObjectBuilder.names(name: String, description: String) =
        prop(name, "array", description) { putJsonObject("items") { put("type", "string") } }

    private fun JsonObjectBuilder.pairs(name: String, description: String) = prop(name, "array", description) {
        putJsonObject("items") {
            put("type", "array")
            putJsonObject("items") { put("type", "string") }
            put("minItems", 2)
            put("maxItems", 2)
        }
    }

    private val tools = listOf(
        Tool(
            name = "list_players",
            title = "List players",
            description = "Lists the players in the CourtCompanion database with skill (1-5) and player types, the " +
                "available player types and the saved keep-apart pairs.",
            inputSchema = schema {},
            readOnly = true,
        ) { textResult(service.describeRoster()) },
        Tool(
            name = "upsert_players",
            title = "Add or update players",
            description = "Adds players, or updates existing ones matched by name (ignoring case). Omitted fields keep " +
                "their value (new players default to skill 3, no types). Types not yet in the type list are added. " +
                "Saved to the desktop app's database; use export_roster to make a file for the Android app.",
            inputSchema = schema(listOf("players")) {
                prop("players", "array", "The players to add or change.") {
                    putJsonObject("items") {
                        put("type", "object")
                        putJsonArray("required") { add(JsonPrimitive("name")) }
                        putJsonObject("properties") {
                            prop("name", "string", "The player's name (how the coach refers to them).")
                            prop("skill", "integer", "Overall skill level, 1 (beginner) to 5 (best).") {
                                put("minimum", 1)
                                put("maximum", 5)
                            }
                            names("tags", "Player types, e.g. Quick, Physical, Tall. Replaces the player's types.")
                            prop("rename", "string", "A new name for an existing player.")
                        }
                    }
                }
            },
            readOnly = false,
        ) { args -> textResult(service.upsertPlayers(playerSpecs(args))) },
        Tool(
            name = "remove_players",
            title = "Remove players",
            description = "Removes players from the database by name.",
            inputSchema = schema(listOf("names")) { names("names", "Names of the players to remove.") },
            readOnly = false,
            destructive = true,
        ) { args -> textResult(service.removePlayers(strings(args["names"]))) },
        Tool(
            name = "set_keep_apart",
            title = "Set keep-apart pairs",
            description = "Adds or removes saved pairs of players who should never be put in the same group.",
            inputSchema = schema {
                pairs("add", "Pairs of player names to keep apart, e.g. [[\"Anna\", \"Bea\"]].")
                pairs("remove", "Pairs of player names to no longer keep apart.")
            },
            readOnly = false,
        ) { args -> textResult(service.setKeepApart(namePairs(args["add"]), namePairs(args["remove"]))) },
        Tool(
            name = "generate_groups",
            title = "Divide players into groups",
            description = "Divides the present players into groups, with the same algorithm as the app, and returns " +
                "the groups as text and as an image. Give group_size (players per group) or group_count. Goals: " +
                "\"similar\" puts players of similar skill together (drills), \"balanced\" makes groups of equal " +
                "strength (teams playing each other), \"random\" just shuffles. Saved keep-apart pairs are always " +
                "respected. Doesn't change anything in the app.",
            inputSchema = schema {
                prop("group_size", "integer", "Players per group. Default 4 if neither this nor group_count is given.") { put("minimum", 1) }
                prop("group_count", "integer", "Number of groups (instead of group_size).") { put("minimum", 1) }
                prop("leftover", "string", "When players don't divide evenly by group_size: \"smaller_group\" (19 in " +
                    "groups of 4: 4,4,4,4,3; default) or \"bigger_groups\" (5,5,5,4).") {
                    putJsonArray("enum") { add(JsonPrimitive("smaller_group")); add(JsonPrimitive("bigger_groups")) }
                }
                prop("goal", "string", "similar (default), balanced or random.") {
                    putJsonArray("enum") { Goal.entries.forEach { add(JsonPrimitive(it.name.lowercase())) } }
                }
                prop("consider_types", "boolean", "similar: also group the same player types together; balanced: " +
                    "spread the player types evenly over the groups.")
                names("absent", "Names of players who are not here. Everyone else in the database is present.")
                names("present", "Instead of absent: exactly the players who are here.")
                pairs("keep_apart", "Extra pairs to keep apart this time only.")
                prop("seed", "integer", "Random seed, for a repeatable result.")
            },
            readOnly = true,
        ) { args -> generate(args) },
        Tool(
            name = "export_roster",
            title = "Export the player file",
            description = "Writes the player database as a JSON file that the CourtCompanion Android app imports " +
                "(Settings > Import players). Default location: ~/Documents/CourtCompanion/courtcompanion-players.json. " +
                "Returns the path.",
            inputSchema = schema { prop("path", "string", "Where to write the file (optional).") },
            readOnly = false,
        ) { args ->
            val path = (args["path"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            val file = service.exportRoster(path?.let { File(it.replaceFirst("~", System.getProperty("user.home"))) })
            textResult("Wrote ${service.roster().players.size} players to ${file.absolutePath}. Send this file to the " +
                "phone (e.g. by email, Drive or a chat app) and import it in CourtCompanion under Settings > Import players.")
        },
    )

    private fun generate(args: JsonObject): JsonObject {
        val size = int(args, "group_size")
        val count = int(args, "group_count")
        if (size != null && count != null) throw InputError("Give group_size or group_count, not both.")
        val goalName = string(args, "goal") ?: "similar"
        val goal = Goal.entries.find { it.name.equals(goalName, ignoreCase = true) }
            ?: throw InputError("Unknown goal \"$goalName\". Use similar, balanced or random.")
        val leftover = when (val l = string(args, "leftover")) {
            null, "smaller_group" -> Leftover.SMALLER_GROUP
            "bigger_groups" -> Leftover.BIGGER_GROUPS
            else -> throw InputError("Unknown leftover \"$l\". Use smaller_group or bigger_groups.")
        }
        val request = GroupRequest(
            sizing = if (count != null) Sizing.GROUP_COUNT else Sizing.GROUP_SIZE,
            number = (count ?: size ?: 4).also { if (it < 1) throw InputError("group_size and group_count must be at least 1.") },
            leftover = leftover,
            goal = goal,
            considerTypes = (args["consider_types"] as? JsonPrimitive)?.booleanOrNull ?: false,
            preferNewPartners = false,
        )
        val (players, grouping) = service.generate(
            request = request,
            absent = strings(args["absent"]),
            present = args["present"]?.let { strings(it) }?.takeIf { it.isNotEmpty() },
            extraKeepApart = namePairs(args["keep_apart"]),
            seed = (args["seed"] as? JsonPrimitive)?.longOrNull,
        )
        return contentResult(
            text = "${players.size} players present.\n" + service.describeGrouping(grouping, goal),
            png = service.image(grouping, goal),
        )
    }

    private fun string(args: JsonObject, name: String) = (args[name] as? JsonPrimitive)?.contentOrNull

    private fun int(args: JsonObject, name: String): Int? {
        val raw = args[name] as? JsonPrimitive ?: return null
        return raw.intOrNull ?: throw InputError("$name must be a whole number.")
    }

    /** Some clients send arrays as JSON text. */
    private fun array(element: JsonElement?): JsonArray? = when (element) {
        null, JsonNull -> null
        is JsonArray -> element
        is JsonPrimitive -> runCatching { json.parseToJsonElement(element.content) as? JsonArray }.getOrNull()
            ?: throw InputError("Expected a list, got ${element.content}.")
        else -> throw InputError("Expected a list.")
    }

    private fun strings(element: JsonElement?): List<String> =
        array(element)?.map { (it as? JsonPrimitive)?.contentOrNull ?: throw InputError("Expected names as text.") } ?: emptyList()

    private fun namePairs(element: JsonElement?): List<Pair<String, String>> = array(element)?.map { pair ->
        val names = strings(pair)
        if (names.size != 2) throw InputError("Each pair needs exactly two names, got $pair.")
        names[0] to names[1]
    } ?: emptyList()

    private fun playerSpecs(args: JsonObject): List<PlayerSpec> = array(args["players"])?.map { item ->
        val obj = item as? JsonObject ?: throw InputError("Each player must be an object with a name.")
        PlayerSpec(
            name = string(obj, "name") ?: throw InputError("Every player needs a name."),
            skill = int(obj, "skill"),
            tags = obj["tags"]?.let { strings(it).toSet() },
            rename = string(obj, "rename"),
        )
    } ?: throw InputError("Missing the \"players\" argument.")

    private fun textResult(text: String) = buildJsonObject {
        putJsonArray("content") { add(textContent(text)) }
        put("isError", false)
    }

    private fun errorResult(text: String) = buildJsonObject {
        putJsonArray("content") { add(textContent(text)) }
        put("isError", true)
    }

    private fun contentResult(text: String, png: ByteArray) = buildJsonObject {
        put("content", buildJsonArray {
            add(textContent(text))
            add(buildJsonObject {
                put("type", "image")
                put("data", Base64.getEncoder().encodeToString(png))
                put("mimeType", "image/png")
            })
        })
        put("isError", false)
    }

    private fun textContent(text: String) = buildJsonObject {
        put("type", "text")
        put("text", text)
    }

    // endregion

    companion object {
        const val VERSION = "0.1.0"

        /** Newest first; an unknown requested version gets the newest. */
        val PROTOCOL_VERSIONS = listOf("2025-06-18", "2025-03-26", "2024-11-05")

        private val INSTRUCTIONS = """
            CourtCompanion divides a youth basketball team into groups at practice, balancing skill (1-5) and player
            types (e.g. Quick, Physical, Tall). To build the player database, call list_players first, then
            upsert_players with all players at once (ask the coach for skill levels and types if not given), and
            set_keep_apart for players who shouldn't be grouped together. Then call export_roster and tell the coach
            the file path, so they can send it to their phone and import it in the Android app. To try a division,
            call generate_groups (it changes nothing) and show the result.
        """.trimIndent()
    }
}
