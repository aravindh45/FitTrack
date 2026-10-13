package com.personal.fittrack.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.fittrack.core.AppContainer
import com.personal.fittrack.ui.dashboard.DashboardScreen
import com.personal.fittrack.ui.dashboard.DashboardViewModel
import com.personal.fittrack.ui.history.HistoryScreen
import com.personal.fittrack.ui.history.HistoryViewModel
import com.personal.fittrack.ui.logfood.LogFoodScreen
import com.personal.fittrack.ui.logfood.LogFoodViewModel
import com.personal.fittrack.ui.onboarding.OnboardingScreen
import com.personal.fittrack.ui.onboarding.OnboardingViewModel
import com.personal.fittrack.ui.settings.SettingsScreen
import com.personal.fittrack.ui.settings.SettingsViewModel

private fun screenTitle(route: String?): String = when (route) {
    Screen.Dashboard.route -> "FitTrack"
    Screen.LogFood.route -> "Log a meal"
    Screen.History.route -> "History"
    Screen.Settings.route -> "Settings"
    Screen.EditProfile.route -> "Edit Profile"
    else -> "FitTrack"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitTrackNavHost(appContainer: AppContainer) {
    val factory = AppViewModelFactory(appContainer)

    // Resolve whether onboarding is needed exactly once before we decide the nav graph's
    // start destination - avoids a flash of the wrong screen on cold start.
    val hasProfile by produceState<Boolean?>(initialValue = null) {
        value = appContainer.profileRepository.getProfileOnce() != null
    }

    val resolvedHasProfile = hasProfile
    if (resolvedHasProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val startDestination = if (resolvedHasProfile) Screen.Dashboard.route else Screen.Onboarding.route

    // Registered once here (not inside the Settings composable) so it's available from the
    // very first composition regardless of whether the user has navigated to Settings yet -
    // Health Connect's permission flow needs an ActivityResultLauncher, and those must be
    // registered before the hosting Activity leaves the CREATED state.
    var healthPermissionRefreshTrigger by remember { mutableIntStateOf(0) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = appContainer.healthConnectManager.permissionRequestContract
    ) { healthPermissionRefreshTrigger++ }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTopBar = currentRoute != Screen.Onboarding.route
    val showBottomBar = showTopBar && currentRoute != Screen.EditProfile.route

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(screenTitle(currentRoute), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (currentRoute == Screen.EditProfile.route) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (showBottomBar && currentRoute != Screen.Settings.route) {
                            IconButton(onClick = { navigateTo(Screen.Settings.route) }) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                CenterFabBottomBar(
                    isHomeSelected = currentRoute == Screen.Dashboard.route,
                    isHistorySelected = currentRoute == Screen.History.route,
                    onHomeClick = { navigateTo(Screen.Dashboard.route) },
                    onLogClick = { navController.navigate(Screen.LogFood.route) },
                    onHistoryClick = { navigateTo(Screen.History.route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                val vm: OnboardingViewModel = viewModel(factory = factory)
                OnboardingScreen(
                    viewModel = vm,
                    onProfileSaved = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(factory = factory)
                DashboardScreen(vm)
            }
            composable(Screen.LogFood.route) {
                val vm: LogFoodViewModel = viewModel(factory = factory)
                LogFoodScreen(vm)
            }
            composable(Screen.History.route) {
                val vm: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(vm)
            }
            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = vm,
                    onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                    onConnectHealthClick = { healthPermissionLauncher.launch(appContainer.healthConnectManager.requiredPermissions) },
                    healthPermissionRefreshTrigger = healthPermissionRefreshTrigger
                )
            }
            composable(Screen.EditProfile.route) {
                val vm: OnboardingViewModel = viewModel(factory = factory)
                OnboardingScreen(
                    viewModel = vm,
                    isEditMode = true,
                    onProfileSaved = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Custom bottom bar: Home and History flank a center-docked FAB for logging a meal - the
 * app's single most frequent action gets the most prominent, easiest-to-reach spot. Settings
 * moved to a top-bar icon instead of fighting the FAB for the middle slot.
 */
@Composable
private fun CenterFabBottomBar(
    isHomeSelected: Boolean,
    isHistorySelected: Boolean,
    onHomeClick: () -> Unit,
    onLogClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarAction(
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                label = "Home",
                selected = isHomeSelected,
                onClick = onHomeClick,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FloatingActionButton(
                    onClick = onLogClick,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Filled.Restaurant, contentDescription = "Log a meal")
                }
            }
            BottomBarAction(
                selectedIcon = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History,
                label = "History",
                selected = isHistorySelected,
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Modern nav-icon convention (same one Gmail/YouTube/Google Fit use): outlined glyph when a
 * tab isn't active, filled glyph when it is - gives a clearer "you are here" signal than a
 * static icon ever could, for free. */
@Composable
private fun BottomBarAction(
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick) {
            Icon(if (selected) selectedIcon else unselectedIcon, contentDescription = label, tint = color)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
