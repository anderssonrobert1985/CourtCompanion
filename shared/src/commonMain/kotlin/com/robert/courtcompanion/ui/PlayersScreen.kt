package com.robert.courtcompanion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robert.courtcompanion.data.AppRepository
import com.robert.courtcompanion.model.KeepApart
import com.robert.courtcompanion.model.MAX_SKILL
import com.robert.courtcompanion.model.MIN_SKILL
import com.robert.courtcompanion.model.Player
import com.robert.courtcompanion.model.Roster

/** Roster edits, shared by the players screen and tests. */
object RosterEdits {
    fun savePlayer(r: Roster, p: Player): Roster =
        if (r.players.any { it.id == p.id }) r.copy(players = r.players.map { if (it.id == p.id) p else it })
        else r.copy(players = r.players + p)

    fun deletePlayer(r: Roster, id: String): Roster =
        r.copy(players = r.players.filter { it.id != id }, keepApart = r.keepApart.filter { !it.involves(id) })

    fun addTag(r: Roster, name: String): Roster =
        if (r.tags.any { it.equals(name, ignoreCase = true) }) r else r.copy(tags = r.tags + name)

    fun renameTag(r: Roster, old: String, new: String): Roster = r.copy(
        tags = r.tags.map { if (it == old) new else it },
        players = r.players.map { p -> if (old in p.tags) p.copy(tags = p.tags - old + new) else p },
    )

    fun deleteTag(r: Roster, name: String): Roster =
        r.copy(tags = r.tags - name, players = r.players.map { it.copy(tags = it.tags - name) })

    fun addKeepApart(r: Roster, a: String, b: String): Roster =
        if (a == b || r.keepApart.any { it.matches(a, b) }) r else r.copy(keepApart = r.keepApart + KeepApart(a, b))

    fun removeKeepApart(r: Roster, pair: KeepApart): Roster = r.copy(keepApart = r.keepApart - pair)
}

/** The player database: players with skill and types, the list of types, and saved keep-apart pairs. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayersScreen(repository: AppRepository, onBack: () -> Unit) {
    val s = LocalStrings.current
    val roster by repository.roster.collectAsState()
    var editing by remember { mutableStateOf<Player?>(null) }
    var renamingTag by remember { mutableStateOf<String?>(null) }
    var deletingTag by remember { mutableStateOf<String?>(null) }
    var addingTag by remember { mutableStateOf(false) }
    var addingPair by remember { mutableStateOf(false) }
    fun edit(change: (Roster) -> Roster) = repository.updateRoster(change)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.playersTitle(roster.players.size)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = Player(name = "") },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(s.newPlayer) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(roster.players.sortedBy { it.name.lowercase() }, key = { it.id }) { p ->
                Row(
                    Modifier.widthIn(max = 720.dp).fillMaxWidth().clickable { editing = p }.padding(16.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.bodyLarge)
                        if (p.tags.isNotEmpty()) {
                            Text(
                                p.tags.sorted().joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    SkillDots(p.skill)
                }
                HorizontalDivider(Modifier.widthIn(max = 720.dp))
            }
            item {
                Column(Modifier.widthIn(max = 720.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionCard(s.types, trailing = { TextButton(onClick = { addingTag = true }) { Text(s.addType) } }) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (tag in roster.tags) {
                                InputChip(
                                    selected = false,
                                    onClick = { renamingTag = tag },
                                    label = { Text("$tag (${roster.players.count { tag in it.tags }})") },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, s.delete, Modifier.size(16.dp).clickable { deletingTag = tag })
                                    },
                                )
                            }
                        }
                    }
                    SectionCard(
                        s.keepApart,
                        trailing = { TextButton(onClick = { addingPair = true }, enabled = roster.players.size >= 2) { Text(s.addPair) } },
                    ) {
                        Text(s.keepApartHelp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (pair in roster.keepApart) {
                                val a = roster.player(pair.a)?.name ?: continue
                                val b = roster.player(pair.b)?.name ?: continue
                                InputChip(
                                    selected = false,
                                    onClick = { edit { RosterEdits.removeKeepApart(it, pair) } },
                                    label = { Text("$a ≠ $b") },
                                    trailingIcon = { Icon(Icons.Default.Close, s.delete, Modifier.size(16.dp)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { player ->
        PlayerDialog(
            player = player,
            isNew = roster.players.none { it.id == player.id },
            tags = roster.tags,
            onSave = { p -> edit { RosterEdits.savePlayer(it, p) } },
            onDelete = { edit { RosterEdits.deletePlayer(it, player.id) } },
            onDismiss = { editing = null },
        )
    }
    if (addingTag) {
        TextInputDialog(s.addType, s.typeName, "", onSave = { name -> edit { RosterEdits.addTag(it, name) } }, onDismiss = { addingTag = false })
    }
    renamingTag?.let { old ->
        TextInputDialog(s.renameType, s.typeName, old, onSave = { new -> edit { RosterEdits.renameTag(it, old, new) } }, onDismiss = { renamingTag = null })
    }
    deletingTag?.let { tag ->
        ConfirmDialog(s.deleteTypeQuestion(tag), s.delete, onConfirm = { edit { RosterEdits.deleteTag(it, tag) } }, onDismiss = { deletingTag = null })
    }
    if (addingPair) {
        PickPairDialog(roster.players, onPicked = { a, b -> edit { RosterEdits.addKeepApart(it, a, b) } }, onDismiss = { addingPair = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerDialog(
    player: Player,
    isNew: Boolean,
    tags: List<String>,
    onSave: (Player) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf(player.name) }
    var skill by remember { mutableStateOf(player.skill) }
    var selected by remember { mutableStateOf(player.tags) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) s.newPlayer else s.editPlayer) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(s.name) }, singleLine = true)
                Text(s.skill, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (level in MIN_SKILL..MAX_SKILL) {
                        FilterChip(selected = skill == level, onClick = { skill = level }, label = { Text(level.toString()) })
                    }
                }
                if (tags.isNotEmpty()) {
                    Text(s.types, style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (tag in tags) {
                            FilterChip(
                                selected = tag in selected,
                                onClick = { selected = if (tag in selected) selected - tag else selected + tag },
                                label = { Text(tag) },
                            )
                        }
                    }
                }
                if (!isNew) {
                    AssistChip(onClick = { confirmDelete = true }, label = { Text(s.delete) })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(player.copy(name = name.trim(), skill = skill, tags = selected))
                    onDismiss()
                },
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
    if (confirmDelete) {
        ConfirmDialog(
            s.deletePlayerQuestion(player.name),
            s.delete,
            onConfirm = {
                onDelete()
                onDismiss()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
