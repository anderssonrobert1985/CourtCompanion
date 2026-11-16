package com.robert.courtcompanion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.robert.courtcompanion.grouping.Goal
import com.robert.courtcompanion.grouping.averageSkill
import com.robert.courtcompanion.model.Player
import com.robert.courtcompanion.ui.theme.groupColor

/** The generated groups, with buttons for another equally good division or the next round. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(vm: PracticeViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    val result by vm.result.collectAsState()
    val session by vm.session.collectAsState()
    val settings by vm.settings.collectAsState()
    val grouping = result
    if (grouping == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val teams = settings.request.goal == Goal.BALANCED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.round(session.rounds.size.coerceAtLeast(1))) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val pad = PaddingValues(horizontal = 6.dp)
                    OutlinedButton(onClick = onBack, contentPadding = pad, modifier = Modifier.weight(0.8f).height(52.dp)) {
                        Text(s.newRequest, maxLines = 1)
                    }
                    OutlinedButton(onClick = vm::reroll, contentPadding = pad, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Text(" " + s.reroll, maxLines = 1)
                    }
                    Button(onClick = { vm.generate() }, contentPadding = pad, modifier = Modifier.weight(1.2f).height(52.dp)) {
                        Text(s.nextRound, maxLines = 1)
                    }
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(170.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val notes = listOfNotNull(
                grouping.keepApartViolations.takeIf { it > 0 }?.let(s::keepApartBroken),
                grouping.repeatedPairs.takeIf { it > 0 && settings.request.preferNewPartners }?.let(s::repeated),
            )
            if (notes.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(notes.joinToString(" "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            itemsIndexed(grouping.groups) { i, group ->
                GroupCard(if (teams) s.teamName(i + 1) else s.groupName(i + 1), groupColor(i), group)
            }
        }
    }
}

@Composable
private fun GroupCard(title: String, color: Color, players: List<Player>) {
    val s = LocalStrings.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            Modifier.fillMaxWidth().background(color).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(s.average("%.1f".format(players.averageSkill())), color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (p in players) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        p.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (p.tags.isNotEmpty()) {
                        Text(
                            tagInitials(p.tags) + "  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SkillDots(p.skill, color = color, dot = 6.dp)
                }
            }
            val tagCounts = players.flatMap { it.tags }.groupingBy { it }.eachCount().toSortedMap()
            if (tagCounts.isNotEmpty()) {
                Text(
                    tagCounts.entries.joinToString("  ") { (tag, n) -> "$tag $n" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
