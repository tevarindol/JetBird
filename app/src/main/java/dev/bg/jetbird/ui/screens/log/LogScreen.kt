package dev.bg.jetbird.ui.screens.log

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bg.jetbird.R
import dev.bg.jetbird.ui.components.MonospaceText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.FileOutputStream

@Composable
fun LogScreen(
    viewModel: LogViewModel = hiltViewModel(),
    logState: StateFlow<List<String>> = viewModel.logEntries
) {
    val ctx = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val scope = rememberCoroutineScope()
    val logEntries = logState.collectAsStateWithLifecycle().value
    val logWriterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        result.data?.data?.let { uri ->
            ctx.contentResolver.openFileDescriptor(uri, "w")?.use { fd ->
                FileOutputStream(fd.fileDescriptor).use {
                    it.write(logEntries.joinToString("\n").toByteArray())
                }
            }
        }
    }
    val netbirdLogWriter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        result.data?.data?.let { uri ->
            scope.launch(Dispatchers.IO) {
                ctx.contentResolver.openFileDescriptor(uri, "w")?.use { fd ->
                    FileOutputStream(fd.fileDescriptor).use {
                        it.write(viewModel.dumpNetbirdLog().joinToString("\n").toByteArray())
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        LazyColumn(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            if (logEntries.isEmpty()) {
                item {
                    MonospaceText(stringResource(R.string.log_empty))
                }
            } else {
                items(logEntries) { log ->
                    SelectionContainer {
                        MonospaceText(log)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedAssistChip(
                onClick = {
                    if (logEntries.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString(logEntries.joinToString("\n")))
                    }
                },
                label = { Text(stringResource(R.string.copy_log)) },
                colors = AssistChipDefaults.elevatedAssistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.baseline_content_copy_24),
                        contentDescription = stringResource(R.string.copy_log),
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            )
            ElevatedAssistChip(
                onClick = {
                    if (logEntries.isNotEmpty()) {
                        logWriterLauncher.launch(getLogIntent())
                    }
                },
                label = { Text(stringResource(R.string.save_log)) },
                colors = AssistChipDefaults.elevatedAssistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.baseline_save_24),
                        contentDescription = stringResource(R.string.save_log),
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            )
            ElevatedAssistChip(
                onClick = { viewModel.clearLog() },
                label = { Text(stringResource(R.string.clear_log)) },
                colors = AssistChipDefaults.elevatedAssistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.copy_log),
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            )
            ElevatedAssistChip(
                onClick = {
                    netbirdLogWriter.launch(getLogIntent())
                },
                label = { Text(stringResource(R.string.dump_netbird_log)) },
                colors = AssistChipDefaults.elevatedAssistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.baseline_save_24),
                        contentDescription = stringResource(R.string.dump_netbird_log),
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

private fun getLogIntent() = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "text/plain"
    putExtra(Intent.EXTRA_TITLE, "${("JetBird").replace("\\s+", "_")}_${System.currentTimeMillis() / 1000}.txt")
}
