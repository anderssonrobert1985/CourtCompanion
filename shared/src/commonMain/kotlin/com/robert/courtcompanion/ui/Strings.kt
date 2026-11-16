package com.robert.courtcompanion.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.robert.courtcompanion.grouping.Goal
import com.robert.courtcompanion.model.Language

/** All UI text, in English and Swedish. Pick one with [Strings.of]; screens read it from [LocalStrings]. */
open class Strings {
    open val appName = "CourtCompanion"
    open val back = "Back"
    open val cancel = "Cancel"
    open val save = "Save"
    open val delete = "Delete"
    open val add = "Add"
    open val ok = "OK"

    // Home
    open val players = "Players"
    open val settings = "Settings"
    open fun present(present: Int, total: Int) = "Who's here? $present/$total"
    open val all = "All"
    open val none = "None"
    open fun missing(names: String) = "Missing: $names"
    open val everyoneHere = "Everyone is here"
    open val noPlayersYet = "No players yet. Add your team under Players, or import a player file in Settings."
    open val addPlayers = "Add players"
    open val divideInto = "Divide"
    open val groupsOf = "Groups of"
    open val numberOfGroups = "Number of groups"
    open fun playersPerGroup(n: Int) = if (n == 1) "player per group" else "players per group"
    open fun groupCount(n: Int) = if (n == 1) "group" else "groups"
    open val smallerGroup = "One smaller group"
    open val biggerGroups = "Bigger groups"
    open fun sizesPreview(players: Int, sizes: String) = "$players players → $sizes"
    open val goal = "Goal"
    open fun goalName(goal: Goal) = when (goal) {
        Goal.SIMILAR -> "Similar level"
        Goal.BALANCED -> "Even teams"
        Goal.RANDOM -> "Random"
    }
    open fun goalHelp(goal: Goal) = when (goal) {
        Goal.SIMILAR -> "Players of the same skill level play together. Good for drills."
        Goal.BALANCED -> "Every group equally strong. Good for games against each other."
        Goal.RANDOM -> "Just shuffle."
    }
    open val considerTypes = "Consider player types"
    open fun considerTypesHelp(goal: Goal) = when (goal) {
        Goal.SIMILAR -> "Same types together too (skill comes first)"
        Goal.BALANCED -> "Mix the types: e.g. one quick, one physical and one tall per team"
        Goal.RANDOM -> "Not used for random groups"
    }
    open val newPartners = "Mix partners"
    open fun newPartnersHelp(rounds: Int) =
        if (rounds == 0) "Avoid pairs from earlier rounds today" else "Avoid pairs from the $rounds earlier round${if (rounds == 1) "" else "s"} today"
    open val keepApartToday = "Keep apart today"
    open fun savedPairs(n: Int) = if (n == 0) "No saved pairs (add them under Players)" else "+ $n saved pair${if (n == 1) "" else "s"} from Players"
    open val addPair = "Add pair"
    open val pickTwo = "Pick two players to keep apart"
    open val generate = "Generate"
    open val notEnoughPlayers = "Select at least one player"

    // Result
    open fun round(n: Int) = "Round $n"
    open fun groupName(n: Int) = "Group $n"
    open fun teamName(n: Int) = "Team $n"
    open fun average(avg: String) = "avg $avg"
    open val reroll = "Re-roll"
    open val nextRound = "Next round"
    open val newRequest = "Change"
    open fun keepApartBroken(n: Int) = "$n keep-apart pair${if (n == 1) "" else "s"} could not be separated."
    open fun repeated(n: Int) = "$n pair${if (n == 1) " was" else "s were"} together earlier today too."

    // Players
    open fun playersTitle(n: Int) = "Players ($n)"
    open val newPlayer = "New player"
    open val editPlayer = "Edit player"
    open val name = "Name"
    open val skill = "Skill"
    open val types = "Player types"
    open val addType = "Add type"
    open val renameType = "Rename type"
    open val typeName = "Type name"
    open val keepApart = "Keep apart"
    open val keepApartHelp = "These pairs are never put in the same group, if at all possible."
    open fun deletePlayerQuestion(name: String) = "Delete $name?"
    open fun deleteTypeQuestion(name: String) = "Delete the type \"$name\"? It is removed from all players."

    // Settings
    open val language = "Language"
    open val data = "Player file"
    open val dataHelp = "Move the player list between devices: export it here and import it on the other device. " +
        "Claude can also create the file for you."
    open val export = "Export players"
    open val import = "Import players"
    open fun exported(where: String) = "Exported to $where"
    open fun importQuestion(players: Int) = "Replace your player list with the $players players in the file?"
    open val replace = "Replace"
    open fun imported(players: Int) = "Imported $players players"
    open fun importFailed(reason: String) = "Could not import: $reason"
    open val today = "Today's practice"
    open fun roundsToday(n: Int) = if (n == 0) "No rounds yet today" else "$n round${if (n == 1) "" else "s"} so far today"
    open val resetToday = "Start over"
    open val resetTodayHelp = "Forget today's rounds and mark everyone as present."

    companion object {
        fun of(language: Language): Strings = when (language) {
            Language.EN -> English
            Language.SV -> Swedish
        }

        fun languageName(language: Language) = when (language) {
            Language.EN -> "English"
            Language.SV -> "Svenska"
        }
    }
}

object English : Strings()

object Swedish : Strings() {
    override val back = "Tillbaka"
    override val cancel = "Avbryt"
    override val save = "Spara"
    override val delete = "Ta bort"
    override val add = "Lägg till"

    override val players = "Spelare"
    override val settings = "Inställningar"
    override fun present(present: Int, total: Int) = "Vilka är här? $present/$total"
    override val all = "Alla"
    override val none = "Ingen"
    override fun missing(names: String) = "Saknas: $names"
    override val everyoneHere = "Alla är här"
    override val noPlayersYet = "Inga spelare än. Lägg till laget under Spelare, eller importera en spelarfil i Inställningar."
    override val addPlayers = "Lägg till spelare"
    override val divideInto = "Dela upp"
    override val groupsOf = "Grupper om"
    override val numberOfGroups = "Antal grupper"
    override fun playersPerGroup(n: Int) = "spelare per grupp"
    override fun groupCount(n: Int) = if (n == 1) "grupp" else "grupper"
    override val smallerGroup = "En mindre grupp"
    override val biggerGroups = "Större grupper"
    override fun sizesPreview(players: Int, sizes: String) = "$players spelare → $sizes"
    override val goal = "Mål"
    override fun goalName(goal: Goal) = when (goal) {
        Goal.SIMILAR -> "Lika nivå"
        Goal.BALANCED -> "Jämna lag"
        Goal.RANDOM -> "Slump"
    }
    override fun goalHelp(goal: Goal) = when (goal) {
        Goal.SIMILAR -> "Spelare på samma nivå spelar ihop. Bra för övningar."
        Goal.BALANCED -> "Alla grupper lika starka. Bra för spel mot varandra."
        Goal.RANDOM -> "Bara blanda."
    }
    override val considerTypes = "Ta hänsyn till spelartyper"
    override fun considerTypesHelp(goal: Goal) = when (goal) {
        Goal.SIMILAR -> "Samma typer ihop också (nivån går först)"
        Goal.BALANCED -> "Blanda typerna: t.ex. en snabb, en fysisk och en lång per lag"
        Goal.RANDOM -> "Används inte för slumpade grupper"
    }
    override val newPartners = "Blanda partners"
    override fun newPartnersHelp(rounds: Int) =
        if (rounds == 0) "Undvik par från tidigare omgångar i dag" else "Undvik par från de $rounds tidigare omgångarna i dag"
    override val keepApartToday = "Håll isär i dag"
    override fun savedPairs(n: Int) = if (n == 0) "Inga sparade par (lägg till under Spelare)" else "+ $n sparade par från Spelare"
    override val addPair = "Lägg till par"
    override val pickTwo = "Välj två spelare att hålla isär"
    override val generate = "Dela upp"
    override val notEnoughPlayers = "Välj minst en spelare"

    override fun round(n: Int) = "Omgång $n"
    override fun groupName(n: Int) = "Grupp $n"
    override fun teamName(n: Int) = "Lag $n"
    override fun average(avg: String) = "snitt $avg"
    override val reroll = "Slumpa om"
    override val nextRound = "Nästa omgång"
    override val newRequest = "Ändra"
    override fun keepApartBroken(n: Int) = "$n par som ska hållas isär gick inte att skilja åt."
    override fun repeated(n: Int) = "$n par var tillsammans tidigare i dag också."

    override fun playersTitle(n: Int) = "Spelare ($n)"
    override val newPlayer = "Ny spelare"
    override val editPlayer = "Ändra spelare"
    override val name = "Namn"
    override val skill = "Nivå"
    override val types = "Spelartyper"
    override val addType = "Lägg till typ"
    override val renameType = "Byt namn på typ"
    override val typeName = "Typens namn"
    override val keepApart = "Håll isär"
    override val keepApartHelp = "Dessa par hamnar aldrig i samma grupp, om det går."
    override fun deletePlayerQuestion(name: String) = "Ta bort $name?"
    override fun deleteTypeQuestion(name: String) = "Ta bort typen \"$name\"? Den tas bort från alla spelare."

    override val language = "Språk"
    override val data = "Spelarfil"
    override val dataHelp = "Flytta spelarlistan mellan enheter: exportera den här och importera den på den andra " +
        "enheten. Claude kan också skapa filen åt dig."
    override val export = "Exportera spelare"
    override val import = "Importera spelare"
    override fun exported(where: String) = "Exporterad till $where"
    override fun importQuestion(players: Int) = "Ersätta din spelarlista med de $players spelarna i filen?"
    override val replace = "Ersätt"
    override fun imported(players: Int) = "Importerade $players spelare"
    override fun importFailed(reason: String) = "Kunde inte importera: $reason"
    override val today = "Dagens träning"
    override fun roundsToday(n: Int) = if (n == 0) "Inga omgångar än i dag" else "$n omgång${if (n == 1) "" else "ar"} hittills i dag"
    override val resetToday = "Börja om"
    override val resetTodayHelp = "Glöm dagens omgångar och markera alla som närvarande."
}

val LocalStrings = staticCompositionLocalOf<Strings> { English }
