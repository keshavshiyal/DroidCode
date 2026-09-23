package com.droidcode.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Workspace : Screen("workspace")
    data object Settings : Screen("settings")
    data object Git : Screen("git")
    data object Terminal : Screen("terminal")
}

@Composable
fun DroidCodeNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    modifier: Modifier = Modifier,
    homeContent: @Composable () -> Unit,
    workspaceContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    gitContent: @Composable () -> Unit = workspaceContent,
    terminalContent: @Composable () -> Unit = workspaceContent
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            homeContent()
        }
        composable(Screen.Workspace.route) {
            workspaceContent()
        }
        composable(Screen.Settings.route) {
            settingsContent()
        }
        composable(Screen.Git.route) {
            gitContent()
        }
        composable(Screen.Terminal.route) {
            terminalContent()
        }
    }
}
