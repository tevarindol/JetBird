package dev.bg.jetbird.ui.screens.settings

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bg.jetbird.BuildConfig
import dev.bg.jetbird.LocalNavController
import dev.bg.jetbird.LocalSnackbar
import dev.bg.jetbird.R
import dev.bg.jetbird.Route
import dev.bg.jetbird.data.Languages
import dev.bg.jetbird.repository.PreferencesState
import dev.bg.jetbird.ui.components.DeleteAlertDialog
import dev.bg.jetbird.ui.components.OutlinedCopyButton
import dev.bg.jetbird.ui.components.OutlinedTextFieldInputType
import dev.bg.jetbird.ui.components.SettingsDropdownTile
import dev.bg.jetbird.ui.components.SettingsSection
import dev.bg.jetbird.ui.components.SettingsSwitch
import dev.bg.jetbird.ui.components.SettingsTile
import dev.bg.jetbird.ui.components.SettingsTileWithValue
import dev.bg.jetbird.ui.components.TextFieldAlertDialog
import dev.bg.jetbird.util.ktx.assertivelyGet
import dev.bg.jetbird.util.ktx.getProfileManager
import dev.bg.jetbird.util.ktx.navigate
import dev.bg.jetbird.util.ktx.toStringOrNotSet
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    preferencesState: StateFlow<PreferencesState> = viewModel.state
) {
    val ctx = LocalContext.current
    val navController = LocalNavController.current
    val snackbar = LocalSnackbar.current

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val state = preferencesState.collectAsStateWithLifecycle().value

    fun deleteConfigFile() {
        val config = File(ctx.getProfileManager().activeConfigPath)
        try {
            when (config.delete()) {
                true -> scope.launch { snackbar.showSnackbar(ctx.getString(R.string.removed_netbird_config)) }
                false -> scope.launch { snackbar.showSnackbar(ctx.getString(R.string.failed_remove_netbird_config)) }
            }
        } catch (e: IOException) {
            Timber.d("Failed to delete NetBird config file: $e")
            scope.launch {
                snackbar.showSnackbar(ctx.getString(R.string.failed_remove_netbird_config))
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            stringResource(R.string.settings),
            fontSize = 36.sp,
            fontWeight = FontWeight.W400
        )

        SettingsSection(
            title = stringResource(R.string.netbird)
        ) {
            var managementUrlDialog by remember { mutableStateOf(false) }
            var preSharedKeyDialog by remember { mutableStateOf(false) }
            var configViewDialog by remember { mutableStateOf(false) }
            var engineLoggingWarning by remember { mutableStateOf(false) }

            SettingsTileWithValue(
                title = stringResource(R.string.management_server),
                description = when {
                    state.managementUrl == null -> null
                    state.ssoSupported -> stringResource(R.string.sso_supported)
                    !state.ssoSupported -> stringResource(R.string.sso_unsupported)
                    else -> null
                },
                value = state.managementUrl.toStringOrNotSet(ctx),
                onClick = { managementUrlDialog = true }
            )
            TextFieldAlertDialog(
                open = managementUrlDialog,
                title = stringResource(R.string.management_server),
                label = stringResource(R.string.management_server),
                enabled = false,
                initialValue = state.managementUrl,
                supportingText = stringResource(R.string.reset_to_change_management_server),
                inputType = OutlinedTextFieldInputType.Any,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                onDismissRequest = { managementUrlDialog = false },
                onEntered = {}
            )
            SettingsTileWithValue(
                title = stringResource(R.string.pre_shared_key),
                value = state.preSharedKey.toStringOrNotSet(ctx),
                onClick = { preSharedKeyDialog = true }
            )
            TextFieldAlertDialog(
                open = preSharedKeyDialog,
                title = stringResource(R.string.pre_shared_key),
                label = stringResource(R.string.pre_shared_key),
                initialValue = state.preSharedKey,
                inputType = OutlinedTextFieldInputType.Any,
                onDismissRequest = { preSharedKeyDialog = false },
                onEntered = { key -> viewModel.updatePreSharedKey(key) }
            )
            SettingsTileWithValue(
                title = stringResource(R.string.setup_key),
                value = state.setupKey.toStringOrNotSet(ctx),
            )
            SettingsSwitch(
                title = stringResource(R.string.ipv6),
                checked = state.ipv6,
                onChange = viewModel::toggleIpv6
            )
            SettingsSwitch(
                title = stringResource(R.string.rosenpass),
                checked = state.rosenpass,
                onChange = { viewModel.toggleRosenpass(it) }
            )
            AnimatedVisibility(state.rosenpass) {
                SettingsSwitch(
                    title = stringResource(R.string.permissive_rosenpass),
                    description = stringResource(R.string.permissive_rosenpass_description),
                    checked = state.rosenpassPermissive,
                    onChange = { viewModel.toggleRosenpassPermissive(it) }
                )
            }
            SettingsSwitch(
                title = stringResource(R.string.enable_logging),
                description = stringResource(R.string.enable_logging_description_netbird),
                checked = state.engineLogging,
                onChange = viewModel::toggleEngineLogging
            )
            AnimatedVisibility(state.engineLogging) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsSwitch(
                        title = stringResource(R.string.enable_verbose_logging),
                        description = stringResource(R.string.enable_verbose_logging_description_netbird),
                        checked = state.verboseEngineLogging,
                        onChange = viewModel::toggleVerboseEngineLogging
                    )
                    SettingsSwitch(
                        title = stringResource(R.string.enable_live_logging),
                        description = stringResource(R.string.enable_live_logging_description),
                        checked = state.liveEngineLogging,
                        onChange = {
                            if (it) {
                                engineLoggingWarning = true
                            }
                            viewModel.toggleLiveEngineLogging(it)
                        }
                    )
                }
            }
            LiveLoggingWarning(
                open = engineLoggingWarning,
                onDismissRequest = { engineLoggingWarning = false }
            )
            SettingsSwitch(
                title = stringResource(R.string.client_routes),
                description = stringResource(R.string.client_routes_description),
                checked = state.clientRoutes,
                onChange = { viewModel.toggleClientRoutes(it) }
            )
            SettingsSwitch(
                title = stringResource(R.string.server_routes),
                description = stringResource(R.string.server_routes_description),
                checked = state.serverRoutes,
                onChange = { viewModel.toggleServerRoutes(it) }
            )
            SettingsSwitch(
                title = stringResource(R.string.force_relay_connection),
                checked = state.forceRelay,
                onChange = { viewModel.toggleForceRelay(it) }
            )
            SettingsSwitch(
                title = stringResource(R.string.dns),
                checked = state.dns,
                onChange = { viewModel.toggleDNS(it) }
            )
            SettingsSwitch(
                title = stringResource(R.string.firewall),
                checked = state.firewall,
                onChange = { viewModel.toggleFirewall(it) }
            )
            SettingsSwitch(
                title = stringResource(R.string.ssh),
                description = stringResource(R.string.ssh_description),
                checked = state.ssh,
                onChange = { viewModel.toggleSSH(it) }
            )
            SettingsSwitch(
                title = stringResource(R.string.block_inbound),
                description = stringResource(R.string.block_inbound_description),
                checked = state.blockInbound,
                onChange = { viewModel.toggleBlockInbound(it) }
            )
            SettingsTile(
                title = stringResource(R.string.view_config),
                description = stringResource(R.string.view_config_description),
                onClick = { configViewDialog = true }
            )
            ConfigViewDialog(
                open = configViewDialog,
                state = state,
                onDismissRequest = { configViewDialog = false }
            )
        }
        HorizontalDivider()

        SettingsSection(
            title = stringResource(R.string.app_name)
        ) {
            var appLanguageDropdown by remember { mutableStateOf(false) }
            var excludeAppsDialog by remember { mutableStateOf(false) }
            var fallbackDnsDialog by remember { mutableStateOf(false) }

            val localeList = remember {
                Languages.resourceLanguageCodeMap.map { (k, v) -> ctx.getString(k) to v }.toMap()
            }
            val locale = remember {
                Languages.languageCodeResourceMap[AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"]
                ?: Languages.languageCodeResourceMap.assertivelyGet("en")
            }

            SettingsDropdownTile(
                title = stringResource(R.string.app_language),
                description = stringResource(locale),
                items = localeList.keys.sorted(),
                selected = stringResource(locale),
                expanded = appLanguageDropdown,
                onExpandedChanged = { appLanguageDropdown = it },
                onDismissRequest = { appLanguageDropdown = false },
                onSelected = {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeList[it]))
                }
            )
            SettingsSwitch(
                title = stringResource(R.string.enable_logging),
                description = stringResource(R.string.enable_logging_description),
                checked = state.logging,
                onChange = viewModel::toggleLogging
            )
            AnimatedVisibility(state.logging) {
                SettingsSwitch(
                    title = stringResource(R.string.enable_verbose_logging),
                    description = stringResource(R.string.enable_verbose_logging_description),
                    checked = state.verboseLogging,
                    onChange = viewModel::toggleVerboseLogging
                )
            }
            SettingsSwitch(
                title = stringResource(R.string.start_on_reboot),
                description = stringResource(R.string.start_on_reboot_description),
                checked = state.startOnReboot,
                onChange = viewModel::toggleStartOnReboot
            )
            SettingsTile(
                title = stringResource(R.string.exclude_apps),
                description = if (state.invertExclusion) {
                    stringResource(R.string.exclude_apps_inverted_description, state.excludedApps.size)
                } else {
                    stringResource(R.string.exclude_apps_description, state.excludedApps.size)
                },
                onClick = { excludeAppsDialog = true }
            )
            ExcludeAppsDialog(
                open = excludeAppsDialog,
                initialPackages = state.excludedApps,
                invertState = state.invertExclusion,
                includeSystemAppsState = state.includeSystemApps,
                onDismissRequest = { excludeAppsDialog = false },
                onSelected = { packageNames, invert, include ->
                    excludeAppsDialog = false
                    viewModel.updateExcludedApps(packageNames)
                    viewModel.toggleInvertExclusion(invert)
                    viewModel.toggleIncludeSystemApps(include)
                }
            )
            SettingsTile(
                title = stringResource(R.string.route_selector),
                description = stringResource(R.string.route_selector_description, state.routeOverrides.size),
                onClick = { navController.navigate(Route.RouteSelector) }
            )
            SettingsTileWithValue(
                title = stringResource(R.string.fallback_dns),
                value = state.fallbackDns.toStringOrNotSet(ctx),
                onClick = { fallbackDnsDialog = true }
            )
            TextFieldAlertDialog(
                open = fallbackDnsDialog,
                title = stringResource(R.string.fallback_dns),
                label = stringResource(R.string.fallback_dns),
                initialValue = state.fallbackDns,
                inputType = OutlinedTextFieldInputType.Any,
                onDismissRequest = { fallbackDnsDialog = false },
                onEntered = { dns ->
                    viewModel.updateFallbackDns(dns)
                }
            )
        }
        HorizontalDivider()

        SettingsSection(
            title = stringResource(R.string.app)
        ) {
            var wipeConfigDialog by remember { mutableStateOf(false) }
            var wipeAllDialog by remember { mutableStateOf(false) }

            SettingsTile(
                title = stringResource(R.string.reset_netbird_config),
                description = stringResource(R.string.reset_netbird_config_description),
                onClick = {
                    wipeConfigDialog = true
                }
            )
            DeleteAlertDialog(
                title = stringResource(R.string.reset_netbird_config),
                description = stringResource(R.string.reset_netbird_config_description),
                open = wipeConfigDialog,
                onDismissRequest = { wipeConfigDialog = false },
                onDelete = {
                    wipeConfigDialog = false
                    deleteConfigFile()
                }
            )
            SettingsTile(
                title = stringResource(R.string.reset_app),
                description = stringResource(R.string.reset_app_description),
                onClick = {
                    wipeAllDialog = true
                }
            )
            DeleteAlertDialog(
                title = stringResource(R.string.reset_app),
                description = stringResource(R.string.reset_app_description),
                open = wipeAllDialog,
                onDismissRequest = { wipeAllDialog = false },
                onDelete = {
                    wipeAllDialog = false
                    viewModel.resetPreferences()
                    deleteConfigFile()
                    navController.navigate(Route.Setup)
                }
            )
        }
        HorizontalDivider()

        SettingsSection(
            title = stringResource(R.string.about)
        ) {
            SettingsTile(
                title = stringResource(R.string.app_name),
                description = BuildConfig.APPLICATION_ID,
                onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:${ctx.packageName}".toUri()
                        }
                    )
                }
            )
            SettingsTile(
                title = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                description = stringResource(
                    R.string.about_build,
                    BuildConfig.VERSION_CODE
                )
            )
            SettingsTile(
                title = stringResource(R.string.about_netbird_version),
                description = "${BuildConfig.NETBIRD_COMMIT} (${BuildConfig.NETBIRD_VERSION})"
            )
            SettingsTile(
                title = stringResource(R.string.credits),
                description = stringResource(R.string.credits_description),
                onClick = {
                    navController.navigate(Route.Credits)
                }
            )
        }
    }
}

@Composable
private fun LiveLoggingWarning(
    open: Boolean,
    onDismissRequest: () -> Unit
) {
    if (open) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 16.dp),
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(R.string.warning)) },
            text = { Text(stringResource(R.string.engine_logging_warning)) },
            confirmButton = {
                Button(onClick = onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun ConfigViewDialog(
    open: Boolean,
    state: PreferencesState,
    onDismissRequest: () -> Unit
) {
    val ctx = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val config = remember(state) {
        try {
            File(ctx.getProfileManager().activeConfigPath).readText()
        } catch (e: FileNotFoundException) {
            "NetBird config not found"
        }
    }

    if (open) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 16.dp),
            onDismissRequest = onDismissRequest,
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SelectionContainer {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(config)
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedCopyButton {
                    clipboardManager.setText(AnnotatedString(config))
                }
            },
            confirmButton = {
                Button(onClick = onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun ExcludeAppsDialog(
    open: Boolean,
    initialPackages: Set<String>,
    invertState: Boolean,
    includeSystemAppsState: Boolean,
    onDismissRequest: () -> Unit,
    onSelected: (
        packageNames: Set<String>,
        invert: Boolean,
        include: Boolean
    ) -> Unit
) {
    val ctx = LocalContext.current

    val packageManager = ctx.packageManager

    var invert by remember { mutableStateOf(invertState) }
    var include by remember { mutableStateOf(includeSystemAppsState) }

    val installedPackages = remember(include) {
        packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .toList()
            .filter { p ->
                ((include || p.flags and ApplicationInfo.FLAG_SYSTEM == 0) && p.packageName != ctx.packageName) ||
                p.packageName == "com.google.android.projection.gearhead" ||
                p.packageName == "com.google.android.apps.messaging" ||
                p.packageName == "com.google.android.gms"
            }
            .sortedBy { it.loadLabel(packageManager).toString() }
    }
    val selectedPackages = remember {
        mutableStateListOf(
            *initialPackages.mapNotNull {
                try {
                    packageManager.getApplicationInfo(it, PackageManager.GET_META_DATA)
                } catch (_: Exception) {
                    null
                }
            }.toTypedArray()
        )
    }

    fun toggleSelected(app: ApplicationInfo) {
        if (!selectedPackages.any { p -> p.packageName == app.packageName }) {
            selectedPackages.add(app)
        } else {
            selectedPackages.removeIf { p -> p.packageName == app.packageName }
        }
    }

    if (open) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 16.dp),
            onDismissRequest = onDismissRequest,
            text = {
                Column {
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        item {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = 0,
                                        count = 2
                                    ),
                                    selected = invert,
                                    onClick = {
                                        invert = true
                                    }
                                ) {
                                    Text(stringResource(R.string.include))
                                }
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = 1,
                                        count = 2
                                    ),
                                    selected = !invert,
                                    onClick = {
                                        invert = false
                                    }
                                ) {
                                    Text(stringResource(R.string.exclude))
                                }
                            }
                        }
                        items(installedPackages) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        toggleSelected(it)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        it.loadIcon(packageManager).toBitmap().asImageBitmap(),
                                        modifier = Modifier.size(36.dp),
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(it.loadLabel(packageManager).toString())
                                }
                                Checkbox(
                                    checked = selectedPackages.any { p -> p.packageName == it.packageName },
                                    onCheckedChange = { _ ->
                                        toggleSelected(it)
                                    }
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.include_system_apps))
                        Switch(
                            checked = include,
                            onCheckedChange = { checked ->
                                include = checked
                            }
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSelected(selectedPackages.map { it.packageName }.toSet(), invert, include)
                    }
                ) {
                    if (invert) {
                        Text(stringResource(R.string.include))
                    } else {
                        Text(stringResource(R.string.exclude))
                    }
                }
            }
        )
    }
}
