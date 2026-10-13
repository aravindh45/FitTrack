package com.personal.fittrack.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                DockBottomBar(
                    currentRoute = currentRoute,
                    onHomeClick = { navigateTo(Screen.Dashboard.route) },
                    onLogClick = { navController.navigate(Screen.LogFood.route) },
                    onHistoryClick = { navigateTo(Screen.History.route) },
                    onSettingsClick = { navigateTo(Screen.Settings.route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
            // Same soft cross-fade on every navigation, destination and back - cheap, consistent
            // "the app is alive" touch rather than the default instant hard-cut between screens.
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(150)) }
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
 * macOS-dock-inspired bottom nav: a compact, floating rounded pill instead of a full-width
 * bar, icon-only (no labels - the glow + filled/outlined swap carries the "you are here"
 * signal on its own, same as a dock doesn't caption its icons either). Settings lives here
 * now too instead of a separate top-bar gear, since every other destination already gets a
 * dock slot - one obvious way to navigate, not two.
 */
@Composable
private fun DockBottomBar(
    currentRoute: String?,
    onHomeClick: () -> Unit,
    onLogClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockIcon(
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    selected = currentRoute == Screen.Dashboard.route,
                    contentDescription = "Home",
                    onClick = onHomeClick
                )
                DockIcon(
                    selectedIcon = Icons.Filled.Restaurant,
                    unselectedIcon = Icons.Filled.Restaurant,
                    selected = currentRoute == Screen.LogFood.route,
                    contentDescription = "Log a meal",
                    onClick = onLogClick
                )
                DockIcon(
                    selectedIcon = Icons.Filled.History,
                    unselectedIcon = Icons.Outlined.History,
                    selected = currentRoute == Screen.History.route,
                    contentDescription = "History",
                    onClick = onHistoryClick
                )
                DockIcon(
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    selected = currentRoute == Screen.Settings.route,
                    contentDescription = "Settings",
                    onClick = onSettingsClick
                )
            }
        }
    }
}

/** One dock slot: a soft radial-gradient glow fades in behind the icon when it's the active
 * destination, the glyph itself swaps outlined-to-filled and nudges up in scale with a bouncy
 * spring - small motion cues that read as "alive" without being distracting. */
@Composable
private fun DockIcon(
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.88f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dockIconScale"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "dockIconTint"
    )
    val glowColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.5f, animationSpec = tween(220)),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.5f, animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.35f), glowColor.copy(alpha = 0f))
                        ),
                        shape = CircleShape
                    )
            )
        }
        IconButton(onClick = onClick, modifier = Modifier.scale(scale)) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
