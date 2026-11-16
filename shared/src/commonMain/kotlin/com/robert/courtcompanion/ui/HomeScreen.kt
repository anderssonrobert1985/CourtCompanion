package com.robert.courtcompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robert.courtcompanion.grouping.Goal
import com.robert.courtcompanion.grouping.GroupRequest
import com.robert.courtcompanion.grouping.GroupSizes
import com.robert.courtcompanion.grouping.Leftover
import com.robert.courtcompanion.grouping.Sizing

/** The practice screen: who's here, how to divide them, and the big Generate button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: PracticeViewModel, onGenerated: () -> Unit, onPlayers: () -> Unit, onSettings: () -> Unit) {
    val s = LocalStrings.current
    val roster by vm.roster.collectAsState()
    val session by vm.session.collectAsState()
    val settings by vm.settings.collectAsState()
    val request = settings.request
    val presentCount = roster.players.count { it.id !in session.absent }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.appName) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = onPlayers) { Icon(Icons.Default.Person, s.players) }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, s.settings) }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 10.dp)) {
                    val sizes = GroupSizes.of(presentCount, request)
                    Text(
                        if (presentCount == 0) s.notEnoughPlayers else s.sizesPreview(presentCount, sizes.joinToString(" · ")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    )
                    Button(
                        onClick = { if (vm.generate()) onGenerated() },
                        enabled = presentCount > 0,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) { Text(s.generate, style = MaterialTheme.typography.titleMedium) }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val column = Modifier.widthIn(max = 720.dp)
            if (roster.players.isEmpty()) {
                SectionCard(s.players, column) {
                    Text(s.noPlayersYet)
                    Button(onClick = onPlayers) { Text(s.addPlayers) }
                }
            } else {
                AttendanceCard(vm, column)
            }
            DivideCard(request, presentCount, column) { change -> vm.updateRequest(change) }
            GoalCard(request, session.rounds.size, column) { change -> vm.updateRequest(change) }
            KeepApartTodayCard(vm, column)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttendanceCard(vm: PracticeViewModel, modifier: Modifier) {
    val s = LocalStrings.current
    val roster by vm.roster.collectAsState()
    val session by vm.session.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(true) }
    val present = roster.players.count { it.id !in session.absent }
    SectionCard(
        s.present(present, roster.players.size),
        modifier,
        trailing = {
            if (expanded) {
                TextButton(onClick = { vm.setAllPresent(true) }) { Text(s.all) }
                TextButton(onClick = { vm.setAllPresent(false) }) { Text(s.none) }
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            }
        },
    ) {
        if (expanded) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (p in roster.players.sortedBy { it.name.lowercase() }) {
                    FilterChip(
                        selected = p.id !in session.absent,
                        onClick = { vm.togglePresent(p.id) },
                        label = { Text(p.name) },
                    )
                }
            }
        } else {
            val missing = roster.players.filter { it.id in session.absent }.map { it.name }
            Text(
                if (missing.isEmpty()) s.everyoneHere else s.missing(missing.sorted().joinToString(", ")),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DivideCard(request: GroupRequest, players: Int, modifier: Modifier, update: ((GroupRequest) -> GroupRequest) -> Unit) {
    val s = LocalStrings.current
    SectionCard(s.divideInto, modifier) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val options = listOf(Sizing.GROUP_SIZE to s.groupsOf, Sizing.GROUP_COUNT to s.numberOfGroups)
            options.forEachIndexed { i, (sizing, label) ->
                SegmentedButton(
                    selected = request.sizing == sizing,
                    onClick = { update { it.copy(sizing = sizing) } },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(label) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalIconButton(
                onClick = { update { it.copy(number = (it.number - 1).coerceAtLeast(1)) } },
                enabled = request.number > 1,
                modifier = Modifier.size(52.dp),
            ) { Text("−", style = MaterialTheme.typography.headlineSmall) }
            Text(
                request.number.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 56.dp),
            )
            FilledTonalIconButton(
                onClick = { update { it.copy(number = (it.number + 1).coerceAtMost(30)) } },
                modifier = Modifier.size(52.dp),
            ) { Icon(Icons.Default.Add, null) }
            Text(
                if (request.sizing == Sizing.GROUP_SIZE) s.playersPerGroup(request.number) else s.groupCount(request.number),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        // Quick picks for the usual numbers.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (n in 2..6) {
                FilterChip(
                    selected = request.number == n,
                    onClick = { update { it.copy(number = n) } },
                    label = { Text(n.toString()) },
                )
            }
        }
        val uneven = request.sizing == Sizing.GROUP_SIZE && players > request.number && players % request.number != 0
        if (uneven) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(Leftover.SMALLER_GROUP to s.smallerGroup, Leftover.BIGGER_GROUPS to s.biggerGroups)
                // The resulting sizes are shown above the Generate button.
                options.forEachIndexed { i, (leftover, label) ->
                    SegmentedButton(
                        selected = request.leftover == leftover,
                        onClick = { update { it.copy(leftover = leftover) } },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label, maxLines = 1) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalCard(request: GroupRequest, rounds: Int, modifier: Modifier, update: ((GroupRequest) -> GroupRequest) -> Unit) {
    val s = LocalStrings.current
    SectionCard(s.goal, modifier) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Goal.entries.forEachIndexed { i, goal ->
                SegmentedButton(
                    selected = request.goal == goal,
                    onClick = { update { it.copy(goal = goal) } },
                    shape = SegmentedButtonDefaults.itemShape(i, Goal.entries.size),
                ) { Text(s.goalName(goal), maxLines = 1) }
            }
        }
        Text(s.goalHelp(request.goal), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SwitchRow(
            title = s.considerTypes,
            help = s.considerTypesHelp(request.goal),
            checked = request.considerTypes && request.goal != Goal.RANDOM,
            enabled = request.goal != Goal.RANDOM,
        ) { on -> update { it.copy(considerTypes = on) } }
        SwitchRow(s.newPartners, s.newPartnersHelp(rounds), request.preferNewPartners) { on ->
            update { it.copy(preferNewPartners = on) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, help: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeepApartTodayCard(vm: PracticeViewModel, modifier: Modifier) {
    val s = LocalStrings.current
    val roster by vm.roster.collectAsState()
    val session by vm.session.collectAsState()
    var picking by remember { mutableStateOf(false) }
    SectionCard(
        s.keepApartToday,
        modifier,
        trailing = { TextButton(onClick = { picking = true }, enabled = roster.players.size >= 2) { Text(s.addPair) } },
    ) {
        if (session.extraKeepApart.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (pair in session.extraKeepApart) {
                    val a = roster.player(pair.a)?.name ?: continue
                    val b = roster.player(pair.b)?.name ?: continue
                    InputChip(
                        selected = false,
                        onClick = { vm.removeKeepApartToday(pair) },
                        label = { Text("$a ↔ $b") },
                        trailingIcon = { Icon(Icons.Default.Close, s.delete, Modifier.size(16.dp)) },
                    )
                }
            }
        }
        Text(s.savedPairs(roster.keepApart.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (picking) {
        PickPairDialog(vm.presentPlayers(), onPicked = vm::addKeepApartToday, onDismiss = { picking = false })
    }
}
