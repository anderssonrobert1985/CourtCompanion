package com.robert.courtcompanion.cli

import com.robert.courtcompanion.DesktopPlatform
import com.robert.courtcompanion.data.AppRepository
import com.robert.courtcompanion.data.RosterJson
import com.robert.courtcompanion.grouping.Goal
import com.robert.courtcompanion.grouping.GroupRequest
import com.robert.courtcompanion.grouping.Grouping
import com.robert.courtcompanion.grouping.GroupingEngine
import com.robert.courtcompanion.grouping.averageSkill
import com.robert.courtcompanion.model.KeepApart
import com.robert.courtcompanion.model.MAX_SKILL
import com.robert.courtcompanion.model.MIN_SKILL
import com.robert.courtcompanion.model.Player
import com.robert.courtcompanion.model.Roster
import com.robert.courtcompanion.ui.RosterEdits
import java.io.File
import kotlin.random.Random

/** Bad input from the user or the MCP client, with a message saying how to fix it. */
class InputError(message: String) : Exception(message)

/** A player to add or change, by name. Null fields keep their current value (or the default for new players). */
data class PlayerSpec(val name: String, val skill: Int? = null, val tags: Set<String>? = null, val rename: String? = null)

/**
 * What the command line and the MCP server can do, without any UI: edit the same player database as the desktop
 * app, divide players into groups, and export the player file for the Android app.
 */
class GroupService(
    val repository: AppRepository,
    /** Where the player file is exported when no file is given. */
    val exportDir: File,
) {
    fun roster(): Roster = repository.roster.value

    /** Adds or updates players by name; returns a summary. Unknown tags are added to the type list. */
    fun upsertPlayers(specs: List<PlayerSpec>): String {
        if (specs.isEmpty()) throw InputError("Give at least one player.")
        for (spec in specs) {
            if (spec.name.isBlank()) throw InputError("Every player needs a name.")
            spec.skill?.let { if (it !in MIN_SKILL..MAX_SKILL) throw InputError("${spec.name}: skill must be $MIN_SKILL–$MAX_SKILL, not $it.") }
        }
        val added = ArrayList<String>()
        val updated = ArrayList<String>()
        repository.updateRoster { start ->
            specs.fold(start) { r, spec ->
                val existing = r.playerNamed(spec.name)
                val tagged = spec.tags.orEmpty().fold(r) { acc, tag -> RosterEdits.addTag(acc, tag.trim()) }
                val player = (existing ?: Player(name = spec.name.trim())).let { p ->
                    p.copy(
                        name = spec.rename?.trim()?.takeIf { it.isNotEmpty() } ?: p.name,
                        skill = spec.skill ?: p.skill,
                        tags = spec.tags?.map { it.trim() }?.toSet() ?: p.tags,
                    )
                }
                (if (existing == null) added else updated) += player.name
                RosterEdits.savePlayer(tagged, player)
            }
        }
        return buildString {
            if (added.isNotEmpty()) append("Added ${added.size}: ${added.joinToString(", ")}. ")
            if (updated.isNotEmpty()) append("Updated ${updated.size}: ${updated.joinToString(", ")}. ")
            append("The roster now has ${roster().players.size} players.")
        }
    }

    fun removePlayers(names: List<String>): String {
        val roster = roster()
        val ids = names.map { player(roster, it).id }.toSet()
        repository.updateRoster { r -> ids.fold(r) { acc, id -> RosterEdits.deletePlayer(acc, id) } }
        return "Removed ${ids.size} player(s). The roster now has ${roster().players.size} players."
    }

    fun setKeepApart(add: List<Pair<String, String>>, remove: List<Pair<String, String>>): String {
        val roster = roster()
        val adds = add.map { (a, b) -> player(roster, a).id to player(roster, b).id }
        val removes = remove.map { (a, b) -> player(roster, a).id to player(roster, b).id }
        repository.updateRoster { r ->
            val removed = removes.fold(r) { acc, (a, b) -> acc.copy(keepApart = acc.keepApart.filterNot { it.matches(a, b) }) }
            adds.fold(removed) { acc, (a, b) -> RosterEdits.addKeepApart(acc, a, b) }
        }
        return "Keep-apart pairs: " + describePairs(roster()).ifEmpty { "none" }
    }

    /** Divides the present players. Doesn't change the app's history of today's rounds. */
    fun generate(
        request: GroupRequest,
        absent: List<String> = emptyList(),
        present: List<String>? = null,
        extraKeepApart: List<Pair<String, String>> = emptyList(),
        seed: Long? = null,
    ): Pair<List<Player>, Grouping> {
        val roster = roster()
        if (roster.players.isEmpty()) throw InputError("The roster is empty. Add players first.")
        val players = if (present != null) {
            present.map { player(roster, it) }.distinctBy { it.id }
        } else {
            val missing = absent.map { player(roster, it).id }.toSet()
            roster.players.filter { it.id !in missing }
        }
        if (players.isEmpty()) throw InputError("Nobody is present.")
        val extra = extraKeepApart.map { (a, b) -> KeepApart(player(roster, a).id, player(roster, b).id) }
        val grouping = GroupingEngine.generate(
            players = players,
            request = request,
            keepApart = roster.keepApart + extra,
            random = seed?.let(::Random) ?: Random.Default,
        )
        return players to grouping
    }

    /** Writes the player file that the apps import, at [target] or in [exportDir]. */
    fun exportRoster(target: File? = null): File {
        val file = target ?: File(exportDir, "courtcompanion-players.json")
        file.absoluteFile.parentFile?.mkdirs()
        file.writeText(RosterJson.write(roster()))
        return file
    }

    fun importRoster(text: String): Roster {
        val roster = try {
            RosterJson.parse(text)
        } catch (e: IllegalArgumentException) {
            throw InputError(e.message ?: "Not a player file.")
        }
        repository.updateRoster { roster }
        repository.updateSession { it.copy(absent = emptySet(), extraKeepApart = emptyList(), rounds = emptyList()) }
        return roster
    }

    fun describeRoster(): String {
        val r = roster()
        if (r.players.isEmpty()) return "The roster is empty. Player types: ${r.tags.joinToString(", ")}."
        return buildString {
            appendLine("${r.players.size} players (skill $MIN_SKILL–$MAX_SKILL):")
            for (p in r.players.sortedBy { it.name.lowercase() }) {
                append("- ${p.name}: skill ${p.skill}")
                if (p.tags.isNotEmpty()) append(", ${p.tags.sorted().joinToString(", ")}")
                appendLine()
            }
            appendLine("Player types: ${r.tags.joinToString(", ").ifEmpty { "none" }}")
            append("Keep apart: ${describePairs(r).ifEmpty { "none" }}")
        }
    }

    fun describeGrouping(grouping: Grouping, goal: Goal): String = buildString {
        val label = if (goal == Goal.BALANCED) "Team" else "Group"
        grouping.groups.forEachIndexed { i, g ->
            appendLine("$label ${i + 1} (${g.size} players, avg skill ${"%.1f".format(g.averageSkill())}): " +
                g.joinToString(", ") { p -> p.name + " (" + (listOf(p.skill.toString()) + p.tags.sorted()).joinToString(", ") + ")" })
        }
        if (grouping.keepApartViolations > 0) appendLine("Note: ${grouping.keepApartViolations} keep-apart pair(s) could not be separated.")
    }.trimEnd()

    fun image(grouping: Grouping, goal: Goal): ByteArray = GroupImage.render(grouping, goal)

    private fun describePairs(r: Roster) = r.keepApart.joinToString(", ") { "${r.player(it.a)?.name} ↔ ${r.player(it.b)?.name}" }

    private fun player(roster: Roster, name: String): Player =
        roster.playerNamed(name) ?: roster.player(name.trim())
            ?: throw InputError("No player called \"$name\". Players: ${roster.players.joinToString(", ") { it.name }}.")

    companion object {
        /** The desktop app's database, and ~/Documents/CourtCompanion for exports. */
        fun default(): GroupService {
            val home = File(System.getProperty("user.home"))
            val documents = File(home, "Documents").takeIf { it.isDirectory } ?: home
            return GroupService(AppRepository(DesktopPlatform.dataDir()), File(documents, "CourtCompanion"))
        }
    }
}
