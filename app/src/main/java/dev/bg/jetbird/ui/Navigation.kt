package dev.bg.jetbird.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bg.jetbird.LocalNavController
import dev.bg.jetbird.LocalSnackbar
import dev.bg.jetbird.LocalVPNServiceBinder
import dev.bg.jetbird.R
import dev.bg.jetbird.Route
import dev.bg.jetbird.ScaffoldVisibility
import dev.bg.jetbird.data.NetBirdErrors
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.ui.screens.credits.CreditsScreen
import dev.bg.jetbird.ui.screens.home.HomeScreen
import dev.bg.jetbird.ui.screens.log.LogScreen
import dev.bg.jetbird.ui.screens.profile.ProfileScreen
import dev.bg.jetbird.ui.screens.routeselector.RouteSelectorScreen
import dev.bg.jetbird.ui.screens.settings.SettingsScreen
import dev.bg.jetbird.ui.screens.setup.SetupScreen
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.ktx.composable
import dev.bg.jetbird.util.ktx.navigate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigation(
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val navController = LocalNavController.current
    val snackbarHostState = LocalSnackbar.current
    val binder = LocalVPNServiceBinder.current

    val backStack by navController.currentBackStackEntryAsState()
    val scaffoldVisibility = Route.getScaffoldVisibility(backStack?.destination?.route)

    var appLanguageDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(binder) {
        binder?.events?.collectLatest { event ->
            val message = when (event.event) {
                NetBirdErrors.NETBIRD_TUNNEL_FAILURE -> {
                    viewModel.resetAuthState()
                    ctx.getString(R.string.auth_error_see_logs)
                }
                NetBirdErrors.NETBIRD_LOGIN_EXPIRED -> {
                    viewModel.resetAuthState()
                    ctx.getString(R.string.login_expired)
                }
                else -> ctx.getString(R.string.unknown_error_see_logs)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    AnimatedVisibility(
                        scaffoldVisibility == ScaffoldVisibility.Top ||
                        scaffoldVisibility == ScaffoldVisibility.NavOnly
                    ) {
                        IconButton(
                            onClick = {
                                navController.navigateUp()
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                },
                actions = {
                    when (backStack?.destination?.route) {
                        Route.Setup.route -> {
                            IconButton(
                                onClick = {
                                    appLanguageDropdown = true
                                }
                            ) {
                                Icon(
                                    painterResource(R.drawable.baseline_language_24),
                                    contentDescription = stringResource(R.string.app_language)
                                )
                            }
                        }
                        else -> {
                            AnimatedVisibility(
                                scaffoldVisibility == ScaffoldVisibility.Top ||
                                        scaffoldVisibility == ScaffoldVisibility.ActionsOnly
                            ) {
                                Row {
                                    IconButton(onClick = { navController.navigate(Route.Profile) }) {
                                        Icon(
                                            painterResource(R.drawable.baseline_account_circle_24),
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(onClick = { navController.navigate(Route.Log) }) {
                                        Icon(
                                            painterResource(R.drawable.baseline_list_24),
                                            contentDescription = stringResource(R.string.log)
                                        )
                                    }
                                    IconButton(onClick = { navController.navigate(Route.Settings) }) {
                                        Icon(
                                            Icons.Filled.Settings,
                                            contentDescription = stringResource(R.string.settings)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {}
    ) { padding ->
        NavHost(
            navController,
            if (viewModel.needsSetup()) Route.Setup.route else Route.Home.route,
            Modifier.padding(padding)
        ) {
            composable(Route.Home) {
                HomeScreen()
            }
            composable(Route.Setup) {
                SetupScreen(
                    appLanguageDropdown = appLanguageDropdown,
                    onAppLanguageDismissed = {
                        appLanguageDropdown = false
                    }
                )
            }
            composable(Route.Profile) {
                ProfileScreen()
            }
            composable(Route.Log) {
                LogScreen()
            }
            composable(Route.Settings) {
                SettingsScreen()
            }
            composable(Route.RouteSelector) {
                RouteSelectorScreen()
            }
            composable(Route.Credits) {
                CreditsScreen()
            }
        }
    }
}

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): ViewModel() {
    fun needsSetup() = preferencesRepository.managementUrl == null

    fun resetAuthState() {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.hasAuthed = false
            preferencesRepository.updateState()
        }
    }
}
