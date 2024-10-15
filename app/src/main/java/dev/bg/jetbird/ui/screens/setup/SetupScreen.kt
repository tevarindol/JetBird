package dev.bg.jetbird.ui.screens.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bg.jetbird.LocalLayoutType
import dev.bg.jetbird.LocalNavController
import dev.bg.jetbird.LocalSnackbar
import dev.bg.jetbird.R
import dev.bg.jetbird.Route
import dev.bg.jetbird.data.LayoutType
import dev.bg.jetbird.ui.components.AppLanguageDropDown
import dev.bg.jetbird.ui.components.LoadingButton
import dev.bg.jetbird.util.ktx.clearAndNavigate
import dev.bg.jetbird.util.ktx.getConfigPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun SetupScreen(
    appLanguageDropdown: Boolean,
    onAppLanguageDismissed: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
    errors: Flow<String> = viewModel.errors.receiveAsFlow()
) {
    val ctx = LocalContext.current
    val snackbar = LocalSnackbar.current
    val navController = LocalNavController.current
    val layoutType = LocalLayoutType.current

    val loading = viewModel.loading.collectAsStateWithLifecycle().value
    val ssoSupport = viewModel.ssoSupported.collectAsStateWithLifecycle().value
    val setupComplete = viewModel.setupComplete.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        errors.onEach {
            snackbar.showSnackbar(it)
        }.collect()
    }

    LaunchedEffect(setupComplete) {
        if (setupComplete) {
            snackbar.showSnackbar(ctx.getString(R.string.setup_key_saved))
            navController.clearAndNavigate(Route.Home)
        }
    }

    when (layoutType) {
        LayoutType.WideLandscape,
        LayoutType.PanelLandscape -> {
            Row(
                Modifier
                    .fillMaxSize()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Welcome(Modifier.weight(1f))
                VerticalDivider(Modifier.fillMaxHeight(0.6f))
                SetupInfo(
                    loading,
                    ssoSupport,
                    updateManagementUrl = { managementUrl ->
                        viewModel.updateManagementUrl(managementUrl, ssoSupport ?: false)
                    },
                    checkManagementUrl = { managementUrl ->
                        viewModel.checkManagementUrl(ctx.getConfigPath(), managementUrl)
                    },
                    updateSetupKey = { managementUrl, setupKey ->
                        viewModel.updateSetupKey(
                            configPath = ctx.getConfigPath(),
                            managementUrl = managementUrl,
                            setupKey = setupKey,
                            ssoSupported = ssoSupport ?: false
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        else -> {
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding(),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Welcome()
                SetupInfo(
                    loading,
                    ssoSupport,
                    updateManagementUrl = { managementUrl ->
                        viewModel.updateManagementUrl(managementUrl, ssoSupport == true)
                    },
                    checkManagementUrl = { managementUrl ->
                        viewModel.checkManagementUrl(ctx.getConfigPath(), managementUrl)
                    },
                    updateSetupKey = { managementUrl, setupKey ->
                        viewModel.updateSetupKey(
                            configPath = ctx.getConfigPath(),
                            managementUrl = managementUrl,
                            setupKey = setupKey,
                            ssoSupported = ssoSupport == true
                        )
                    }
                )
            }
        }
    }
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        AppLanguageDropDown(
            expanded = appLanguageDropdown,
            onDismissRequest = onAppLanguageDismissed
        )
    }
}

@Composable
private fun Welcome(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.welcome_jetbird),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            painterResource(R.drawable.ic_launcher_monochrome),
            modifier = Modifier.size(196.dp),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(R.string.app_name),
        )
    }
}

@Composable
private fun SetupInfo(
    loading: Boolean,
    ssoSupport: Boolean?,
    updateManagementUrl: (managementUrl: String) -> Unit,
    checkManagementUrl: (managementUrl: String) -> Unit,
    updateSetupKey: (managementUrl: String, setupKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val navController = LocalNavController.current

    val managementUrlRegex = remember { Regex("^https://.*$") }
    val setupKeyRegex = remember { Regex("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$") }

    var managementUrl by rememberSaveable { mutableStateOf("") }
    var setupKey by rememberSaveable { mutableStateOf("") }

    AnimatedContent(
        targetState = ssoSupport,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        label = "Setup animation"
    ) { sso ->
        when (sso) {
            null -> {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.get_started_url),
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = managementUrl,
                        onValueChange = { managementUrl = it },
                        placeholder = { Text(stringResource(R.string.management_url)) }
                    )
                    LoadingButton(
                        onClick = {
                            focusManager.clearFocus()
                            checkManagementUrl(managementUrl)
                        },
                        loading = loading,
                        enabled = !loading && managementUrl.matches(managementUrlRegex)
                    ) {
                        Text(stringResource(R.string.connect))
                    }
                }
            }

            else -> {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (sso) {
                        Text(stringResource(R.string.setup_sso_supported))
                    } else {
                        Text(stringResource(R.string.setup_sso_unsupported))
                    }
                    Button(
                        onClick = {
                            updateManagementUrl(managementUrl)
                            navController.clearAndNavigate(Route.Home)
                        },
                        enabled = sso
                    ) {
                        Text(stringResource(R.string.use_sso))
                    }
                    HorizontalDivider()
                    if (sso) {
                        Text(stringResource(R.string.setup_setup_key_alternative))
                    } else {
                        Text(stringResource(R.string.setup_setup_key_required))
                    }
                    OutlinedTextField(
                        value = setupKey,
                        onValueChange = { setupKey = it },
                        placeholder = { Text(stringResource(R.string.setup_key)) }
                    )
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            updateSetupKey(managementUrl, setupKey)
                            navController.clearAndNavigate(Route.Home)
                        },
                        enabled = setupKey.matches(setupKeyRegex)
                    ) {
                        Text(stringResource(R.string.use_setup_key))
                    }
                }
            }
        }
    }
}
