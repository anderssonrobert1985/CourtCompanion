package com.robert.courtcompanion.grouping

object GroupSizes {
    /**
     * The group sizes for [players] players, largest first and never differing by more than one.
     * 19 players in groups of 4 gives 4, 4, 4, 4, 3 ([Leftover.SMALLER_GROUP]) or 5, 5, 5, 4 ([Leftover.BIGGER_GROUPS]).
     */
    fun of(players: Int, request: GroupRequest): List<Int> {
        if (players <= 0) return emptyList()
        val n = request.number.coerceAtLeast(1)
        val groups = when (request.sizing) {
            Sizing.GROUP_COUNT -> n
            Sizing.GROUP_SIZE -> when (request.leftover) {
                Leftover.SMALLER_GROUP -> (players + n - 1) / n
                Leftover.BIGGER_GROUPS -> players / n
            }
        }.coerceIn(1, players)
        val base = players / groups
        val extra = players % groups
        return List(groups) { if (it < extra) base + 1 else base }
    }
}
