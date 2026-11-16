package com.robert.courtcompanion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robert.courtcompanion.ui.HomeScreen
import com.robert.courtcompanion.ui.LocalStrings
import com.robert.courtcompanion.ui.PlayersScreen
import com.robert.courtcompanion.ui.PracticeViewModel
import com.robert.courtcompanion.ui.ResultScreen
import com.robert.courtcompanion.ui.SettingsScreen
import com.robert.courtcompanion.ui.Strings
import com.robert.courtcompanion.ui.SystemBackHandler
import com.robert.courtcompanion.ui.theme.CourtCompanionTheme

private enum class Screen { HOME, RESULT, PLAYERS, SETTINGS }

/** The whole app: practice setup, generated groups, player database and settings. Each platform hosts this. */
@Composable
fun App(platform: Platform) {
    val settings by platform.repository.settings.collectAsState()
    CompositionLocalProvider(LocalPlatform provides platform, LocalStrings provides Strings.of(settings.language)) {
        CourtCompanionTheme {
            val practice = viewModel { PracticeViewModel(platform.repository) }
            var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
            val home = { screen = Screen.HOME }
            if (screen != Screen.HOME) SystemBackHandler(home)
            when (screen) {
                Screen.HOME -> HomeScreen(
                    vm = practice,
                    onGenerated = { screen = Screen.RESULT },
                    onPlayers = { screen = Screen.PLAYERS },
                    onSettings = { screen = Screen.SETTINGS },
                )
                Screen.RESULT -> ResultScreen(vm = practice, onBack = home)
                Screen.PLAYERS -> PlayersScreen(repository = platform.repository, onBack = home)
                Screen.SETTINGS -> SettingsScreen(practice = practice, onBack = home)
            }
        }
    }
}
