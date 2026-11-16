package com.robert.courtcompanion.ui

import androidx.lifecycle.ViewModel
import com.robert.courtcompanion.data.AppRepository
import com.robert.courtcompanion.grouping.GroupRequest
import com.robert.courtcompanion.grouping.Grouping
import com.robert.courtcompanion.grouping.GroupingEngine
import com.robert.courtcompanion.model.KeepApart
import com.robert.courtcompanion.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/** Attendance, the request, and generating groups at practice. Roster and session changes go to [repository]. */
class PracticeViewModel(
    val repository: AppRepository,
    private val random: Random = Random.Default,
) : ViewModel() {
    val roster = repository.roster
    val session = repository.session
    val settings = repository.settings

    private val _result = MutableStateFlow<Grouping?>(null)

    /** The last generated groups, shown on the result screen. */
    val result: StateFlow<Grouping?> = _result.asStateFlow()

    fun presentPlayers(): List<Player> {
        val absent = session.value.absent
        return roster.value.players.filter { it.id !in absent }
    }

    fun togglePresent(id: String) = repository.updateSession { s ->
        s.copy(absent = if (id in s.absent) s.absent - id else s.absent + id)
    }

    fun setAllPresent(present: Boolean) = repository.updateSession { s ->
        s.copy(absent = if (present) emptySet() else roster.value.players.map { it.id }.toSet())
    }

    fun updateRequest(change: (GroupRequest) -> GroupRequest) =
        repository.updateSettings { it.copy(request = change(it.request)) }

    fun addKeepApartToday(a: String, b: String) {
        if (a == b) return
        repository.updateSession { s ->
            if (s.extraKeepApart.any { it.matches(a, b) }) s else s.copy(extraKeepApart = s.extraKeepApart + KeepApart(a, b))
        }
    }

    fun removeKeepApartToday(pair: KeepApart) = repository.updateSession { s -> s.copy(extraKeepApart = s.extraKeepApart - pair) }

    /** Generates a new round with the current request and adds it to today's history. Returns false if nobody is here. */
    fun generate(): Boolean {
        repository.refreshDay()
        val grouping = compute(history = session.value.rounds) ?: return false
        repository.updateSession { it.copy(rounds = it.rounds + listOf(grouping.ids())) }
        _result.value = grouping
        return true
    }

    /** Replaces the last round with another one that is as good (the last one wasn't used). */
    fun reroll() {
        val rounds = session.value.rounds
        if (rounds.isEmpty()) {
            generate()
            return
        }
        val grouping = compute(history = rounds.dropLast(1), notLike = rounds.last()) ?: return
        repository.updateSession { it.copy(rounds = it.rounds.dropLast(1) + listOf(grouping.ids())) }
        _result.value = grouping
    }

    fun resetToday() {
        repository.updateSession { it.copy(absent = emptySet(), extraKeepApart = emptyList(), rounds = emptyList()) }
        _result.value = null
    }

    private fun compute(history: List<List<List<String>>>, notLike: List<List<String>>? = history.lastOrNull()): Grouping? {
        val players = presentPlayers()
        if (players.isEmpty()) return null
        return GroupingEngine.generate(
            players = players,
            request = settings.value.request,
            keepApart = roster.value.keepApart + session.value.extraKeepApart,
            history = history,
            notLike = notLike,
            random = random,
        )
    }
}
