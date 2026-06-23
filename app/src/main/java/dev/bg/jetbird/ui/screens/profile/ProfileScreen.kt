package dev.bg.jetbird.ui.screens.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bg.jetbird.LocalSnackbar
import dev.bg.jetbird.LocalVPNServiceBinder
import dev.bg.jetbird.R
import dev.bg.jetbird.data.model.Profile
import dev.bg.jetbird.ui.components.DeleteAlertDialog
import dev.bg.jetbird.ui.components.OutlinedTextFieldInputType
import dev.bg.jetbird.ui.components.TypedOutlinedTextField
import dev.bg.jetbird.util.ktx.conditional
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val binder = LocalVPNServiceBinder.current
    val snackbar = LocalSnackbar.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    var addProfileDialog by remember { mutableStateOf(false) }
    var deleteProfileDialog by remember { mutableStateOf<String?>(null) }
    var activeProfileId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // NetBird doesn't make profile list immediately available after adding/deleting
        // Active profile ID isn't always available from ProfileArray
        while (true) {
            viewModel.getProfiles()
            activeProfileId = viewModel.activeProfileId()
            delay(2500L)
        }
    }

    LaunchedEffect(viewModel.errors) {
        viewModel.errors.collectLatest { error ->
            snackbar.showSnackbar(error)
        }
    }

    LazyColumn(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                stringResource(R.string.profiles),
                fontSize = 36.sp,
                fontWeight = FontWeight.W400
            )
        }
        when (state.profiles) {
            null -> item {
                Box(
                    Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth * 0.75f
                    )
                }
            }
            else -> {
                items(state.profiles ?: emptyList()) { profile ->
                    ProfileCard(
                        profile,
                        isActive = profile.id == activeProfileId,
                        onClick = {
                            binder?.stop()
                            viewModel.switchProfile(profile.id)
                            activeProfileId = viewModel.activeProfileId()
                        },
                        onLongClick = {
                            deleteProfileDialog = profile.id
                        }
                    )
                }
            }
        }
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
                addProfileDialog = true
            }
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null
            )
        }
    }
    AddProfileDialog(
        addProfileDialog,
        onDismissRequest = {
            addProfileDialog = false
        },
        onSubmit = viewModel::addProfile
    )
    DeleteAlertDialog(
        title = stringResource(R.string.remove_profile, deleteProfileDialog ?: ""),
        description = stringResource(R.string.remove_profile_description),
        open = deleteProfileDialog != null,
        onDismissRequest = { deleteProfileDialog = null },
        onDelete = {
            if (deleteProfileDialog != null) {
                viewModel.removeProfile(deleteProfileDialog ?: "")
                deleteProfileDialog = null
            }
        }
    )
}

@Composable
private fun ProfileCard(
    profile: Profile,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val c = MaterialTheme.colorScheme.primary

    Card(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .conditional(isActive) {
                border(1.dp, c, RoundedCornerShape(12.dp))
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (profile.id != "default") {
                        onLongClick()
                    }
                }
            )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp)
        ) {
            if (profile.id == "default") {
                Text(stringResource(R.string.default_profile))
            } else {
                Text(profile.name)
            }
            Text(
                profile.id,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun AddProfileDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onSubmit: (name: String, managementUrl: String) -> Unit
) {
    val managementUrlRegex = remember { Regex("^https://.*$") }

    var name by remember { mutableStateOf("") }
    var managementUrl by remember { mutableStateOf("") }

    if (visible) {
        AlertDialog(
            title = {
                Text(stringResource(R.string.add_a_profile))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypedOutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        label = { Text(stringResource(R.string.name)) },
                        inputType = OutlinedTextFieldInputType.Any,
                        maxLines = 1
                    )
                    TypedOutlinedTextField(
                        value = managementUrl,
                        onValueChange = {
                            managementUrl = it
                        },
                        label = { Text(stringResource(R.string.management_url)) },
                        placeholder = { Text("https://api.netbird.io") },
                        inputType = OutlinedTextFieldInputType.Any,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        maxLines = 1
                    )
                }
            },
            onDismissRequest = onDismissRequest,
            dismissButton = {
                OutlinedButton(
                    onClick = onDismissRequest
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotEmpty() && managementUrl.matches(managementUrlRegex),
                    onClick = {
                        onSubmit(name, managementUrl)
                        name = ""
                        managementUrl = ""
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.add))
                }
            }
        )
    }
}
