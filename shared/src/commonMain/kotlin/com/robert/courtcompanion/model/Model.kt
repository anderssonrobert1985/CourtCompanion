package com.robert.courtcompanion.model

import kotlinx.serialization.Serializable
import java.util.UUID

const val MIN_SKILL = 1
const val MAX_SKILL = 5

/** A player in the team's database. [tags] are player types such as "Quick" or "Tall" (names from [Roster.tags]). */
@Serializable
data class Player(
    val id: String = newId(),
    val name: String,
    /** Overall skill level, [MIN_SKILL]..[MAX_SKILL]. */
    val skill: Int = 3,
    val tags: Set<String> = emptySet(),
)

/** Two players (by id) who should not end up in the same group. */
@Serializable
data class KeepApart(val a: String, val b: String) {
    fun involves(id: String) = a == id || b == id
    fun matches(x: String, y: String) = (a == x && b == y) || (a == y && b == x)
}

/**
 * The team's player database: players, the editable list of player types, and saved keep-apart pairs. This is also
 * the export/import format shared by the Android app, the desktop app and the command line tool.
 */
@Serializable
data class Roster(
    val version: Int = 1,
    val players: List<Player> = emptyList(),
    val tags: List<String> = DEFAULT_TAGS,
    val keepApart: List<KeepApart> = emptyList(),
) {
    fun player(id: String): Player? = players.find { it.id == id }

    /** A player by name, ignoring case and surrounding spaces. */
    fun playerNamed(name: String): Player? = players.find { it.name.trim().equals(name.trim(), ignoreCase = true) }

    companion object {
        val DEFAULT_TAGS = listOf("Quick", "Physical", "Tall")
    }
}

fun newId(): String = UUID.randomUUID().toString().take(8)

@Serializable
enum class Language { EN, SV }
