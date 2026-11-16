package com.robert.courtcompanion.grouping

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupSizesTest {
    private fun sizes(players: Int, request: GroupRequest) = GroupSizes.of(players, request)

    @Test
    fun unevenGroupSizeMakesOneSmallerGroupOrBiggerGroups() {
        assertEquals(listOf(4, 4, 4, 4, 3), sizes(19, GroupRequest(number = 4)))
        assertEquals(listOf(5, 5, 5, 4), sizes(19, GroupRequest(number = 4, leftover = Leftover.BIGGER_GROUPS)))
        assertEquals(listOf(4, 4, 4, 4, 4), sizes(20, GroupRequest(number = 4)))
    }

    @Test
    fun smallerGroupsNeverDifferByMoreThanOne() {
        // 17 in groups of 4 gives 4,4,3,3,3 rather than a lone player.
        assertEquals(listOf(4, 4, 3, 3, 3), sizes(17, GroupRequest(number = 4)))
        assertEquals(listOf(2, 2, 2, 1), sizes(7, GroupRequest(number = 2)))
        assertEquals(listOf(3, 2, 2), sizes(7, GroupRequest(number = 2, leftover = Leftover.BIGGER_GROUPS)))
    }

    @Test
    fun groupCountSplitsEvenly() {
        assertEquals(listOf(7, 6, 6), sizes(19, GroupRequest(sizing = Sizing.GROUP_COUNT, number = 3)))
        assertEquals(listOf(1, 1), sizes(2, GroupRequest(sizing = Sizing.GROUP_COUNT, number = 6)))
    }

    @Test
    fun edgeCases() {
        assertEquals(emptyList<Int>(), sizes(0, GroupRequest()))
        assertEquals(listOf(3), sizes(3, GroupRequest(number = 4, leftover = Leftover.BIGGER_GROUPS)))
        assertEquals(listOf(3), sizes(3, GroupRequest(number = 4)))
    }
}
