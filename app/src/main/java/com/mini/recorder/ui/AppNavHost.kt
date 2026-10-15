package com.mini.recorder.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mini.recorder.ui.components.MiniPlayerBar
import com.mini.recorder.ui.components.MiniPlayerViewModel
import com.mini.recorder.ui.display.DisplaySettingsScreen
import com.mini.recorder.ui.list.RecordingListScreen
import com.mini.recorder.ui.player.PlayerScreen
import com.mini.recorder.ui.record.RecordScreen
import com.mini.recorder.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.StateFlow

object Routes {
    const val LIST = "list"
    const val RECORD = "record"
    const val SETTINGS = "settings"
    const val DISPLAY = "display"
    const val PLAYER = "player/{id}?autoplay={autoplay}"
    fun player(id: Long, autoplay: Boolean) = "player/$id?autoplay=$autoplay"
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNavHost(
    startOnRecord: Boolean,
    openPlayerRequest: StateFlow<Boolean>,
    onOpenPlayerConsumed: () -> Unit,
    viewModel: AppNavViewModel = hiltViewModel(),
    miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle(initialValue = false)
    val openRequested by openPlayerRequest.collectAsStateWithLifecycle()
    val nowPlaying by miniPlayerViewModel.nowPlaying.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabs = listOf(
        BottomItem(Routes.LIST, "錄音檔", Icons.AutoMirrored.Filled.List),
        BottomItem(Routes.RECORD, "錄音", Icons.Filled.Mic),
        BottomItem(Routes.SETTINGS, "設定", Icons.Filled.Settings)
    )
    val showBottomBar = currentRoute == Routes.LIST || currentRoute == Routes.RECORD || currentRoute == Routes.SETTINGS

    LaunchedEffect(isRecording) {
        if (isRecording && currentRoute != Routes.RECORD) {
            navController.navigate(Routes.RECORD) {
                popUpTo(Routes.LIST) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(openRequested, nowPlaying?.recordingId) {
        val target = nowPlaying?.recordingId ?: return@LaunchedEffect
        if (openRequested && target > 0L) {
            onOpenPlayerConsumed()
            navController.navigate(Routes.player(target, false)) { launchSingleTop = true }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                nowPlaying?.let {
                    if (currentRoute != Routes.PLAYER && currentRoute != Routes.DISPLAY) {
                        MiniPlayerBar(
                            onOpenPlayer = { pid ->
                                navController.navigate(Routes.player(pid, false)) { launchSingleTop = true }
                            },
                            viewModel = miniPlayerViewModel
                        )
                        HorizontalDivider()
                    }
                }
                if (showBottomBar) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(Routes.LIST) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startOnRecord) Routes.RECORD else Routes.LIST,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 4. 平滑轉場：頂層標籤頁採用淡入淡出（Fade Through）
            composable(
                route = Routes.LIST,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(180)) }
            ) {
                RecordingListScreen(
                    onOpenRecording = { id, autoplay -> navController.navigate(Routes.player(id, autoplay)) },
                    onOpenDisplaySettings = { navController.navigate(Routes.DISPLAY) },
                    onNavigateToRecord = {
                        navController.navigate(Routes.RECORD) {
                            popUpTo(Routes.LIST) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = Routes.RECORD,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(180)) }
            ) { RecordScreen() }
            composable(
                route = Routes.SETTINGS,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(180)) }
            ) { SettingsScreen() }

            // 4. 次級頁面採用側邊滑入與滑出（Slide & Fade）
            composable(
                route = Routes.DISPLAY,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(280))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(220)
                    ) + fadeOut(animationSpec = tween(220))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(280))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(220)
                    ) + fadeOut(animationSpec = tween(220))
                }
            ) {
                DisplaySettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.PLAYER,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("autoplay") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                ),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(280))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(220)
                    ) + fadeOut(animationSpec = tween(220))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(280))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(220)
                    ) + fadeOut(animationSpec = tween(220))
                }
            ) {
                PlayerScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
