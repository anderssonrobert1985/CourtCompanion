package com.robert.courtcompanion.cli

import com.robert.courtcompanion.grouping.Goal
import com.robert.courtcompanion.grouping.GroupRequest
import com.robert.courtcompanion.grouping.Leftover
import com.robert.courtcompanion.grouping.Sizing
import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """Usage: courtcompanion-cli <command>

Commands:
  list                   List the players, player types and keep-apart pairs.
  generate [--size N | --groups N] [--goal similar|balanced|random] [--types] [--bigger]
           [--absent "Name1,Name2"] [--seed N] [-o groups.png]
                         Divide the players (all except --absent) into groups of N, or into N groups
                         (default: groups of 4, similar level). --types also considers player types,
                         --bigger makes bigger groups instead of one smaller group when uneven.
                         -o also writes an image of the groups.
  export [-o file.json]  Write the player file for the Android app
                         (default: ~/Documents/CourtCompanion/courtcompanion-players.json).
  import <file.json>     Replace the player database with a player file.
  mcp                    Run as an MCP server over stdin/stdout, for Claude and other MCP clients.

The database is shared with the desktop app (~/.local/share/courtcompanion)."""

/** Bad arguments or input: printed without a stack trace, exit code 2 (1 for invalid data). */
private class CliError(message: String, val code: Int = 2) : Exception(message)

fun main(args: Array<String>) {
    // Images are drawn with Skia directly; make sure nothing tries to open a window.
    System.setProperty("java.awt.headless", "true")
    val code = try {
        dispatch(args.toList())
        0
    } catch (e: CliError) {
        System.err.println(e.message)
        e.code
    } catch (e: InputError) {
        System.err.println(e.message)
        1
    }
    exitProcess(code)
}

private fun dispatch(args: List<String>) {
    val command = args.firstOrNull() ?: throw CliError(USAGE)
    val rest = args.drop(1)
    when (command) {
        "list" -> println(GroupService.default().describeRoster())
        "generate" -> generate(rest)
        "export" -> export(rest)
        "import" -> import(rest)
        "mcp" -> McpServer(GroupService.default()).run()
        "help", "-h", "--help" -> println(USAGE)
        else -> throw CliError("Unknown command \"$command\".\n\n$USAGE")
    }
}

private class Options(val values: Map<String, String>, val flags: Set<String>, val positional: List<String>)

private fun options(args: List<String>, valued: Set<String>, flags: Set<String> = emptySet()): Options {
    val values = HashMap<String, String>()
    val set = HashSet<String>()
    val positional = ArrayList<String>()
    var i = 0
    while (i < args.size) {
        val a = args[i]
        when {
            a in valued -> values[a] = args.getOrNull(++i) ?: throw CliError("$a needs a value.")
            a in flags -> set += a
            a.startsWith("-") -> throw CliError("Unknown option $a.\n\n$USAGE")
            else -> positional += a
        }
        i++
    }
    return Options(values, set, positional)
}

private fun Options.int(name: String): Int? =
    values[name]?.let { it.toIntOrNull()?.takeIf { n -> n >= 1 } ?: throw CliError("$name needs a number of at least 1.") }

private fun generate(args: List<String>) {
    val o = options(args, valued = setOf("--size", "--groups", "--goal", "--absent", "--seed", "-o"), flags = setOf("--types", "--bigger"))
    if (o.positional.isNotEmpty()) throw CliError("Unexpected argument \"${o.positional.first()}\".\n\n$USAGE")
    val size = o.int("--size")
    val count = o.int("--groups")
    if (size != null && count != null) throw CliError("Give --size or --groups, not both.")
    val goal = o.values["--goal"]?.let { g -> Goal.entries.find { it.name.equals(g, ignoreCase = true) } ?: throw CliError("Unknown goal \"$g\".") }
        ?: Goal.SIMILAR
    val request = GroupRequest(
        sizing = if (count != null) Sizing.GROUP_COUNT else Sizing.GROUP_SIZE,
        number = count ?: size ?: 4,
        leftover = if ("--bigger" in o.flags) Leftover.BIGGER_GROUPS else Leftover.SMALLER_GROUP,
        goal = goal,
        considerTypes = "--types" in o.flags,
        preferNewPartners = false,
    )
    val service = GroupService.default()
    val absent = o.values["--absent"]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    val seed = o.values["--seed"]?.let { it.toLongOrNull() ?: throw CliError("--seed needs a number.") }
    val (players, grouping) = service.generate(request, absent = absent, seed = seed)
    println("${players.size} players present.")
    println(service.describeGrouping(grouping, goal))
    o.values["-o"]?.let {
        File(it).writeBytes(service.image(grouping, goal))
        println("Image: $it")
    }
}

private fun export(args: List<String>) {
    val o = options(args, valued = setOf("-o"))
    val service = GroupService.default()
    val file = service.exportRoster(o.values["-o"]?.let(::File))
    println("Wrote ${service.roster().players.size} players to ${file.path}")
}

private fun import(args: List<String>) {
    val path = args.singleOrNull() ?: throw CliError("Give one player file to import.\n\n$USAGE")
    val file = File(path)
    if (!file.isFile) throw CliError("No such file: $path")
    val roster = GroupService.default().importRoster(file.readText())
    println("Imported ${roster.players.size} players.")
}
