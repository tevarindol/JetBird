package dev.bg.jetbird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.bg.jetbird.ui.components.CopyLog
import dev.bg.jetbird.ui.components.MonospaceText
import dev.bg.jetbird.ui.components.SaveLog
import dev.bg.jetbird.ui.theme.JetBirdTheme
import dev.bg.jetbird.util.getLogIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream

class ExceptionActivity: ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val stacktrace = intent.getStringExtra("stacktrace").toString()

        setContent {
            JetBirdTheme {
                val ctx = LocalContext.current
                val clipboardManager = LocalClipboardManager.current

                val scope = rememberCoroutineScope()

                val logWriter = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
                    result.data?.data?.let { uri ->
                        scope.launch(Dispatchers.IO) {
                            ctx.contentResolver.openFileDescriptor(uri, "w")?.use { fd ->
                                FileOutputStream(fd.fileDescriptor).use {
                                    it.write(stacktrace.toByteArray())
                                }
                            }
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.log)) },
                            actions = {
                                IconButton(
                                    onClick = {
                                        finishAffinity()
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) { padding ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 32.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            MonospaceText(stacktrace)
                        }
                    }
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.background),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CopyLog {
                                if (stacktrace.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(stacktrace))
                                }
                            }
                            SaveLog {
                                if (stacktrace.isNotEmpty()) {
                                    logWriter.launch(getLogIntent(exception = true))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            finishAffinity()
        }
    }

}
