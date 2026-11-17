package com.robert.courtcompanion.grouping

import com.robert.courtcompanion.model.KeepApart
import com.robert.courtcompanion.model.Player
import kotlin.math.abs
import kotlin.random.Random

/** A finished division into groups. */
data class Grouping(
    val groups: List<List<Player>>,
    /** Keep-apart pairs that had to share a group anyway (only when there was no other way). */
    val keepApartViolations: Int,
    /** Pairs who were together in an earlier round today as well. */
    val repeatedPairs: Int,
) {
    /** Player ids per group, as stored in today's history. */
    fun ids(): List<List<String>> = groups.map { g -> g.map { it.id } }
}

fun List<Player>.averageSkill(): Double = if (isEmpty()) 0.0 else sumOf { it.skill }.toDouble() / size

/**
 * Divides players into groups. Every candidate division gets a cost (lower is better) built from the request's goal,
 * keep-apart pairs and today's earlier rounds; a randomized local search from many random starts finds good
 * divisions, and one of the (near-)best ones is picked at random. So players of equal skill get mixed differently on
 * every press of "Generate", while the balance stays as good as possible.
 */
object GroupingEngine {
    private const val KEEP_APART_COST = 1000.0
    private const val REPEAT_COST = 2.0
    private const val SKILL_WEIGHT = 10.0
    private const val TYPE_WEIGHT = 1.0
    private const val SPREAD_WEIGHT = 2.0
    private const val TYPE_SPREAD_WEIGHT = 3.0

    /** Candidates this much worse than the best still count as equally good, to pick from at random. */
    private const val TOLERANCE = 0.5

    fun generate(
        players: List<Player>,
        request: GroupRequest,
        keepApart: List<KeepApart> = emptyList(),
        /** Earlier rounds today: each round is a list of groups of player ids. */
        history: List<List<List<String>>> = emptyList(),
        /** A division to avoid repeating exactly, if an equally good different one exists (by default the last round). */
        notLike: List<List<String>>? = history.lastOrNull(),
        random: Random = Random.Default,
        restarts: Int = 32,
    ): Grouping {
        val sizes = GroupSizes.of(players.size, request)
        if (sizes.isEmpty()) return Grouping(emptyList(), 0, 0)
        val problem = Problem(players, request, keepApart, history)

        val candidates = ArrayList<Candidate>()
        repeat(restarts) { i ->
            val start = when {
                request.goal == Goal.SIMILAR && i % 2 == 0 -> problem.sortedStart(sizes, random)
                request.goal == Goal.BALANCED && i % 2 == 0 -> problem.snakeStart(sizes, random)
                else -> problem.randomStart(sizes, random)
            }
            candidates += problem.improve(start, random)
        }

        val best = candidates.minOf { it.cost }
        val pool = candidates.filter { it.cost <= best + TOLERANCE }.distinctBy { it.key }
        // A new press should give a new division when there is an equally good one.
        val previous = notLike?.let { round -> problem.keyOfIds(round) }
        val fresh = pool.filter { it.key != previous }.ifEmpty { pool }
        val chosen = fresh[random.nextInt(fresh.size)]
        return problem.toGrouping(chosen.groups)
    }

    private class Candidate(val groups: List<IntArray>, val cost: Double, val key: Set<Set<Int>>)

    private class Problem(
        val players: List<Player>,
        val request: GroupRequest,
        keepApart: List<KeepApart>,
        history: List<List<List<String>>>,
    ) {
        val n = players.size
        val index = players.withIndex().associate { (i, p) -> p.id to i }
        val skill = IntArray(n) { players[it].skill }
        val tagNames = players.flatMap { it.tags }.distinct()
        val tags = LongArray(n) { i -> tagNames.foldIndexed(0L) { t, acc, name -> if (name in players[i].tags) acc or (1L shl (t % 64)) else acc } }
        val apart = Array(n) { BooleanArray(n) }
        val together = Array(n) { IntArray(n) }
        val meanSkill = skill.average()
        val skillVariance = skill.sumOf { (it - meanSkill) * (it - meanSkill) } / n
        val tagShare = DoubleArray(tagNames.size) { t -> players.count { tagNames[t] in it.tags }.toDouble() / n }

        init {
            for (pair in keepApart) {
                val a = index[pair.a] ?: continue
                val b = index[pair.b] ?: continue
                if (a != b) {
                    apart[a][b] = true
                    apart[b][a] = true
                }
            }
            if (request.preferNewPartners) {
                for (round in history) for (group in round) {
                    val members = group.mapNotNull { index[it] }
                    for (x in members) for (y in members) if (x != y) together[x][y]++
                }
            }
        }

        fun keyOf(groups: List<IntArray>): Set<Set<Int>> = groups.map { it.toSet() }.toSet()

        fun keyOfIds(round: List<List<String>>): Set<Set<Int>> = round.map { g -> g.mapNotNull { index[it] }.toSet() }.toSet()

        fun cost(group: IntArray): Double {
            var c = 0.0
            val size = group.size
            for (x in 0 until size) for (y in x + 1 until size) {
                val a = group[x]
                val b = group[y]
                if (apart[a][b]) c += KEEP_APART_COST
                c += REPEAT_COST * together[a][b]
            }
            if (size == 0) return c
            when (request.goal) {
                Goal.SIMILAR -> {
                    val mean = group.sumOf { skill[it] }.toDouble() / size
                    c += SKILL_WEIGHT * group.sumOf { (skill[it] - mean) * (skill[it] - mean) }
                    if (request.considerTypes && size > 1) {
                        var diff = 0
                        for (x in 0 until size) for (y in x + 1 until size) {
                            diff += (tags[group[x]] xor tags[group[y]]).countOneBits()
                        }
                        c += TYPE_WEIGHT * diff / (size - 1)
                    }
                }
                Goal.BALANCED -> {
                    val total = group.sumOf { skill[it] }.toDouble()
                    val off = total - size * meanSkill
                    c += SKILL_WEIGHT * off * off
                    // Equal totals from very different players (5 + 1 against 3 + 3) count as less balanced.
                    val mean = total / size
                    val variance = group.sumOf { (skill[it] - mean) * (skill[it] - mean) } / size
                    c += SPREAD_WEIGHT * abs(variance - skillVariance)
                    if (request.considerTypes) {
                        for (t in tagNames.indices) {
                            val bit = 1L shl (t % 64)
                            val count = group.count { tags[it] and bit != 0L }
                            val expected = size * tagShare[t]
                            c += TYPE_SPREAD_WEIGHT * (count - expected) * (count - expected)
                        }
                    }
                }
                Goal.RANDOM -> Unit
            }
            return c
        }

        fun randomStart(sizes: List<Int>, random: Random): List<IntArray> = split((0 until n).shuffled(random), sizes)

        /** Players sorted by skill (random order within a level), cut into consecutive groups: ideal for [Goal.SIMILAR]. */
        fun sortedStart(sizes: List<Int>, random: Random): List<IntArray> {
            val order = (0 until n).shuffled(random).sortedByDescending { skill[it] }
            return split(order, sizes.shuffled(random))
        }

        /** A snake draft over the players sorted by skill: a good start for [Goal.BALANCED]. */
        fun snakeStart(sizes: List<Int>, random: Random): List<IntArray> {
            val order = (0 until n).shuffled(random).sortedByDescending { skill[it] }
            val groups = sizes.map { ArrayList<Int>(it) }
            val next = order.iterator()
            var forward = true
            while (next.hasNext()) {
                val round = if (forward) groups.indices else groups.indices.reversed()
                for (g in round) if (groups[g].size < sizes[g] && next.hasNext()) groups[g].add(next.next())
                forward = !forward
            }
            return groups.map { it.toIntArray() }
        }

        private fun split(order: List<Int>, sizes: List<Int>): List<IntArray> {
            var at = 0
            return sizes.map { size -> IntArray(size) { order[at + it] }.also { at += size } }
        }

        /** Swaps players between groups while that lowers the cost. */
        fun improve(start: List<IntArray>, random: Random): Candidate {
            val groups = start.map { it.copyOf() }
            val costs = DoubleArray(groups.size) { cost(groups[it]) }
            val slots = groups.flatMapIndexed { g, members -> members.indices.map { g to it } }
            var improved = true
            var passes = 0
            while (improved && passes++ < 50) {
                improved = false
                val order = slots.shuffled(random)
                for (i in order.indices) for (j in i + 1 until order.size) {
                    val (ga, ia) = order[i]
                    val (gb, ib) = order[j]
                    if (ga == gb) continue
                    val a = groups[ga]
                    val b = groups[gb]
                    val pa = a[ia]
                    val pb = b[ib]
                    a[ia] = pb
                    b[ib] = pa
                    val ca = cost(a)
                    val cb = cost(b)
                    if (ca + cb < costs[ga] + costs[gb] - 1e-9) {
                        costs[ga] = ca
                        costs[gb] = cb
                        improved = true
                    } else {
                        a[ia] = pa
                        b[ib] = pb
                    }
                }
            }
            return Candidate(groups, costs.sum(), keyOf(groups))
        }

        fun toGrouping(groups: List<IntArray>): Grouping {
            var violations = 0
            var repeats = 0
            for (g in groups) for (x in g.indices) for (y in x + 1 until g.size) {
                if (apart[g[x]][g[y]]) violations++
                if (together[g[x]][g[y]] > 0) repeats++
            }
            val ordered = groups
                .map { g -> g.map { players[it] }.sortedWith(compareByDescending<Player> { it.skill }.thenBy { it.name.lowercase() }) }
                .let { list -> if (request.goal == Goal.SIMILAR) list.sortedByDescending { it.averageSkill() } else list }
            return Grouping(ordered, violations, repeats)
        }
    }
}
