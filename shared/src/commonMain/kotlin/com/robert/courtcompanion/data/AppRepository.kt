package com.robert.courtcompanion.data

import com.robert.courtcompanion.grouping.GroupRequest
import com.robert.courtcompanion.model.KeepApart
import com.robert.courtcompanion.model.Language
import com.robert.courtcompanion.model.MAX_SKILL
import com.robert.courtcompanion.model.MIN_SKILL
import com.robert.courtcompanion.model.Roster
import com.robert.courtcompanion.model.newId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** App preferences, kept across practices. */
@Serializable
data class AppSettings(
    val language: Language = Language.EN,
    /** The last request, so the next practice starts from it. */
    val request: GroupRequest = GroupRequest(),
)

/** Today's practice: who is missing, one-off keep-apart pairs and the rounds generated so far. Reset every day. */
@Serializable
data class PracticeSession(
    val date: String = today().toString(),
    /** Ids of players who are not here. Everyone else in the roster counts as present. */
    val absent: Set<String> = emptySet(),
    /** Keep-apart pairs for today only, on top of the roster's saved ones. */
    val extraKeepApart: List<KeepApart> = emptyList(),
    /** Every generated round today, each a list of groups of player ids; used to mix partners. */
    val rounds: List<List<List<String>>> = emptyList(),
)

/** Today's date in the device's time zone. */
fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/**
 * Where [AppRepository] keeps its JSON documents, by name: files in a directory on Android and desktop, the
 * browser's local storage on the web.
 */
interface TextStore {
    /** The text saved under [name], or null if there is none. */
    fun read(name: String): String?

    /** Replaces the text saved under [name], as one step: a crash leaves either the old or the new text. */
    fun write(name: String, text: String)
}

/**
 * Stores the roster, settings and today's session as JSON documents in [store], and publishes them as [StateFlow]s
 * so every screen sees changes at once. Create one instance per store.
 */
class AppRepository(private val store: TextStore, private val today: () -> LocalDate = ::today) {
    private val rosterFile = "roster.json"
    private val settingsFile = "settings.json"
    private val sessionFile = "session.json"

    private val _roster = MutableStateFlow(read(rosterFile, Roster.serializer())?.let(RosterJson::normalize) ?: Roster())
    val roster: StateFlow<Roster> = _roster.asStateFlow()

    private val _settings = MutableStateFlow(read(settingsFile, AppSettings.serializer()) ?: AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _session = MutableStateFlow(
        read(sessionFile, PracticeSession.serializer())?.takeIf { it.date == today().toString() } ?: PracticeSession(today().toString()),
    )
    val session: StateFlow<PracticeSession> = _session.asStateFlow()

    fun updateRoster(change: (Roster) -> Roster): Unit = locked(this) {
        val next = RosterJson.normalize(change(_roster.value))
        write(rosterFile, Roster.serializer(), next)
        _roster.value = next
    }

    fun updateSettings(change: (AppSettings) -> AppSettings): Unit = locked(this) {
        val next = change(_settings.value)
        write(settingsFile, AppSettings.serializer(), next)
        _settings.value = next
    }

    /** Changes today's session; a session from an earlier day is replaced by a fresh one first. */
    fun updateSession(change: (PracticeSession) -> PracticeSession): Unit = locked(this) {
        val date = today().toString()
        val current = _session.value.takeIf { it.date == date } ?: PracticeSession(date)
        val next = change(current)
        write(sessionFile, PracticeSession.serializer(), next)
        _session.value = next
    }

    /** Starts the day over if the app was left open overnight. */
    fun refreshDay() {
        if (_session.value.date != today().toString()) updateSession { it }
    }

    private fun <T> read(name: String, serializer: KSerializer<T>): T? =
        runCatching { json.decodeFromString(serializer, store.read(name)!!) }.getOrNull()

    private fun <T> write(name: String, serializer: KSerializer<T>, value: T) =
        store.write(name, json.encodeToString(serializer, value))

    companion object {
        internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        }
    }
}

/** The roster's file format, used for export and import between devices. */
object RosterJson {
    fun write(roster: Roster): String = AppRepository.json.encodeToString(Roster.serializer(), normalize(roster))

    /** Parses an exported roster. Throws [IllegalArgumentException] with a readable message if it isn't one. */
    fun parse(text: String): Roster = try {
        normalize(AppRepository.json.decodeFromString(Roster.serializer(), text))
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Not a CourtCompanion player file: ${e.message?.lineSequence()?.firstOrNull()}", e)
    }

    /**
     * Cleans up a roster from any source: trims names, drops nameless players, clamps skills, gives every player a
     * unique id, adds tags used by players to the tag list and drops keep-apart pairs with unknown players.
     */
    fun normalize(roster: Roster): Roster {
        // Tag names are matched ignoring case; the tag list's spelling wins.
        val tags = (roster.tags + roster.players.flatMap { it.tags })
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        val canonical = tags.associateBy { it.lowercase() }
        val seen = HashSet<String>()
        val players = roster.players
        .filter { it.name.isNotBlank() }
        .map { p ->
            val id = p.id.takeIf { it.isNotBlank() && seen.add(it) } ?: newId().also { seen.add(it) }
            p.copy(
                id = id,
                name = p.name.trim(),
                skill = p.skill.coerceIn(MIN_SKILL, MAX_SKILL),
                tags = p.tags.mapNotNull { canonical[it.trim().lowercase()] }.toSet(),
            )
        }
        val ids = players.map { it.id }.toSet()
        val pairs = roster.keepApart
        .filter { it.a in ids && it.b in ids && it.a != it.b }
        .distinctBy { setOf(it.a, it.b) }
        return roster.copy(players = players, tags = tags, keepApart = pairs)
    }
}

/** Runs [block] holding [lock]'s monitor on the JVM, where the MCP server may call from several threads. */
internal expect inline fun <T> locked(lock: Any, block: () -> T): T
