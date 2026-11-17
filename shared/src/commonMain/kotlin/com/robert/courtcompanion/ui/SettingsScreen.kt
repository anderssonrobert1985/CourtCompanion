package com.robert.courtcompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import com.robert.courtcompanion.LocalPlatform
import com.robert.courtcompanion.data.RosterJson
import com.robert.courtcompanion.model.Language
import com.robert.courtcompanion.model.Roster

/** Language, export/import of the player file, and today's practice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(practice: PracticeViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    val platform = LocalPlatform.current
    val repository = practice.repository
    val settings by repository.settings.collectAsState()
    val session by repository.session.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<Roster?>(null) }
    val pickFile = rememberJsonFilePicker { text ->
        runCatching { RosterJson.parse(text) }
            .onSuccess { pendingImport = it }
            .onFailure { message = s.importFailed(it.message ?: "") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settings) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val column = Modifier.widthIn(max = 720.dp)
            SectionCard(s.language, column) {
                SingleChoiceSegmentedButtonRow {
                    Language.entries.forEachIndexed { i, language ->
                        SegmentedButton(
                            selected = settings.language == language,
                            onClick = { repository.updateSettings { it.copy(language = language) } },
                            shape = SegmentedButtonDefaults.itemShape(i, Language.entries.size),
                        ) { Text(Strings.languageName(language)) }
                    }
                }
            }
            SectionCard(s.data, column) {
                Text(s.dataHelp, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        val text = RosterJson.write(repository.roster.value)
                        message = platform.exportFile("courtcompanion-players.json", text)?.let(s::exported)
                    }) { Text(s.export) }
                    OutlinedButton(onClick = pickFile) { Text(s.import) }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            SectionCard(s.today, column) {
                Text(s.roundsToday(session.rounds.size), style = MaterialTheme.typography.bodyMedium)
                Text(s.resetTodayHelp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = practice::resetToday) { Text(s.resetToday) }
            }
        }
    }

    pendingImport?.let { roster ->
        ConfirmDialog(
            s.importQuestion(roster.players.size),
            s.replace,
            onConfirm = {
                repository.updateRoster { roster }
                // Attendance and rounds refer to the old players' ids.
                practice.resetToday()
                message = s.imported(roster.players.size)
            },
            onDismiss = { pendingImport = null },
        )
    }
}
