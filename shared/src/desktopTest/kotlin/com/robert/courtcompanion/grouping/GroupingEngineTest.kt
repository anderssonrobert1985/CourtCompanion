package com.robert.courtcompanion.grouping

import com.robert.courtcompanion.model.KeepApart
import com.robert.courtcompanion.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class GroupingEngineTest {
    /** 19 players: skills 5,5,5,5,4,4,4,4,3,3,3,3,2,2,2,2,1,1,1, with a few types. */
    private val players = List(19) { i ->
        val skill = 5 - i / 4
        val tags = when (i % 3) {
            0 -> setOf("Quick")
            1 -> setOf("Tall")
            else -> setOf("Physical")
        }
        Player(id = "p$i", name = "Player $i", skill = skill, tags = tags)
    }

    private fun Grouping.allIds() = groups.flatten().map { it.id }

    @Test
    fun everyPlayerIsPlacedOnceInGroupsOfTheRightSizes() {
        val g = GroupingEngine.generate(players, GroupRequest(number = 4), random = Random(1))
        assertEquals(listOf(4, 4, 4, 4, 3), g.groups.map { it.size }.sortedDescending())
        assertEquals(players.map { it.id }.sorted(), g.allIds().sorted())
    }

    @Test
    fun similarGoalGroupsEqualSkills() {
        val g = GroupingEngine.generate(players, GroupRequest(number = 4, goal = Goal.SIMILAR), random = Random(2))
        // Skills come in fours, so the best division has every group at one level (the 3 ones together).
        for (group in g.groups) assertEquals(group.toString(), 1, group.map { it.skill }.distinct().size)
        // Sorted best first.
        assertEquals(g.groups.map { it.averageSkill() }.sortedDescending(), g.groups.map { it.averageSkill() })
    }

    @Test
    fun balancedGoalMakesTeamsOfEqualStrength() {
        for (seed in 1..5) {
            val g = GroupingEngine.generate(players.take(18), GroupRequest(sizing = Sizing.GROUP_COUNT, number = 3, goal = Goal.BALANCED), random = Random(seed))
            val totals = g.groups.map { team -> team.sumOf { it.skill } }
            assertTrue("Totals $totals", totals.max() - totals.min() <= 1)
        }
    }

    @Test
    fun balancedWithTypesSpreadsTheTypes() {
        val g = GroupingEngine.generate(
            players.take(18),
            GroupRequest(sizing = Sizing.GROUP_COUNT, number = 6, goal = Goal.BALANCED, considerTypes = true),
            random = Random(3),
        )
        // 18 players, 6 of each type, 6 teams of 3: one of each type per team.
        for (team in g.groups) assertEquals(team.toString(), setOf("Quick", "Tall", "Physical"), team.flatMap { it.tags }.toSet())
        val totals = g.groups.map { team -> team.sumOf { it.skill } }
        assertTrue("Totals $totals", totals.max() - totals.min() <= 2)
    }

    @Test
    fun keepApartPairsAreSeparated() {
        // p0..p3 are all skill 5 and would naturally form a group.
        val apart = listOf(KeepApart("p0", "p1"), KeepApart("p2", "p3"))
        repeat(5) { seed ->
            val g = GroupingEngine.generate(players, GroupRequest(number = 4), keepApart = apart, random = Random(seed))
            assertEquals(0, g.keepApartViolations)
            for (group in g.groups) {
                val ids = group.map { it.id }
                assertTrue(!(ids.contains("p0") && ids.contains("p1")))
                assertTrue(!(ids.contains("p2") && ids.contains("p3")))
            }
        }
    }

    @Test
    fun equalPlayersAreMixedDifferentlyEachTime() {
        val equal = List(12) { Player(id = "e$it", name = "E$it", skill = 3) }
        val request = GroupRequest(number = 3, goal = Goal.SIMILAR, preferNewPartners = false)
        val results = (1..10).map { seed ->
            GroupingEngine.generate(equal, request, random = Random(seed)).groups.map { g -> g.map { it.id }.toSet() }.toSet()
        }.toSet()
        assertTrue("Only ${results.size} different divisions", results.size >= 5)
    }

    @Test
    fun newPartnersAvoidEarlierPairs() {
        val equal = List(12) { Player(id = "e$it", name = "E$it", skill = 3) }
        val request = GroupRequest(number = 3, goal = Goal.RANDOM, preferNewPartners = true)
        val first = GroupingEngine.generate(equal, request, random = Random(1))
        val second = GroupingEngine.generate(equal, request, history = listOf(first.ids()), random = Random(2))
        assertEquals(0, second.repeatedPairs)
    }

    @Test
    fun notLikeAvoidsTheSameDivision() {
        val equal = List(8) { Player(id = "e$it", name = "E$it", skill = 3) }
        val request = GroupRequest(number = 4, goal = Goal.RANDOM, preferNewPartners = false)
        val first = GroupingEngine.generate(equal, request, random = Random(1))
        repeat(5) { seed ->
            val again = GroupingEngine.generate(equal, request, notLike = first.ids(), random = Random(seed))
            val key = { g: Grouping -> g.ids().map { it.toSet() }.toSet() }
            assertTrue(key(again) != key(first))
        }
    }

    @Test
    fun similarWithTypesKeepsSkillFirst() {
        val g = GroupingEngine.generate(players, GroupRequest(number = 2, goal = Goal.SIMILAR, considerTypes = true), random = Random(4))
        val spread = g.groups.filter { it.size == 2 }.sumOf { abs(it[0].skill - it[1].skill) }
        assertEquals(0, spread)
    }
}
