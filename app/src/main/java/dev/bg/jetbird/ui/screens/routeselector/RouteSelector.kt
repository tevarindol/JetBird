package dev.bg.jetbird.ui.screens.routeselector

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bg.jetbird.R
import dev.bg.jetbird.ui.components.OutlinedTextFieldInputType
import dev.bg.jetbird.ui.components.SettingsSwitch
import dev.bg.jetbird.ui.components.SettingsTile
import dev.bg.jetbird.ui.components.SwipeToDelete
import dev.bg.jetbird.ui.components.TypedOutlinedTextField

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RouteSelectorScreen(
    viewModel: RouteSelectorViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val calculatedRoutes = viewModel.calculatedRoutes.collectAsStateWithLifecycle().value

    var addRouteOverrideDialog by remember { mutableStateOf(false) }
    var viewRoutesDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.route_selector),
            fontSize = 36.sp
        )
        Text(
            stringResource(R.string.route_selector_info),
            fontSize = 14.sp
        )
        HorizontalDivider()
        SettingsSwitch(
            title = stringResource(R.string.route_override),
            description = stringResource(R.string.route_override_description),
            checked = state.routeOverridesEnabled,
            onChange = viewModel::toggleRouteOverridesEnabled
        )
        SettingsTile(
            title = stringResource(R.string.view_routes),
            description = stringResource(R.string.view_routes_description),
            onClick = { viewRoutesDialog = true }
        )
        HorizontalDivider()
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.routeOverrides.isEmpty()) {
                item {
                    Text(stringResource(R.string.no_route_overrides))
                }
            }
            items(state.routeOverrides.toList()) { route ->
                SwipeToDelete(
                    modifier = Modifier.animateItemPlacement(),
                    onDeleted = {
                        viewModel.removeRoute(route)
                    }
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 1.dp
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = route,
                                fontSize = 24.sp,
                            )
                        }
                    }
                }
            }
        }
        AddRouteOverrideDialog(
            open = addRouteOverrideDialog,
            onDismissRequest = {
                addRouteOverrideDialog = false
            }
        ) { route ->
            viewModel.addRoute(route)
            addRouteOverrideDialog = false
        }
        ViewRoutesDialog(
            open = viewRoutesDialog,
            routes = state.routeOverrides,
            calculatedRoutes = calculatedRoutes,
            onDismissRequest = { viewRoutesDialog = false }
        )
    }
    Box(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = {
                addRouteOverrideDialog = true
            }
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun AddRouteOverrideDialog(
    open: Boolean,
    onDismissRequest: () -> Unit,
    onRoute: (route: String) -> Unit
) {
    var route by remember { mutableStateOf("") }
    var isValidRoute by remember { mutableStateOf(false) }

    LaunchedEffect(route) {
        isValidRoute = Regex("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(/(3[0-2]|[1-2][0-9]|[0-9]))\$").matches(route) ||
                Regex("^s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:)))(%.+)?s*(/(12[0-8]|1[0-1][0-9]|[1-9][0-9]|[0-9]))\$").matches(route)
    }

    if (open) {
        AlertDialog(
            title = {
                Text(stringResource(R.string.add_route))
            },
            text = {
                TypedOutlinedTextField(
                    value = route,
                    onValueChange = {
                        route = it
                    },
                    label = { Text(stringResource(R.string.cidr)) },
                    placeholder = { Text(stringResource(R.string.example_cidr)) },
                    inputType = OutlinedTextFieldInputType.Any,
                    maxLines = 1
                )
            },
            onDismissRequest = onDismissRequest,
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        route = ""
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRoute(route)
                        route = ""
                    },
                    enabled = isValidRoute
                ) {
                    Text(stringResource(R.string.add_route))
                }
            }
        )
    }
}

@Composable
private fun ViewRoutesDialog(
    open: Boolean,
    routes: Set<String>,
    calculatedRoutes: Set<String>,
    onDismissRequest: () -> Unit
) {
    if (open) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 16.dp),
            title = {
                Text(stringResource(R.string.add_route))
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                stringResource(R.string.upstream_routes),
                                fontSize = 16.sp
                            )
                            Text(
                                stringResource(R.string.your_overrides, routes.joinToString()),
                                fontSize = 16.sp
                            )
                            HorizontalDivider()
                        }
                    }
                    items(calculatedRoutes.toList()) { route ->
                        Text(
                            route,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            onDismissRequest = onDismissRequest,
            confirmButton = {
                Button(
                    onClick = onDismissRequest
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
