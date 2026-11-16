package com.robert.courtcompanion.grouping

import kotlinx.serialization.Serializable

/** Whether [GroupRequest.number] is the players per group or the number of groups. */
@Serializable
enum class Sizing { GROUP_SIZE, GROUP_COUNT }

/** What to do when the players don't divide evenly into groups of the requested size. */
@Serializable
enum class Leftover {
    /** 19 players in groups of 4: 4, 4, 4, 4, 3. */
    SMALLER_GROUP,

    /** 19 players in groups of 4: 5, 5, 5, 4. */
    BIGGER_GROUPS,
}

@Serializable
enum class Goal {
    /** Players of similar skill in each group (drills). */
    SIMILAR,

    /** Groups of equal strength (teams playing each other). */
    BALANCED,

    /** Just shuffle. Keep-apart pairs and new partners are still respected. */
    RANDOM,
}

/** One "divide the players" request, as set up on the home screen. Saved so the next practice starts from it. */
@Serializable
data class GroupRequest(
    val sizing: Sizing = Sizing.GROUP_SIZE,
    /** Players per group ([Sizing.GROUP_SIZE]) or number of groups ([Sizing.GROUP_COUNT]). */
    val number: Int = 4,
    val leftover: Leftover = Leftover.SMALLER_GROUP,
    val goal: Goal = Goal.SIMILAR,
    /**
     * With [Goal.SIMILAR]: also put players of the same type together (skill still comes first).
     * With [Goal.BALANCED]: spread the player types evenly over the groups.
     */
    val considerTypes: Boolean = false,
    /** Avoid pairs who were already together in earlier rounds today. */
    val preferNewPartners: Boolean = true,
)
